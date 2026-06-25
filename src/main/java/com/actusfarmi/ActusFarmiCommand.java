package com.actusfarmi;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class ActusFarmiCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        // /actusfarmi komutları (farm inşası)
        dispatcher.register(
            CommandManager.literal("actusfarmi")
                .then(CommandManager.literal("baslat")
                    .executes(ctx -> {
                        var source = ctx.getSource();
                        if (source.getPlayer() == null) { source.sendError(Text.literal("Sadece oyuncular!")); return 0; }
                        var manager = ActusFarmManager.getInstance();
                        if (manager.isRunning()) { source.sendFeedback(() -> Text.literal("§eZaten çalışıyor!"), false); return 0; }
                        manager.start(source.getPlayer());
                        return 1;
                    }))
                .then(CommandManager.literal("durdur")
                    .executes(ctx -> {
                        ActusFarmManager.getInstance().stop();
                        ctx.getSource().sendFeedback(() -> Text.literal("§cFarm durduruldu."), false);
                        return 1;
                    }))
                .then(CommandManager.literal("durum")
                    .executes(ctx -> {
                        var manager = ActusFarmManager.getInstance();
                        if (manager.isRunning()) {
                            ctx.getSource().sendFeedback(() -> Text.literal("§aFarm: İNŞA DEVAM EDİYOR | Blok: §f" + manager.getPlantedCount()), false);
                        } else {
                            ctx.getSource().sendFeedback(() -> Text.literal("§7Farm: DURDU"), false);
                        }
                        return 1;
                    }))
        );

        // /ipkoy komutu — kaktüs tarayıp yanlarına ip koyar
        dispatcher.register(
            CommandManager.literal("ipkoy")
                .then(CommandManager.literal("baslat")
                    .executes(ctx -> {
                        var source = ctx.getSource();
                        if (source.getPlayer() == null) { source.sendError(Text.literal("Sadece oyuncular!")); return 0; }
                        var hack = IpKoyHack.getInstance();
                        if (hack.isAktif()) { source.sendFeedback(() -> Text.literal("§eZaten aktif!"), false); return 0; }
                        hack.baslat(source.getPlayer());
                        return 1;
                    }))
                .then(CommandManager.literal("durdur")
                    .executes(ctx -> {
                        IpKoyHack.getInstance().durdur();
                        return 1;
                    }))
                .executes(ctx -> {
                    // /ipkoy tek başına yazılırsa toggle gibi davran
                    var source = ctx.getSource();
                    if (source.getPlayer() == null) return 0;
                    var hack = IpKoyHack.getInstance();
                    if (hack.isAktif()) {
                        hack.durdur();
                    } else {
                        hack.baslat(source.getPlayer());
                    }
                    return 1;
                })
        );
    }
}
