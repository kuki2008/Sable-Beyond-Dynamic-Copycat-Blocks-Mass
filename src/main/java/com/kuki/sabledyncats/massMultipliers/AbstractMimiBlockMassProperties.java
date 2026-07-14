package com.kuki.sabledyncats.massMultipliers;

import com.google.gson.JsonObject;
import com.kuki.sabledyncats.SableDynoCats;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class AbstractMimiBlockMassProperties implements BlockMassPropertiesProvider {

    private final MimiBlockMassUnit defaultUnit;
    private final Map<BlockState, MimiBlockMassUnit> unitsByState;

    protected AbstractMimiBlockMassProperties(
            MimiBlockMassUnit defaultUnit,
            Map<BlockState, MimiBlockMassUnit> unitsByState
    ) {
        this.defaultUnit = defaultUnit;
        this.unitsByState = unitsByState;
    }

    @Override
    public double massMultiplier(Level level, BlockState state, BlockPos pos, boolean recurse) {
        MimiBlockMassUnit properties =
                unitsByState.getOrDefault(state, defaultUnit);

        BlockState copiedState = getCopiedState(level, state, pos);
        SableDynoCats.LOGGER.info(
                "AbstractMimicking called recurse={} state={}",
                recurse,
                state
        );

        if (!recurse || isEmptyState(level, copiedState, state, pos))
            return properties.massMultiplier();

        return BlockMassPropertiesHandler
                .getProperties(copiedState)
                .massMultiplier(level, copiedState, pos, false)
                * properties.massMultiplier();
    }

    public int getElementCount(Level level, BlockState state, BlockPos pos) {
        return 1;
    }

    @Override
    public List<BlockState> containedBlockStates(
            Level level,
            BlockState state,
            BlockPos pos,
            boolean recurse
    ) {
        return List.of(getCopiedState(level, state, pos));
    }

    protected abstract BlockState getCopiedState(
            Level level,
            BlockState state,
            BlockPos pos
    );

    protected boolean isEmptyState(
            Level level,
            BlockState copiedState,
            BlockState state,
            BlockPos pos
    ) {
        return copiedState == null || copiedState.isAir();
    }

    public MimiBlockMassUnit getDefaultProperties() {
        return defaultUnit;
    }

    public Map<BlockState, MimiBlockMassUnit> getPropertiesByState() {
        return unitsByState;
    }

    public static <T extends AbstractMimiBlockMassProperties>
    BlockMassPropertiesSerializer<T> createMimiSerializer(Factory<T> factory) {

        return new BlockMassPropertiesSerializer<>() {

            @Override
            public T loadBlockMassPropertiesFromJson(Block block, JsonObject obj) {

                MimiBlockMassUnit defaultProperties =
                        MimiBlockMassUnit.fromJson(obj);

                Map<BlockState, MimiBlockMassUnit> propertiesByState =
                        new Reference2ObjectOpenHashMap<>();

                if (obj.has("variants")
                        && obj.get("variants").isJsonObject()) {

                    propertiesByState.putAll(
                            MimiBlockMassUnit.readAllProperties(
                                    block,
                                    obj.getAsJsonObject("variants")
                            )
                    );
                }

                return factory.apply(defaultProperties, propertiesByState);
            }

            @Override
            public void toNetwork(T properties, FriendlyByteBuf buf) {
                properties.getDefaultProperties().toNetwork(buf);

                MimiBlockMassUnit.writePropertiesToBuf(
                        properties.getPropertiesByState(),
                        buf
                );
            }

            @Override
            public T fromNetwork(FriendlyByteBuf buf) {

                MimiBlockMassUnit defaultProperties =
                        MimiBlockMassUnit.fromNetwork(buf);

                Map<BlockState, MimiBlockMassUnit> propertiesByState =
                        MimiBlockMassUnit.readPropertiesFromBuf(buf);

                return factory.apply(defaultProperties, propertiesByState);
            }
        };
    }

    @FunctionalInterface
    public interface Factory<T extends AbstractMimiBlockMassProperties>
            extends BiFunction<
                        MimiBlockMassUnit,
                        Map<BlockState, MimiBlockMassUnit>,
                        T> {
    }
}