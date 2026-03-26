package dev.notmarra.tenbatsu.listeners;

import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.utils.DurationParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final Tenbatsu plugin;

    public ChatListener(Tenbatsu plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        var player = event.getPlayer();

        // Staff chat intercept
        if (plugin.getStaffChatManager().isInStaffChat(player.getUniqueId())) {
            event.setCancelled(true);
            String plainMsg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(event.message());
            plugin.getStaffChatManager().sendStaffMessage(player, plainMsg);
            return;
        }

        // Mute check
        if (player.hasPermission("tenbatsu.bypass.mute")) return;

        plugin.getPunishmentManager().getMute(player.getUniqueId()).thenAccept(mute -> {
            if (mute != null) {
                event.setCancelled(true);
                String expires = DurationParser.format(mute.getExpiresAt());
                plugin.getLang().get("mute.blocked")
                        .with("%expires%", expires)
                                .sendTo(player);
            }
        });
    }
}

