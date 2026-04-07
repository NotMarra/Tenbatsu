package dev.notmarra.tenbatsu.managers;

import dev.notmarra.notlib.cache.CachedRepository;
import dev.notmarra.notlib.cache.WriteStrategy;
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
        db.registerCached(Punishment.class, WriteStrategy.READ_THROUGH);
        repo = db.cached(Punishment.class);
    }

    // ── BAN ──────────────────────────────────────────────────────────────────

    public CompletableFuture<Punishment> banPlayer(UUID target, String targetName,
                                                   String staffName, String reason,
                                                   long expiresAt) {
        Punishment p = new Punishment(target, targetName, "CONSOLE", staffName,
                PunishmentType.BAN, reason, expiresAt);
        return repo.insertAsync(p).thenApply(v -> {
            Player online = Bukkit.getPlayer(target);
            if (online != null) {
                String screen = lang.get("ban.screen")
                        .with("%reason%", reason)
                        .with("%expires%", DurationParser.format(expiresAt))
                        .with("%staff%", staffName)
                        .build().buildString();
                plugin.scheduler().global(() -> online.kick(MM.deserialize(screen)));
            }
            return p;
        });
    }

    public CompletableFuture<Boolean> unbanPlayer(UUID target) {
        return repo.findAllAsync().thenCompose(all -> {
            Punishment active = all.stream()
                    .filter(p -> p.getPlayerUuid().equals(target)
                            && p.getType() == PunishmentType.BAN
                            && p.isActive()
                            && !p.isExpired())
                    .findFirst()
                    .orElse(null);

            if (active == null) return CompletableFuture.completedFuture(false);

            active.setActive(false);
            return repo.upsertAsync(active).thenApply(v -> true);
        });
    }

    public CompletableFuture<Boolean> isBanned(UUID target) {
        return repo.findAllAsync().thenApply(all -> {
            Punishment active = all.stream()
                    .filter(p -> p.getPlayerUuid().equals(target)
                            && p.getType() == PunishmentType.BAN
                            && p.isActive())
                    .findFirst()
                    .orElse(null);

            if (active == null) return false;

            if (active.isExpired()) {
                active.setActive(false);
                repo.upsert(active);
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
        return repo.insertAsync(p).thenApply(v -> p);
    }

    public CompletableFuture<Boolean> unmutePlayer(UUID target) {
        return repo.findAllAsync().thenCompose(all -> {
            Punishment active = all.stream()
                    .filter(p -> p.getPlayerUuid().equals(target)
                            && p.getType() == PunishmentType.MUTE
                            && p.isActive()
                            && !p.isExpired())
                    .findFirst()
                    .orElse(null);

            if (active == null) return CompletableFuture.completedFuture(false);

            active.setActive(false);
            return repo.upsertAsync(active).thenApply(v -> true);
        });
    }

    public CompletableFuture<Punishment> getMute(UUID target) {
        return repo.findAllAsync().thenApply(all -> {
            Punishment active = all.stream()
                    .filter(p -> p.getPlayerUuid().equals(target)
                            && p.getType() == PunishmentType.MUTE
                            && p.isActive())
                    .findFirst()
                    .orElse(null);

            if (active == null) return null;

            if (active.isExpired()) {
                active.setActive(false);
                repo.upsert(active);
                return null;
            }
            return active;
        });
    }

    // ── WARN ─────────────────────────────────────────────────────────────────

    /**
     * Issues a new warning. id=0 ensures the ORM treats this as INSERT (auto-increment),
     * so each warn becomes its own DB row instead of overwriting an existing one.
     */
    public CompletableFuture<Integer> warnPlayer(UUID target, String targetName,
                                                 String staffName, String reason) {
        Punishment p = new Punishment(target, targetName, "CONSOLE", staffName,
                PunishmentType.WARN, reason, -1);
        return repo.insertAsync(p).thenCompose(v -> getWarnCount(target));
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
        Punishment p = new Punishment(target.getUniqueId(), target.getName(),
                "CONSOLE", staffName, PunishmentType.KICK, reason, 0);
        p.setActive(false);
        repo.insert(p);

        String screen = lang.get("kick.screen")
                .with("%reason%", reason)
                .with("%staff%", staffName)
                .build().buildString();
        plugin.scheduler().global(() -> target.kick(MM.deserialize(screen)));
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

    public CachedRepository<Integer, Punishment> getRepo() { return repo; }
}