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
    private final LanguageManager lang;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public StaffChatManager(Tenbatsu plugin) {
        this.lang = plugin.getLang();
    }

    private final Set<UUID> staffChatToggled = new HashSet<>();

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
        String formatted = lang.get("staffchat.format")
                .withPlayer(sender)
                .with("%message%", message)
                .toString();


        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("tenbatsu.staffchat")) {
                lang.get("staffchat.format")
                        .withPlayer(sender)
                        .with("%message%", message)
                        .sendTo(p);
            }
        }
        // Also log to console
        Bukkit.getConsoleSender().sendMessage(MM.deserialize(formatted));
    }
}