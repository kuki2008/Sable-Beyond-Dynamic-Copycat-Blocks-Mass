package com.kuki.sabledyncats.massMultipliers.differents;

import com.copycatsplus.copycats.content.copycat.half_layer.CopycatHalfLayerBlock;
import com.kuki.sabledyncats.massMultipliers.AbstractMimiBlockMassProperties;
import com.kuki.sabledyncats.massMultipliers.CopycatElementCounter;
import com.kuki.sabledyncats.massMultipliers.MimiBlockMassUnit;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

public class CopycatHalfLayeredMassProperties
        extends AbstractMimiBlockMassProperties
        implements CopycatElementCounter {

    private final BooleanProperty[] properties;

    public CopycatHalfLayeredMassProperties(
            MimiBlockMassUnit defaultUnit,
            Map<BlockState, MimiBlockMassUnit> unitsByState,
            BooleanProperty... properties
    ) {
        super(defaultUnit, unitsByState);
        this.properties = properties;
    }

    @Override
    protected BlockState getCopiedState(
            Level level,
            BlockState state,
            BlockPos pos
    ) {
        return CopycatBlock.getMaterial(level, pos);
    }

    @Override
    public int getElementCount(Level level, BlockState state, BlockPos pos) {
        return Math.max(
                state.getValue(CopycatHalfLayerBlock.POSITIVE_LAYERS)
                        + state.getValue(CopycatHalfLayerBlock.NEGATIVE_LAYERS),
                1
        );
    }

    public static Factory<CopycatHalfLayeredMassProperties> of(
            BooleanProperty... properties
    ) {
        return (defaultUnit, unitsByState) ->
                new CopycatHalfLayeredMassProperties(
                        defaultUnit,
                        unitsByState,
                        properties
                );
    }
}
