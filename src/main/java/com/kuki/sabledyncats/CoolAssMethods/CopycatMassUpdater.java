package com.kuki.sabledyncats.CoolAssMethods;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public final class CopycatMassUpdater {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation COPYCAT_BASE_ID =
            ResourceLocation.parse("create:copycat_base");
    public static void update(BlockEntity be, BlockState material) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();

        if (level == null || level.isClientSide())
            return;

        double mass;
        if (BuiltInRegistries.BLOCK.getKey(material.getBlock()).equals(COPYCAT_BASE_ID)) {
            mass = ((BlockStateExtension) be.getBlockState())
                    .sable$getProperty(PhysicsBlockPropertyTypes.MASS.get());
        } else {
            mass = ((BlockStateExtension) material)
                    .sable$getProperty(PhysicsBlockPropertyTypes.MASS.get());
        }

        DynamicMass.setBlockMass(level, pos, mass);
    }
}
