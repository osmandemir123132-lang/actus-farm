package com.actusfarmi;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IpKoyHack:
 * - Aktif edilince oyuncunun etrafındaki TÜM kaktüsleri tarar (sınır yok, chunk yüklü olduğu sürece)
 * - Her kaktüsün hizasında (aynı Y), 4 yanına (N/S/E/W) birer ip koyar
 * - Kaktüsün üstüne/altına ip KOYMAZ
 * - Zaten ip olan yerleri atlar
 * - Kaktüs olan yanlara ip koymaz (kaktüs yanyana duramaz zaten ama güvenlik için)
 * - Açık olduğu sürece yeni dikilen kaktüsleri de otomatik algılar
 */
public class IpKoyHack {

    private static IpKoyHack INSTANCE;

    private boolean aktif = false;
    private ServerPlayerEntity oyuncu;
    private int tickSayaci = 0;

    // Kaç tick'te bir tarama yapılır (20 = 1 saniye)
    private static final int TARAMA_ARALIGI = 40;

    // Tarama yarıçapı (chunk bazında — 10 chunk = 160 blok)
    private static final int TARAMA_YARICAP = 80;

    // Zaten işlenmiş pozisyonları tekrar işleme
    private final Set<BlockPos> islenmis = new HashSet<>();

    // İşlenecek ip pozisyonları kuyruğu (ani lag yapmamak için kademeli koy)
    private final List<BlockPos> kuyruk = new ArrayList<>();
    private static final int HER_TICK_MAX_IP = 5; // Tick başına max kaç ip koyar

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
        this.islenmis.clear();
        this.kuyruk.clear();
        this.tickSayaci = 0;
        player.sendMessage(Text.literal("§a[IpKoy] Aktif! Kaktüsler taranıyor..."), false);
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

    // -------------------------------------------------------------------------

    private void onTick(MinecraftServer server) {
        if (!aktif || oyuncu == null) return;
        if (!oyuncu.isAlive() || oyuncu.isRemoved()) { durdur(); return; }

        tickSayaci++;

        // Kuyruktaki ipleri kademeli koy
        int koyulan = 0;
        while (!kuyruk.isEmpty() && koyulan < HER_TICK_MAX_IP) {
            BlockPos pos = kuyruk.remove(0);
            ipKoy(pos);
            koyulan++;
        }

        // Belirli aralıklarla yeni kaktüs taraması yap
        if (tickSayaci % TARAMA_ARALIGI == 0) {
            kaktusleriBul();
        }
    }

    /**
     * Oyuncunun etrafındaki yüklü chunklarda kaktüs arar.
     * Her kaktüs için 4 yan pozisyonu kuyruğa ekler.
     */
    private void kaktusleriBul() {
        ServerWorld world = (ServerWorld) oyuncu.getWorld();
        BlockPos merkez = oyuncu.getBlockPos();

        int yeniSayac = 0;

        for (int x = -TARAMA_YARICAP; x <= TARAMA_YARICAP; x++) {
            for (int z = -TARAMA_YARICAP; z <= TARAMA_YARICAP; z++) {
                // Sadece yüklü chunkları tara
                int chunkX = (merkez.getX() + x) >> 4;
                int chunkZ = (merkez.getZ() + z) >> 4;
                if (!world.isChunkLoaded(chunkX, chunkZ)) continue;

                // Y ekseninde kaktüs ara (çöl seviyesi: Y=60-80 arası genelde ama tüm Y'yi tara)
                for (int y = world.getBottomY(); y < world.getTopY(); y++) {
                    BlockPos pos = new BlockPos(merkez.getX() + x, y, merkez.getZ() + z);

                    // Kaktüs mü?
                    if (world.getBlockState(pos).getBlock() != Blocks.CACTUS) continue;

                    // Zaten işlendi mi?
                    if (islenmis.contains(pos)) continue;

                    // 4 yandaki pozisyonları kuyruğa ekle
                    BlockPos[] yanlar = {
                        pos.north(),
                        pos.south(),
                        pos.east(),
                        pos.west()
                    };

                    for (BlockPos yan : yanlar) {
                        // Zaten ip var mı?
                        if (world.getBlockState(yan).getBlock() == Blocks.TRIPWIRE) continue;
                        // Hava değil mi? (başka blok varsa koyma)
                        if (!world.getBlockState(yan).isAir()) continue;
                        // Zaten kuyrukta mı?
                        if (kuyruk.contains(yan)) continue;

                        kuyruk.add(yan);
                        yeniSayac++;
                    }

                    islenmis.add(pos);
                }
            }
        }

        if (yeniSayac > 0) {
            oyuncu.sendMessage(Text.literal(
                "§7[IpKoy] " + yeniSayac + " yeni ip pozisyonu bulundu, koyuluyor..."
            ), false);
        }
    }

    /**
     * Belirtilen pozisyona ip koyar — envanterdeki ipi tüketir.
     */
    private void ipKoy(BlockPos pos) {
        ServerWorld world = (ServerWorld) oyuncu.getWorld();

        // Hava değilse atla
        if (!world.getBlockState(pos).isAir()) return;

        // Envanterde ip var mı?
        if (!envanterdeMalzemeVar(Items.STRING)) {
            oyuncu.sendMessage(Text.literal("§c[IpKoy] Envanterde ip kalmadı!"), false);
            durdur();
            return;
        }

        world.setBlockState(pos, Blocks.TRIPWIRE.getDefaultState());
        envanterdanTuket(Items.STRING);
    }

    private boolean envanterdeMalzemeVar(net.minecraft.item.Item item) {
        for (ItemStack stack : oyuncu.getInventory().main) {
            if (!stack.isEmpty() && stack.getItem() == item) return true;
        }
        return false;
    }

    private void envanterdanTuket(net.minecraft.item.Item item) {
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
