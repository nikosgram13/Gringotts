package org.gestern.gringotts;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.SQLitePlatform;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import com.avaje.ebeaninternal.server.lib.sql.TransactionIsolation;
import net.milkbowl.vault.economy.Economy;
import net.tnemc.core.Reserve;
import org.apache.commons.lang.Validate;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.accountholder.AccountHolderFactory;
import org.gestern.gringotts.accountholder.AccountHolderProvider;
import org.gestern.gringotts.api.Eco;
import org.gestern.gringotts.api.impl.GringottsEco;
import org.gestern.gringotts.api.impl.ReserveConnector;
import org.gestern.gringotts.api.impl.VaultConnector;
import org.gestern.gringotts.commands.GringottsExecutor;
import org.gestern.gringotts.commands.MoneyExecutor;
import org.gestern.gringotts.commands.MoneyadminExecutor;
import org.gestern.gringotts.currency.Denomination;
import org.gestern.gringotts.data.DAO;
import org.gestern.gringotts.data.DerbyDAO;
import org.gestern.gringotts.data.EBeanDAO;
import org.gestern.gringotts.data.Migration;
import org.gestern.gringotts.event.AccountListener;
import org.gestern.gringotts.event.PlayerVaultListener;
import org.gestern.gringotts.event.VaultCreator;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.gestern.gringotts.Configuration.CONF;
import static org.gestern.gringotts.Language.LANG;
import static org.gestern.gringotts.dependency.Dependency.DEP;


public class Gringotts extends JavaPlugin {

    private static final String MESSAGES_YML = "messages.yml";
    private static Gringotts instance;

    private final AccountHolderFactory accountHolderFactory = new AccountHolderFactory();
    private Accounting accounting;
    private DAO dao;
    private EbeanServer ebean;
    private Eco eco;

