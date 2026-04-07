package dev.notmarra.tenbatsu.listeners;

import dev.notmarra.notlib.extensions.NotListener;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.utils.DurationParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ChatListener extends NotListener {
    private final Tenbatsu plugin;

    public ChatListener(Tenbatsu plugin) {
        super(plugin);
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

        // Mute check — must cancel synchronously inside the event handler.
        // getMute uses the cached repo so this is fast (no real I/O in hot path).
        if (player.hasPermission("tenbatsu.bypass.mute")) return;

        try {
            var mute = plugin.getPunishmentManager()
                    .getMute(player.getUniqueId())
                    .get(2, java.util.concurrent.TimeUnit.SECONDS);

            if (mute != null) {
                event.setCancelled(true);
                String expires = DurationParser.format(mute.getExpiresAt());
                plugin.getLang().get("mute.blocked")
                        .with("%expires%", expires)
                        .sendTo(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check mute for " + player.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public String getId() {
        return "ChatListener-Tenbatsu";
    }
}
