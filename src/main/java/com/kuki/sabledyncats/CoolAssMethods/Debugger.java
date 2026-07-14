package com.kuki.sabledyncats.CoolAssMethods;

import com.kuki.sabledyncats.SableDynoCats;
import net.neoforged.fml.loading.FMLEnvironment;

public class Debugger {
    public static void info(String message, Object... args) {
        if (!FMLEnvironment.production) {
            SableDynoCats.LOGGER.info(message, args);
        }
    }
}
