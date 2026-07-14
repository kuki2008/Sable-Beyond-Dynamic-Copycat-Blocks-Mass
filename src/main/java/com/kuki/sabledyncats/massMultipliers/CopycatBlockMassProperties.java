package com.kuki.sabledyncats.massMultipliers;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class CopycatBlockMassProperties
        extends AbstractMimiBlockMassProperties {
    private static final ResourceLocation COPYCAT_BASE_ID =
            ResourceLocation.parse("create:copycat_base");

    public CopycatBlockMassProperties(
            MimiBlockMassUnit defaultUnit,
            Map<BlockState, MimiBlockMassUnit> unitsByState
    ) {
        super(defaultUnit, unitsByState);
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
    protected boolean isEmptyState(
            Level level,
            BlockState copiedState,
            BlockState state,
            BlockPos pos
    ) {
        return copiedState.getBlock() == BuiltInRegistries.BLOCK.get(COPYCAT_BASE_ID);
    }
}