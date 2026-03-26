package dev.notmarra.tenbatsu;

import dev.notmarra.notlib.database.DatabaseManager;
import dev.notmarra.notlib.extensions.NotPlugin;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.managers.PunishmentManager;
import dev.notmarra.tenbatsu.managers.ReportManager;
import dev.notmarra.tenbatsu.managers.StaffChatManager;
import org.bukkit.configuration.file.FileConfiguration;

public final class Tenbatsu extends NotPlugin {
    private DatabaseManager db;
    private LanguageManager lang;
    private PunishmentManager punishmentManager;
    private ReportManager reportManager;
    private StaffChatManager staffChatManager;

    @Override
    public void initPlugin() {
        saveDefaultConfig();
        saveDefaultConfig("gui/report.yml");
        saveDefaultConfig("gui/moderation.yml");
        getCfm().register("gui/report.yml");
        getCfm().register("gui/moderation.yml");

        String dbType = getConfig().getString("database.type", "sqlite");
        if (dbType.equalsIgnoreCase("mariadb")) {
            db = mariaDatabase(
                    getConfig().getString("database.host", "localhost"),
                    getConfig().getString("database.port", "3306"),
                    getConfig().getString("database.database", "notban"),
                    getConfig().getString("database.username", "root"),
                    getConfig().getString("database.password", "")
            ).build();
        } else {
            db = sqliteDatabase(getDataFolder(), getConfig().getString("database.file", "data")).build();
        }

        lang = languageManager()
                .defaultLocale("en_US")
                .seedFile("languages/en_US.yml")
                .build();

        punishmentManager = new PunishmentManager(this);
        punishmentManager.init();
        reportManager = new ReportManager(this);
        reportManager.init();
        staffChatManager = new StaffChatManager(this);
    }

    @Override
    public void onPluginEnable() {

    }

    @Override
    public void onPluginDisable() {

    }

    public DatabaseManager getDb() { return db; }
    public LanguageManager getLang() { return lang; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public ReportManager getReportManager() { return reportManager; }
    public StaffChatManager getStaffChatManager() { return staffChatManager; }
}
