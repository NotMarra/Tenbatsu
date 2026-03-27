package dev.notmarra.tenbatsu.listeners;

import dev.notmarra.notlib.extensions.NotListener;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.utils.DurationParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoginListener extends NotListener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Tenbatsu plugin;

    public LoginListener(Tenbatsu plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        // Skip bypass players — we can't check permissions here, so we check after login
        try {
            var punishment = plugin.getPunishmentManager()
                    .getRepo()
                    .findAllAsync()
                    .get(3, TimeUnit.SECONDS)
                    .stream()
                    .filter(p -> p.getPlayerUuid().equals(event.getUniqueId())
                            && (p.getType().name().equals("BAN") || p.getType().name().equals("IP_BAN"))
                            && p.isActive())
                    .findFirst()
                    .orElse(null);

            if (punishment == null) return;

            // Check expiry
            if (punishment.isExpired()) {
                punishment.setActive(false);
                plugin.getPunishmentManager().getRepo().upsert(punishment);
                return;
            }

            String screen = plugin.getLang().get("ban.screen")
                    .with("%reason%", punishment.getReason())
                    .with("%expires%", DurationParser.format(punishment.getExpiresAt()))
                    .with("%staff%", punishment.getStaffName()).build().buildString();

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MM.deserialize(screen));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            plugin.getLogger().warning("Failed to check ban status for " + event.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerJoinEvent event) {
        // Notify staff of joins so they can see if a reported player joined
        var player = event.getPlayer();
        plugin.getReportManager().getPendingReports().thenAccept(reports -> {
            long count = reports.stream()
                    .filter(r -> r.getTargetUuid().equals(player.getUniqueId().toString()))
                    .count();
            if (count > 0) {
                String msg = plugin.getLang().get("report.staff_join_alert")
                                        .with("%count%", count)
                                        .withPlayer(player).build().buildString();

                plugin.getStaffChatManager().broadcastToStaff(msg);
            }
        });
    }

    @Override
    public String getId() {
        return "LoginListener-tenbatsu";
    }
}
