package com.kuki.sabledyncats;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// Fuh confih
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_MASS_MULTIPLIERS = BUILDER
            .push("Mass Calculation")
            .comment(" Enable mass multipliers from JSON files located in data/copycats/")
            .translation("sabledyncats.config.enable_mass_multipliers")
            .define("enableMassMultipliers", true);
    private static final ModConfigSpec.BooleanValue INCLUDE_COPYCAT_MASS = BUILDER
            .comment(" Whether to add copycat's own mass to total mass")
            .translation("sabledyncats.config.include_copycat_mass")
            .define("includeCopycatMass", true);

    static final ModConfigSpec SPEC;

    static {
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static boolean enableMassMultipliers;
    public static boolean includeCopycatMass;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            enableMassMultipliers = ENABLE_MASS_MULTIPLIERS.get();
            includeCopycatMass = INCLUDE_COPYCAT_MASS.get();
            SableDynoCats.LOGGER.info(
                    "Config loaded: enableMassMultipliers={}, includeCopycatMass={}",
                    enableMassMultipliers,
                    includeCopycatMass
            );
        }
    }
}
