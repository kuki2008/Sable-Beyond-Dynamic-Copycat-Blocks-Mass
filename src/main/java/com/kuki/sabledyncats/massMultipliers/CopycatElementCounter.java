package com.kuki.sabledyncats.massMultipliers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CopycatElementCounter {
    int getElementCount(
            Level level,
            BlockState state,
            BlockPos pos
    );
}
