package dev.notmarra.tenbatsu.gui;

import dev.notmarra.notlib.file.ManagedConfig;
import dev.notmarra.notlib.gui.GUI;
import dev.notmarra.tenbatsu.Tenbatsu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ModerationMenu {
    private final Tenbatsu plugin;
    private final ManagedConfig moderationGui;

    public ModerationMenu(Tenbatsu plugin) {
        this.plugin = plugin;
        this.moderationGui = plugin.getCfm().get("gui/moderation.yml");
    }

    public void open(Player staff) {
        open(staff, 0);
    }

    public void open(Player staff, int requestedPage) {
        // Must run on main thread - inventory operations require it
        plugin.scheduler().global(() -> {
            List<Player> onlinePlayers = new ArrayList<>();
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.equals(staff)) {
                    onlinePlayers.add(online);
                }
            }

            String pattern = moderationGui.getString("pattern", "");
            int pageSize = Math.max(1, getPlayerSlotsPerPage(pattern));
            int totalPages = Math.max(1, (int) Math.ceil((double) onlinePlayers.size() / pageSize));
            int currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
            int startIndex = currentPage * pageSize;
            final int[] localPlayerIndex = {0};

            GUI gui = GUI.create(moderationGui.getString("title", "Moderation Menu"));
            gui.pattern(pattern)
                    .emptySlotChars(List.of('#'))
                    .onPatternMatch(info -> {
                        if (info.ch == 'P') {
                            int idx = startIndex + localPlayerIndex[0]++;
                            if (idx < 0 || idx >= onlinePlayers.size()) {
                                return gui.createItem(Material.AIR).name(" ");
                            }
                            Player target = onlinePlayers.get(idx);
                            return gui.createItem(Material.PLAYER_HEAD)
                                    .skullOwner(target.getName())
                                    .name(moderationGui.getString("P.name", "").replace("%player%", target.getName()))
                                    .lore(moderationGui.getString("P.lore", ""))
                                    .action((event, container) -> {
                                        if (event.isRightClick()) {
                                            new PlayerHistoryGui(plugin).open(staff, target.getUniqueId(), target.getName());
                                        } else {
                                            new PlayerActionGui(plugin).open(staff, target);
                                        }
                                    });
                        }
                        if (info.ch == 'R') {
                            return gui.createItem(Material.getMaterial(moderationGui.getString("R.item", "PAPER")))
                                    .name(moderationGui.getString("R.name", "Refresh"))
                                    .action((event, container) -> new ReportsGui(plugin).open(staff));
                        }
                        if (info.ch == 'C') {
                            return gui.createItem(Material.getMaterial(moderationGui.getString("C.item", "BARRIER")))
                                    .name(moderationGui.getString("C.name", "Close"))
                                    .lore(moderationGui.getString("C.lore", ""))
                                    .action((event, container) -> event.getWhoClicked().closeInventory());
                        }
                        if (info.ch == 'B') {
                            return gui.createItem(Material.getMaterial(moderationGui.getString("B.item", "ARROW")))
                                    .name(moderationGui.getString("B.name", "Back"))
                                    .lore(moderationGui.getString("B.lore", ""))
                                    .action((event, container) -> {
                                        if (currentPage > 0) {
                                            open(staff, currentPage - 1);
                                            return;
                                        }
                                        event.getWhoClicked().closeInventory();
                                    });
                        }
                        if (info.ch == 'N') {
                            if (currentPage >= totalPages - 1) {
                                return gui.createItem(Material.AIR).name(" ");
                            }
                            return gui.createItem(Material.getMaterial(moderationGui.getString("N.item", "ARROW")))
                                    .name(moderationGui.getString("N.name", "Next"))
                                    .lore(moderationGui.getString("N.lore", ""))
                                    .action((event, container) -> {
                                        if (currentPage < totalPages - 1) {
                                            open(staff, currentPage + 1);
                                        }
                                    });
                        }

                        return null;
                    });

            gui.open(staff);
        });
    }

    private int getPlayerSlotsPerPage(String pattern) {
        int count = 0;
        for (char c : pattern.toCharArray()) {
            if (c == 'P') count++;
        }
        return count;
    }
}
