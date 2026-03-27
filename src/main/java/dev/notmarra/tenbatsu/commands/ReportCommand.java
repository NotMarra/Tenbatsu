package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.command.arguments.PlayerArg;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.gui.ReportsGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class ReportCommand extends CommandGroup {
    private final Tenbatsu plugin;

    public ReportCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public Command buildReport() {
        Command cmd = Command.of("report", c -> {
            if (!(c.getSender() instanceof Player)) {
                plugin.getLang().get("general.console_only").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.report";

        PlayerArg target = cmd.playerArg("target", arg -> {
            if (!(arg.getSender() instanceof Player reporter)) {
                plugin.getLang().get("general.console_only").sendTo(arg.getSender());
                return;
            }

            OfflinePlayer t = arg.getPlayer();
            if (t == null) {
                plugin.getLang().get("general.player_not_found").with("%target%", "unknown").sendTo(reporter);
                return;
            }

            plugin.getLang().get("report.no_reason").sendTo(reporter);
        });

        target.greedyStringArg("reason", arg -> {
            if (!(arg.getSender() instanceof Player reporter)) {
                plugin.getLang().get("general.console_only").sendTo(arg.getSender());
                return;
            }

            OfflinePlayer t = target.getPlayer();
            String reason = arg.get();
            if (t == null) {
                plugin.getLang().get("general.player_not_found").with("%target%", "unknown").sendTo(reporter);
                return;
            }

            if (reason == null || reason.isBlank()) {
                plugin.getLang().get("report.no_reason").sendTo(reporter);
                return;
            }

            if (!plugin.getConfig().getBoolean("settings.report_offline", false) && !t.isOnline()) {
                plugin.getLang().get("general.player_not_found").with("%target%", t.getName()).sendTo(reporter);
                return;
            }

            if (plugin.getReportManager().isOnCooldown(reporter.getUniqueId())) {
                long remaining = plugin.getReportManager().getCooldownRemaining(reporter.getUniqueId());
                plugin.getLang().get("report.cooldown")
                        .with("%time%", remaining)
                        .sendTo(reporter);
                return;
            }

            if (plugin.getReportManager().hasReachedMax(reporter.getUniqueId())) {
                plugin.getLang().get("report.max_reached").sendTo(reporter);
                return;
            }

            plugin.getReportManager().submitReport(
                    reporter.getUniqueId(),
                    reporter.getName(),
                    t.getUniqueId(),
                    t.getName(),
                    reason
            ).thenAccept(report -> {
                plugin.getReportManager().recordReport(reporter.getUniqueId());
                plugin.getLang().get("report.sent")
                        .with("%target%", t.getName())
                        .sendTo(reporter);

                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("tenbatsu.reports")) {
                        plugin.getLang().get("report.staff_alert")
                                .with("%reporter%", reporter.getName())
                                .with("%target%", t.getName())
                                .with("%reason%", reason)
                                .sendTo(online);
                    }
                }
            });
        });

        return cmd;
    }

    public Command buildReports() {
        Command cmd = Command.of("reports", c -> {
            if (!c.getSender().hasPermission("tenbatsu.reports")) {
                plugin.getLang().get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.reports";

        cmd.onExecute(c -> {
            if (!(c.getSender() instanceof Player player)) {
                plugin.getLang().get("general.console_only").sendTo(c.getSender());
                return;
            }
            new ReportsGui(plugin).open(player);
        });

        return cmd;
    }

    public Command buildClearReports() {
        Command cmd = Command.of("clearreports", c -> {
            if (!c.getSender().hasPermission("tenbatsu.reports.clear")) {
                plugin.getLang().get("general.no_permission").sendTo(c.getSender());
            }
        });
        cmd.permission = "tenbatsu.reports.clear";

        cmd.onExecute(c -> plugin.getReportManager().clearAllReports().thenRun(() ->
                plugin.getLang().get("report.cleared").sendTo(c.getSender())
        ));

        return cmd;
    }

    @Override
    public String getId() {
        return "report-command-tenbatsu";
    }

    @Override
    public List<Command> notCommands() {
        return List.of(
                this.buildReport(),
                this.buildReports(),
                this.buildClearReports()
        );
    }
}
