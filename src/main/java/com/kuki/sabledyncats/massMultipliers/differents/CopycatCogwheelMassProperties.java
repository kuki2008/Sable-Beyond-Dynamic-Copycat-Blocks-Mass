package com.kuki.sabledyncats.massMultipliers.differents;

import com.kuki.sabledyncats.massMultipliers.AbstractMimiBlockMassProperties;
import com.kuki.sabledyncats.massMultipliers.CopycatElementCounter;
import com.kuki.sabledyncats.massMultipliers.MimiBlockMassUnit;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

public class CopycatCogwheelMassProperties
        extends AbstractMimiBlockMassProperties
        implements CopycatElementCounter {

    private final BooleanProperty[] properties;

    public CopycatCogwheelMassProperties(
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

    // Why
    @Override
    public int getElementCount(Level level, BlockState state, BlockPos pos) {
        return 2;
    }

    public static Factory<CopycatCogwheelMassProperties> of(
            BooleanProperty... properties
    ) {
        return (defaultUnit, unitsByState) ->
                new CopycatCogwheelMassProperties(
                        defaultUnit,
                        unitsByState,
                        properties
                );
    }
}