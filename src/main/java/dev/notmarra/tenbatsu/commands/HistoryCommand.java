package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.gui.PlayerHistoryGui;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryCommand extends CommandGroup {
    private final Tenbatsu plugin;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public HistoryCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public Command build() {
        Command cmd = Command.of("history")
                        .setPermission("tenbatsu.history")
                        .onExecute(c -> {
                            plugin.getLang().get("history.usage").sendTo(c.getSender());
                        });

        cmd.playerArg("target")
                .onExecute(c -> {
                    OfflinePlayer target = c.get();
                    if (target == null) {
                        plugin.getLang().get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender());
                        return;
                    }

                    if (c.getSender() instanceof Player staff) {
                        new PlayerHistoryGui(plugin).open(staff, target.getUniqueId(), target.getName());
                        return;
                    }

                    sendConsoleHistory(c.getSender(), target);
                });

        return cmd;
    }

    private void sendConsoleHistory(CommandSender sender, OfflinePlayer target) {
        plugin.getPunishmentManager().getHistory(target.getUniqueId()).thenAccept(history -> {
            if (history.isEmpty()) {
                plugin.getLang().get("history.empty").with("%target%", target.getName()).sendTo(sender);
                return;
            }
            plugin.getLang().get("history.header").with("%target%", target.getName()).sendTo(sender);
            history.forEach(p -> plugin.getLang().get("history.entry")
                    .with("%id%", p.getId())
                    .with("%type%", p.getType().name())
                    .with("%staff%", p.getStaffName())
                    .with("%reason%", p.getReason())
                    .with("%date%", DATE_FMT.format(new Date(p.getIssuedAt() * 1000)))
                    .with("%expires%", p.getExpiresAt() == -1 ? "Never" : DATE_FMT.format(new Date(p.getExpiresAt() * 1000)))
                    .sendTo(sender));
        });
    }

    @Override
    public List<Command> notCommands() { return List.of(this.build()); }
}
