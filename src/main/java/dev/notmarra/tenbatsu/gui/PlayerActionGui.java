package dev.notmarra.tenbatsu.gui;

import dev.notmarra.notlib.file.ManagedConfig;
import dev.notmarra.notlib.gui.GUI;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.utils.DurationParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerActionGui {
    private final Tenbatsu plugin;
    private final ManagedConfig actionGui;

    public PlayerActionGui(Tenbatsu plugin) {
        this.plugin = plugin;
        this.actionGui = plugin.getCfm().get("gui/player_action.yml");
    }

    public void open(Player staff, Player target) {
        // GUI must be built and opened on the main thread
        plugin.scheduler().global(() -> {
            String pattern = actionGui.getString("pattern", "");
            String title = actionGui.getString("title", "<red>Moderation - %player%")
                    .replace("%player%", target.getName());

            GUI gui = GUI.create(title);
            gui.pattern(pattern)
                    .emptySlotChars(List.of('#'))
                    .onPatternMatch(info -> {
                        switch (info.ch) {
                            case 'D', 'I', 'M' -> {
                                return gui.createItem(resolveMaterial(info.ch, "PAPER"))
                                        .name(actionGui.getString(info.ch + ".name", ""))
                                        .lore(actionGui.getString(info.ch + ".lore", ""))
                                        .action((event, container) -> applyTimedPunishment(staff, target, info.ch, true));
                            }
                            case 'F', 'J', 'Q' -> {
                                return gui.createItem(resolveMaterial(info.ch, "PAPER"))
                                        .name(actionGui.getString(info.ch + ".name", ""))
                                        .lore(actionGui.getString(info.ch + ".lore", ""))
                                        .action((event, container) -> applyTimedPunishment(staff, target, info.ch, false));
                            }
                            case 'G' -> {
                                return gui.createItem(resolveMaterial(info.ch, "PAPER"))
                                        .name(actionGui.getString(info.ch + ".name", ""))
                                        .lore(actionGui.getString(info.ch + ".lore", ""))
                                        .action((event, container) -> {
                                            if (!target.isOnline()) {
                                                plugin.getLang().get("general.player_not_found")
                                                        .with("%target%", target.getName())
                                                        .sendTo(staff);
                                                plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff));
                                                return;
                                            }
                                            String reason = resolveReason('G', target, "Warned via moderation GUI");
                                            plugin.getPunishmentManager().warnPlayer(target.getUniqueId(), target.getName(), staff.getName(), reason)
                                                    .thenAccept(count -> {
                                                        plugin.getLang().get("warn.success")
                                                                .with("%target%", target.getName())
                                                                .with("%count%", count)
                                                                .with("%max%", 3)
                                                                .sendTo(staff);
                                                        plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff));
                                                    });
                                        });
                            }
                            case 'K' -> {
                                return gui.createItem(resolveMaterial(info.ch, "PAPER"))
                                        .name(actionGui.getString(info.ch + ".name", ""))
                                        .lore(actionGui.getString(info.ch + ".lore", ""))
                                        .action((event, container) -> {
                                            if (!target.isOnline()) {
                                                plugin.getLang().get("general.player_not_found")
                                                        .with("%target%", target.getName())
                                                        .sendTo(staff);
                                                plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff));
                                                return;
                                            }
                                            String reason = resolveReason('K', target, "Kicked via moderation GUI");
                                            plugin.getPunishmentManager().kickPlayer(target, staff.getName(), reason);
                                            plugin.getLang().get("kick.success")
                                                    .with("%target%", target.getName())
                                                    .sendTo(staff);
                                            plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff));
                                        });
                            }
                            case 'B' -> {
                                return gui.createItem(resolveMaterial('B', "ARROW"))
                                        .name(actionGui.getString("B.name", "<white>Back"))
                                        .lore(actionGui.getString("B.lore", ""))
                                        .action((event, container) -> plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff)));
                            }
                            case 'C' -> {
                                return gui.createItem(resolveMaterial('C', "BARRIER"))
                                        .name(actionGui.getString("C.name", "<red>Close"))
                                        .lore(actionGui.getString("C.lore", ""))
                                        .action((event, container) -> event.getWhoClicked().closeInventory());
                            }
                        }
                        return null;
                    });

            gui.open(staff);
        });
    }

    private void applyTimedPunishment(Player staff, Player target, char slot, boolean ban) {
        if (!target.isOnline()) {
            plugin.getLang().get("general.player_not_found")
                    .with("%target%", target.getName())
                    .sendTo(staff);
            plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff));
            return;
        }

        String durationInput = actionGui.getString(slot + ".duration", "-1");
        long expiresAt = DurationParser.parse(durationInput);
        if (expiresAt == 0) {
            plugin.getLang().get("general.invalid_duration").sendTo(staff);
            plugin.scheduler().global(() -> open(staff, target));
            return;
        }

        if (ban) {
            String reason = resolveReason(slot, target, "Banned via moderation GUI");
            plugin.getPunishmentManager().banPlayer(target.getUniqueId(), target.getName(), staff.getName(), reason, expiresAt)
                    .thenAccept(p -> {
                        String key = expiresAt == -1 ? "ban.permanent" : "ban.success";
                        plugin.getLang().get(key)
                                .with("%target%", target.getName())
                                .with("%reason%", reason)
                                .sendTo(staff);
                        plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff));
                    });
            return;
        }

        String reason = resolveReason(slot, target, "Muted via moderation GUI");
        plugin.getPunishmentManager().mutePlayer(target.getUniqueId(), target.getName(), staff.getName(), reason, expiresAt)
                .thenAccept(p -> {
                    String key = expiresAt == -1 ? "mute.permanent" : "mute.success";
                    plugin.getLang().get(key)
                            .with("%target%", target.getName())
                            .with("%reason%", reason)
                            .sendTo(staff);
                    plugin.scheduler().global(() -> new ModerationMenu(plugin).open(staff));
                });
    }

    private String resolveReason(char slot, Player target, String fallback) {
        return actionGui.getString(slot + ".reason", fallback)
                .replace("%target%", target.getName());
    }

    private Material resolveMaterial(char slot, String fallback) {
        Material material = Material.getMaterial(actionGui.getString(slot + ".item", fallback));
        return material != null ? material : Material.getMaterial(fallback);
    }
}
