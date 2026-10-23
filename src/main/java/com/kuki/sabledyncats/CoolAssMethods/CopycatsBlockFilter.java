package com.kuki.sabledyncats.CoolAssMethods;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Restrict dynamic-mass changes to blocks provided by Copycats+.
 * Copycat blocks provided by Create are intentionally not modified.
 * This checks the host block, not the material copied into it.
 */
public final class CopycatsBlockFilter {
    private CopycatsBlockFilter() { }

    public static boolean isCopycatsPlusBlock(BlockState state) {
        if (state == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "copycats".equals(id.getNamespace()) && id.getPath().startsWith("copycat_");
    }
}
