package com.kuki.sabledyncats.massMultipliers.differents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Hate this particular stupid ass shit
public interface CopycatElementCounter {
    int getElementCount(Level level, BlockState state, BlockPos pos);
}
