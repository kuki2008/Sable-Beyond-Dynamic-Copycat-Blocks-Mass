package com.kuki.sabledyncats.massMultipliers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kuki.sabledyncats.CoolAssMethods.BlockStatePredicateHelper;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.*;
import java.util.function.Predicate;

public record VariantBlockMassProperties(SimpleBlockMassProperties defaultProperties, Map<BlockState, SimpleBlockMassProperties> propertiesByState)
        implements BlockMassPropertiesProvider {

    @Override
    public double massMultiplier(Level level, BlockState state, BlockPos pos, boolean recurse) {
        return this.propertiesByState.getOrDefault(state, this.defaultProperties).massMultiplier(level, state, pos, recurse);
    }

    @Override
    public List<BlockState> containedBlockStates(Level level, BlockState state, BlockPos pos, boolean recurse) {
        return List.of(state);
    }

    public static VariantBlockMassProperties fromJson(Block block, JsonObject obj) {
        StateDefinition<Block, BlockState> definition = block.getStateDefinition();
        Set<BlockState> states = new HashSet<>(definition.getPossibleStates());
        Map<BlockState, SimpleBlockMassProperties> propertiesByState = new Reference2ObjectOpenHashMap<>();

        if (obj.has("variants") && obj.get("variants").isJsonObject()) {
            JsonObject variants = obj.getAsJsonObject("variants");
            for (String key : variants.keySet()) {
                Predicate<BlockState> pred = BlockStatePredicateHelper.variantPredicate(definition, key);
                JsonElement el = variants.get(key);
                if (!el.isJsonObject()) {
                    throw new JsonSyntaxException("Invalid info for variant '" + key + "''");
                }
                JsonObject variantInfo = el.getAsJsonObject();
                SimpleBlockMassProperties properties = SimpleBlockMassProperties.fromJson(variantInfo);
                for (Iterator<BlockState> stateIter = states.iterator(); stateIter.hasNext(); ) {
                    BlockState state = stateIter.next();
                    if (pred.test(state)) {
                        propertiesByState.put(state, properties);
                        stateIter.remove();
                    }
                }
            }
        }
        SimpleBlockMassProperties defaultProperties = SimpleBlockMassProperties.fromJson(obj);
        return new VariantBlockMassProperties(defaultProperties, propertiesByState);
    }

    public void toNetwork(FriendlyByteBuf buf) {
        this.defaultProperties.toNetwork(buf);
        buf.writeVarInt(this.propertiesByState.size());
        for (Map.Entry<BlockState, SimpleBlockMassProperties> entry : this.propertiesByState.entrySet()) {
            buf.writeVarInt(Block.getId(entry.getKey()));
            entry.getValue().toNetwork(buf);
        }
    }

    public static VariantBlockMassProperties fromNetwork(FriendlyByteBuf buf) {
        SimpleBlockMassProperties defaultProperties = SimpleBlockMassProperties.fromNetwork(buf);
        int sz = buf.readVarInt();
        Map<BlockState, SimpleBlockMassProperties> propertiesByState = new Reference2ObjectOpenHashMap<>();
        for (int i = 0; i < sz; ++i) {
            BlockState state = Block.stateById(buf.readVarInt());
            SimpleBlockMassProperties properties = SimpleBlockMassProperties.fromNetwork(buf);
            propertiesByState.put(state, properties);
        }
        return new VariantBlockMassProperties(defaultProperties, propertiesByState);
    }

}