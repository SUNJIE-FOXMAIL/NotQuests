package notquests.notquests.Managers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import notquests.notquests.NotQuests;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;


/**
 * This is the Data Manager which handles loading and saving Player Data, Quest Data and Configurations.
 * The Configuration files 'quests.yml' and 'general.yml' are created here.
 * The MySQL Database is also created here.
 *
 * @author Alessio Gravili
 */
public class DataManager {

    /**
     * Instance of NotQuests is copied over
     */
    private final NotQuests main;
    /**
     * ArrayList for Command Tab Completions. They will be re-used where possible. This is sort of like a buffer for completions.
     * It does not return the real completions, but it's for example used in ObjectivesAdminCommand handleCompletions() which is
     * called by the real Tab Completer CommandNotQuestsAdmin to split it up a little.
     */
    public final List<String> completions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for players. They will be re-used where possible.
     */
    public final List<String> standardPlayerCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for entity types. They will be initialized on startup be re-used where possible.
     */
    public final List<String> standardEntityTypeCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for numbers from -1 to 12. They will be initialized on startup be re-used where possible.
     */
    public final List<String> numberCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for numbers from 1 to 12. They will be initialized on startup be re-used where possible.
     */
    public final List<String> numberPositiveCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions. They will be re-used where possible.
     */
    public final List<String> partialCompletions = new ArrayList<>();

    /**
     * MYSQL Database Connection Object
     */
    private Connection connection;
    /**
     * MYSQL Database Connection Object
     */
    private Statement statement;

    /**
     * Quests.yml Configuration
     */
    private FileConfiguration questsData;
    /**
     * Quests.yml Configuration File
     */
    private File questsDataFile = null;


    /**
     * savingEnabled is true by default. It will be set to false if any error happens when data is loaded from the Database.
     * When this is set to false, no quest or player data will be saved when the plugin is disabled.
     */
    private boolean savingEnabled = true;

    /**
     * loadingEnabled is true by default. It will be set to false if any error happens when data is loaded from the Database.
     * When this is set to false, no data will be loaded. This is to prevent trying to load data while the plugin is shutting down.
     */
    private boolean loadingEnabled = true;

    /**
     * If this is set to false, the plugin will try to load NPCs once Citizens is re-loaded or enabled
     */
    private boolean alreadyLoadedNPCs = false;

    /**
     * General.yml Configuration
     */
    private File generalConfigFile = null;
    /**
     * General.yml Configuration File
     */
    private FileConfiguration generalConfig;

    /**
     * Configuration objects which contains values from General.yml
     */
    private final Configuration configuration;

    /**
     * The Data Manager is initialized here. This mainly creates some
     * Array List for generic Tab Completions for various commands.
     * <p>
     * The actual loading of Data doesn't happen here yet.
     *
     * @param main an instance of NotQuests which will be passed over
     */
    public DataManager(NotQuests main) {
        this.main = main;

        // create an instance of the Configuration object
        configuration = new Configuration();

        /*
         * Fill up the numberCompletions Array List from 0-12 which will be
         * re-used whenever a command accepts a number
         */
        for (int i = -1; i <= 12; i++) {
            numberCompletions.add("" + i);
        }

        /*
         * Same as for numberCompletions, but this one only goes from 1-12
         */
        for (int i = 1; i <= 12; i++) {
            numberPositiveCompletions.add("" + i);
        }

        /*
         * Fill up the standardEntityTypeCompletions Array List with all the
         * Entities which are in the game.
         */
        for (EntityType entityType : EntityType.values()) {
            standardEntityTypeCompletions.add(entityType.toString());
        }

    }

