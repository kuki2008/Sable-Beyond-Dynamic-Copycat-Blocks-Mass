package com.kuki.sabledyncats.massMultipliers.differents;

import com.copycatsplus.copycats.content.copycat.slab.CopycatSlabBlock;
import com.kuki.sabledyncats.massMultipliers.AbstractMimiBlockMassProperties;
import com.kuki.sabledyncats.massMultipliers.CopycatElementCounter;
import com.kuki.sabledyncats.massMultipliers.MimiBlockMassUnit;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Map;

public class CopycatSlabMassProperties
        extends AbstractMimiBlockMassProperties
        implements CopycatElementCounter {

    private final BooleanProperty[] properties;

    public CopycatSlabMassProperties(
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
        return state.getValue(CopycatSlabBlock.SLAB_TYPE) == SlabType.DOUBLE
                ? 2
                : 1;
    }

    public static Factory<CopycatSlabMassProperties> of(
            BooleanProperty... properties
    ) {
        return (defaultUnit, unitsByState) ->
                new CopycatSlabMassProperties(
                        defaultUnit,
                        unitsByState,
                        properties
                );
    }
}