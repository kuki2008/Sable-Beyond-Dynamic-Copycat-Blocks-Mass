package com.kuki.sabledyncats.mixin;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.kuki.sabledyncats.CoolAssMethods.CopycatMassUpdater;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ICopycatBlockEntity.class, remap = false)
public interface ICopycatBlockEntityMixin {
    @Inject(method = "setMaterial", at = @At("TAIL"))
    private void sable$onMaterialChanged(BlockState material, CallbackInfo ci) {
        CopycatMassUpdater.update((BlockEntity)(Object)this, material);
    }
}
