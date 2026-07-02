package com.kuki.sabledyncats.mixin;

import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IMultiStateCopycatBlockEntity.class, remap = false)
public interface IMultiStateCopycatBlockEntityMixin {

    @Inject(method = "setMaterial", at = @At("TAIL"))
    private void sable$onMaterialChanged(String property, BlockState blockState, CallbackInfo ci) {
        final Logger LOGGER = LogUtils.getLogger();
//        LOGGER.info("[SableDynCats]");

        BlockEntity be = (BlockEntity)(Object)this;

        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();

        if (level == null || level.isClientSide())
            return;

        double mass = ((BlockStateExtension) blockState)
                .sable$getProperty(PhysicsBlockPropertyTypes.MASS.get());

        DynamicMass.setBlockMass(level, pos, mass);
//        LOGGER.info("1. setMaterial");
//
//        LOGGER.info("2. level={}", level);
//
//        LOGGER.info("3. pos={}", pos);
//
//        LOGGER.info("4. state={}", blockState);
//
//        LOGGER.info("5. mass={}", mass);
//
//        DynamicMass.setBlockMass(level, pos, mass);
//
//        LOGGER.info("6. stored={}",
//                DynamicMass.getBlockMass(level, pos));
    }
}