    public Gringotts() {
        ServerConfig dbConfig = new ServerConfig();

        dbConfig.setDefaultServer(false);
        dbConfig.setRegister(false);
        dbConfig.setClasses(getDatabaseClasses());
        dbConfig.setName(getDescription().getName());
        configureDbConfig(dbConfig);

        DataSourceConfig dsConfig = dbConfig.getDataSourceConfig();

        dsConfig.setUrl(replaceDatabaseString(dsConfig.getUrl()));
        getDataFolder().mkdirs();

        ClassLoader previous = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClassLoader());
        ebean = EbeanServerFactory.create(dbConfig);
        Thread.currentThread().setContextClassLoader(previous);
    }

    /**
     * The Gringotts plugin instance.
     */
    public static Gringotts getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        try {
            // just call DAO once to ensure it's loaded before startup is complete
            dao = getDAO();

            // load and init configuration
            saveDefaultConfig(); // saves default configuration if no config.yml exists yet
            reloadConfig();

            accounting = new Accounting();

            eco = new GringottsEco();
            registerCommands();
            registerEvents();
            registerEconomy();

            // Setup Metrics support.
            Metrics metrics = new Metrics(this);

            // Tracking how many vaults exists.
            metrics.addCustomChart(new Metrics.SingleLineChart("vaultsChart", () -> {
                int returned = 0;

                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    AccountHolder holder = accountHolderFactory.get(player);
                    GringottsAccount account = accounting.getAccount(holder);

                    returned += Gringotts.getInstance().getDao().getChests(account).size();
                }

                return returned;
            }));

            // Tracking the balance of the users exists.
            metrics.addCustomChart(new Metrics.SingleLineChart("economyChart", () -> {
                int returned = 0;

                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.isOp()) {
                        continue;
                    }

                    AccountHolder holder = accountHolderFactory.get(player);
                    GringottsAccount account = accounting.getAccount(holder);

                    returned += account.getBalance();
                }

                return returned;
            }));

            // Tracking the exists denominations.
            metrics.addCustomChart(new Metrics.AdvancedPie("denominationsChart", () -> {
                Map<String, Integer> returned = new HashMap<>();

                for (Denomination denomination : CONF.getCurrency().getDenominations()) {
                    String name = denomination.getKey().type.getType().name();

                    if (!returned.containsKey(name)) {
                        returned.put(name, 0);
                    }

                    returned.put(name, returned.get(name) + 1);
                }

                return returned;
            }));
        } catch (GringottsStorageException | GringottsConfigurationException e) {
            getLogger().severe(e.getMessage());
            this.disable();
        } catch (RuntimeException e) {
            this.disable();
            throw e;
        }


        getLogger().fine("enabled");
    }

    private void disable() {
        Bukkit.getPluginManager().disablePlugin(this);
        getLogger().warning("Gringotts disabled due to startup errors.");
    }

    @Override
    public void onDisable() {

        // shut down db connection
        try {
            if (dao != null) {
                dao.shutdown();
            }
        } catch (GringottsStorageException e) {
            getLogger().severe(e.toString());
        }

        getLogger().info("disabled");
    }

    private void registerCommands() {
        CommandExecutor playerCommands = new MoneyExecutor();
        CommandExecutor moneyAdminCommands = new MoneyadminExecutor();
        CommandExecutor adminCommands = new GringottsExecutor();

        getCommand("balance").setExecutor(playerCommands);
        getCommand("money").setExecutor(playerCommands);
        getCommand("moneyadmin").setExecutor(moneyAdminCommands);
        getCommand("gringotts").setExecutor(adminCommands);
    }

    private void registerEvents() {
        PluginManager manager = getServer().getPluginManager();

        manager.registerEvents(new AccountListener(), this);
        manager.registerEvents(new PlayerVaultListener(), this);
        manager.registerEvents(new VaultCreator(), this);

        // listeners for other account types are loaded with dependencies
    }


    /**
     * Register Gringotts as economy provider for vault/Reserve.
     */
    private void registerEconomy() {
        if (DEP.vault.exists()) {
            getServer().getServicesManager().register(Economy.class, new VaultConnector(), this, ServicePriority.Highest);
            getLogger().info("Registered Vault interface.");
        }

        if (DEP.reserve.exists()) {
            Reserve.instance().registerProvider(new ReserveConnector());
            getLogger().info("Registered Reserve interface.");
        }

        if(!DEP.vault.exists() && !DEP.reserve.exists()) {
            getLogger().info("Neither Vault or Reserve was found. Other plugins may not be able to access Gringotts accounts.");
        }
    }

    /**
     * Register an accountholder provider with Gringotts. This is necessary for Gringotts to find and create account
     * holders
     * of any non-player type. Registering a provider for the same type twice will overwrite the previously
     * registered provider.
     *
     * @param type     type id for an account type
     * @param provider provider for the account type
     */
    public void registerAccountHolderProvider(String type, AccountHolderProvider provider) {
        accountHolderFactory.registerAccountHolderProvider(type, provider);
    }

    /**
     * Get the configured player interaction messages.
     *
     * @return the configured player interaction messages
     */
    public FileConfiguration getMessages() {

        String langPath = "i18n/messages_" + CONF.language + ".yml";

        // try configured language first
        InputStream langStream = getResource(langPath);
        final FileConfiguration conf;
        if (langStream != null) {
            Reader langReader = new InputStreamReader(getResource(langPath), Charset.forName("UTF-8"));
            conf = YamlConfiguration.loadConfiguration(langReader);
        } else {
            // use custom/default
            File langFile = new File(getDataFolder(), MESSAGES_YML);
            conf = YamlConfiguration.loadConfiguration(langFile);
        }

        return conf;
    }

    // override to handle custom config logic and language loading
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        CONF.readConfig(getConfig());
        LANG.readLanguage(getMessages());
    }

    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
        File defaultMessages = new File(getDataFolder(), MESSAGES_YML);

        if (!defaultMessages.exists()) {
            saveResource(MESSAGES_YML, false);
        }
    }

    private DAO getDAO() {

        setupEBean();

        // legacy support: migrate derby if it hasn't happened yet
        // automatically migrate derby to eBeans if db exists and migration flag hasn't been set
        Migration migration = new Migration();

        DerbyDAO derbyDAO;
        if (!migration.isDerbyMigrated() && (derbyDAO = DerbyDAO.getDao()) != null) {
            getLogger().info("Derby database detected. Migrating to Bukkit-supported database ...");
            EBeanDAO eBeanDAO = EBeanDAO.getDao();
            migration.doDerbyMigration(derbyDAO, eBeanDAO);
        }

        if (!migration.isUUIDMigrated()) {
            getLogger().info("Player database not migrated to UUIDs yet. Attempting migration");
            migration.doUUIDMigration();
        }

        return EBeanDAO.getDao();
    }

    public List<Class<?>> getDatabaseClasses() {
        return EBeanDAO.getDatabaseClasses();
    }

    /**
     * Some awkward ritual that Bukkit requires to initialize all the DB classes.
     * Does nothing if they have already been set up.
     */
    private void setupEBean() {
        try {
            EbeanServer db = getDatabase();

            for (Class<?> c : getDatabaseClasses()) {
                db.find(c).findRowCount();
            }
        } catch (Exception ignored) {
            getLogger().info("Initializing database tables.");
            installDDL();
        }
    }

    /**
     * Gets the {@link EbeanServer} tied to this plugin.
     * <p>
     * <i>For more information on the use of <a href="http://www.avaje.org/">
     * Avaje Ebeans ORM</a>, see <a
     * href="http://www.avaje.org/ebean/documentation.html">Avaje Ebeans
     * Documentation</a></i>
     * <p>
     * <i>For an example using Ebeans ORM, see <a
     * href="https://github.com/Bukkit/HomeBukkit">Bukkit's Homebukkit Plugin
     * </a></i>
     *
     * @return ebean server instance or null if not enabled
     * all EBean related methods has been removed with Minecraft 1.12
     * - see https://www.spigotmc.org/threads/194144/
     */
    public EbeanServer getDatabase() {
        return ebean;
    }

    protected void installDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();

        gen.runScript(false, gen.generateCreateDdl());
    }

    protected void removeDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();

        gen.runScript(true, gen.generateDropDdl());
    }

    private String replaceDatabaseString(String input) {
        input = input.replaceAll(
                "\\{DIR}",
                getDataFolder().getPath().replaceAll("\\\\", "/") + "/");
        input = input.replaceAll(
                "\\{NAME}",
                getDescription().getName().replaceAll("[^\\w_-]", ""));

        return input;
    }

    public void configureDbConfig(ServerConfig config) {
        Validate.notNull(config, "Config cannot be null");

        DataSourceConfig ds = new DataSourceConfig();
        ds.setDriver("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite:{DIR}{NAME}.db");
        ds.setUsername("bukkit");
        ds.setPassword("walrus");
        ds.setIsolationLevel(TransactionIsolation.getLevel("SERIALIZABLE"));

        if (ds.getDriver().contains("sqlite")) {
            config.setDatabasePlatform(new SQLitePlatform());
            config.getDatabasePlatform().getDbDdlSyntax().setIdentity("");
        }

        config.setDataSourceConfig(ds);
    }

    public DAO getDao() {
        return dao;
    }

    /**
     * The account holder factory is the place to go if you need an AccountHolder instance for an id.
     */
    public AccountHolderFactory getAccountHolderFactory() {
        return accountHolderFactory;
    }

    /**
     * Manages accounts.
     */
    public Accounting getAccounting() {
        return accounting;
    }

    public Eco getEco() {
        return eco;
    }
}
