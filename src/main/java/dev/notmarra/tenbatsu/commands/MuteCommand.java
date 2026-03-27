package dev.notmarra.tenbatsu.commands;

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

public class MuteCommand extends CommandGroup {
    private final Tenbatsu plugin;
    private final LanguageManager lang;

    public MuteCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
        this.lang = plugin.getLang();
    }

    public Command build() {
        Command cmd = Command.of("mute", c -> {
            if (!c.getSender().hasPermission("tenbatsu.mute")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.mute";

        PlayerArg target = cmd.playerArg("target", arg -> {
            OfflinePlayer t = arg.getPlayer();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }
            mutePlayer(t, arg.getSender(), arg.getSender().getName(), defaultReason(), -1);
        });

        target.greedyStringArg("reason", arg -> {
            String reason = arg.get();
            OfflinePlayer t = target.getPlayer();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }
            mutePlayer(t, arg.getSender(), arg.getSender().getName(), reason, -1);
        });

        return cmd;
    }

    public Command buildTemp() {
        Command cmd = Command.of("tempmute", c -> {
            if (!c.getSender().hasPermission("tenbatsu.tempmute")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.tempmute";

        PlayerArg target = cmd.playerArg("target", arg -> {
            if (arg.getPlayer() == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
            }
        });

        StringArg duration = target.stringArg("duration", arg -> {
            String d = arg.get();
            OfflinePlayer t = target.getPlayer();
            long expiresAt = DurationParser.parse(d);
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }
            if (expiresAt <= 0) {
                lang.get("general.invalid_duration").sendTo(arg.getSender());
                return;
            }
            mutePlayer(t, arg.getSender(), arg.getSender().getName(), defaultReason(), expiresAt);
        });

        duration.greedyStringArg("reason", arg -> {
            String reason = arg.get();
            OfflinePlayer t = target.getPlayer();
            long expiresAt = DurationParser.parse(duration.get());
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }
            if (expiresAt <= 0) {
                lang.get("general.invalid_duration").sendTo(arg.getSender());
                return;
            }
            mutePlayer(t, arg.getSender(), arg.getSender().getName(), reason, expiresAt);
        });

        return cmd;
    }

    public Command buildUnmute() {
        Command cmd = Command.of("unmute", c -> {
            if (!c.getSender().hasPermission("tenbatsu.unmute")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.unmute";

        cmd.playerArg("target", arg -> {
            OfflinePlayer t = arg.getPlayer();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }

            plugin.getPunishmentManager().unmutePlayer(t.getUniqueId()).thenAccept(unmuted -> {
                if (unmuted) {
                    lang.get("mute.unmute_success").with("%target%", t.getName()).sendTo(arg.getSender());
                } else {
                    lang.get("mute.not_muted").with("%target%", t.getName()).sendTo(arg.getSender());
                }
            });
        });

        return cmd;
    }

    private void mutePlayer(OfflinePlayer target, CommandSender sender, String staffName, String reason, long expiresAt) {
        plugin.getPunishmentManager().getMute(target.getUniqueId()).thenAccept(activeMute -> {
            if (activeMute != null) {
                lang.get("mute.already_muted").with("%target%", target.getName()).sendTo(sender);
                return;
            }

            plugin.getPunishmentManager()
                    .mutePlayer(target.getUniqueId(), target.getName(), staffName, reason, expiresAt)
                    .thenAccept(p -> {
                        String key = expiresAt == -1 ? "mute.permanent" : "mute.success";
                        lang.get(key)
                                .with("%target%", target.getName())
                                .with("%reason%", reason)
                                .sendTo(sender);

                        if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
                            lang.get("mute.broadcast")
                                    .with("%target%", target.getName())
                                    .with("%reason%", reason)
                                    .with("%staff%", staffName)
                                    .sendToAll(plugin.getServer().getOnlinePlayers());
                        }
                    });
        });
    }

    private String defaultReason() {
        return plugin.getConfig().getString("settings.default_reason", "No reason provided");
    }

    @Override
    public String getId() {
        return "mute-command-tenbatsu";
    }

    @Override
    public List<Command> notCommands() {
        return List.of(
                this.build(),
                this.buildTemp(),
                this.buildUnmute()
        );
    }
}
