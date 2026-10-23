package com.kuki.sabledyncats.compat;

import com.kuki.sabledyncats.CoolAssMethods.CopycatMassQueue;
import com.kuki.sabledyncats.CoolAssMethods.CopycatsBlockFilter;
import dev.rew1nd.sableschematicapi.api.blueprint.BlueprintPlaceSession;
import dev.rew1nd.sableschematicapi.api.blueprint.BlueprintPlacedBlock;
import dev.rew1nd.sableschematicapi.api.blueprint.SableBlueprintEvent;
import dev.rew1nd.sableschematicapi.api.blueprint.SableBlueprintEventRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Triggers mass restoration after a Photomancy blueprint is placed. Each
 * placed block's storage position belongs to its newly allocated Sable plot,
 * not to the visible-world position of the vehicle.
 *
 * The general-purpose mass queue performs the actual calculation after NBT
 * restoration and merges duplicate notifications from Copycats+ mixins.
 */
public final class PhotomancyMassCompat implements SableBlueprintEvent {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("sabledyncats", "photomancy_mass_restore");
    private static boolean registered;

    private PhotomancyMassCompat() { }

    /** Called only when the optional sable_schematic_api mod is loaded. */
    public static void register() {
        if (registered) {
            return;
        }
        SableBlueprintEventRegistry.register(new PhotomancyMassCompat());
        registered = true;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onPlaceAfterBlocks(BlueprintPlaceSession session, CompoundTag unusedSidecar) {
        for (BlueprintPlacedBlock placed : session.placedBlocks().blocks()) {
            // An entity may have no serialized payload or may not yet be
            // attached here; filtering solely by loaded BlockEntity would
            // silently drop such Copycats. BlockState is always available.
            if (CopycatsBlockFilter.isCopycatsPlusBlock(placed.state())) {
                CopycatMassQueue.schedule(placed.placedSubLevel().getLevel(), placed.storagePos());
            }
        }
    }
}
