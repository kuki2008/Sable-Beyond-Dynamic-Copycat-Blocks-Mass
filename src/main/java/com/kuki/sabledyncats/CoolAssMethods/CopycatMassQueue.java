package com.kuki.sabledyncats.CoolAssMethods;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.kuki.sabledyncats.SableDynoCats;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Dynamic updates remain event-driven (material changes and Photomancy).
 * Existing ships can be reconciled explicitly with /sabledyncats mass recalc_loaded.
 * No global chunk-load scans and no continuous polling.
 */
public final class CopycatMassQueue {
    private static final int MAX_BLOCKS_PER_TICK = 256;
    private static final int MAX_SCAN_POSITIONS_PER_TICK = 256;
    private static final int MAX_SCAN_CHUNKS_PER_TICK = 4;
    private static final int MAX_EMPTY_PLOTS_PER_TICK = 16;
    // Only populated when Sable Beyond rejects an update due to an
    // inconsistent/not-yet-ready sublevel mass. No global polling.
    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final int MAX_RETRIES_PER_TICK = 32;
    private static final int MAX_RETRY_CHECKS_PER_TICK = 128;
    private static final int[] RETRY_DELAYS_TICKS = {5, 20, 60, 200};

    private static final Map<ServerLevel, Set<Long>> pendingBlocks = new IdentityHashMap<>();
    private static final Map<ServerLevel, Map<Long, DeferredRetry>> retryLookup = new IdentityHashMap<>();
    private static final ArrayDeque<DeferredRetry> delayedRetries = new ArrayDeque<>();
    private static long serverTick;
    private static int activeRetries;
    private static ManualScan manualScan;
    private static String lastManualSummary = "No manual mass reconciliation has been run";

    private CopycatMassQueue() { }

