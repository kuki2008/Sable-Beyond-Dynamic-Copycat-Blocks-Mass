package com.kuki.sabledyncats.massMultipliers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kuki.sabledyncats.CoolAssMethods.BlockStatePredicateHelper;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public record MimiBlockMassUnit(double massMultiplier) {

    public static MimiBlockMassUnit fromJson(JsonObject obj) {
        return new MimiBlockMassUnit(
                Math.max(
                        GsonHelper.getAsDouble(obj, "mass_multiplier", 1d),
                        0d
                )
        );
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(this.massMultiplier);
    }

    public static MimiBlockMassUnit fromNetwork(FriendlyByteBuf buf) {
        return new MimiBlockMassUnit(buf.readDouble());
    }

    public static Map<BlockState, MimiBlockMassUnit> readAllProperties(
            Block block,
            JsonObject obj
    ) {
        StateDefinition<Block, BlockState> definition = block.getStateDefinition();

        Set<BlockState> states = new HashSet<>(definition.getPossibleStates());

        Map<BlockState, MimiBlockMassUnit> propertiesByState =
                new Reference2ObjectOpenHashMap<>();

        for (String key : obj.keySet()) {

            Predicate<BlockState> predicate =
                    BlockStatePredicateHelper.variantPredicate(definition, key);

            JsonElement element = obj.get(key);

            if (!element.isJsonObject()) {
                throw new JsonSyntaxException(
                        "Invalid info for variant '" + key + "'"
                );
            }

            MimiBlockMassUnit properties =
                    MimiBlockMassUnit.fromJson(element.getAsJsonObject());

            for (Iterator<BlockState> iterator = states.iterator(); iterator.hasNext();) {

                BlockState state = iterator.next();

                if (predicate.test(state)) {
                    propertiesByState.put(state, properties);
                    iterator.remove();
                }
            }
        }

        return propertiesByState;
    }

    public static void writePropertiesToBuf(
            Map<BlockState, MimiBlockMassUnit> map,
            FriendlyByteBuf buf
    ) {

        buf.writeVarInt(map.size());

        for (Map.Entry<BlockState, MimiBlockMassUnit> entry : map.entrySet()) {
            buf.writeVarInt(Block.getId(entry.getKey()));
            entry.getValue().toNetwork(buf);
        }
    }

    public static Map<BlockState, MimiBlockMassUnit> readPropertiesFromBuf(
            FriendlyByteBuf buf
    ) {

        int size = buf.readVarInt();

        Map<BlockState, MimiBlockMassUnit> map =
                new Reference2ObjectOpenHashMap<>();

        for (int i = 0; i < size; i++) {

            BlockState state = Block.stateById(buf.readVarInt());

            MimiBlockMassUnit properties =
                    MimiBlockMassUnit.fromNetwork(buf);

            map.put(state, properties);
        }

        return map;
    }
}

