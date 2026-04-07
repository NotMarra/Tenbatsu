package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.tenbatsu.Tenbatsu;
import org.bukkit.entity.Player;

import java.util.List;

public class StaffChatCommand extends CommandGroup {
    private final Tenbatsu plugin;

    public StaffChatCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public Command buildToggle() {
        return Command.of("staffchat")
                .setPermission("tenbatsu.staffchat")
                .onExecute(c -> {
                    if (!(c.getSender() instanceof Player player)) {
                        plugin.getLang().get("general.console_only").sendTo(c.getSender());
                        return;
                    }

                    plugin.getStaffChatManager().toggle(player.getUniqueId());
                    boolean enabled = plugin.getStaffChatManager().isInStaffChat(player.getUniqueId());
                    plugin.getLang().get(enabled ? "staffchat.toggle_on" : "staffchat.toggle_off").sendTo(player);
                });
    }

    public Command buildSend() {
        Command cmd = Command.of("sc")
                        .setPermission("tenbatsu.staffchat")
                        .onExecute(c -> {
                            plugin.getLang().get("staffchat.usage").sendTo(c.getSender());
                        });

        cmd.greedyStringArg("message")
                .onExecute(arg -> {
                    if (!(arg.getSender() instanceof Player player)) {
                        plugin.getLang().get("general.console_only").sendTo(arg.getSender());
                        return;
                    }

                    String msg = arg.get();
                    if (msg == null || msg.isBlank()) {
                        return;
                    }
                    plugin.getStaffChatManager().sendStaffMessage(player, msg);
                });

        return cmd;
    }

    @Override
    public List<Command> notCommands() {
        return List.of(
                this.buildToggle(),
                this.buildSend()
        );
    }
}
