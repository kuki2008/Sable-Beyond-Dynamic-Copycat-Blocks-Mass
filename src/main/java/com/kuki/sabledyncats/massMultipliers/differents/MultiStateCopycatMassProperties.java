package com.kuki.sabledyncats.massMultipliers.differents;

import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kuki.sabledyncats.SableDynoCats;
import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesHandler;
import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesProvider;
import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesSerializer;
import com.kuki.sabledyncats.massMultipliers.MimiBlockMassUnit;
import com.simibubi.create.AllBlocks;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;


public abstract class MultiStateCopycatMassProperties
        implements BlockMassPropertiesProvider {

    private final MimiBlockMassUnit defaultTotalMultiplier;
    private final Map<Integer, MimiBlockMassUnit> totalMultiplierByCount;

    protected MultiStateCopycatMassProperties(
            MimiBlockMassUnit defaultTotalMultiplier,
            Map<Integer, MimiBlockMassUnit> totalMultiplierByCount) {
        this.defaultTotalMultiplier = defaultTotalMultiplier;
        this.totalMultiplierByCount = totalMultiplierByCount;
    }

    @Override
    public double massMultiplier(Level level, BlockState state, BlockPos pos, boolean recurse) {
        if (!recurse)
            return 1.0;

        int count = Math.max(getElementCount(level, state, pos), 1);

        return totalMultiplierByCount
                .getOrDefault(count, defaultTotalMultiplier)
                .massMultiplier();
    }

    @Override
    public List<BlockState> containedBlockStates(
            Level level,
            BlockState state,
            BlockPos pos,
            boolean recurse
    ) {
        if (!recurse
                || !(level.getBlockEntity(pos) instanceof IMultiStateCopycatBlockEntity msbe))
            return List.of();

        return new ArrayList<>(
                msbe.getMaterialItemStorage()
                        .getMaterialMap()
                        .values()
        );
    }

    public abstract int getElementCount(Level level, BlockState state, BlockPos pos);

    public MimiBlockMassUnit getDefaultTotalMultiplier() {
        return defaultTotalMultiplier;
    }

    public Map<Integer, MimiBlockMassUnit> getTotalMultiplierByCount() {
        return totalMultiplierByCount;
    }

    public static <T extends MultiStateCopycatMassProperties> BlockMassPropertiesSerializer<T> createMultistateSerializer(Factory<T> fac) {
        return new BlockMassPropertiesSerializer<>() {
            @Override
            public T loadBlockMassPropertiesFromJson(Block block, JsonObject obj) {
                MimiBlockMassUnit defaultProperties = MimiBlockMassUnit.fromJson(obj);

                Map<Integer, MimiBlockMassUnit> propertiesByCount = new Int2ObjectOpenHashMap<>();

                if (obj.has("variants") && obj.get("variants").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("variants").entrySet()) {
                        int count;

                        try {
                            count = Integer.parseInt(entry.getKey());
                        } catch (NumberFormatException e) {
                            SableDynoCats.LOGGER.warn(
                                    "Multistate copycat variant for block {} must be an integer (was {})",
                                    block,
                                    entry.getKey());
                            continue;
                        }

                        if (!entry.getValue().isJsonObject()) {
                            SableDynoCats.LOGGER.warn(
                                    "Multistate copycat variant for block {} must be specified by a JSON object (variant {})",
                                    block,
                                    entry.getKey());
                            continue;
                        }

                        propertiesByCount.put(
                                count,
                                MimiBlockMassUnit.fromJson(entry.getValue().getAsJsonObject())
                        );
                    }
                }

                return fac.apply(defaultProperties, propertiesByCount);
            }

            @Override
            public void toNetwork(T properties, FriendlyByteBuf buf) {
                properties.getDefaultTotalMultiplier().toNetwork(buf);

                Map<Integer, MimiBlockMassUnit> multipliers = properties.getTotalMultiplierByCount();

                buf.writeVarInt(multipliers.size());

                for (Map.Entry<Integer, MimiBlockMassUnit> entry : multipliers.entrySet()) {
                    buf.writeVarInt(entry.getKey());
                    entry.getValue().toNetwork(buf);
                }
            }

            @Override
            public T fromNetwork(FriendlyByteBuf buf) {
                MimiBlockMassUnit defaultProperties = MimiBlockMassUnit.fromNetwork(buf);

                Map<Integer, MimiBlockMassUnit> propertiesByCount = new Int2ObjectOpenHashMap<>();

                int size = buf.readVarInt();

                for (int i = 0; i < size; i++) {
                    propertiesByCount.put(
                            buf.readVarInt(),
                            MimiBlockMassUnit.fromNetwork(buf)
                    );
                }

                return fac.apply(defaultProperties, propertiesByCount);
            }
        };
    }

    @FunctionalInterface
    public interface Factory<T extends MultiStateCopycatMassProperties>
            extends BiFunction<MimiBlockMassUnit, Map<Integer, MimiBlockMassUnit>, T> {
    }
}