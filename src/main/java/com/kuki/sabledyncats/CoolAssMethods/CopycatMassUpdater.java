package com.kuki.sabledyncats.CoolAssMethods;

import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.kuki.sabledyncats.Config;
import com.kuki.sabledyncats.SableDynoCats;
import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesHandler;
import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesProvider;
import com.kuki.sabledyncats.massMultipliers.CopycatElementCounter;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.OptionalDouble;

/**
 * Sable DynoCats 2.0.0 mass arithmetic with current lifecycle fixes.
 *
 * This intentionally uses the 2.0.0 per-material-entry calculation, NOT
 * volume-weighted section counts. The original algorithm is retained because
 * existing vehicle designs rely on its effective masses. Photomancy restores
 * Copycat NBT before calling this calculation through CopycatMassQueue.
 *
 * Only host blocks owned by Copycats+ are updated. The material itself can
 * come from any mod; original Create Copycats are never mass-modified here.
 */
public final class CopycatMassUpdater {
    private static final String COPYCATS_NAMESPACE = "copycats";
    private static final String COPYCAT_BASE_PATH = "copycat_base";

    private static final String INVALID_SUBLEVEL_MASS =
            "Dynamic block mass would make the containing sublevel mass invalid.";

    public enum UpdateResult {
        OK,
        RETRY_LATER
    }

    private CopycatMassUpdater() { }

    /**
     * For the rate-limited queue. Retrying is needed only for the known
     * transient Sable Beyond sublevel-mass rejection; no arbitrary physics
     * exceptions are hidden.
     */
    public static UpdateResult tryUpdate(BlockEntity be, BlockState material) {
        try {
            update(be, material);
            return UpdateResult.OK;
        } catch (IllegalArgumentException invalidMass) {
            if (INVALID_SUBLEVEL_MASS.equals(invalidMass.getMessage())) {
                return UpdateResult.RETRY_LATER;
            }
            throw invalidMass;
        }
    }

    /**
     * Keep the original DynoCats 2.0.0 JVM descriptor (...BlockEntity,
     * BlockState)V for third-party compatibility, including Project CELKA's
     * CelkaSableDynoCatsMovedMassGuardMixin targeting this exact method.
     * The DynamicMass.setBlockMass call deliberately stays IN this method:
     * CELKA wraps that call site in the original method body.
     */
    public static void update(BlockEntity be, BlockState material) {
        if (be == null || be.isRemoved()) {
            return;
        }
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = be.getBlockState();
        if (!CopycatsBlockFilter.isCopycatsPlusBlock(state)) {
            return;
        }

        BlockPos pos = be.getBlockPos();
        BlockMassPropertiesProvider provider = BlockMassPropertiesHandler.getProperties(state);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        // In 2.0.0 these two block types had no JSON mass definitions and
        // therefore used the simple provider, NOT the element counter. Keep
        // this effective legacy behavior even if new JSON files are present.
        // copycat_slab used multiplier=1.0 in 2.0.0; its JSON has been updated
        // to that valid default, and remains customizable through datapacks.
        boolean oldFallback = isLegacyFallbackType(blockId);
        double multiplier = Config.enableMassMultipliers && !oldFallback
                ? provider.massMultiplier(level, state, pos, true)
                : 1.0;

        double totalMass = 0.0;
        if (!oldFallback && provider instanceof CopycatElementCounter counter) {
            int remaining = Math.max(0, counter.getElementCount(level, state, pos));
            if (be instanceof IMultiStateCopycatBlockEntity multiBe
                    && multiBe.getMaterialItemStorage() != null) {
                // Exactly like 2.0.0: one copied material per map entry,
                // independent of the geometric size of its named section.
                Map<String, BlockState> materials = multiBe.getMaterialItemStorage().getMaterialMap();
                for (Map.Entry<String, BlockState> entry : materials.entrySet()) {
                    if (remaining <= 0) {
                        break;
                    }
                    BlockState part = entry.getValue();
                    if (!isFilled(part)) {
                        continue;
                    }
                    totalMass += getMass(part) * multiplier;
                    remaining--;
                }
            } else if (isFilled(material)) {
                totalMass += getMass(material) * multiplier * remaining;
                // 2.0.0 did NOT decrement remaining in this branch.
            }

            if (Config.includeCopycatMass) {
                totalMass += getMass(state) * multiplier * remaining;
            }
        } else {
            // In 2.0.0, a missing special definition used the simple provider.
            // Its setMaterial() callback supplied the *last edited* material.
            // That value is not serialized by Photomancy, so use a stable
            // material stored in the BlockEntity instead, for BOTH the manual
            // edit and blueprint paths. This removes placement-order drift.
            BlockState effectiveMaterial = resolveSimpleMaterial(be, material);
            if (isFilled(effectiveMaterial)) {
                totalMass += getMass(effectiveMaterial) * multiplier;
            }
            if (Config.includeCopycatMass) {
                totalMass += getMass(state) * multiplier;
            }
        }

        if (Double.isFinite(totalMass) && totalMass >= 0.0) {
            // setBlockMass() replaces the absolute override, NOT an additive
            // modifier. Ignore duplicate callbacks (chunk load, onLoad,
            // material changes, Photomancy) before touching the physics body.
            OptionalDouble previous = DynamicMass.getBlockMass(level, pos);
            if (previous.isEmpty() || Double.compare(previous.getAsDouble(), totalMass) != 0) {
                // CELKA 0.4.0.test2 wraps this invocation in the legacy
                // void update(...) method. Keep the invocation here, not in
                // an extracted helper, to preserve its mixin injection site.
                // A rejection that reaches our caller is handled by tryUpdate().
                DynamicMass.setBlockMass(level, pos, totalMass);
            }
        } else {
            SableDynoCats.LOGGER.warn("DynoCats refused non-finite/negative mass {} for {} at {}",
                    totalMass, blockId, pos);
        }
        return;
    }

    private static BlockState resolveSimpleMaterial(BlockEntity be, BlockState material) {
        if (!(be instanceof IMultiStateCopycatBlockEntity multiBe)
                || multiBe.getMaterialItemStorage() == null) {
            return material;
        }
        // Stable across save/load even when getMaterialMap() has a different
        // iteration order. The 2.0.0 formula still counts only ONE material in
        // its simple fallback; this chooses which one reproducibly.
        String chosenKey = null;
        BlockState chosen = null;
        for (Map.Entry<String, BlockState> entry :
                multiBe.getMaterialItemStorage().getMaterialMap().entrySet()) {
            if (isFilled(entry.getValue()) && entry.getKey() != null
                    && (chosenKey == null || entry.getKey().compareTo(chosenKey) < 0)) {
                chosenKey = entry.getKey();
                chosen = entry.getValue();
            }
        }
        return chosen;
    }

    private static boolean isLegacyFallbackType(ResourceLocation id) {
        return COPYCATS_NAMESPACE.equals(id.getNamespace())
                && ("copycat_vertical_half_layer".equals(id.getPath())
                || "copycat_stacked_half_layer".equals(id.getPath()));
    }

    private static boolean isFilled(BlockState candidate) {
        if (candidate == null || candidate.isAir()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(candidate.getBlock());
        return !(COPYCAT_BASE_PATH.equals(id.getPath())
                && ("create".equals(id.getNamespace())
                || COPYCATS_NAMESPACE.equals(id.getNamespace())));
    }

    private static double getMass(BlockState state) {
        return ((BlockStateExtension) state).sable$getProperty(PhysicsBlockPropertyTypes.MASS.get());
    }
}
