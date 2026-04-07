package dev.notmarra.tenbatsu.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.command.arguments.PlayerArg;
import dev.notmarra.notlib.command.arguments.StringArg;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.utils.DurationParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class BanCommand extends CommandGroup {
    private final Tenbatsu plugin;
    private final LanguageManager lang;

    public BanCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
        this.lang = plugin.getLang();
    }

    public Command build() {
        Command cmd = Command.of("ban")
                .setPermission("tenbatsu.ban")
                        .onExecute(c -> {
                            lang.get("ban.usage").sendTo(c.getSender());
                        });

        PlayerArg target = (PlayerArg) cmd.playerArg("target")
                        .onExecute(c -> {
                            OfflinePlayer t = c.getPlayer();
                            if (t == null) { lang.get("general.specify_player").sendTo(c.getSender()); return; }
                            banPlayer(t, c.getSender());
                        });

        target.greedyStringArg("reason")
                        .onExecute(c -> {
                            OfflinePlayer t = target.get();
                            String r = c.get();
                            if (t == null) { lang.get("general.specify_player").sendTo(c.getSender()); return; }
                            banPlayer(t, c.getSender(), r);
                        });

        return cmd;
    }

    public Command buildTemp() {
        Command cmd = Command.of("tempban")
                .setPermission("tenbatsu.tempban")
                .onExecute(c -> {
                    lang.get("ban.usage_duration").sendTo(c.getSender());
                });

        PlayerArg target = (PlayerArg) cmd.playerArg("target")
                .onExecute(c -> {
                    OfflinePlayer t = c.getPlayer();
                    if (t == null) { lang.get("general.specify_player").sendTo(c.getSender()); return; }
                    lang.get("general.specify_duration").sendTo(c.getSender());
                });

        StringArg duration = (StringArg) target.stringArg("duration")
                .onExecute(c -> {
                    String d = c.get();
                    if (target.get() == null) { lang.get("general.specify_player").sendTo(c.getSender()); return; }
                    if (DurationParser.parse(d) <= 0) { lang.get("general.invalid_duration").sendTo(c.getSender()); return; }
                    banPlayerTemp(target.get(), c.getSender(), d);
                });

        duration.greedyStringArg("reason")
                .onExecute(c -> {
                    String r = c.get();
                    if (target.get() == null) { lang.get("general.specify_player").sendTo(c.getSender()); return; }
                    if (DurationParser.parse(duration.get()) <= 0) { lang.get("general.invalid_duration").sendTo(c.getSender()); return; }
                    banPlayerTemp(target.get(), c.getSender(), duration.get(), r);
                });

        return cmd;
    }

    public Command buildUnban() {
        Command cmd = Command.of("unban")
                .setPermission("tenbatsu.unban")
                .onExecute(c -> {
                    lang.get("unban.usage").sendTo(c.getSender());
                });

        cmd.playerProfilesArg("target")
                 .onExecute(c -> {
                     PlayerProfile player = c.get().getFirst();
                     if (player == null) { lang.get("general.specify_player").sendTo(c.getSender()); return; }
                     plugin.getPunishmentManager().unbanPlayer(player.getId()).thenAccept(unbanned -> {
                         if (unbanned) lang.get("unban.success").with("%target%", player.getName()).sendTo(c.getSender());
                         else lang.get("unban.not_banned").with("%target%", player.getName()).sendTo(c.getSender());
                     });
                 });

        return cmd;
    }

    private void banPlayer(OfflinePlayer target, CommandSender sender) {
        banPlayer(target, sender, plugin.getConfig().getString("settings.default_ban_reason", "No reason"));
    }

    private void banPlayer(OfflinePlayer target, CommandSender sender, String reason) {
        String staffName = sender.getName();
        plugin.getPunishmentManager().isBanned(target.getUniqueId()).thenAccept(isBanned -> {
            if (isBanned) { lang.get("ban.already_banned").with("%target%", target.getName()).sendTo(sender); return; }
            plugin.getPunishmentManager().banPlayer(target.getUniqueId(), target.getName(), staffName, reason, -1)
                    .thenAccept(p -> {
                        lang.get("ban.permanent").with("%target%", target.getName()).with("%reason%", reason).sendTo(sender);
                        if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
                            lang.get("ban.broadcast").with("%target%", target.getName()).with("%reason%", reason).with("%staff%", staffName)
                                    .sendToAll(plugin.getServer().getOnlinePlayers());
                        }
                    });
        });
    }

    private void banPlayerTemp(OfflinePlayer target, CommandSender sender, String duration) {
        banPlayerTemp(target, sender, duration, plugin.getConfig().getString("settings.default_ban_reason", "No reason"));
    }

    private void banPlayerTemp(OfflinePlayer target, CommandSender sender, String duration, String reason) {
        String staffName = sender.getName();
        plugin.getPunishmentManager().isBanned(target.getUniqueId()).thenAccept(isBanned -> {
            if (isBanned) { lang.get("ban.already_banned").with("%target%", target.getName()).sendTo(sender); return; }
            long expiresAt = DurationParser.parse(duration);
            plugin.getPunishmentManager().banPlayer(target.getUniqueId(), target.getName(), staffName, reason, expiresAt)
                    .thenAccept(p -> {
                        lang.get("ban.success").with("%target%", target.getName()).with("%reason%", reason).with("%expires%", DurationParser.format(expiresAt)).sendTo(sender);
                        if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
                            lang.get("ban.broadcast").with("%target%", target.getName()).with("%reason%", reason).with("%expires%", DurationParser.format(expiresAt)).with("%staff%", staffName)
                                    .sendToAll(plugin.getServer().getOnlinePlayers());
                        }
                    });
        });
    }

    @Override
    public List<Command> notCommands() {
        return List.of(this.build(), this.buildTemp(), this.buildUnban());
    }
}
