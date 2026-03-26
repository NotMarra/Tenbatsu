package dev.notmarra.tenbatsu.managers;

import dev.notmarra.notlib.cache.CachedRepository;
import dev.notmarra.notlib.database.DatabaseManager;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.Tenbatsu;
import dev.notmarra.tenbatsu.models.Report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReportManager {

    private final Tenbatsu plugin;
    private final DatabaseManager db;
    private CachedRepository<Integer, Report> repo;

    // Cooldown & session tracking (in-memory)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> sessionCounts = new HashMap<>();

    public ReportManager(Tenbatsu plugin) {
        this.plugin = plugin;
        this.db = plugin.getDb();
    }

    public void init() {
        db.registerCached(Report.class);
        repo = db.cached(Report.class);
    }

    public CompletableFuture<Report> submitReport(UUID reporter, String reporterName,
                                                  UUID target, String targetName,
                                                  String reason) {
        Report r = new Report(reporter, reporterName, target, targetName, reason);
        return repo.upsertAsync(r).thenApply(v -> r);
    }

    public CompletableFuture<List<Report>> getPendingReports() {
        return repo.findAllAsync().thenApply(list ->
                list.stream().filter(r -> !r.isHandled()).toList()
        );
    }

    public CompletableFuture<Void> markHandled(int id, String staffName) {
        return repo.findByIdAsync(id).thenAccept(opt ->
                opt.ifPresent(r -> {
                    r.setHandled(true);
                    r.setHandledBy(staffName);
                    repo.upsert(r);
                })
        );
    }

    public CompletableFuture<Void> clearAllReports() {
        return repo.findAllAsync().thenAccept(list ->
                list.stream().filter(r -> !r.isHandled()).forEach(r -> {
                    r.setHandled(true);
                    r.setHandledBy("SYSTEM");
                    repo.upsert(r);
                })
        );
    }

    public boolean isOnCooldown(UUID player) {
        Long last = cooldowns.get(player);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < 30_000; // 30s cooldown
    }

    public long getCooldownRemaining(UUID player) {
        Long last = cooldowns.get(player);
        if (last == null) return 0;
        long remaining = 30_000 - (System.currentTimeMillis() - last);
        return Math.max(0, remaining / 1000);
    }

    public boolean hasReachedMax(UUID player) {
        int max = plugin.getConfig().getInt("settings.max_reports_per_session", 3);
        return sessionCounts.getOrDefault(player, 0) >= max;
    }

    public void recordReport(UUID player) {
        cooldowns.put(player, System.currentTimeMillis());
        sessionCounts.merge(player, 1, Integer::sum);
    }

    public CachedRepository<Integer, Report> getRepo() { return repo; }
}