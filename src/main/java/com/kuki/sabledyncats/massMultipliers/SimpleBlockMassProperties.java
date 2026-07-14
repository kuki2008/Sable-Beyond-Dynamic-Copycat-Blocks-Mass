package com.kuki.sabledyncats.massMultipliers;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record SimpleBlockMassProperties(double massMultiplier) implements BlockMassPropertiesProvider {

    @Override
    public double massMultiplier(Level level, BlockState state, BlockPos pos, boolean recurse) {
        return this.massMultiplier;
    }

    @Override
    public List<BlockState> containedBlockStates(Level level, BlockState state, BlockPos pos, boolean recurse) {
        return List.of(state);
    }

    public static SimpleBlockMassProperties fromJson(JsonObject obj) {
        double massMultiplier = Math.max(GsonHelper.getAsDouble(obj, "mass_multiplier", 1), 0);
        return new SimpleBlockMassProperties(massMultiplier);
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(this.massMultiplier);
    }

    public static SimpleBlockMassProperties fromNetwork(FriendlyByteBuf buf) {
        double massMultiplier = buf.readDouble();
        return new SimpleBlockMassProperties(massMultiplier);
    }

}