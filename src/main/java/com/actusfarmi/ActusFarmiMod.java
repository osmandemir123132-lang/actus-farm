package com.actusfarmi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActusFarmiMod implements ModInitializer {

    public static final String MOD_ID = "actusfarmi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Aktüs Farmı Modu yüklendi!");

        // Oyuncu sunucuya her girdiğinde otomatik başlat
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;

            // Farm inşasını başlat
            ActusFarmManager farmManager = ActusFarmManager.getInstance();
            if (!farmManager.isRunning()) {
                farmManager.start(player);
            }

            // İp koyma sistemini başlat
            IpKoyHack ipKoy = IpKoyHack.getInstance();
            if (!ipKoy.isAktif()) {
                ipKoy.baslat(player);
            }
        });
    }
}