    public final void loadQuestsConfig(){
        main.getLogger().log(Level.INFO, "§aNotQuests > Loading quests.yml config");

        /*
         * If the generalConfigFile Object doesn't exist yet, this will load the file
         * or create a new general.yml file if it does not exist yet and load it into the
         * generalConfig FileConfiguration object.
         */
        if (questsDataFile == null) {

            //Create the Data Folder if it does not exist yet (the NotQuests folder)
            if(!main.getDataFolder().exists()){
                main.getLogger().log(Level.INFO, "§aNotQuests > Data Folder not found. Creating a new one...");

                if (!main.getDataFolder().mkdirs()) {
                    main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the NotQuests data folder");
                    disablePluginAndSaving("There was an error creating the NotQuests data folder.");
                    return;
                }


            }

            questsDataFile = new File(main.getDataFolder(), "quests.yml");

            if (!questsDataFile.exists()) {
                main.getLogger().log(Level.INFO, "§aNotQuests > Quests Configuration (quests.yml) does not exist. Creating a new one...");

                //Does not work yet, since comments are overridden if something is saved
                //saveDefaultConfig();


                try {
                    //Try to create the general.yml config file, and throw an error if it fails.


                    if (!questsDataFile.createNewFile()) {
                        main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the quests.yml config file. (1)");
                        disablePluginAndSaving("There was an error creating the quests.yml config file.");
                        return;

                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    disablePluginAndSaving("There was an error creating the quests.yml config file. (2)");
                    return;
                }
            }

            questsData = new YamlConfiguration();
            try {
                questsData.load(questsDataFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            questsData = YamlConfiguration.loadConfiguration(questsDataFile);
        }
    }

    /**
     * The general.yml configuration file is initialized here. This will create the
     * general.yml config file if it hasn't been created yet. It will also create all
     * the necessary default config values if they are non-existent.
     * <p>
     * This will also load the value from the general.yml config file, like the
     * MySQL Database connection information. If that data is not found, the plugin will
     * stop and throw a warning, since it cannot function without a MySQL database.
     */
    public final void loadGeneralConfig() {
        main.getLogger().log(Level.INFO, "§aNotQuests > Loading general config");

        /*
         * If the generalConfigFile Object doesn't exist yet, this will load the file
         * or create a new general.yml file if it does not exist yet and load it into the
         * generalConfig FileConfiguration object.
         */
        if (generalConfigFile == null) {

            //Create the Data Folder if it does not exist yet (the NotQuests folder)
            if(!main.getDataFolder().exists()){
                main.getLogger().log(Level.INFO, "§aNotQuests > Data Folder not found. Creating a new one...");

                if (!main.getDataFolder().mkdirs()) {
                    main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the NotQuests data folder");
                    disablePluginAndSaving("There was an error creating the NotQuests data folder.");
                    return;
                }


            }

            generalConfigFile = new File(main.getDataFolder(), "general.yml");

            if (!generalConfigFile.exists()) {
                main.getLogger().log(Level.INFO, "§aNotQuests > General Configuration (general.yml) does not exist. Creating a new one...");

                //Does not work yet, since comments are overridden if something is saved
                //saveDefaultConfig();


                try {
                    //Try to create the general.yml config file, and throw an error if it fails.

                    if (!generalConfigFile.createNewFile()) {
                        main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the general.yml config file. (1)");
                        disablePluginAndSaving("There was an error creating the general.yml config file (1).");
                        return;
                    }
                    main.getLogger().log(Level.INFO, "§5Loading default §bgeneral.yml§5...");

                    InputStream inputStream = main.getResource("general.yml");
                    //Instead of creating a new general.yml file, we will copy the one from inside of the plugin jar into the plugin folder:
                    if (inputStream != null) {


                        try (OutputStream outputStream = new FileOutputStream(generalConfigFile)) {
                            IOUtils.copy(inputStream, outputStream);
                        } catch (Exception e) {
                            main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the general.yml config file. (2)");
                            disablePluginAndSaving("There was an error creating the general.yml config file (2).");
                            return;
                        }


                    }


                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    disablePluginAndSaving("There was an error creating the general.yml config file. (2)");
                    return;
                }
            }

            generalConfig = new YamlConfiguration();
            try {
                generalConfig.load(generalConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            generalConfig = YamlConfiguration.loadConfiguration(generalConfigFile);
        }

        //Load all the MySQL Database Connection information from the general.yml
        configuration.setMySQLEnabled(getGeneralConfig().getBoolean("storage.database.enabled", false));

        configuration.setDatabaseHost(getGeneralConfig().getString("storage.database.host", ""));
        configuration.setDatabasePort(getGeneralConfig().getInt("storage.database.port", -1));
        configuration.setDatabaseName(getGeneralConfig().getString("storage.database.database", ""));
        configuration.setDatabaseUsername(getGeneralConfig().getString("storage.database.username", ""));
        configuration.setDatabasePassword(getGeneralConfig().getString("storage.database.password", ""));

        //Verifies that the loaded information is not empty or null.
        boolean errored = false;
        //For upgrades from older versions who didn't have the enable flag but still used MySQL
        boolean mysqlstorageenabledbooleannotloadedyet = false;

        boolean valueChanged = false;

        if (!getGeneralConfig().isBoolean("storage.database.enabled")) {
            getGeneralConfig().set("storage.database.enabled", false);
            mysqlstorageenabledbooleannotloadedyet = true;
            valueChanged = true;
        }

        if (!getGeneralConfig().isString("storage.database.host")) {
            getGeneralConfig().set("storage.database.host", "");
            errored = true;
            valueChanged = true;
        }
        if (!getGeneralConfig().isInt("storage.database.port")) {
            getGeneralConfig().set("storage.database.port", 3306);
            //errored = true;
            configuration.setDatabasePort(3306);
            valueChanged = true;
        }
        if (!getGeneralConfig().isString("storage.database.database")) {
            getGeneralConfig().set("storage.database.database", "");
            errored = true;
            valueChanged = true;
        }
        if (!getGeneralConfig().isString("storage.database.username")) {
            getGeneralConfig().set("storage.database.username", "");
            errored = true;
            valueChanged = true;
        }
        if (!getGeneralConfig().isString("storage.database.password")) {
            getGeneralConfig().set("storage.database.password", "");
            errored = true;
            valueChanged = true;
        }

        //For upgrades from older versions who didn't have the enable flag but still used MySQL
        if(mysqlstorageenabledbooleannotloadedyet && !errored){
            configuration.setMySQLEnabled(true);
            getGeneralConfig().set("storage.database.enabled", true);
        }

        if (!configuration.isMySQLEnabled()) {
            //No need to error previous stuff, since SQLite will be used
            errored = false;
        }


        //Other values from general.yml
        if (!getGeneralConfig().isInt("general.max-active-quests-per-player")) {
            getGeneralConfig().set("general.max-active-quests-per-player", -1);
            valueChanged = true;
        }
        configuration.setMaxActiveQuestsPerPlayer(getGeneralConfig().getInt("general.max-active-quests-per-player"));


        if (!getGeneralConfig().isString("visual.language")) {
            getGeneralConfig().set("visual.language", "en");
            valueChanged = true;
        }
        configuration.setLanguageCode(getGeneralConfig().getString("visual.language"));

        if (!getGeneralConfig().isBoolean("visual.quest-giver-indicator-particle.enabled")) {
            getGeneralConfig().set("visual.quest-giver-indicator-particle.enabled", true);
            valueChanged = true;
        }
        configuration.setQuestGiverIndicatorParticleEnabled(getGeneralConfig().getBoolean("visual.quest-giver-indicator-particle.enabled"));

        if (!getGeneralConfig().isString("visual.quest-giver-indicator-particle.type")) {
            getGeneralConfig().set("visual.quest-giver-indicator-particle.type", "VILLAGER_ANGRY");
            valueChanged = true;
        }
        configuration.setQuestGiverIndicatorParticleType(Particle.valueOf(getGeneralConfig().getString("visual.quest-giver-indicator-particle.type")));

        if (!getGeneralConfig().isInt("visual.quest-giver-indicator-particle.spawn-interval")) {
            getGeneralConfig().set("visual.quest-giver-indicator-particle.spawn-interval", 10);
            valueChanged = true;
        }
        configuration.setQuestGiverIndicatorParticleSpawnInterval(getGeneralConfig().getInt("visual.quest-giver-indicator-particle.spawn-interval"));

        if (!getGeneralConfig().isInt("visual.quest-giver-indicator-particle.count")) {
            getGeneralConfig().set("visual.quest-giver-indicator-particle.count", 1);
            valueChanged = true;
        }
        configuration.setQuestGiverIndicatorParticleCount(getGeneralConfig().getInt("visual.quest-giver-indicator-particle.count"));


        if (!getGeneralConfig().isBoolean("gui.questpreview.enabled")) {
            getGeneralConfig().set("gui.questpreview.enabled", true);
            valueChanged = true;
        }
        configuration.setQuestPreviewUseGUI(getGeneralConfig().getBoolean("gui.questpreview.enabled"));

        if (!getGeneralConfig().isBoolean("gui.usercommands.enabled")) {
            getGeneralConfig().set("gui.usercommands.enabled", true);
            valueChanged = true;
        }
        configuration.setUserCommandsUseGUI(getGeneralConfig().getBoolean("gui.usercommands.enabled"));


        if (!getGeneralConfig().isString("placeholders.player_active_quests_list_horizontal.separator")) {
            getGeneralConfig().set("placeholders.player_active_quests_list_horizontal.separator", " | ");
            valueChanged = true;
        }
        configuration.placeholder_player_active_quests_list_horizontal_separator = getGeneralConfig().getString("placeholders.player_active_quests_list_horizontal.separator");

        if (!getGeneralConfig().isInt("placeholders.player_active_quests_list_horizontal.limit")) {
            getGeneralConfig().set("placeholders.player_active_quests_list_horizontal.limit", -1);
            valueChanged = true;
        }
        configuration.placeholder_player_active_quests_list_horizontal_limit = getGeneralConfig().getInt("placeholders.player_active_quests_list_horizontal.limit");

        if (!getGeneralConfig().isInt("placeholders.player_active_quests_list_vertical.limit")) {
            getGeneralConfig().set("placeholders.player_active_quests_list_vertical.limit", -1);
            valueChanged = true;
        }
        configuration.placeholder_player_active_quests_list_vertical_limit = getGeneralConfig().getInt("placeholders.player_active_quests_list_vertical.limit");


        if (!getGeneralConfig().isBoolean("placeholders.player_active_quests_list_horizontal.use-displayname-if-available")) {
            getGeneralConfig().set("placeholders.player_active_quests_list_horizontal.use-displayname-if-available", true);
            valueChanged = true;
        }
        configuration.placeholder_player_active_quests_list_horizontal_use_displayname_if_available = getGeneralConfig().getBoolean("placeholders.player_active_quests_list_horizontal.use-displayname-if-available");

        if (!getGeneralConfig().isBoolean("placeholders.player_active_quests_list_vertical.use-displayname-if-available")) {
            getGeneralConfig().set("placeholders.player_active_quests_list_vertical.use-displayname-if-available", true);
            valueChanged = true;
        }
        configuration.placeholder_player_active_quests_list_vertical_use_displayname_if_available = getGeneralConfig().getBoolean("placeholders.player_active_quests_list_vertical.use-displayname-if-available");

        if (valueChanged) {
            main.getLogger().info("§5General.yml Configuration was updated with new values! Saving it...");
            saveGeneralConfig();
        }


        //If there was an error loading data from general.yml, the plugin will be disabled
        if (errored) {
            disablePluginAndSaving("Please specify your database information");
        }


    }

    /**
     * Saves the default configuration files (Doesn't replace existing).
     */
    public void saveDefaultConfig() {
        if (!this.generalConfigFile.exists()) {
            System.out.println("AAA");
            this.saveResource("general.yml", false);
        }
    }

    /**
     * This is used for the saving of the default config
     *
     * @param resourcePath path of the resource
     * @param replace      should replace existing resource file?
     */
    public void saveResource(String resourcePath, boolean replace) {
        if (!resourcePath.equals("")) {
            resourcePath = resourcePath.replace('\\', '/');
            InputStream in = main.getResource(resourcePath);
            if (in == null) {
                throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + main.getDataFolder());
            } else {
                File outFile = new File(main.getDataFolder(), resourcePath);
                int lastIndex = resourcePath.lastIndexOf(47);
                File outDir = new File(main.getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));
                if (!outDir.exists()) {
                    outDir.mkdirs();
                }

                try {
                    if (outFile.exists() && !replace) {
                        main.getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
                    } else {
                        OutputStream out = new FileOutputStream(outFile);
                        byte[] buf = new byte[1024];

                        int len;
                        while((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }

                        out.close();
                        in.close();
                    }
                } catch (IOException var10) {
                    main.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, var10);
                }

            }
        } else {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
    }


    /**
     * This will try to save the general.yml configuration file with the data which is currently in the
     * generalConfig FileConfiguration object.
     */
    public void saveGeneralConfig() {
        try {
            getGeneralConfig().save(generalConfigFile);

        } catch (IOException ioException) {
            main.getLogger().log(Level.SEVERE, "§cNotQuests > General Config file could not be saved.");
        }
    }

    /**
     * This will set saving to false, so the plugin will not try to save any kind of data anymore. After that, it
     * disables the plugin.
     *
     * @param reason the reason for disabling saving and the plugin. Will be shown in the console error message
     */
    public void disablePluginAndSaving(final String reason) {
        main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin, saving and loading has been disabled. Reason: " + reason);
        setSavingEnabled(false);
        setLoadingEnabled(false);
        main.getServer().getPluginManager().disablePlugin(main);
    }

    /**
     * If saving is enabled, this will try to save the following data:
     * (1) Player Data into the MySQL Database
     * (2) The quests.yml Quest Configuration file
     * <p>
     * This will not save the general.yml Configuration file
     */
    public void saveData() {
        if (isSavingEnabled()) {
            main.getLogger().log(Level.INFO, "§aNotQuests > Citizens nquestgiver trait has been registered!");
            main.getLogger().log(Level.INFO, "§aNotQuests > Saving player data...");
            main.getQuestPlayerManager().savePlayerData();

            if (questsData == null || questsDataFile == null) {
                main.getLogger().log(Level.SEVERE, "§cNotQuests > Could not save data to quests.yml");
                return;
            }
            try {
                getQuestsData().save(questsDataFile);
                main.getLogger().log(Level.INFO, "§aNotQuests > Saved Data to quests.yml");
            } catch (IOException e) {
                main.getLogger().log(Level.SEVERE, "§cNotQuests > Could not save config to §b" + questsDataFile + "§c. Stacktrace:");
                e.printStackTrace();
            }
        } else {
            main.getLogger().log(Level.WARNING, "§eNotQuests > Saving is disabled => no data has been saved.");
        }


    }

    /**
     * If the plugin has been running for some time, the MySQL Database connection is
     * sometimes interrupted which causes errors when it tries to save data once the plugin
     * is disabled.
     * <p>
     * This is especially bad, because if the plugin has been running for a while, that data will be lost.
     * <p>
     * This method will try to re-open the database connection statement, so data can be saved to the database
     * safely again.
     *
     * @param newTask sets if the plugin should force an asynchronous thread to re-open the database connection. If
     *                set to false, it will do it in whatever thread this method is run in.
     */
    public void refreshDatabaseConnection(final boolean newTask) {
        if (newTask) {
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                    openConnection();
                    try {
                        statement = connection.createStatement();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }


                });
            } else {
                openConnection();
                try {
                    statement = connection.createStatement();
                } catch (SQLException e) {
                    e.printStackTrace();
                }


            }
        } else {
            openConnection();
            try {
                statement = connection.createStatement();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * This will LOAD the following data:
     * (1) general.yml General Configuration - in whatever Thread
     * (2) CREATE all the necessary MySQL Database tables if they don't exist yet - in an asynchronous Thread (forced)
     * (3) Load all the Quests Data from the quests.yml - in an asynchronous Thread (forced)
     * (4) AFTER THAT load the Player Data from the MySQL Database - in an asynchronous Thread (forced)
     * (5) Then it will try to load the Data from Citizens NPCs
     */
    public void reloadData() {
        if(isLoadingEnabled()){
            loadGeneralConfig();
            main.getLanguageManager().loadLanguageConfig();

            //Check for isLoadingEnabled again, in case it changed during loading of the general config
            if(!isLoadingEnabled()){
                main.getLogger().log(Level.SEVERE, "§cNotQuests > Data loading has been skipped, because it has been disabled. This is because there was an error loading from the general config.");
                return;
            }

            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                    openConnection();
                    if(connection == null){
                        main.getLogger().log(Level.SEVERE, "§cNotQuests > There was a database error, so loading has been disabled.");
                        return;
                    }
                    try {
                        statement = connection.createStatement();
                    } catch (SQLException e) {
                        main.getLogger().log(Level.SEVERE, "§cNotQuests > There was a database error, so loading has been disabled.");
                        e.printStackTrace();
                        return;
                    }


                    //Create Database tables if they don't exist yet
                    try {
                        main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'QuestPlayerData' if it doesn't exist yet...");
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), PRIMARY KEY (PlayerUUID))");

                        main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveQuests' if it doesn't exist yet...");
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200))");

                        main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'CompletedQuests' if it doesn't exist yet...");
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255))");

                        main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveObjectives' if it doesn't exist yet...");
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN)");

