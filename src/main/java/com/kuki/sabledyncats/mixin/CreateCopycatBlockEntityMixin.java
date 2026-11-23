package com.kuki.sabledyncats.mixin;

import com.kuki.sabledyncats.CoolAssMethods.CopycatMassQueue;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create's CopycatBlockEntity is not the Copycats+ CCCopycatBlockEntity.
 * All hooks are server-side, deduplicated in the normal mass queue.
 */
@Mixin(value = CopycatBlockEntity.class, remap = false)
public abstract class CreateCopycatBlockEntityMixin {
    @Inject(method = "setMaterial", at = @At("TAIL"))
    private void sable$onMaterialChanged(BlockState material, CallbackInfo ci) {
        CopycatMassQueue.schedule((BlockEntity) (Object) this);
    }

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void sable$onLoaded(CallbackInfo ci) {
        CopycatMassQueue.schedule((BlockEntity) (Object) this);
    }

    @Inject(method = "transform", at = @At("TAIL"))
    private void sable$onTransformed(BlockEntity target, StructureTransform transform, CallbackInfo ci) {
        CopycatMassQueue.schedule((BlockEntity) (Object) this);
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void sable$onCleared(CallbackInfo ci) {
        CopycatMassQueue.schedule((BlockEntity) (Object) this);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void sable$onRead(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket) {
            CopycatMassQueue.schedule((BlockEntity) (Object) this);
        }
    }
}
