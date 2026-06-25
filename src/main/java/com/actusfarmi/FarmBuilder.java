package com.actusfarmi;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Farm yapısını inşa eden sınıf.
 *
 * Yapı düzeni (her sütun için, yukarı doğru):
 *   Y+0 : Kum  (zemin - oyuncu tarafından önceden yerleştirilmiş olmalı)
 *   Y+1 : Kaktüs (1. kat)
 *   Y+2 : İp    (kaktüsün hizasına)
 *   Y+3 : Kum   (ipin üstüne)
 *   Y+4 : Kaktüs (2. kat)
 *   Y+5 : İp
 *   Y+6 : Kum
 *   ...  katlar boyunca devam eder
 */
public class FarmBuilder {

    // Farm boyutu: kaç sütun x kaç sütun (aralarında boşluk bırakarak)
    public static final int FARM_WIDTH = 5;   // X yönünde sütun sayısı
    public static final int FARM_DEPTH = 5;   // Z yönünde sütun sayısı
    public static final int KAT_SAYISI = 3;   // Kaç kat kaktüs-ip-kum döngüsü

    // Sütunlar arası mesafe: kaktüs yanyana duramaz, 1 blok boşluk olmalı
    public static final int SUTUN_ARALIGI = 2;

    private final ServerPlayerEntity player;
    private final ServerWorld world;
    private final BlockPos baslangicPos; // Oyuncunun ayaklarının altındaki kum bloğu

    // İnşa adımlarını sıraya koyduk — her tick'te bir adım işlenir
    private final List<BuildStep> adimlar = new ArrayList<>();
    private int mevcutAdim = 0;

    public FarmBuilder(ServerPlayerEntity player) {
        this.player = player;
        this.world = (ServerWorld) player.getWorld();
        // Oyuncunun ayaklarının altındaki bloğu zemin olarak al
        this.baslangicPos = player.getBlockPos().down();
        hesaplaAdimlar();
    }

    /**
     * Tüm yerleştirme adımlarını önceden hesaplar.
     * Sıra: önce tüm kum zeminleri, sonra kat kat kaktüs + ip + kum
     */
    private void hesaplaAdimlar() {
        for (int x = 0; x < FARM_WIDTH; x++) {
            for (int z = 0; z < FARM_DEPTH; z++) {
                int offsetX = x * SUTUN_ARALIGI;
                int offsetZ = z * SUTUN_ARALIGI;

                // Zemin kumu — Y+0 (zaten var olabilir, kontrol edilir)
                BlockPos zeminKumu = baslangicPos.add(offsetX, 0, offsetZ);
                adimlar.add(new BuildStep(zeminKumu, Blocks.SAND.getDefaultState(), Items.SAND));

                // Her kat için döngü
                for (int kat = 0; kat < KAT_SAYISI; kat++) {
                    int katOffset = kat * 3; // Her kat 3 blok yükseklik kaplar

                    // Y+1, Y+4, Y+7 ... : Kaktüs
                    BlockPos kaktusPoz = baslangicPos.add(offsetX, 1 + katOffset, offsetZ);
                    adimlar.add(new BuildStep(kaktusPoz, Blocks.CACTUS.getDefaultState(), Items.CACTUS));

                    // Y+2, Y+5, Y+8 ... : İp (kaktüsün hizası, kaktüs büyüyünce buraya çarpar)
                    BlockPos ipPoz = baslangicPos.add(offsetX, 2 + katOffset, offsetZ);
                    adimlar.add(new BuildStep(ipPoz, Blocks.TRIPWIRE.getDefaultState(), Items.STRING));

                    // Y+3, Y+6, Y+9 ... : Kum (ipin üstüne, bir sonraki katın zemini)
                    BlockPos kumPoz = baslangicPos.add(offsetX, 3 + katOffset, offsetZ);
                    adimlar.add(new BuildStep(kumPoz, Blocks.SAND.getDefaultState(), Items.SAND));
                }
            }
        }
    }

    /**
     * Bir sonraki adımı işler. Her tick çağrılır.
     * @return true = tamamlandı, false = devam ediyor
     */
    public boolean tickAdim() {
        if (mevcutAdim >= adimlar.size()) {
            return true; // Tüm adımlar bitti
        }

        BuildStep adim = adimlar.get(mevcutAdim);

        // Blok zaten doğru şekilde yerleştirilmişse atla
        if (world.getBlockState(adim.pos).getBlock() == adim.state.getBlock()) {
            mevcutAdim++;
            return false;
        }

        // Envanterden gerekli malzemeyi kontrol et ve tüket
        if (!envanterdeMalzemeVar(adim.gerekliItem)) {
            player.sendMessage(Text.literal(
                "§c[AktüsFarm] Envanterde " + adim.gerekliItem.toString().replace("minecraft:", "") + " kalmadı! Farm duruyor."
            ), false);
            return true; // Hata — farmı durdur
        }

        envanterdanTuket(adim.gerekliItem);
        world.setBlockState(adim.pos, adim.state);
        mevcutAdim++;
        return false;
    }

    public boolean tamamlandi() {
        return mevcutAdim >= adimlar.size();
    }

    public int getToplamAdim() {
        return adimlar.size();
    }

    public int getMevcutAdim() {
        return mevcutAdim;
    }

    public BlockPos getBaslangicPos() {
        return baslangicPos;
    }

    // --- Envanter yardımcıları ---

    private boolean envanterdeMalzemeVar(net.minecraft.item.Item item) {
        for (ItemStack stack : player.getInventory().main) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private void envanterdanTuket(net.minecraft.item.Item item) {
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    player.getInventory().main.set(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    // --- İç sınıf: Bir yerleştirme adımı ---
    public static class BuildStep {
        public final BlockPos pos;
        public final BlockState state;
        public final Item gerekliItem;

        public BuildStep(BlockPos pos, BlockState state, Item gerekliItem) {
            this.pos = pos;
            this.state = state;
            this.gerekliItem = gerekliItem;
        }
    }
}