    public static void schedule(BlockEntity be) {
        if (be == null || be.isRemoved()
                || !CopycatsBlockFilter.isCopycatsPlusBlock(be.getBlockState())) {
            return;
        }
        Level level = be.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            schedule(serverLevel, be.getBlockPos());
        }
    }

    public static void schedule(ServerLevel level, BlockPos pos) {
        if (level != null && pos != null) {
            pendingBlocks.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.asLong());
        }
    }

    /**
     * Called by a permission-gated server command. The scan only touches
     * currently loaded Sable sublevels and their already-loaded plot chunks.
     * Returns -1 if one is already running, otherwise the number of ships.
     */
    public static int startManualRecalculation(MinecraftServer server) {
        if (manualScan != null) {
            return -1;
        }
        ManualScan scan = new ManualScan(server);
        for (ServerLevel level : server.getAllLevels()) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }
            for (SubLevel candidate : container.getAllSubLevels()) {
                if (candidate instanceof ServerSubLevel subLevel && !subLevel.isRemoved()) {
                    // A snapshot prevents concurrent changes to Sable's loaded-chunk list
                    // from invalidating an iterator on a later server tick.
                    scan.plots.addLast(new PlotScan(subLevel,
                            new ArrayList<>(subLevel.getPlot().getLoadedChunks()).iterator()));
                    scan.shipCount++;
                }
            }
        }
        if (scan.shipCount > 0) {
            manualScan = scan;
            lastManualSummary = "Mass reconciliation started for " + scan.shipCount + " loaded ships";
        }
        return scan.shipCount;
    }

    public static String manualStatus() {
        if (manualScan == null) {
            return lastManualSummary;
        }
        return "Recalculating loaded Sable ships: " + manualScan.shipCount
                + " ships, " + manualScan.chunksVisited + " chunks scanned, "
                + manualScan.positionsVisited + " BlockEntities examined, "
                + manualScan.copycatsQueued + " Copycats+ queued; "
                + pendingCount() + " pending mass updates, " + activeRetries + " deferred retries";
    }

    private static int pendingCount() {
        int total = 0;
        for (Set<Long> positions : pendingBlocks.values()) {
            total += positions.size();
        }
        return total;
    }

    @SubscribeEvent
    public static void afterServerTick(ServerTickEvent.Post event) {
        serverTick++;
        // Ordinary changes have priority over rare failed physics updates.
        processPendingBlocks(event.getServer());
        processDeferredRetries(event.getServer());
        if (manualScan != null && manualScan.server == event.getServer()) {
            scanSomeLoadedShips(manualScan);
            if (manualScan.plots.isEmpty() && pendingCount() == 0 && activeRetries == 0) {
                lastManualSummary = "Mass reconciliation complete: " + manualScan.shipCount
                        + " loaded ships, " + manualScan.chunksVisited + " plot chunks, "
                        + manualScan.positionsVisited + " BlockEntities checked, "
                        + manualScan.copycatsQueued + " Copycats+ inspected";
                SableDynoCats.LOGGER.info(lastManualSummary);
                manualScan = null;
            }
        }
    }

    private static void processPendingBlocks(MinecraftServer server) {
        int budget = MAX_BLOCKS_PER_TICK;
        for (Iterator<Map.Entry<ServerLevel, Set<Long>>> levels = pendingBlocks.entrySet().iterator();
             levels.hasNext() && budget > 0;) {
            Map.Entry<ServerLevel, Set<Long>> entry = levels.next();
            ServerLevel level = entry.getKey();
            if (level.getServer() != server) {
                levels.remove();
                continue;
            }
            for (Iterator<Long> positions = entry.getValue().iterator();
                 positions.hasNext() && budget > 0;) {
                long key = positions.next();
                positions.remove();
                budget--;
                // A fresh event supersedes any pending retry for the same block.
                cancelRetry(level, key);
                if (runUpdate(level, key) == CopycatMassUpdater.UpdateResult.RETRY_LATER) {
                    defer(level, key, 0);
                }
            }
            if (entry.getValue().isEmpty()) {
                levels.remove();
            }
        }
    }

    private static CopycatMassUpdater.UpdateResult runUpdate(ServerLevel level, long key) {
        BlockPos pos = BlockPos.of(key);
        // Do not force-load chunks, including Sable plot chunks.
        if (!level.hasChunkAt(pos)) {
            return CopycatMassUpdater.UpdateResult.OK;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.isRemoved()
                || !CopycatsBlockFilter.isCopycatsPlusBlock(be.getBlockState())) {
            return CopycatMassUpdater.UpdateResult.OK;
        }
        if (be instanceof IMultiStateCopycatBlockEntity) {
            return CopycatMassUpdater.tryUpdate(be, null);
        }
        if (be instanceof ICopycatBlockEntity copycat) {
            return CopycatMassUpdater.tryUpdate(be, copycat.getMaterial());
        }
        return CopycatMassUpdater.UpdateResult.OK;
    }

    private static void defer(ServerLevel level, long key, int previousAttempts) {
        if (previousAttempts >= MAX_RETRY_ATTEMPTS) {
            SableDynoCats.LOGGER.warn(
                    "DynoCats skipped dynamic mass update after {} retries: dimension={}, pos={}. "
                    + "The Sable sublevel mass is inconsistent; check this ship and retry with "
                    + "/sabledyncats mass recalc_loaded after physics initialization.",
                    previousAttempts, level.dimension().location(), BlockPos.of(key));
            return;
        }
        Map<Long, DeferredRetry> sameLevel =
                retryLookup.computeIfAbsent(level, ignored -> new HashMap<>());
        if (sameLevel.containsKey(key)) {
            return;
        }
        int attempts = previousAttempts + 1;
        DeferredRetry retry = new DeferredRetry(level, key, attempts,
                serverTick + RETRY_DELAYS_TICKS[previousAttempts]);
        sameLevel.put(key, retry);
        delayedRetries.addLast(retry);
        activeRetries++;
    }

    private static void cancelRetry(ServerLevel level, long key) {
        Map<Long, DeferredRetry> byKey = retryLookup.get(level);
        if (byKey == null) {
            return;
        }
        DeferredRetry existing = byKey.remove(key);
        if (existing != null) {
            existing.cancelled = true;
            activeRetries--;
        }
        if (byKey.isEmpty()) {
            retryLookup.remove(level);
        }
    }

    private static void processDeferredRetries(MinecraftServer server) {
        int checks = Math.min(MAX_RETRY_CHECKS_PER_TICK, delayedRetries.size());
        int attemptsBudget = MAX_RETRIES_PER_TICK;
        while (checks-- > 0 && attemptsBudget > 0) {
            DeferredRetry retry = delayedRetries.removeFirst();
            if (retry.cancelled) {
                continue;
            }
            if (retry.level.getServer() != server) {
                cancelRetry(retry.level, retry.key);
                continue;
            }
            if (serverTick < retry.dueTick) {
                delayedRetries.addLast(retry);
                continue;
            }
            attemptsBudget--;
            cancelRetry(retry.level, retry.key);
            CopycatMassUpdater.UpdateResult result = runUpdate(retry.level, retry.key);
            if (result == CopycatMassUpdater.UpdateResult.RETRY_LATER) {
                defer(retry.level, retry.key, retry.attempts);
            }
        }
    }

    private static void scanSomeLoadedShips(ManualScan scan) {
        int positionsLeft = MAX_SCAN_POSITIONS_PER_TICK;
        int chunksLeft = MAX_SCAN_CHUNKS_PER_TICK;
        int emptyPlotsLeft = MAX_EMPTY_PLOTS_PER_TICK;
        while (positionsLeft > 0 && !scan.plots.isEmpty()) {
            PlotScan plot = scan.plots.peekFirst();
            if (plot.subLevel.isRemoved()) {
                scan.plots.removeFirst();
                if (--emptyPlotsLeft == 0) break;
                continue;
            }
            if (plot.positions == null || !plot.positions.hasNext()) {
                plot.positions = null;
                // Even a million completely empty chunks cannot stall one tick.
                if (chunksLeft == 0) {
                    break;
                }
                if (!plot.chunks.hasNext()) {
                    scan.plots.removeFirst();
                    if (--emptyPlotsLeft == 0) break;
                    continue;
                }
                chunksLeft--;
                PlotChunkHolder holder = plot.chunks.next();
                LevelChunk chunk = holder.getChunk();
                scan.chunksVisited++;
                if (chunk == null) {
                    continue;
                }
                plot.chunk = chunk;
                plot.positions = chunk.getBlockEntitiesPos().iterator();
                continue;
            }
            BlockPos pos = plot.positions.next();
            positionsLeft--;
            scan.positionsVisited++;
            if (CopycatsBlockFilter.isCopycatsPlusBlock(plot.chunk.getBlockState(pos))) {
                // HashSet de-duplicates against normal material/Photomancy events.
                schedule(plot.subLevel.getLevel(), pos);
                scan.copycatsQueued++;
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        pendingBlocks.clear();
        retryLookup.clear();
        delayedRetries.clear();
        activeRetries = 0;
        serverTick = 0;
        manualScan = null;
        lastManualSummary = "No manual mass reconciliation has been run";
    }

    private static final class DeferredRetry {
        final ServerLevel level;
        final long key;
        final int attempts;
        final long dueTick;
        boolean cancelled;
        DeferredRetry(ServerLevel level, long key, int attempts, long dueTick) {
            this.level = level;
            this.key = key;
            this.attempts = attempts;
            this.dueTick = dueTick;
        }
    }

    private static final class ManualScan {
        final MinecraftServer server;
        final ArrayDeque<PlotScan> plots = new ArrayDeque<>();
        int shipCount;
        long chunksVisited;
        long positionsVisited;
        long copycatsQueued;
        ManualScan(MinecraftServer server) { this.server = server; }
    }

    private static final class PlotScan {
        final ServerSubLevel subLevel;
        final Iterator<PlotChunkHolder> chunks;
        Iterator<BlockPos> positions;
        LevelChunk chunk;
        PlotScan(ServerSubLevel subLevel, Iterator<PlotChunkHolder> chunks) {
            this.subLevel = subLevel;
            this.chunks = chunks;
        }
    }
}
