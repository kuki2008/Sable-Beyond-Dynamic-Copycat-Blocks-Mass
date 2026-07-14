package com.kuki.sabledyncats.massMultipliers.differents;

import com.kuki.sabledyncats.SableDynoCats;
import com.kuki.sabledyncats.massMultipliers.AbstractMimiBlockMassProperties;
import com.kuki.sabledyncats.massMultipliers.CopycatElementCounter;
import com.kuki.sabledyncats.massMultipliers.MimiBlockMassUnit;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

public class BinaryCopycatMassProperties
        extends AbstractMimiBlockMassProperties
        implements CopycatElementCounter {

    private final BooleanProperty[] properties;

    public BinaryCopycatMassProperties(
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
    public int getElementCount(
            Level level,
            BlockState state,
            BlockPos pos
    ) {
        int count = 0;

        for (BooleanProperty property : properties) {
            if (state.getOptionalValue(property).orElse(false))
                count++;
        }

        return Math.max(count, 1);
    }

    public static Factory<BinaryCopycatMassProperties> of(
            BooleanProperty... properties
    ) {
        return (defaultUnit, unitsByState) ->
                new BinaryCopycatMassProperties(
                        defaultUnit,
                        unitsByState,
                        properties
                );
    }
}