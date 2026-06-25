package com.actusfarmi;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Farm inşasını yöneten sınıf.
 * Hasat gerekmez — köydeki çiftçi otomatik toplar.
 * Bu mod sadece yapıyı inşa eder: kum → kaktüs → ip → kum → kaktüs → ...
 */
public class ActusFarmManager {

    private static ActusFarmManager INSTANCE;

    private boolean running = false;
    private ServerPlayerEntity oyuncu;
    private FarmBuilder builder;

    private int ekimSayisi = 0;

    // İnşa sırasında kaç tick'te bir adım atılır
    private static final int INSA_TICK_ARALIGI = 2;
    private int tickSayaci = 0;

    private ActusFarmManager() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    public static ActusFarmManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ActusFarmManager();
        }
        return INSTANCE;
    }

    public void start(ServerPlayerEntity player) {
        this.oyuncu = player;
        this.running = true;
        this.tickSayaci = 0;
        this.ekimSayisi = 0;
        this.builder = new FarmBuilder(player);

        int toplamAdim = builder.getToplamAdim();
        player.sendMessage(Text.literal(
            "§a[AktüsFarm] İnşa başlıyor! §7Toplam " + toplamAdim + " blok yerleştirilecek."
        ), false);
        player.sendMessage(Text.literal(
            "§7[AktüsFarm] Yapı: " +
            FarmBuilder.FARM_WIDTH + "x" + FarmBuilder.FARM_DEPTH + " sütun, " +
            FarmBuilder.KAT_SAYISI + " kat"
        ), false);

        kontrolEnvanter(player);
    }

    public void stop() {
        this.running = false;
        this.oyuncu = null;
        this.builder = null;
    }

    public boolean isRunning() {
        return running;
    }

    public int getHarvestedCount() {
        return 0; // Hasat yok, çiftçi halleder
    }

    public int getPlantedCount() {
        return ekimSayisi;
    }

    // -------------------------------------------------------------------------
    // Tick döngüsü — sadece inşa
    // -------------------------------------------------------------------------

    private void onServerTick(MinecraftServer server) {
        if (!running || oyuncu == null) return;

        if (!oyuncu.isAlive() || oyuncu.isRemoved()) {
            stop();
            return;
        }

        tickSayaci++;

        if (tickSayaci % INSA_TICK_ARALIGI == 0) {
            tickInsa();
        }
    }

    private void tickInsa() {
        if (builder == null) return;

        boolean bitti = builder.tickAdim();
        ekimSayisi = builder.getMevcutAdim();

        // Her 25 adımda bir ilerleme mesajı
        int mevcutAdim = builder.getMevcutAdim();
        int toplamAdim = builder.getToplamAdim();
        if (mevcutAdim > 0 && mevcutAdim % 25 == 0) {
            int yuzde = (mevcutAdim * 100) / toplamAdim;
            oyuncu.sendMessage(Text.literal(
                "§7[AktüsFarm] İnşa: §e%" + yuzde + " §7(" + mevcutAdim + "/" + toplamAdim + ")"
            ), false);
        }

        if (bitti) {
            running = false;
            if (builder.tamamlandi()) {
                oyuncu.sendMessage(Text.literal(
                    "§a[AktüsFarm] ✓ İnşa tamamlandı! " + toplamAdim + " blok yerleştirildi."
                ), false);
                oyuncu.sendMessage(Text.literal(
                    "§7[AktüsFarm] Artık çiftçin hasat işini halleder."
                ), false);
            }
            // Malzeme bitti durumunda FarmBuilder zaten mesaj gönderdi
        }
    }

    // -------------------------------------------------------------------------
    // Envanter kontrolü
    // -------------------------------------------------------------------------

    private void kontrolEnvanter(ServerPlayerEntity player) {
        int kaktusSayisi = 0;
        int kumSayisi = 0;
        int ipSayisi = 0;

        for (ItemStack stack : player.getInventory().main) {
            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.CACTUS) kaktusSayisi += stack.getCount();
                if (stack.getItem() == Items.SAND)   kumSayisi    += stack.getCount();
                if (stack.getItem() == Items.STRING) ipSayisi     += stack.getCount();
            }
        }

        int sutunSayisi  = FarmBuilder.FARM_WIDTH * FarmBuilder.FARM_DEPTH;
        int gerekliKaktus = sutunSayisi * FarmBuilder.KAT_SAYISI;
        int gerekliKum    = sutunSayisi + (sutunSayisi * FarmBuilder.KAT_SAYISI);
        int gerekliIp     = sutunSayisi * FarmBuilder.KAT_SAYISI;

        player.sendMessage(Text.literal("§6[AktüsFarm] Envanter Kontrolü:"), false);
        mesajGonder(player, "Kaktüs", kaktusSayisi, gerekliKaktus);
        mesajGonder(player, "Kum",    kumSayisi,    gerekliKum);
        mesajGonder(player, "İp",     ipSayisi,     gerekliIp);
    }

    private void mesajGonder(ServerPlayerEntity player, String isim, int sahip, int gerekli) {
        String renk = sahip >= gerekli ? "§a" : "§c";
        String durum = sahip < gerekli ? " §c(EKSİK!)" : " §a(✓)";
        player.sendMessage(Text.literal(
            "  " + renk + isim + ": " + sahip + "/" + gerekli + durum
        ), false);
    }
}
