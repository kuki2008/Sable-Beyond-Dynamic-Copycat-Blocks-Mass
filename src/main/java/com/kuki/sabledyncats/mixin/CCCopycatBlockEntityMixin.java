package com.kuki.sabledyncats.mixin;

import com.copycatsplus.copycats.foundation.copycat.CCCopycatBlockEntity;
import com.kuki.sabledyncats.CoolAssMethods.CopycatMassUpdater;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CCCopycatBlockEntity.class)
public abstract class CCCopycatBlockEntityMixin {
    @Shadow public abstract BlockState getMaterial();

    @Inject(method = "setBlockState(Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("TAIL"))
    private void sable$onMaterialChanged(BlockState blockState, CallbackInfo ci) {
        CopycatMassUpdater.update((BlockEntity)(Object)this, (BlockState)(Object)this.getMaterial());
    }

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void sable$onMaterialChanged(CallbackInfo ci) {
        CopycatMassUpdater.update((BlockEntity)(Object)this, (BlockState)(Object)this.getMaterial());
    }
}
