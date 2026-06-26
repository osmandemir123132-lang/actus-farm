package com.actusfarmi;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActusFarmiMod implements ClientModInitializer {

    public static final String MOD_ID = "actusfarmi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("IpKoy Modu yüklendi!");

        // Herhangi bir sunucuya (veya singleplayer) girince otomatik başlat
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                IpKoyHack ipKoy = IpKoyHack.getInstance();
                if (!ipKoy.isAktif()) {
                    ipKoy.baslat();
                }
            });
        });

        // Sunucudan çıkınca durdur
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IpKoyHack.getInstance().durdur();
        });
    }
}
