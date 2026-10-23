package com.kuki.sabledyncats.mixin;

import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.kuki.sabledyncats.CoolAssMethods.CopycatMassQueue;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IMultiStateCopycatBlockEntity.class, remap = false)
public interface IMultiStateCopycatBlockEntityMixin {
    @Inject(method = "setMaterial", at = @At("TAIL"))
    private void sable$onMaterialChanged(String property, BlockState material, CallbackInfo ci) {
        CopycatMassQueue.schedule((BlockEntity)(Object)this);
    }
}