                        main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveTriggers' if it doesn't exist yet...");

                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveTriggers` (`TriggerType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `TriggerID` INT(255))");


                    } catch (SQLException e) {
                        main.getLogger().log(Level.SEVERE, "§9NotQuests > §cThere was an error while trying to load MySQL database tables! This is the stacktrace:");

                        e.printStackTrace();

                        disablePluginAndSaving("Plugin disabled, because there was an error while initializing tables.");
                        return;
                    }

                    if (isSavingEnabled()) {
                        main.getLogger().log(Level.INFO, "§aNotQuests > Loaded player data");

                        if (questsDataFile == null) {
                            loadQuestsConfig();
                            main.getQuestManager().loadData();

                        } else {
                            main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml... - reloadData");
                            try{
                                questsData = loadYAMLConfiguration(questsDataFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                                disablePluginAndSaving("Plugin disabled, because there was an error loading data from quests.yml.");
                                return;
                            }
                            main.getQuestManager().loadData();

                        }

                        main.getQuestPlayerManager().loadPlayerData();

                        //Citizens stuff if Citizens is enabled
                        if(main.isCitizensEnabled()){
                            //IF an NPC exist, try to load NPC data.
                            boolean foundNPC = false;
                            for (final NPC ignored : CitizensAPI.getNPCRegistry().sorted()) {
                                foundNPC = true;
                                break;
                            }
                            if (foundNPC) {
                                loadNPCData();
                            }
                        }

                    }


                });
            } else { //If this is already an asynchronous thread, this else{ thingy does not try to create a new asynchronous thread for better performance. The contents of this else section is identical.2
                openConnection();
                if(connection == null){
                    main.getLogger().log(Level.SEVERE, "§cNotQuests > There was a database error, so loading has been disabled.");
                    return;
                }
                try {
                    statement = connection.createStatement();
                } catch (SQLException e) {
                    e.printStackTrace();
                    main.getLogger().log(Level.SEVERE, "§cNotQuests > There was a database error, so loading has been disabled.");
                    return;
                }


                //Create Database tables if they don't exist yet
                try {
                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'QuestPlayerData' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), PRIMARY KEY (PlayerUUID))");

                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveQuests' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200))");

                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'CompletedQuests' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255))");

                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveObjectives' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN)");

                } catch (SQLException e) {
                    main.getLogger().log(Level.SEVERE, "§9NotQuests > §cThere was an error while trying to load MySQL database tables! This is the stacktrace:");

                    e.printStackTrace();
                    disablePluginAndSaving("Plugin disabled, because there was an error while initializing tables.");
                    return;
                }

                if (isSavingEnabled()) {

                    main.getLogger().log(Level.INFO, "§aNotQuests > Loaded player data");

                    if (questsDataFile == null) {
                        questsDataFile = new File(main.getDataFolder(), "quests.yml");
                        main.getLogger().log(Level.INFO, "§aNotQuests > First load of quests.yml...");
                        try{
                            questsData = loadYAMLConfiguration(questsDataFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                            disablePluginAndSaving("Plugin disabled, because there was an error loading data from quests.yml.");
                            return;
                        }
                        main.getQuestManager().loadData();
                    } else {
                        main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml...");
                        try{
                            questsData = loadYAMLConfiguration(questsDataFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                            disablePluginAndSaving("Plugin disabled, because there was an error loading data from quests.yml.");
                            return;
                        }
                        main.getQuestManager().loadData();
                    }

                    main.getQuestPlayerManager().loadPlayerData();

                    //IF an NPC exist, try to load NPC data.
                    boolean foundNPC = false;
                    for (final NPC ignored : CitizensAPI.getNPCRegistry().sorted()) {
                        foundNPC = true;
                        break;
                    }
                    if (foundNPC) {
                        loadNPCData();
                    }

                }
            }
        }else{
            main.getLogger().log(Level.SEVERE, "§cNotQuests > Data loading has been skipped, because it has been disabled. This might be caused because of an error during plugin startup earlier.");

        }



    }

    /**
     * This will return the quests.yml Configuration FileConfiguration object.
     * If it does not exist, it will try to load ALL the data again in reloadData()
     * which should also create the quests.yml file
     *
     * @return the quests.yml Configuration FileConfiguration object
     */
    public final FileConfiguration getQuestsData() {
        if (questsData == null) {
            reloadData();
        }
        return questsData;
    }

    /**
     * This will return the general.yml Configuration FileConfiguration object.
     * If it does not exist, it will try to load / create it.
     *
     * @return the general.yml Configuration FileConfiguration object
     */
    public final FileConfiguration getGeneralConfig() {
        if (generalConfig == null) {
            loadGeneralConfig();
        }
        return generalConfig;
    }


    /**
     * This will open a MySQL connection statement which is needed to save and load
     * to the MySQL Database.
     * <p>
     * This is where it tries to log-in to the Database.
     *
     */
    public void openConnection() {



        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }


            if(!getConfiguration().isMySQLEnabled()){
                File dataFolder = new File(main.getDataFolder(), "database_sqlite.db");
                if (!dataFolder.exists()){
                    try {
                        dataFolder.createNewFile();
                    } catch (IOException e) {
                        main.getLogger().log(Level.SEVERE, "File write error: database_sqlite.db");
                    }
                }
                try {
                    if(connection!=null&&!connection.isClosed()){
                        return;
                    }
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
                    return;
                } catch (SQLException ex) {
                    main.getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
                } catch (ClassNotFoundException ex) {
                    main.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
                }
                return;
            }else{
                // Class.forName("com.mysql.jdbc.Driver"); - Use this with old version of the Driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://"
                                + configuration.getDatabaseHost() + ":" + configuration.getDatabasePort() + "/" + configuration.getDatabaseName() + "?autoReconnect=true",
                        configuration.getDatabaseUsername(), configuration.getDatabasePassword());
            }




        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            disablePluginAndSaving("Could not connect to MySQL Database. Please check the information you entered in the general.yml. A MySQL Database is NECESSARY for this plugin to work (as described on the spigot page).");
        }

    }


    /**
     * This returns the MySQL Database Statement, with which Database operations can be
     * performed / executed.
     *
     * @return the MySQL Database Statement
     */
    public final Statement getDatabaseStatement() {
        return statement;
    }

    /**
     * @return if the plugin will try to save data once it's disabled.
     * This should be true unless a severe error occurred during data
     * loading
     */
    public final boolean isSavingEnabled() {
        return savingEnabled;
    }

    /**
     * @param savingEnabled sets if data saving should be enabled or disabled
     */
    public void setSavingEnabled(final boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }

    /**
     * @return if the plugin will try to load data.
     * This should be true unless a severe error occurred during data
     * loading
     */
    public final boolean isLoadingEnabled() {
        return loadingEnabled;
    }

    /**
     * @param loadingEnabled sets if data loading should be enabled or disabled
     */
    public void setLoadingEnabled(final boolean loadingEnabled) {
        this.loadingEnabled = loadingEnabled;
    }

    /**
     * This will load the Data from Citizens NPCs asynchronously. It will also make sure
     * that the quests.yml configuration object is valid first.
     * <p>
     * The actual loading of Citizens NPC data will happen in the loadNPCData() function of
     * the Quest Manager. In that function, most of that will run synchronously as that's required
     * for some operations with the Citizens API.
     */
    public void loadNPCData() {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {

                if (questsDataFile == null) {
                    loadQuestsConfig();

                    main.getQuestManager().loadNPCData();

                } else {
                    //Unnecessary?main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml - loadNPCData");
                    //Unnecessary?questsData = YamlConfiguration.loadConfiguration(questsDataFile); //Why necessary?
                    main.getQuestManager().loadNPCData();

                }
            });
        } else {

            if (questsDataFile == null) {
                loadQuestsConfig();
                main.getQuestManager().loadNPCData();

            } else {
                //Unnecessary?main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml - loadNPCData");
                //Unnecessary?questsData = YamlConfiguration.loadConfiguration(questsDataFile); //Why necessary?
                main.getQuestManager().loadNPCData();

            }

        }

    }


    /**
     * @return if Citizen NPCs have been already successfully loaded by the plugin
     */
    public boolean isAlreadyLoadedNPCs() {
        return alreadyLoadedNPCs;
    }

    /**
     * @param alreadyLoadedNPCs sets if Citizen NPCs have been already successfully loaded by the plugin
     */
    public void setAlreadyLoadedNPCs(boolean alreadyLoadedNPCs) {
        this.alreadyLoadedNPCs = alreadyLoadedNPCs;
    }

    /**
     * Utility function: Returns the UUID of an online player. If the player is
     * offline, it will return null.
     *
     * @param playerName the name of the online player you want to get the UUID from
     * @return the UUID of the specified, online player
     */
    public final UUID getOnlineUUID(final String playerName) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        } else {
            return null;
        }
    }

    /**
     * Utility function: Tries to return the UUID of an offline player (can also be online)
     * via some weird Bukkit function. This probably makes calls to the Minecraft API, I don't
     * know for sure. It's definitely slower.
     *
     * @param playerName the name of the player you want to get the UUID from
     * @return the UUID from the player based on his current username.
     */
    public final UUID getOfflineUUID(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }

    /**
     * @return the configuration object which contains values from the General.yml
     */
    public final Configuration getConfiguration(){
        return configuration;
    }




    /**
     *
     * (Taken from API, but this method also throws errors)
     *
     * Creates a new {@link YamlConfiguration}, loading from the given file.
     * <p>
     * Any errors loading the Configuration will be logged and then ignored.
     * If the specified input is not a valid config, a blank config will be
     * returned.
     * <p>
     * The encoding used may follow the system dependent default.
     *
     * @param file Input file
     * @return Resulting configuration
     * @throws IllegalArgumentException Thrown if file is null
     */
    public static YamlConfiguration loadYAMLConfiguration(File file) throws IOException, InvalidConfigurationException {
        Validate.notNull(file, "File cannot be null");

        YamlConfiguration config = new YamlConfiguration();

        config.load(file);


        return config;
    }

}
