package com.kuki.sabledyncats.CoolAssMethods;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Restrict mass overrides to known Copycats+ blocks and the two native
 * Create copycats. Other Create blocks (including copycat_base) are untouched.
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

    public static boolean isCreateCopycatBlock(BlockState state) {
        if (state == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "create".equals(id.getNamespace())
                && ("copycat_panel".equals(id.getPath()) || "copycat_step".equals(id.getPath()));
    }

    public static boolean isSupportedCopycatBlock(BlockState state) {
        if (state == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if ("copycats".equals(id.getNamespace())) {
            return id.getPath().startsWith("copycat_");
        }
        return "create".equals(id.getNamespace())
                && ("copycat_panel".equals(id.getPath()) || "copycat_step".equals(id.getPath()));
    }
}
