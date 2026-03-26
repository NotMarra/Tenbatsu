package dev.notmarra.tenbatsu.managers;

import dev.notmarra.notlib.cache.CachedRepository;
import dev.notmarra.notlib.database.DatabaseManager;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.models.Punishment;
import dev.notmarra.tenbatsu.models.PunishmentType;
import dev.notmarra.tenbatsu.utils.DurationParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentManager {

    private final Tenbatsu plugin;
    private final DatabaseManager db;
    private final LanguageManager lang;
    private CachedRepository<Integer, Punishment> repo;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public PunishmentManager(Tenbatsu plugin) {
        this.plugin = plugin;
        this.db = plugin.getDb();
        this.lang = plugin.getLang();
    }

    public void init() {
        db.registerCached(Punishment.class);
        repo = db.cached(Punishment.class);
    }

    // ── BAN ──────────────────────────────────────────────────────────────────

    public CompletableFuture<Punishment> banPlayer(UUID target, String targetName,
                                                   String staffName, String reason,
                                                   long expiresAt) {
        Punishment p = new Punishment(target, targetName, "CONSOLE", staffName,
                PunishmentType.BAN, reason, expiresAt);
        return repo.upsertAsync(p).thenApply(v -> {
            // Kick if online
            Player online = Bukkit.getPlayer(target);
            if (online != null) {
                String screen = lang.get("ban.screen")
                        .with("%reason%", reason)
                        .with("%expires%", DurationParser.format(expiresAt))
                        .with("%staff%", staffName)
                        .toString();
                online.kick(MM.deserialize(screen));
            }
            return p;
        });
    }

    public CompletableFuture<Boolean> unbanPlayer(UUID target) {
        return getActivePunishment(target, PunishmentType.BAN).thenCompose(opt -> {
            if (opt == null) return CompletableFuture.completedFuture(false);
            opt.setActive(false);
            return repo.upsertAsync(opt).thenApply(v -> true);
        });
    }

    public CompletableFuture<Boolean> isBanned(UUID target) {
        return getActivePunishment(target, PunishmentType.BAN).thenApply(p -> {
            if (p == null) return false;
            if (p.isExpired()) {
                p.setActive(false);
                repo.upsert(p);
                return false;
            }
            return true;
        });
    }

    // ── MUTE ─────────────────────────────────────────────────────────────────

    public CompletableFuture<Punishment> mutePlayer(UUID target, String targetName,
                                                    String staffName, String reason,
                                                    long expiresAt) {
        Punishment p = new Punishment(target, targetName, "CONSOLE", staffName,
                PunishmentType.MUTE, reason, expiresAt);
        return repo.upsertAsync(p).thenApply(v -> p);
    }

    public CompletableFuture<Boolean> unmutePlayer(UUID target) {
        return getActivePunishment(target, PunishmentType.MUTE).thenCompose(opt -> {
            if (opt == null) return CompletableFuture.completedFuture(false);
            opt.setActive(false);
            return repo.upsertAsync(opt).thenApply(v -> true);
        });
    }

    public CompletableFuture<Punishment> getMute(UUID target) {
        return getActivePunishment(target, PunishmentType.MUTE).thenApply(p -> {
            if (p == null) return null;
            if (p.isExpired()) {
                p.setActive(false);
                repo.upsert(p);
                return null;
            }
            return p;
        });
    }

    // ── WARN ─────────────────────────────────────────────────────────────────

    public CompletableFuture<Integer> warnPlayer(UUID target, String targetName,
                                                 String staffName, String reason) {
        Punishment p = new Punishment(target, targetName, "CONSOLE", staffName,
                PunishmentType.WARN, reason, -1);
        return repo.upsertAsync(p).thenCompose(v -> getWarnCount(target));
    }

    public CompletableFuture<Integer> getWarnCount(UUID target) {
        return repo.findAllAsync().thenApply(list ->
                (int) list.stream()
                        .filter(p -> p.getPlayerUuid().equals(target)
                                && p.getType() == PunishmentType.WARN
                                && p.isActive())
                        .count()
        );
    }

    public CompletableFuture<Void> clearWarnings(UUID target) {
        return repo.findAllAsync().thenAccept(list ->
                list.stream()
                        .filter(p -> p.getPlayerUuid().equals(target)
                                && p.getType() == PunishmentType.WARN
                                && p.isActive())
                        .forEach(p -> {
                            p.setActive(false);
                            repo.upsert(p);
                        })
        );
    }

    // ── KICK ─────────────────────────────────────────────────────────────────

    public void kickPlayer(Player target, String staffName, String reason) {
        // Log kick to DB but don't make it "active" since player is gone
        Punishment p = new Punishment(target.getUniqueId(), target.getName(),
                "CONSOLE", staffName, PunishmentType.KICK, reason, 0);
        p.setActive(false);
        repo.upsert(p);

        String screen = lang.get("kick.screen")
                .with("%reason%", reason)
                .with("%staff%", staffName)
                .toString();
        target.kick(MM.deserialize(screen));
    }

    // ── HISTORY ──────────────────────────────────────────────────────────────

    public CompletableFuture<List<Punishment>> getHistory(UUID target) {
        return repo.findAllAsync().thenApply(list ->
                list.stream()
                        .filter(p -> p.getPlayerUuid().equals(target))
                        .sorted((a, b) -> Long.compare(b.getIssuedAt(), a.getIssuedAt()))
                        .toList()
        );
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private CompletableFuture<Punishment> getActivePunishment(UUID target, PunishmentType type) {
        return repo.findAllAsync().thenApply(list ->
                list.stream()
                        .filter(p -> p.getPlayerUuid().equals(target)
                                && p.getType() == type
                                && p.isActive())
                        .findFirst()
                        .orElse(null)
        );
    }

    public CachedRepository<Integer, Punishment> getRepo() { return repo; }
}
