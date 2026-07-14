package com.kuki.sabledyncats.massMultipliers;


import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;

public interface BlockMassPropertiesSerializer<T extends BlockMassPropertiesProvider> {

    T loadBlockMassPropertiesFromJson(Block block, JsonObject obj);
    void toNetwork(T properties, FriendlyByteBuf buf);
    T fromNetwork(FriendlyByteBuf buf);

}