package com.kuki.sabledyncats.massMultipliers;


import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.kuki.sabledyncats.SableDynoCats;
import com.kuki.sabledyncats.network.RootPacket;
import com.kuki.sabledyncats.utils.SDCRegistryUtils;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.Executor;

public class BlockMassPropertiesHandler {
    private static final Map<Block, BlockMassPropertiesProvider> PROPERTIES = new Reference2ObjectOpenHashMap<>();
    
    private static final Map<Block, BlockMassPropertiesSerializer<?>> CUSTOM_SERIALIZERS = new Reference2ReferenceOpenHashMap<>();
    private static final VariantBlockMassProperties FALLBACK_PROPERTIES = new VariantBlockMassProperties(new SimpleBlockMassProperties(1), new Reference2ObjectOpenHashMap<>());

    private static final BlockMassPropertiesProvider FALLBACK_PROVIDER = new BlockMassPropertiesProvider() {
        @Override public double massMultiplier(Level level, BlockState state, BlockPos pos, boolean recurse) { return 1; }
        @Override public List<BlockState> containedBlockStates(Level level, BlockState state, BlockPos pos, boolean recurse) { return Lists.newArrayList(state); }
    };

    public static class BlockReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final BlockReloadListener INSTANCE = new BlockReloadListener();

        public BlockReloadListener() { super(GSON, "copycats_massing"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            SableDynoCats.LOGGER.info("Loading BlockMassProperties...");
            SableDynoCats.LOGGER.info("Entries: {}", map.size());
            cleanUp();

            Set<Block> missingSerializers = new ReferenceOpenHashSet<>(CUSTOM_SERIALIZERS.keySet());
            ResourceKey<Registry<Block>> regKey = SDCRegistryUtils.getBlockRegistryKey();
            HolderLookup.RegistryLookup<Block> reg = this.getRegistryLookup().lookupOrThrow(regKey);

            for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                JsonElement el = entry.getValue();
                if (!el.isJsonObject()) continue;
                try {
                    ResourceLocation loc = entry.getKey();
                    Block block = reg.get(ResourceKey.create(regKey, loc)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown block '" + loc + "'");
                    }).value();
                    if (CUSTOM_SERIALIZERS.containsKey(block)) {
                        PROPERTIES.put(block, CUSTOM_SERIALIZERS.get(block).loadBlockMassPropertiesFromJson(block, el.getAsJsonObject()));
                        missingSerializers.remove(block);
                    } else {
                        PROPERTIES.put(block, VariantBlockMassProperties.fromJson(block, el.getAsJsonObject()));
                    }
                    SableDynoCats.LOGGER.info(
                            "Registered mass provider for {}",
                            BuiltInRegistries.BLOCK.getKey(block)
                    );
                } catch (Exception e) {
                    SableDynoCats.LOGGER.error(
                            "Exception loading mass properties for {}",
                            entry.getKey(),
                            e
                    );
                }
            }
            for (Block missing : missingSerializers)
                SableDynoCats.LOGGER.warn("Missing custom Mass properties entry for block {}", SDCRegistryUtils.getBlockLocation(missing));
        }
    }

//    public static void syncToAll(MinecraftServer server) {
//        NetworkPlatform.sendToClientAll(new ClientboundSyncBlockMassPropertiesPacket(), server);
//    }
//
//    public static void syncTo(ServerPlayer player) {
//        NetworkPlatform.sendToClientPlayer(new ClientboundSyncBlockMassPropertiesPacket(), player);
//    }

    // serializers are permanent
    public static void cleanUp() {
        PROPERTIES.clear();
    }

    public static BlockMassPropertiesProvider getProperties(BlockState state) {
        return getProperties(state.getBlock());
    }

    public static BlockMassPropertiesProvider getProperties(Block block) {
        BlockMassPropertiesProvider provider = PROPERTIES.get(block);

        if (provider == null) {
            SableDynoCats.LOGGER.warn(
                    "No BlockMassPropertiesProvider for {}. Using FALLBACK_PROVIDER.",
                    BuiltInRegistries.BLOCK.getKey(block)
            );
            return FALLBACK_PROVIDER;
        }

        SableDynoCats.LOGGER.info(
                "Using {} for {}",
                provider.getClass().getSimpleName(),
                BuiltInRegistries.BLOCK.getKey(block)
        );

        return provider;
    }

    public static <T extends BlockMassPropertiesSerializer<?>> T registerCustomSerializer(Block block, T ser) {
        if (CUSTOM_SERIALIZERS.containsKey(block)) {
            throw new IllegalStateException("Serializer for block " + SDCRegistryUtils.getBlockLocation(block) + " already registered");
        }
        CUSTOM_SERIALIZERS.put(block, ser);
        return ser;
    }

    public static void writeBuf(RegistryFriendlyByteBuf buf, ClientboundSyncBlockMassPropertiesPacket packet) {
        buf.writeVarInt(packet.blockProperties.size());
        for (Map.Entry<Block, BlockMassPropertiesProvider> entry : packet.blockProperties.entrySet()) {
            buf.writeResourceLocation(SDCRegistryUtils.getBlockLocation(entry.getKey()));
            toNetworkCasted(entry.getKey(), entry.getValue(), buf);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockMassPropertiesProvider> void toNetworkCasted(Block block, T properties, RegistryFriendlyByteBuf buf) {
        BlockMassPropertiesSerializer<T> ser = (BlockMassPropertiesSerializer<T>) CUSTOM_SERIALIZERS.get(block);
        if (ser != null) {
            ser.toNetwork(properties, buf);
        } else if (properties instanceof VariantBlockMassProperties vbap) {
            vbap.toNetwork(buf);
        } else {
            SableDynoCats.LOGGER.warn("Invalid block properties for block {}", block);
            FALLBACK_PROPERTIES.toNetwork(buf);
        }
    }

    public static ClientboundSyncBlockMassPropertiesPacket readBuf(RegistryFriendlyByteBuf buf) {
        Map<Block, BlockMassPropertiesProvider> blockMap = new Reference2ObjectOpenHashMap<>();
        int blockSz = buf.readVarInt();
        for (int i = 0; i < blockSz; ++i) {
            Block block = SDCRegistryUtils.getBlock(buf.readResourceLocation());
            BlockMassPropertiesSerializer<?> ser = CUSTOM_SERIALIZERS.get(block);
            blockMap.put(block, ser == null ? VariantBlockMassProperties.fromNetwork(buf) : ser.fromNetwork(buf));
        }
        return new ClientboundSyncBlockMassPropertiesPacket(blockMap);
    }

    public record ClientboundSyncBlockMassPropertiesPacket(
            Map<Block, BlockMassPropertiesProvider> blockProperties
    ) implements RootPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncBlockMassPropertiesPacket> STREAM_CODEC =
                StreamCodec.of(BlockMassPropertiesHandler::writeBuf, BlockMassPropertiesHandler::readBuf);

        public ClientboundSyncBlockMassPropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(PROPERTIES)); }

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            PROPERTIES.clear();
            PROPERTIES.putAll(this.blockProperties);
        }
    }
}
