package com.kuki.sabledyncats;

import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesHandler;
import com.kuki.sabledyncats.massMultipliers.CopycatsDefinitions;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

// Me VS hydrogen bomb let's go!
@Mod(SableDynoCats.MODID)
public class SableDynoCats {
    // Hi, my name is Derek. Welcome to PizzaHut, let me guess, pizza?
    public static final String MODID = "sabledyncats";
    public static final Logger LOGGER = LogUtils.getLogger();


    public SableDynoCats(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );

        NeoForge.EVENT_BUS.register(this);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.register(Config.class);

        modEventBus.addListener(this::onCommonSetup);
    }


    private void onCommonSetup(FMLCommonSetupEvent event) {

        CopycatsDefinitions.init();

        LOGGER.info("Copycats mass compatibility initialized");
    }


    @SubscribeEvent
    public void onReload(AddReloadListenerEvent event) {

        LOGGER.info("Registering BlockMassPropertiesReloadListener");

        event.addListener(
                BlockMassPropertiesHandler.BlockReloadListener.INSTANCE
        );
    }
    // Remember, I never said that I'm a good developer...
}
