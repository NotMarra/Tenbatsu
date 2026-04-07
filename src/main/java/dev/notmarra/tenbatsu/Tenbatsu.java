package dev.notmarra.tenbatsu;

import dev.notmarra.notlib.database.DatabaseManager;
import dev.notmarra.notlib.extensions.NotPlugin;
import dev.notmarra.notlib.language.LanguageManager;
import dev.notmarra.tenbatsu.commands.BanCommand;
import dev.notmarra.tenbatsu.commands.HistoryCommand;
import dev.notmarra.tenbatsu.commands.KickCommand;
import dev.notmarra.tenbatsu.commands.MuteCommand;
import dev.notmarra.tenbatsu.commands.ReportCommand;
import dev.notmarra.tenbatsu.commands.StaffChatCommand;
import dev.notmarra.tenbatsu.commands.TenbatsuCommand;
import dev.notmarra.tenbatsu.commands.WarnCommand;
import dev.notmarra.tenbatsu.listeners.ChatListener;
import dev.notmarra.tenbatsu.listeners.LoginListener;
import dev.notmarra.tenbatsu.managers.PunishmentManager;
import dev.notmarra.tenbatsu.managers.ReportManager;
import dev.notmarra.tenbatsu.managers.StaffChatManager;

public final class Tenbatsu extends NotPlugin {
    private DatabaseManager db;
    private LanguageManager lang;
    private PunishmentManager punishmentManager;
    private ReportManager reportManager;
    private StaffChatManager staffChatManager;

    @Override
    public void initPlugin() {
        saveDefaultConfig();
        getCfm().register("gui/report.yml");
        getCfm().register("gui/moderation.yml");
        getCfm().register("gui/history.yml");
        getCfm().register("gui/player_action.yml");

        String dbType = getConfig().getString("database.type", "sqlite");
        if (dbType.equalsIgnoreCase("mariadb")) {
            db = mariaDatabase(
                    getConfig().getString("database.host", "localhost"),
                    getConfig().getString("database.port", "3306"),
                    getConfig().getString("database.database", "tenbatsu"),
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

        addListener(new ChatListener(this));
        addListener(new LoginListener(this));

        addCommandGroup(new BanCommand(this));
        addCommandGroup(new WarnCommand(this));
        addCommandGroup(new MuteCommand(this));
        addCommandGroup(new KickCommand(this));
        addCommandGroup(new HistoryCommand(this));
        addCommandGroup(new ReportCommand(this));
        addCommandGroup(new StaffChatCommand(this));
        addCommandGroup(new TenbatsuCommand(this));
    }

    public void reloadPluginData() {
        reloadConfig();
        getCfm().reloadAll();
    }

    @Override
    public void onPluginEnable() {
        this.getLogger().info("Tenbatsu has been enabled!");
    }

    @Override
    public void onPluginDisable() {
        this.getLogger().info("Tenbatsu has been disabled!");
    }

    public DatabaseManager getDb() { return db; }
    public LanguageManager getLang() { return lang; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public ReportManager getReportManager() { return reportManager; }
    public StaffChatManager getStaffChatManager() { return staffChatManager; }
}
