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
        Command cmd = Command.of("mute")
                        .setPermission("tenbatsu.mute")
                        .onExecute(c -> {
                            lang.get("mute.usage").sendTo(c.getSender());
                        });
        PlayerArg target = (PlayerArg) cmd.playerArg("target")
                        .onExecute(c -> {
                            OfflinePlayer t = c.getPlayer();
                            if (t == null) { lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender()); return; }
                            mutePlayer(t, c.getSender(), c.getSender().getName(), defaultReason(), -1);
                        });

        target.greedyStringArg("reason")
                        .onExecute(c -> {
                            OfflinePlayer t = target.get();
                            String reason = c.get();
                            if (t == null) { lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender()); return; }
                            mutePlayer(t, c.getSender(), c.getSender().getName(), reason, -1);
                        });

        return cmd;
    }

    public Command buildTemp() {
        Command cmd = Command.of("tempmute")
                .setPermission("tenbatsu.tempmute")
                .onExecute(c -> {
                    lang.get("mute.usage_duration").sendTo(c.getSender());
                });

        PlayerArg target = (PlayerArg) cmd.playerArg("target")
                        .onExecute(c -> {
                            OfflinePlayer t = c.getPlayer();
                            if (t == null) { lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender()); return; }
                            lang.get("general.specify_duration").sendTo(c.getSender());
                        });

        StringArg duration = (StringArg) target.stringArg("duration")
                        .onExecute(c -> {
                            OfflinePlayer t = target.get();
                            String d = c.get();
                            if (t == null) { lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender()); return; }
                            long expiresAt = DurationParser.parse(d);
                            if (expiresAt <= 0) { lang.get("general.invalid_duration").sendTo(c.getSender()); return; }
                            mutePlayer(t, c.getSender(), c.getSender().getName(), defaultReason(), expiresAt);
                        });

        duration.greedyStringArg("reason")
                        .onExecute(c -> {
                            OfflinePlayer t = target.get();
                            String d = duration.get();
                            String reason = c.get();
                            if (t == null) { lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender()); return; }
                            if (d == null) { lang.get("general.specify_duration").sendTo(c.getSender()); return; }
                            long expiresAt = DurationParser.parse(d);
                            if (expiresAt <= 0) { lang.get("general.invalid_duration").sendTo(c.getSender()); return; }
                            mutePlayer(t, c.getSender(), c.getSender().getName(), reason, expiresAt);
                        });

        return cmd;
    }

    public Command buildUnmute() {
        Command cmd = Command.of("unmute")
                .setPermission("tenbatsu.unmute")
                .onExecute(c -> {
                    lang.get("unmute.usage").sendTo(c.getSender());
                });

        cmd.playerArg("target")
                .onExecute(c -> {
                    OfflinePlayer t = c.get();
                    if (t == null) { lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender()); return; }
                    plugin.getPunishmentManager().unmutePlayer(t.getUniqueId()).thenAccept(unmuted -> {
                        if (unmuted) lang.get("mute.unmute_success").with("%target%", t.getName()).sendTo(c.getSender());
                        else lang.get("mute.not_muted").with("%target%", t.getName()).sendTo(c.getSender());
                    });
                });

        return cmd;
    }

    private void mutePlayer(OfflinePlayer target, CommandSender sender, String staffName, String reason, long expiresAt) {
        plugin.getPunishmentManager().getMute(target.getUniqueId()).thenAccept(activeMute -> {
            if (activeMute != null) { lang.get("mute.already_muted").with("%target%", target.getName()).sendTo(sender); return; }
            plugin.getPunishmentManager().mutePlayer(target.getUniqueId(), target.getName(), staffName, reason, expiresAt)
                    .thenAccept(p -> {
                        String key = expiresAt == -1 ? "mute.permanent" : "mute.success";
                        lang.get(key).with("%target%", target.getName()).with("%reason%", reason).sendTo(sender);
                        if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
                            lang.get("mute.broadcast").with("%target%", target.getName()).with("%reason%", reason).with("%staff%", staffName)
                                    .sendToAll(plugin.getServer().getOnlinePlayers());
                        }
                    });
        });
    }

    private String defaultReason() {
        return plugin.getConfig().getString("settings.default_reason", "No reason provided");
    }

    @Override
    public List<Command> notCommands() {
        return List.of(this.build(), this.buildTemp(), this.buildUnmute());
    }
}
