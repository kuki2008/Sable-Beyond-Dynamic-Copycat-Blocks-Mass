package com.kuki.sabledyncats.CoolAssMethods;

import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Counts occupied mass units, not merely the number of material-map entries. */
public final class CopycatPartWeights {
    private CopycatPartWeights() { }

    public static int occupiedUnits(BlockState state, String part) {
        if (state.getBlock() instanceof IMultiStateCopycatBlock multi
                && !multi.partExists(state, part)) {
            return 0;
        }
        // "positive_layers" / "negative_layers" are IntegerProperties;
        // board, byte and other named sections use BooleanProperties.
        for (Property<?> property : state.getProperties()) {
            if (!property.getName().equals(part)) {
                continue;
            }
            Comparable<?> value = state.getValue(property);
            if (value instanceof Integer count) {
                return Math.max(0, count);
            }
            if (value instanceof Boolean enabled) {
                return enabled ? 1 : 0;
            }
            break;
        }
        // Material-map sections like slab "top"/"bottom" are each one unit;
        // partExists() above excludes inactive sections.
        return 1;
    }
}
