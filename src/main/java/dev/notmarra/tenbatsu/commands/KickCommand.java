package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.command.arguments.PlayerArg;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class KickCommand extends CommandGroup {
    private final Tenbatsu plugin;
    private final LanguageManager lang;

    public KickCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
        this.lang = plugin.getLang();
    }

    public Command build() {
        Command cmd = Command.of("kick", c -> {
            if (!c.getSender().hasPermission("tenbatsu.kick")) {
                lang.get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.kick";

        PlayerArg target = cmd.playerArg("target", arg -> {
            OfflinePlayer t = arg.get();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }
            if (!kickTarget(t, arg.getSender().getName(), defaultReason())) {
                lang.get("general.player_not_found").with("%target%", t.getName()).sendTo(arg.getSender());
                return;
            }
            lang.get("kick.success").with("%target%", t.getName()).sendTo(arg.getSender());
        });

        target.greedyStringArg("reason", arg -> {
            String reason = arg.get();
            OfflinePlayer t = target.getPlayer();
            if (t == null) {
                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(arg.getSender());
                return;
            }
            if (!kickTarget(t, arg.getSender().getName(), reason)) {
                lang.get("general.player_not_found").with("%target%", t.getName()).sendTo(arg.getSender());
                return;
            }
            lang.get("kick.success").with("%target%", t.getName()).sendTo(arg.getSender());
        });

        return cmd;
    }

    private boolean kickTarget(OfflinePlayer target, String staffName, String reason) {
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online == null) {
            return false;
        }

        plugin.getPunishmentManager().kickPlayer(online, staffName, reason);

        if (plugin.getConfig().getBoolean("settings.broadcast_punishments", false)) {
            lang.get("kick.broadcast")
                    .with("%target%", target.getName())
                    .with("%reason%", reason)
                    .with("%staff%", staffName)
                    .sendToAll(plugin.getServer().getOnlinePlayers());
        }
        return true;
    }

    private String defaultReason() {
        return plugin.getConfig().getString("settings.default_reason", "No reason provided");
    }

    @Override
    public String getId() {
        return "kick-command-tenbatsu";
    }

    @Override
    public List<Command> notCommands() {
        return List.of(this.build());
    }
}
