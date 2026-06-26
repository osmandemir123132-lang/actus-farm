package com.actusfarmi;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * IpKoyHack:
 * - Oyuncunun 5 blok yakınındaki kaktüsleri tarar
 * - Her kaktüsün 4 yanına ip koyar (üstüne ve altına koymaz)
 * - Her ip yerleştirmede el swing animasyonu oynatır
 * - Bloklar arası 4-6 tick gecikme ile yerleştirir (doğal görünüm)
 */
public class IpKoyHack {

    private static IpKoyHack INSTANCE;

    private boolean aktif = false;
    private ServerPlayerEntity oyuncu;
    private int tickSayaci = 0;

    // Yakın kaktüs tarama yarıçapı
    private static final int TARAMA_YARICAP = 5;

    // Kaç tick'te bir yakın tarama yapılır
    private static final int TARAMA_ARALIGI = 15;

    // İpler arası bekleme — 4 ile 6 arasında rastgele (doğal görünüm)
    private int sonrakiIpTick = 0;
    private static final int MIN_GECIKME = 4;
    private static final int MAX_GECIKME = 7;

    // Yerleştirilecek ip pozisyonları kuyruğu
    private final Queue<BlockPos> kuyruk = new LinkedList<>();

    private IpKoyHack() {
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    public static IpKoyHack getInstance() {
        if (INSTANCE == null) INSTANCE = new IpKoyHack();
        return INSTANCE;
    }

    public void baslat(ServerPlayerEntity player) {
        this.oyuncu = player;
        this.aktif = true;
        this.tickSayaci = 0;
        this.sonrakiIpTick = 0;
        this.kuyruk.clear();
        player.sendMessage(Text.literal("§a[IpKoy] Aktif! Yakındaki kaktüslere ip koyuyorum..."), false);
    }

    public void durdur() {
        this.aktif = false;
        this.kuyruk.clear();
        if (oyuncu != null) {
            oyuncu.sendMessage(Text.literal("§c[IpKoy] Durduruldu."), false);
        }
        this.oyuncu = null;
    }

    public boolean isAktif() { return aktif; }

    private void onTick(MinecraftServer server) {
        if (!aktif || oyuncu == null) return;
        if (!oyuncu.isAlive() || oyuncu.isRemoved()) { durdur(); return; }

        tickSayaci++;

        // Belirli aralıklarla yakındaki kaktüsleri tara
        if (tickSayaci % TARAMA_ARALIGI == 0) {
            yakinKaktusleriBul();
        }

        // Sıradaki ipi koy (gecikme ile — doğal görünüm)
        if (!kuyruk.isEmpty() && tickSayaci >= sonrakiIpTick) {
            BlockPos hedef = kuyruk.poll();
            ipKoyAnimasyonlu(hedef);
            // Bir sonraki ip için rastgele gecikme
            int gecikme = MIN_GECIKME + (int)(Math.random() * (MAX_GECIKME - MIN_GECIKME));
            sonrakiIpTick = tickSayaci + gecikme;
        }
    }

    /**
     * Oyuncunun etrafındaki 5 blokluk alanda kaktüs arar.
     * Kaktüs bulunca 4 yanını kuyruğa ekler.
     */
    private void yakinKaktusleriBul() {
        ServerWorld world = (ServerWorld) oyuncu.getWorld();
        BlockPos merkez = oyuncu.getBlockPos();

        for (int x = -TARAMA_YARICAP; x <= TARAMA_YARICAP; x++) {
            for (int z = -TARAMA_YARICAP; z <= TARAMA_YARICAP; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = merkez.add(x, y, z);

                    if (world.getBlockState(pos).getBlock() != Blocks.CACTUS) continue;

                    // Kaktüsün 4 yanını kontrol et
                    BlockPos[] yanlar = { pos.north(), pos.south(), pos.east(), pos.west() };
                    for (BlockPos yan : yanlar) {
                        // Hava mı? İp yok mu? Zaten kuyrukta mı?
                        if (!world.getBlockState(yan).isAir()) continue;
                        if (kuyruk.contains(yan)) continue;
                        kuyruk.add(yan);
                    }
                }
            }
        }
    }

    /**
     * Belirtilen pozisyona ip koyar + el animasyonu oynatır.
     */
    private void ipKoyAnimasyonlu(BlockPos pos) {
        ServerWorld world = (ServerWorld) oyuncu.getWorld();

        // Tekrar kontrol — hâlâ hava mı?
        if (!world.getBlockState(pos).isAir()) return;

        // Envanterde ip var mı?
        if (!envanterdeMalzemeVar(Items.STRING)) {
            oyuncu.sendMessage(Text.literal("§c[IpKoy] Envanterde ip kalmadı!"), false);
            durdur();
            return;
        }

        // El swing animasyonu — oyuncu gerçekten yerleştiriyormuş gibi görünür
        oyuncu.swingHand(Hand.MAIN_HAND);

        // İpi yerleştir
        world.setBlockState(pos, Blocks.TRIPWIRE.getDefaultState());
        envanterdanTuket(Items.STRING);
    }

    private boolean envanterdeMalzemeVar(Item item) {
        for (ItemStack stack : oyuncu.getInventory().main) {
            if (!stack.isEmpty() && stack.getItem() == item) return true;
        }
        return false;
    }

    private void envanterdanTuket(Item item) {
        for (int i = 0; i < oyuncu.getInventory().main.size(); i++) {
            ItemStack stack = oyuncu.getInventory().main.get(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.decrement(1);
                if (stack.isEmpty()) oyuncu.getInventory().main.set(i, ItemStack.EMPTY);
                return;
            }
        }
    }
}
