package com.kuki.sabledyncats.massMultipliers;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public interface BlockMassPropertiesProvider {

    double massMultiplier(Level level, BlockState state, BlockPos pos, boolean recurse);

    default List<BlockState> containedBlockStates(
            Level level,
            BlockState state,
            BlockPos pos,
            boolean recurse
    ) {
        return List.of(state);
    }
}