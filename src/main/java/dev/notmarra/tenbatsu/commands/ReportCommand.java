package dev.notmarra.tenbatsu.commands;

import dev.notmarra.notlib.command.Command;
import dev.notmarra.notlib.command.arguments.PlayerArg;
import dev.notmarra.notlib.extensions.CommandGroup;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.gui.ReportsGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class ReportCommand extends CommandGroup {
    private final Tenbatsu plugin;
    private final LanguageManager lang;

    public ReportCommand(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
        this.lang = plugin.getLang();
    }

    public Command buildReport() {
        Command cmd = Command.of("report")
                        .setPermission("tenbatsu.report")
                        .onExecute(c -> {
                            lang.get("report.usage").sendTo(c.getSender());
                        });

        PlayerArg target = (PlayerArg) cmd.playerArg("target")
                        .onExecute(c -> {
                            OfflinePlayer t = c.getPlayer();
                            if (t == null) {
                                lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender());
                                return;
                            }
                            lang.get("report.no_reason").sendTo(c.getSender());
                        });

        target.greedyStringArg("reason")
                .onExecute(c -> {
                    OfflinePlayer t = target.get();
                    String reason = c.get();
                    if (t == null) {
                        lang.get("general.player_not_found").with("%target%", "unknown").sendTo(c.getSender());
                        return;
                    }
                    if (!(c.getSender() instanceof Player reporter)) {
                        lang.get("general.console_only").sendTo(c.getSender());
                        return;
                    }
                    // Prevent self-reporting
                    if (t.getUniqueId().equals(reporter.getUniqueId())) {
                        lang.get("report.cannot_report_self").sendTo(reporter);
                        return;
                    }
                    if (reason == null || reason.isBlank()) {
                        lang.get("report.no_reason").sendTo(reporter);
                        return;
                    }
                    if (!plugin.getConfig().getBoolean("settings.report_offline", false) && !t.isOnline()) {
                        lang.get("general.player_not_found").with("%target%", t.getName()).sendTo(reporter);
                        return;
                    }
                    if (plugin.getReportManager().isOnCooldown(reporter.getUniqueId())) {
                        long remaining = plugin.getReportManager().getCooldownRemaining(reporter.getUniqueId());
                        lang.get("report.cooldown").with("%time%", remaining).sendTo(reporter);
                        return;
                    }
                    if (plugin.getReportManager().hasReachedMax(reporter.getUniqueId())) {
                        lang.get("report.max_reached").sendTo(reporter);
                        return;
                    }
                    plugin.getReportManager().submitReport(
                            reporter.getUniqueId(), reporter.getName(),
                            t.getUniqueId(), t.getName(), reason
                    ).thenAccept(report -> {
                        plugin.getReportManager().recordReport(reporter.getUniqueId());
                        lang.get("report.sent").with("%target%", t.getName()).sendTo(reporter);
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (online.hasPermission("tenbatsu.reports")) {
                                lang.get("report.staff_alert")
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
        Command cmd = Command.of("reports")
                        .setPermission("tenbatsu.reports");

        cmd.onExecute(c -> {
            if (!(c.getSender() instanceof Player player)) {
                lang.get("general.console_only").sendTo(c.getSender());
                return;
            }
            new ReportsGui(plugin).open(player);
        });

        return cmd;
    }

    public Command buildClearReports() {
        Command cmd = Command.of("clearreports")
                        .setPermission("tenbatsu.clearreports");

        cmd.onExecute(c -> plugin.getReportManager().clearAllReports()
                .thenRun(() -> lang.get("report.cleared").sendTo(c.getSender())));

        return cmd;
    }

    @Override
    public List<Command> notCommands() {
        return List.of(this.buildReport(), this.buildReports(), this.buildClearReports());
    }
}
