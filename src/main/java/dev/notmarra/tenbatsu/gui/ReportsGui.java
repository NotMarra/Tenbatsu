package dev.notmarra.tenbatsu.gui;

import dev.notmarra.notlib.chat.Text;
import dev.notmarra.notlib.file.ManagedConfig;
import dev.notmarra.notlib.gui.GUI;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.models.Report;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReportsGui {

    private final Tenbatsu plugin;
    private final ManagedConfig reportGui;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM HH:mm");

    public ReportsGui(Tenbatsu plugin) {
        this.plugin = plugin;
        this.reportGui = plugin.getCfm().get("gui/report.yml");
    }

    public void open(Player staff) {
        open(staff, 0);
    }

    private void open(Player staff, int requestedPage) {
        plugin.getReportManager().getPendingReports().thenAccept(reports -> plugin.scheduler().async(() -> {
            String pattern = reportGui.getString("pattern", "");
            int pageSize = Math.max(1, getReportSlotsPerPage(pattern));
            int totalPages = Math.max(1, (int) Math.ceil((double) reports.size() / pageSize));
            int currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
            int startIndex = currentPage * pageSize;
            final int[] localReportIndex = {0};

            GUI gui = GUI.create(reportGui.getString("title", "Reports"));
            gui.pattern(pattern)
                    .emptySlotChars(List.of('#'))
                    .onPatternMatch( info -> {
                        switch (info.ch){
                            case 'R' -> {
                                int idx = startIndex + localReportIndex[0]++;
                                if (idx < 0 || idx >= reports.size()) {
                                    return gui.createItem(Material.AIR).name(" ");
                                }
                                Report report = reports.get(idx);
                                return gui.createItem(Material.getMaterial(reportGui.getString("R.item", "PAPER")))
                                        .name(
                                                Text.of(
                                                        reportGui.getString("R.name", "")
                                                                .replace("%report_id%", String.valueOf(report.getId()))
                                                                .replace("%player%", report.getTargetName())
                                                ).buildString()
                                        )
                                        .lore(
                                                reportGui.getString("R.lore", "")
                                                        .replace("%reporter_name%", report.getReporterName())
                                                        .replace("%target%", report.getTargetName())
                                                        .replace("%reason%", report.getReason())
                                                        .replace("%date%", DATE_FMT.format(new Date(report.getReportedAt() * 1000)))
                                        )
                                        .action((event, container) -> {
                                            if(event.isRightClick()) {
                                                Player target = Bukkit.getPlayer(report.getTargetUuid());
                                                if(target != null) {
                                                    staff.teleport(target.getLocation());
                                                    plugin.getLang().get("general.teleported_successfully").withPlayer(target).sendTo(staff);
                                                } else {
                                                    plugin.getLang().get("general.player_not_found")
                                                            .with("%target%", report.getTargetName()).sendTo(staff);
                                                }
                                            } else  {
                                                plugin.getReportManager().markHandled(report.getId(), staff.getName());
                                                plugin.getLang().get("marked_as_handled").with("%report_id%", report.getId()).sendTo(staff);
                                                open(staff, currentPage);
                                            }
                                        });
                            }
                            case 'C' -> {
                                return gui.createItem(Material.getMaterial(reportGui.getString("C.item", "BARRIER")))
                                        .name(
                                                Text.of(reportGui.getString("C.name", "")).toString()
                                        )
                                        .lore(
                                                Text.of(reportGui.getString("C.lore", ""))
                                        )
                                        .action((event,container) -> event.getWhoClicked().closeInventory());
                            }
                            case 'B' -> {
                                return gui.createItem(Material.getMaterial(reportGui.getString("B.item", "ARROW")))
                                        .name(
                                                Text.of(reportGui.getString("B.name", "")).toString()
                                        )
                                        .lore(
                                                Text.of(reportGui.getString("B.lore", ""))
                                        )
                                        .action((event,container) ->{
                                            if (currentPage > 0) {
                                                open(staff, currentPage - 1);
                                                return;
                                            }
                                            new ModerationMenu(plugin).open(staff);
                                        });
                            }
                            case 'N' -> {
                                if (currentPage >= totalPages - 1) {
                                    return gui.createItem(Material.AIR).name(" ");
                                }

                                return gui.createItem(Material.getMaterial(reportGui.getString("N.item", "ARROW")))
                                        .name(
                                                Text.of(reportGui.getString("N.name", "")).toString()
                                        )
                                        .lore(
                                                Text.of(reportGui.getString("N.lore", ""))
                                        )
                                        .action((event,container) ->{
                                            if (currentPage < totalPages - 1) {
                                                open(staff, currentPage + 1);
                                            }
                                        });
                            }
                        }

                        return null;
                    });

            gui.open(staff);
        }));
    }

    private int getReportSlotsPerPage(String pattern) {
        int slots = 0;
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == 'R') {
                slots++;
            }
        }
        return slots;
    }
}