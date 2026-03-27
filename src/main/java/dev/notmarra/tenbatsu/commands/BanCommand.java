package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.command.arguments.PlayerArg;
import dev.notmarra.notlib.command.arguments.StringArg;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.utils.DurationParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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
        Command cmd = Command.of("ban", c -> {
                    if (!c.getSender().hasPermission("tenbatsu.tempban")) {
                        lang.get("general.no_permission").sendTo(c.getSender());
                    }
                });
        cmd.permission = "tenbatsu.ban";

        PlayerArg target = cmd.playerArg("target", arg -> {
            OfflinePlayer t = arg.getPlayer();
            if(t == null) {
                lang.get("general.specify_player").sendTo(arg.getSender());
            } else {
                banPlayer(t, (Player) arg.getSender());
            }
        });

        target.greedyStringArg("reason", arg -> {
            String r = arg.get();
            OfflinePlayer t = target.getPlayer();
            if(t == null) {
                lang.get("general.specify_player").sendTo(arg.getSender());
            } else {
                banPlayer(t, (Player) arg.getSender(), r);
            }
        });

        return cmd;
    }

    public Command buildTemp() {
        Command cmd = Command.of("tempban", c -> {
            if (!c.getSender().hasPermission("tenbatsu.tempban")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.tempban";

        PlayerArg target = cmd.playerArg("target", arg ->{
            OfflinePlayer t = arg.getPlayer();
            if(t == null) {
                lang.get("general.specify_player").sendTo(arg.getSender());
            } else {
                lang.get("tempban.specify_duration").sendTo(arg.getSender());
            }
        });

        StringArg duration = target.stringArg("duration", arg -> {
            String d = arg.get();
            OfflinePlayer t = target.getPlayer();
            if(t == null) {
                lang.get("general.specify_player").sendTo(arg.getSender());
            } else if (DurationParser.parse(d) <= 0) {
                lang.get("tempban.invalid_duration").sendTo(arg.getSender());
            } else {
                banPlayerTemp(t, (Player) arg.getSender(), d);
            }
        });

        duration.greedyStringArg("reason", arg -> {
            String r = arg.get();
            OfflinePlayer t = target.getPlayer();
            String d = duration.get();
            if(t == null) {
                lang.get("general.specify_player").sendTo(arg.getSender());
            } else if (DurationParser.parse(d) <= 0) {
                lang.get("tempban.invalid_duration").sendTo(arg.getSender());
            } else {
                banPlayerTemp(t, (Player) arg.getSender(), d, r);
            }
        });

        return cmd;
    }

    public Command buildUnban() {
        Command cmd = Command.of("unban", c -> {
            if (!c.getSender().hasPermission("tenbatsu.unban")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.unban";

        cmd.playerArg("target", arg -> {
            OfflinePlayer t = arg.getPlayer();
            if(t == null) {
                lang.get("general.specify_player").sendTo(arg.getSender());
            } else {
                plugin.getPunishmentManager()
                        .unbanPlayer(t.getUniqueId())
                        .thenAccept(unbanned -> {
                            if (unbanned) {
                                lang.get("unban.success").with("%target%", t.getName()).sendTo(arg.getSender());
                            } else {
                                lang.get("unban.not_banned").with("%target%", t.getName()).sendTo(arg.getSender());
                            }
                        });
            }
        });

        return cmd;
    }

    private void banPlayer(OfflinePlayer target, Player staff) {
        banPlayer(target, staff, plugin.getConfig().getString("default_ban_reason", "No reason"));
    }

    private void banPlayer(OfflinePlayer target, Player staff, String reason) {
        plugin.getPunishmentManager()
                .isBanned(target.getUniqueId())
                .thenAccept(isBanned -> {
                    if (isBanned) {
                        lang.get("ban.already_banned").with("%target%", target.getName()).sendTo(staff);
                        return;
                    }
                    plugin.getPunishmentManager()
                            .banPlayer(target.getUniqueId(), target.getName(), staff.getName(), reason, -1)
                            .thenAccept(p -> {
                                lang.get("ban.permanent")
                                        .with("%target%", target.getName())
                                        .with("%reason%", reason)
                                        .sendTo(staff);
                                if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
                                    lang.get("ban.broadcast")
                                            .with("%target%", target.getName())
                                            .with("%reason%", reason)
                                            .with("%staff%", staff.getName())
                                            .sendToAll(plugin.getServer().getOnlinePlayers());
                                }
                            });

                });
    }

    private void banPlayerTemp(OfflinePlayer target, Player staff, String duration) {
        banPlayerTemp(target, staff, duration, plugin.getConfig().getString("default_ban_reason", "No reason"));
    }

    private void banPlayerTemp(OfflinePlayer target, Player staff, String duration, String reason) {
        plugin.getPunishmentManager()
                .isBanned(target.getUniqueId())
                .thenAccept(isBanned -> {
                    if (isBanned) {
                        lang.get("ban.already_banned").with("%target%", target.getName()).sendTo(staff);
                        return;
                    }
                    long expiresAt = System.currentTimeMillis() + DurationParser.parse(duration);
                    plugin.getPunishmentManager()
                            .banPlayer(target.getUniqueId(), target.getName(), staff.getName(), reason, expiresAt)
                            .thenAccept(p -> {
                                lang.get("ban.success")
                                        .with("%target%", target.getName())
                                        .with("%reason%", reason)
                                        .with("%expires%", DurationParser.format(expiresAt))
                                        .sendTo(staff);
                                if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
                                    lang.get("ban.broadcast")
                                            .with("%target%", target.getName())
                                            .with("%reason%", reason)
                                            .with("%expires%", DurationParser.format(expiresAt))
                                            .with("%staff%", staff.getName())
                                            .sendToAll(plugin.getServer().getOnlinePlayers());
                                }
                            });

                });
    }

    @Override
    public String getId() {
        return "ban-commands-tenbatsu";
    }

    @Override
    public List<Command> notCommands() {
        return List.of(
                this.build(),
                this.buildTemp(),
                this.buildUnban()
        );
    }
}
