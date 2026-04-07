package dev.notmarra.tenbatsu.managers;

import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StaffChatManager {
    private final Tenbatsu plugin;
    private final LanguageManager lang;
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Set<UUID> staffChatToggled = new HashSet<>();

    public StaffChatManager(Tenbatsu plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLang();
    }

    public boolean isInStaffChat(UUID uuid) {
        return staffChatToggled.contains(uuid);
    }

    public void toggle(UUID uuid) {
        if (staffChatToggled.contains(uuid)) {
            staffChatToggled.remove(uuid);
        } else {
            staffChatToggled.add(uuid);
        }
    }

    public void sendStaffMessage(Player sender, String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("tenbatsu.staffchat")) {
                lang.get("staffchat.format")
                        .withPlayer(sender)
                        .with("%message%", message)
                        .sendTo(p);
            }
        }
        // Log to console
        String formatted = lang.get("staffchat.format")
                .withPlayer(sender)
                .with("%message%", message)
                .build()
                .buildString();
        Bukkit.getConsoleSender().sendMessage(MM.deserialize(formatted));
    }

    public void broadcastToStaff(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("tenbatsu.staffchat")) {
                p.sendMessage(MM.deserialize(message));
            }
        }
        Bukkit.getConsoleSender().sendMessage(MM.deserialize(message));
    }
}
