package com.kuki.sabledyncats.mixin;

import com.kuki.sabledyncats.CoolAssMethods.CopycatMassQueue;
import com.kuki.sabledyncats.CoolAssMethods.CopycatsBlockFilter;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A second, independent notification path for vanilla Create's panel/step.
 *
 * Sable's ship interaction may use a transient BlockEntity reference while
 * applying a material. The CopycatBlockEntity#setMaterial hook can therefore
 * fire before its Level has the authoritative copy. The block interaction
 * provides the actual Level/BlockPos used by Create and is queued after the
 * interaction finishes, so the next server tick reads the authoritative BE.
 *
 * Deliberately does not intercept arbitrary Create blocks or Copycats+.
 */
@Mixin(value = CopycatBlock.class, remap = false)
public abstract class CreateCopycatInteractionMixin {
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void sable$afterMaterialInteraction(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!level.isClientSide() && CopycatsBlockFilter.isCreateCopycatBlock(state)
                && cir.getReturnValue() == ItemInteractionResult.SUCCESS) {
            CopycatMassQueue.scheduleCreateInteraction(level, pos);
        }
    }

    @Inject(method = "onWrenched", at = @At("RETURN"))
    private void sable$afterMaterialRemoved(BlockState state,
            net.minecraft.world.item.context.UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.SUCCESS
                && CopycatsBlockFilter.isCreateCopycatBlock(state)) {
            CopycatMassQueue.scheduleCreateInteraction(context.getLevel(), context.getClickedPos());
        }
    }

    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void sable$afterPlaced(Level level, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (!level.isClientSide() && CopycatsBlockFilter.isCreateCopycatBlock(state)) {
            CopycatMassQueue.scheduleCreateInteraction(level, pos);
        }
    }
}
