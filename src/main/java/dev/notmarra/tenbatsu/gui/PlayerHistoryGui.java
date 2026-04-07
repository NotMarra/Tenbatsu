package dev.notmarra.tenbatsu.gui;

import dev.notmarra.notlib.chat.Text;
import dev.notmarra.notlib.file.ManagedConfig;
import dev.notmarra.notlib.gui.GUI;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.models.Punishment;
import dev.notmarra.tenbatsu.models.PunishmentType;
import dev.notmarra.tenbatsu.utils.DurationParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PlayerHistoryGui {
    private final Tenbatsu plugin;
    private final ManagedConfig historyGui;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public PlayerHistoryGui(Tenbatsu plugin) {
        this.plugin = plugin;
        historyGui = plugin.getCfm().get("gui/history.yml");
    }

    public void open(Player staff, UUID targetUuid, String targetName) {
        open(staff, targetUuid, targetName, 0);
    }

    public void open(Player staff, UUID targetUuid, String targetName, int requestedPage) {
        // Fetch history async, then build and open GUI on main thread
        plugin.getPunishmentManager().getHistory(targetUuid).thenAccept(history -> {
            plugin.scheduler().global(() -> {
                String pattern = historyGui.getString("pattern", "");
                int pageSize = Math.max(1, getHistorySlotsPerPage(pattern));
                int totalPages = Math.max(1, (int) Math.ceil((double) history.size() / pageSize));
                int currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
                int startIndex = currentPage * pageSize;
                final int[] localHistoryIndex = {0};

                GUI gui = GUI.create(historyGui.getString("title", "Player History").replace("%player%", targetName));
                gui.pattern(pattern)
                        .emptySlotChars(List.of('#'))
                        .onPatternMatch(info -> {
                            if (info.ch == 'H') {
                                int idx = startIndex + localHistoryIndex[0]++;
                                if (idx < 0 || idx >= history.size()) {
                                    return gui.createItem(Material.AIR).name(" ");
                                }
                                Punishment p = history.get(idx);
                                String typeKey = p.getType().name().toLowerCase();
                                Material material = Material.getMaterial(historyGui.getString("types." + typeKey + ".item", "PAPER"));
                                if (material == null) {
                                    material = materialForType(p.getType());
                                }

                                String expires = DurationParser.format(p.getExpiresAt());
                                String active = (p.isActive() && !p.isExpired()) ? "Yes" : "No";

                                return gui.createItem(material)
                                        .name(
                                                Text.of(
                                                        historyGui.getString("H.name", "")
                                                                .replace("%type%", p.getType().name())
                                                                .replace("%staff%", p.getStaffName())
                                                                .replace("%reason%", p.getReason())
                                                ).buildString()
                                        )
                                        .lore(
                                                historyGui.getString("H.lore", "")
                                                        .replace("%reason%", p.getReason())
                                                        .replace("%staff%", p.getStaffName())
                                                        .replace("%date%", DATE_FMT.format(new Date(p.getIssuedAt() * 1000)))
                                                        .replace("%expires%", expires)
                                                        .replace("%active%", active)
                                        );
                            }
                            if (info.ch == 'C') {
                                return gui.createItem(Material.getMaterial(historyGui.getString("C.item", "BARRIER")))
                                        .name(Text.of(historyGui.getString("C.name", "")).buildString())
                                        .lore(Text.of(historyGui.getString("C.lore", "")))
                                        .action((event, container) -> event.getWhoClicked().closeInventory());
                            }
                            if (info.ch == 'B') {
                                return gui.createItem(Material.getMaterial(historyGui.getString("B.item", "ARROW")))
                                        .name(Text.of(historyGui.getString("B.name", "")).buildString())
                                        .lore(Text.of(historyGui.getString("B.lore", "")))
                                        .action((event, container) -> {
                                            if (currentPage > 0) {
                                                open(staff, targetUuid, targetName, currentPage - 1);
                                                return;
                                            }
                                            new ModerationMenu(plugin).open(staff);
                                        });
                            }
                            if (info.ch == 'N') {
                                if (currentPage >= totalPages - 1) {
                                    return gui.createItem(Material.AIR).name(" ");
                                }
                                return gui.createItem(Material.getMaterial(historyGui.getString("N.item", "ARROW")))
                                        .name(Text.of(historyGui.getString("N.name", "")).buildString())
                                        .lore(Text.of(historyGui.getString("N.lore", "")))
                                        .action((event, container) -> {
                                            if (currentPage < totalPages - 1) {
                                                open(staff, targetUuid, targetName, currentPage + 1);
                                            }
                                        });
                            }
                            return null;
                        });

                gui.open(staff);
            });
        });
    }

    private int getHistorySlotsPerPage(String pattern) {
        int count = 0;
        for (char c : pattern.toCharArray()) {
            if (c == 'H') count++;
        }
        return count;
    }

    private Material materialForType(PunishmentType type) {
        return switch (type) {
            case BAN -> Material.BARRIER;
            case IP_BAN -> Material.NETHER_STAR;
            case MUTE -> Material.NAME_TAG;
            case WARN -> Material.YELLOW_DYE;
            case KICK -> Material.LEATHER_BOOTS;
        };
    }
}
