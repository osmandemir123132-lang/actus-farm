package com.actusfarmi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActusFarmiMod implements ModInitializer {

    public static final String MOD_ID = "actusfarmi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Aktüs Farmı Modu yüklendi!");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ActusFarmiCommand.register(dispatcher);
        });
    }
}
