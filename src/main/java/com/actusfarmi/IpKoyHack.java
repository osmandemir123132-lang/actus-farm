package com.actusfarmi;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;

import java.util.LinkedList;
import java.util.Queue;

/**
 * IpKoyHack — Client-side
 * Oyuncunun etrafındaki kaktüslerin 4 yanına otomatik ip koyar.
 * Sunucuya gerçek blok yerleştirme paketi gönderir — çok oyunculuda çalışır.
 */
public class IpKoyHack {

    private static IpKoyHack INSTANCE;

    private boolean aktif = false;
    private int tickSayaci = 0;

    // Tarama yarıçapı (blok)
    private static final int TARAMA_YARICAP = 8;

    // Kaç tick'te bir kaktüs taraması yapılır
    private static final int TARAMA_ARALIGI = 40;

    // İpler arası gecikme (6-12 tick = 0.3-0.6sn — Vulcan bypass için yeterli)
    private int sonrakiIpTick = 0;
    private static final int MIN_GECIKME = 6;
    private static final int MAX_GECIKME = 12;

    // Yerleştirilecek ip pozisyonları kuyruğu
    private final Queue<BlockPos> kuyruk = new LinkedList<>();

    private IpKoyHack() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public static IpKoyHack getInstance() {
        if (INSTANCE == null) INSTANCE = new IpKoyHack();
        return INSTANCE;
    }

    public void baslat() {
        this.aktif = true;
        this.tickSayaci = 0;
        this.sonrakiIpTick = 0;
        this.kuyruk.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§a[IpKoy] Aktif! Kaktüslere ip koyuyorum..."), false);
        }
    }

    public void durdur() {
        this.aktif = false;
        this.kuyruk.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§c[IpKoy] Durduruldu."), false);
        }
    }

    public boolean isAktif() {
        return aktif;
    }

    private void onTick(MinecraftClient mc) {
        if (!aktif) return;
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isAlive()) return;

        tickSayaci++;

        // Periyodik kaktüs taraması
        if (tickSayaci % TARAMA_ARALIGI == 0) {
            yakinKaktusleriBul(mc);
        }

        // Sıradaki ipi yerleştir
        if (!kuyruk.isEmpty() && tickSayaci >= sonrakiIpTick) {
            BlockPos hedef = kuyruk.poll();
            ipKoy(mc, hedef);
            int gecikme = MIN_GECIKME + (int)(Math.random() * (MAX_GECIKME - MIN_GECIKME));
            sonrakiIpTick = tickSayaci + gecikme;
        }
    }

    /**
     * Oyuncunun etrafındaki kaktüsleri tarar,
     * boş olan yanlarını kuyruğa ekler.
     */
    private void yakinKaktusleriBul(MinecraftClient mc) {
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;
        BlockPos merkez = player.getBlockPos();

        for (int x = -TARAMA_YARICAP; x <= TARAMA_YARICAP; x++) {
            for (int z = -TARAMA_YARICAP; z <= TARAMA_YARICAP; z++) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos pos = merkez.add(x, y, z);

                    if (world.getBlockState(pos).getBlock() != Blocks.CACTUS) continue;

                    BlockPos[] yanlar = { pos.north(), pos.south(), pos.east(), pos.west() };
                    for (BlockPos yan : yanlar) {
                        if (!world.getBlockState(yan).isAir()) continue;
                        if (kuyruk.contains(yan)) continue;
                        kuyruk.add(yan);
                    }
                }
            }
        }
    }

    /**
     * Belirtilen pozisyona ip koyar.
     * Sunucuya gerçek PlayerInteractBlockC2SPacket gönderir.
     * Vulcan bypass: önce oyuncuyu o bloğa döndürür.
     */
    private void ipKoy(MinecraftClient mc, BlockPos pos) {
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;

        // Hâlâ hava mı?
        if (!world.getBlockState(pos).isAir()) return;

        // Envanterde ip var mı?
        int ipSlot = ipSlotBul(player);
        if (ipSlot == -1) {
            player.sendMessage(Text.literal("§c[IpKoy] Envanterde ip kalmadı!"), false);
            durdur();
            return;
        }

        // Hangi yüzden koyacağız — sadece yatay komşu
        Direction koyYonu = koyulacakYuzBul(world, pos);
        if (koyYonu == null) return;

        BlockPos komsuPos = pos.offset(koyYonu.getOpposite());

        // Hit noktası: komşu bloğun yüzünün ortası
        Vec3d hitVec = Vec3d.ofCenter(komsuPos).add(
            Vec3d.of(koyYonu.getVector()).multiply(0.5)
        );

        // Vulcan bypass: oyuncuyu hedef bloğa baktır (yaw/pitch hesapla)
        Vec3d gozPos = player.getEyePos();
        Vec3d fark = hitVec.subtract(gozPos);
        double yatayUzaklik = Math.sqrt(fark.x * fark.x + fark.z * fark.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(-fark.x, fark.z)));
        float pitch = (float)(Math.toDegrees(-Math.atan2(fark.y, yatayUzaklik)));

        // Bakış açısını server'a gönder (PlayerMoveC2SPacket ile)
        player.setYaw(yaw);
        player.setPitch(pitch);

        // İp slotunu seç
        int eskiSlot = player.getInventory().selectedSlot;
        if (ipSlot < 9) {
            player.getInventory().selectedSlot = ipSlot;
        }

        // El swing
        player.swingHand(Hand.MAIN_HAND);

        // Sunucuya blok yerleştirme paketi gönder
        BlockHitResult hitResult = new BlockHitResult(hitVec, koyYonu.getOpposite(), komsuPos, false);
        mc.getNetworkHandler().sendPacket(
            new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0)
        );

        // Eski slota dön
        player.getInventory().selectedSlot = eskiSlot;
    }

    /**
     * Verilen pozisyonun etrafındaki katı bloğu bulur,
     * SADECE yatay yönlere (N/S/E/W) bakar — üst/alt yön yok.
     * Kaktüsün yanına koyması için şart.
     */
    private Direction koyulacakYuzBul(ClientWorld world, BlockPos pos) {
        // Sadece yatay yönler — UP/DOWN kesinlikle deneme
        Direction[] yatayYonler = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        for (Direction dir : yatayYonler) {
            BlockPos komsu = pos.offset(dir);
            if (!world.getBlockState(komsu).isAir()) {
                return dir;
            }
        }
        // Yatayda komşu bulunamazsa alta bak (zemin)
        if (!world.getBlockState(pos.down()).isAir()) {
            return Direction.DOWN;
        }
        return null;
    }

    /**
     * Envanterde ip (string) slotunu bulur. -1 = yok.
     */
    private int ipSlotBul(ClientPlayerEntity player) {
        // Önce hotbar'a bak (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.STRING) return i;
        }
        // Ana envanter (9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.STRING) return i;
        }
        return -1;
    }
}
