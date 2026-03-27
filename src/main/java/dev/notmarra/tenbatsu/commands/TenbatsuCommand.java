package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.gui.ModerationMenu;
import org.bukkit.entity.Player;

import java.util.List;

public class TenbatsuCommand extends CommandGroup {
    private final Tenbatsu plugin;

    public TenbatsuCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public Command build() {
        Command cmd = Command.of("tenbatsu", c -> {
            if (!c.getSender().hasPermission("tenbatsu.use")) {
                plugin.getLang().get("general.no_permission").sendTo(c.getSender());
                return;
            }
            sendHelp(c.getSender());
        });

        cmd.permission = "tenbatsu.use";

        cmd.stringArg("action", arg -> {
            String action = arg.get();
            if (action == null) {
                sendHelp(arg.getSender());
                return;
            }

            switch (action.toLowerCase()) {
                case "menu" -> {
                    if (!arg.getSender().hasPermission("tenbatsu.menu")) {
                        plugin.getLang().get("general.no_permission").sendTo(arg.getSender());
                        return;
                    }
                    if (!(arg.getSender() instanceof Player player)) {
                        plugin.getLang().get("general.console_only").sendTo(arg.getSender());
                        return;
                    }
                    new ModerationMenu(plugin).open(player);
                }
                case "reload" -> {
                    if (!arg.getSender().hasPermission("tenbatsu.reload")) {
                        plugin.getLang().get("general.no_permission").sendTo(arg.getSender());
                        return;
                    }
                    try {
                        plugin.reloadPluginData();
                        plugin.getLang().get("tenbatsu.reload.success").sendTo(arg.getSender());
                    } catch (Exception ex) {
                        plugin.getLang().get("tenbatsu.reload.failed")
                                .with("%error%", ex.getMessage() == null ? "unknown" : ex.getMessage())
                                .sendTo(arg.getSender());
                    }
                }
                case "help" -> sendHelp(arg.getSender());
                default -> sendHelp(arg.getSender());
            }
        });

        return cmd;
    }

    private void sendHelp(org.bukkit.command.CommandSender sender) {
        plugin.getLang().get("tenbatsu.help.header").sendTo(sender);
        String lines = plugin.getLang().get("tenbatsu.help.lines").toString();
        if (lines != null && !lines.isBlank()) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(lines));
        }
    }

    @Override
    public String getId() {
        return "tenbatsu-command-tenbatsu";
    }

    @Override
    public List<Command> notCommands() {
        return List.of(this.build());
    }
}
