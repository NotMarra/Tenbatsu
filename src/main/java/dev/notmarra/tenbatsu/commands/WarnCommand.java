package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.command.arguments.PlayerArg;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;

public class WarnCommand extends CommandGroup {
    private final Tenbatsu plugin;
    private final LanguageManager lang;

    public WarnCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
        this.lang = plugin.getLang();
    }

    public Command build() {
        Command cmd = Command.of("warn", c -> {
            if (!c.getSender().hasPermission("tenbatsu.warn")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.warn";

        PlayerArg target = cmd.playerArg("target", arg -> {
            OfflinePlayer t = arg.getPlayer();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }

            if (!(arg.getSender() instanceof Player staff)) {
                lang.get("general.console_only").sendTo(arg.getSender());
                return;
            }

            warnPlayer(t, staff);
        });

        target.greedyStringArg("reason", arg -> {
            String reason = arg.get();
            OfflinePlayer t = target.getPlayer();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }

            if (!(arg.getSender() instanceof Player staff)) {
                lang.get("general.console_only").sendTo(arg.getSender());
                return;
            }

            warnPlayer(t, staff, reason);
        });

        return cmd;
    }

    public Command buildClearWarnings() {
        Command cmd = Command.of("clearwarns", c -> {
            if (!c.getSender().hasPermission("tenbatsu.clearwarns")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.clearwarns";

        cmd.playerArg("target", arg -> {
            OfflinePlayer t = arg.getPlayer();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }

            plugin.getPunishmentManager().getWarnCount(t.getUniqueId()).thenAccept(count -> {
                if (count <= 0) {
                    lang.get("warn.no_warnings").with("%target%", t.getName()).sendTo(arg.getSender());
                    return;
                }

                plugin.getPunishmentManager().clearWarnings(t.getUniqueId()).thenRun(() ->
                        lang.get("warn.cleared").with("%target%", t.getName()).sendTo(arg.getSender())
                );
            });
        });

        return cmd;
    }

    private void warnPlayer(OfflinePlayer target, Player staff) {
        String defaultReason = plugin.getConfig().getString("settings.default_reason", "No reason provided");
        warnPlayer(target, staff, defaultReason);
    }

    private void warnPlayer(OfflinePlayer target, Player staff, String reason) {
        plugin.getPunishmentManager()
                .warnPlayer(target.getUniqueId(), target.getName(), staff.getName(), reason)
                .thenAccept(count -> {
                    int maxWarnings = plugin.getConfig().getInt("settings.max_warnings", 3);

                    lang.get("warn.success")
                            .with("%target%", target.getName())
                            .with("%count%", count)
                            .with("%max%", maxWarnings)
                            .sendTo(staff);

                    Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                    if (onlineTarget != null) {
                        lang.get("warn.received")
                                .with("%staff%", staff.getName())
                                .with("%reason%", reason)
                                .with("%count%", count)
                                .with("%max%", maxWarnings)
                                .sendTo(onlineTarget);
                    }

                    if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
                        lang.get("warn.broadcast")
                                .with("%target%", target.getName())
                                .with("%reason%", reason)
                                .with("%staff%", staff.getName())
                                .sendToAll(plugin.getServer().getOnlinePlayers());
                    }

                    if (count < maxWarnings) {
                        return;
                    }

                    long autoBanDurationMinutes = plugin.getConfig().getLong("settings.auto_ban_duration", -1);
                    long expiresAt = autoBanDurationMinutes < 0
                            ? -1
                            : Instant.now().getEpochSecond() + (autoBanDurationMinutes * 60);

                    plugin.getPunishmentManager()
                            .banPlayer(target.getUniqueId(), target.getName(), "SYSTEM", "Reached max warnings", expiresAt)
                            .thenAccept(p -> {
                                lang.get("warn.auto_ban")
                                        .with("%target%", target.getName())
                                        .with("%max%", maxWarnings)
                                        .sendToAll(plugin.getServer().getOnlinePlayers());

                                plugin.getPunishmentManager().clearWarnings(target.getUniqueId());
                            });
                });
    }

    @Override
    public String getId() {
        return "warn-command-tenbatsu";
    }

    @Override
    public List<Command> notCommands() {
        return List.of(
                this.build(),
                this.buildClearWarnings()
        );
    }
}
