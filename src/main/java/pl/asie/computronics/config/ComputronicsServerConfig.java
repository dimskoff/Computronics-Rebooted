package pl.asie.computronics.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ComputronicsServerConfig {

    public static final ComputronicsServerConfig SERVER;
    public static final ModConfigSpec SPEC;

    static {
        final Pair<ComputronicsServerConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(ComputronicsServerConfig::new);
        SERVER = pair.getLeft();
        SPEC = pair.getRight();
    }

    // =========================================================================
    // General & Feature Toggles
    // =========================================================================
    public final ModConfigSpec.BooleanValue enableControlPanel;
    public final ModConfigSpec.BooleanValue enableChatBox;
    public final ModConfigSpec.BooleanValue enableSpeechBox;
    public final ModConfigSpec.BooleanValue enableTapeDrive;
    public final ModConfigSpec.BooleanValue enableCamera;
    public final ModConfigSpec.BooleanValue enableRadar;
    public final ModConfigSpec.BooleanValue enableEntityDetector;
    public final ModConfigSpec.BooleanValue enableAcousticSensor;
    public final ModConfigSpec.BooleanValue enableWeatherStation;
    public final ModConfigSpec.BooleanValue enableLaserTransceiver;
    public final ModConfigSpec.BooleanValue enableRFJammer;
    public final ModConfigSpec.BooleanValue enableInductiveCharger;
    public final ModConfigSpec.BooleanValue enableAnalogGauge;
    public final ModConfigSpec.BooleanValue enableIronNoteblock;

    public final ModConfigSpec.BooleanValue enableWirelessMonitor;
    public final ModConfigSpec.BooleanValue enableNetworkCardTier3;
    public final ModConfigSpec.BooleanValue enablePacketSniffer;
    public final ModConfigSpec.BooleanValue enableCardBoom;
    public final ModConfigSpec.BooleanValue enableCardBeep;
    public final ModConfigSpec.BooleanValue enableCardFX;
    public final ModConfigSpec.BooleanValue enableSpeechUpgrade;
    public final ModConfigSpec.BooleanValue enableChatUpgrade;
    public final ModConfigSpec.BooleanValue enableRadarUpgrade;
    public final ModConfigSpec.BooleanValue enableCameraUpgrade;
    public final ModConfigSpec.BooleanValue enableWinchUpgrade;

    public final ModConfigSpec.BooleanValue enableRackLightBoard;
    public final ModConfigSpec.BooleanValue enableRackSwitchBoard;
    public final ModConfigSpec.BooleanValue enableRackCoProcessor;
    public final ModConfigSpec.BooleanValue enableRackCapacitor;
    public final ModConfigSpec.BooleanValue enableRackBoom;

    // =========================================================================
    // Control Panel
    // =========================================================================
    public final ModConfigSpec.IntValue panelMaxLines;
    public final ModConfigSpec.IntValue panelMaxCharactersPerLine;
    public final ModConfigSpec.BooleanValue panelAllowCustomColors;
    public final ModConfigSpec.IntValue panelMaxUpdatesPerSecond;

    // =========================================================================
    // Chat Box & Speech Box
    // =========================================================================
    public final ModConfigSpec.IntValue chatBoxMaxDistance;
    public final ModConfigSpec.BooleanValue chatBoxAllowInterdimensional;
    public final ModConfigSpec.ConfigValue<String> chatBoxDefaultPrefix;
    public final ModConfigSpec.IntValue chatBoxCooldownTicks;

    public final ModConfigSpec.DoubleValue speechBoxVolume;
    public final ModConfigSpec.IntValue speechBoxMaxRange;
    public final ModConfigSpec.IntValue speechBoxMaxTextLength;
    public final ModConfigSpec.IntValue speechBoxCooldownTicks;

    // =========================================================================
    // Radar & Entity Detector
    // =========================================================================
    public final ModConfigSpec.IntValue radarMaxRadius;
    public final ModConfigSpec.DoubleValue radarEnergyCostPerScan;
    public final ModConfigSpec.IntValue radarScanCooldownTicks;
    public final ModConfigSpec.BooleanValue radarDetectPlayers;
    public final ModConfigSpec.BooleanValue radarDetectMobs;

    public final ModConfigSpec.IntValue entityDetectorMaxRange;
    public final ModConfigSpec.IntValue entityDetectorScanCooldownTicks;
    public final ModConfigSpec.BooleanValue entityDetectorShowExactCoordinates;

    // =========================================================================
    // Acoustic Sensor
    // =========================================================================
    public final ModConfigSpec.IntValue acousticSensorMaxRadius;
    public final ModConfigSpec.BooleanValue acousticSensorDetectPlayersOnly;

    // =========================================================================
    // Tape Drive & Audio Media
    // =========================================================================
    public final ModConfigSpec.IntValue tapeDriveAudioRadius;
    public final ModConfigSpec.IntValue tapeDriveBufferMs;
    public final ModConfigSpec.IntValue tapeIronMinutes;
    public final ModConfigSpec.IntValue tapeGoldMinutes;
    public final ModConfigSpec.IntValue tapeDiamondMinutes;
    public final ModConfigSpec.IntValue tapeNetherStarMinutes;

    // =========================================================================
    // Networking & RF Jammer
    // =========================================================================
    public final ModConfigSpec.IntValue rfJammerMaxRange;
    public final ModConfigSpec.DoubleValue rfJammerEnergyPerSecond;
    public final ModConfigSpec.IntValue packetSnifferMaxPacketsPerTick;
    public final ModConfigSpec.IntValue jumboFramesMaxPacketSize;
    public final ModConfigSpec.IntValue laserMaxDistance;

    // =========================================================================
    // Self-Destruct (Grief Prevention & Safety)
    // =========================================================================
    public final ModConfigSpec.BooleanValue cardBoomAllowBlockDamage;
    public final ModConfigSpec.DoubleValue cardBoomExplosionPower;
    public final ModConfigSpec.IntValue cardBoomFuseTicks;

    public final ModConfigSpec.BooleanValue rackBoomAllowBlockDamage;
    public final ModConfigSpec.DoubleValue rackBoomExplosionPower;

    // =========================================================================
    // Wireless Monitor (Portable Widescreen)
    // =========================================================================
    public final ModConfigSpec.DoubleValue wirelessMonitorMaxEnergy;
    public final ModConfigSpec.DoubleValue wirelessMonitorEnergyUsagePerSecond;
    public final ModConfigSpec.IntValue wirelessMonitorDefaultPort;
    public final ModConfigSpec.BooleanValue wirelessMonitorAllowEEPROMFlashing;

    // =========================================================================
    // Server Rack Boards
    // =========================================================================
    public final ModConfigSpec.DoubleValue rackCapacitorCapacity;
    public final ModConfigSpec.DoubleValue rackLightBoardEnergyPerTick;
    public final ModConfigSpec.DoubleValue rackCoProcessorSpeedMultiplier;
    public final ModConfigSpec.IntValue rackSwitchBoardSignalCooldown;

    public ComputronicsServerConfig(ModConfigSpec.Builder b) {
        b.comment("Computronics Rebooted - Server & Mechanics Configuration");

        // ---------------------------------------------------------------------
        b.push("features");
        b.comment("Granular feature toggles. Set to false to disable specific blocks, cards, or peripherals.");
        enableControlPanel = b.comment("Enable Modular Control Panels").define("enableControlPanel", true);
        enableChatBox = b.comment("Enable Chat Box block").define("enableChatBox", true);
        enableSpeechBox = b.comment("Enable Speech Box TTS block").define("enableSpeechBox", true);
        enableTapeDrive = b.comment("Enable Tape Drive and Audio Cassettes").define("enableTapeDrive", true);
        enableCamera = b.comment("Enable Optical Camera block").define("enableCamera", true);
        enableRadar = b.comment("Enable 3D Radar block").define("enableRadar", true);
        enableEntityDetector = b.comment("Enable Directional Entity Detector").define("enableEntityDetector", true);
        enableAcousticSensor = b.comment("Enable Sculk Acoustic Vibration Sensor").define("enableAcousticSensor", true);
        enableWeatherStation = b.comment("Enable Weather Station block").define("enableWeatherStation", true);
        enableLaserTransceiver = b.comment("Enable Laser Network Transceiver").define("enableLaserTransceiver", true);
        enableRFJammer = b.comment("Enable RF Jammer block").define("enableRFJammer", true);
        enableInductiveCharger = b.comment("Enable Inductive Charger pad").define("enableInductiveCharger", true);
        enableAnalogGauge = b.comment("Enable Analog Dial Gauge").define("enableAnalogGauge", true);
        enableIronNoteblock = b.comment("Enable Iron Note Block").define("enableIronNoteblock", true);

        enableWirelessMonitor = b.comment("Enable Wireless Monitor (Portable Widescreen)").define("enableWirelessMonitor", true);
        enableNetworkCardTier3 = b.comment("Enable Tier 3 Jumbo-Frame Network Card").define("enableNetworkCardTier3", true);
        enablePacketSniffer = b.comment("Enable Network Packet Sniffer Card").define("enablePacketSniffer", true);
        enableCardBoom = b.comment("Enable Self-Destruct Card").define("enableCardBoom", true);
        enableCardBeep = b.comment("Enable Audio Beep Synthesizer Card").define("enableCardBeep", true);
        enableCardFX = b.comment("Enable Particle FX Card").define("enableCardFX", true);
        enableSpeechUpgrade = b.comment("Enable Speech Synthesizer Robot Upgrade").define("enableSpeechUpgrade", true);
        enableChatUpgrade = b.comment("Enable Chat Box Robot Upgrade").define("enableChatUpgrade", true);
        enableRadarUpgrade = b.comment("Enable Radar Robot Upgrade").define("enableRadarUpgrade", true);
        enableCameraUpgrade = b.comment("Enable Camera Robot Upgrade").define("enableCameraUpgrade", true);
        enableWinchUpgrade = b.comment("Enable Winch Robot Upgrade").define("enableWinchUpgrade", true);

        enableRackLightBoard = b.comment("Enable Server Rack Light Board").define("enableRackLightBoard", true);
        enableRackSwitchBoard = b.comment("Enable Server Rack Switch Board").define("enableRackSwitchBoard", true);
        enableRackCoProcessor = b.comment("Enable Server Rack Crypto Co-Processor").define("enableRackCoProcessor", true);
        enableRackCapacitor = b.comment("Enable Server Rack Capacitor Board").define("enableRackCapacitor", true);
        enableRackBoom = b.comment("Enable Server Rack Self-Destruct Board").define("enableRackBoom", true);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("control_panel");
        b.comment("Modular Control Panel mechanics and rate limits.");
        panelMaxLines = b.comment("Maximum number of text lines per button/lamp/text element (1-8).")
                .defineInRange("panelMaxLines", 4, 1, 8);
        panelMaxCharactersPerLine = b.comment("Maximum text length per line.")
                .defineInRange("panelMaxCharactersPerLine", 32, 1, 128);
        panelAllowCustomColors = b.comment("Allow players to customize RGB hex colors on panel elements.")
                .define("panelAllowCustomColors", true);
        panelMaxUpdatesPerSecond = b.comment("Rate limit: Maximum software update calls per second per panel (anti-spam).")
                .defineInRange("panelMaxUpdatesPerSecond", 20, 1, 100);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("chat_box");
        b.comment("Chat Box peripheral configuration and limits.");
        chatBoxMaxDistance = b.comment("Maximum broadcast radius in blocks. Set to -1 for unlimited interdimensional reach.")
                .defineInRange("chatBoxMaxDistance", 40, -1, 32767);
        chatBoxAllowInterdimensional = b.comment("Whether Chat Boxes can send messages across dimensions.")
                .define("chatBoxAllowInterdimensional", false);
        chatBoxDefaultPrefix = b.comment("Default name prefix displayed in chat.")
                .define("chatBoxDefaultPrefix", "[ChatBox]");
        chatBoxCooldownTicks = b.comment("Minimum tick cooldown between chat broadcasts per box (20 ticks = 1 sec).")
                .defineInRange("chatBoxCooldownTicks", 20, 0, 1200);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("speech_box");
        b.comment("Speech Box audio speech synthesizer configuration.");
        speechBoxVolume = b.comment("Base audio volume (0.0 to 2.0).")
                .defineInRange("speechBoxVolume", 1.0, 0.0, 2.0);
        speechBoxMaxRange = b.comment("Maximum block radius where speech sounds can be heard.")
                .defineInRange("speechBoxMaxRange", 32, 4, 128);
        speechBoxMaxTextLength = b.comment("Maximum characters allowed in a single speech synthesis call.")
                .defineInRange("speechBoxMaxTextLength", 120, 1, 500);
        speechBoxCooldownTicks = b.comment("Cooldown ticks between spoken phrases per box.")
                .defineInRange("speechBoxCooldownTicks", 40, 0, 1200);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("radar");
        b.comment("Radar scanning options and server performance balancing.");
        radarMaxRadius = b.comment("Maximum scanning radius in blocks.")
                .defineInRange("radarMaxRadius", 32, 4, 128);
        radarEnergyCostPerScan = b.comment("Energy cost per radar scan in OpenComputers EU.")
                .defineInRange("radarEnergyCostPerScan", 15.0, 0.0, 1000.0);
        radarScanCooldownTicks = b.comment("Cooldown in ticks between radar scans.")
                .defineInRange("radarScanCooldownTicks", 20, 0, 600);
        radarDetectPlayers = b.comment("Whether radar can detect other players.")
                .define("radarDetectPlayers", true);
        radarDetectMobs = b.comment("Whether radar can detect living mobs and animals.")
                .define("radarDetectMobs", true);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("entity_detector");
        b.comment("Directional Entity Detector settings.");
        entityDetectorMaxRange = b.comment("Maximum forward detection cone range in blocks.")
                .defineInRange("entityDetectorMaxRange", 16, 2, 64);
        entityDetectorScanCooldownTicks = b.comment("Cooldown in ticks between detection sweeps.")
                .defineInRange("entityDetectorScanCooldownTicks", 10, 0, 200);
        entityDetectorShowExactCoordinates = b.comment("If true, returns exact coordinates. If false, only distance and relative angle are returned (useful for PvP servers).")
                .define("entityDetectorShowExactCoordinates", true);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("acoustic_sensor");
        b.comment("Sculk acoustic vibration sensor settings.");
        acousticSensorMaxRadius = b.comment("Maximum vibration detection radius in blocks.")
                .defineInRange("acousticSensorMaxRadius", 32, 4, 64);
        acousticSensorDetectPlayersOnly = b.comment("If true, ignores mobs and only triggers on player movements/actions.")
                .define("acousticSensorDetectPlayersOnly", false);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("tape_drive");
        b.comment("Audio tape drive and tape media parameters.");
        tapeDriveAudioRadius = b.comment("Maximum listening radius for tape audio playback in blocks.")
                .defineInRange("tapeDriveAudioRadius", 24, 4, 128);
        tapeDriveBufferMs = b.comment("Audio streaming buffer size in milliseconds.")
                .defineInRange("tapeDriveBufferMs", 750, 100, 5000);
        tapeIronMinutes = b.comment("Storage capacity of Iron Cassette Tape in minutes.")
                .defineInRange("tapeIronMinutes", 4, 1, 60);
        tapeGoldMinutes = b.comment("Storage capacity of Gold Cassette Tape in minutes.")
                .defineInRange("tapeGoldMinutes", 8, 1, 120);
        tapeDiamondMinutes = b.comment("Storage capacity of Diamond Cassette Tape in minutes.")
                .defineInRange("tapeDiamondMinutes", 16, 1, 240);
        tapeNetherStarMinutes = b.comment("Storage capacity of Nether Star Cassette Tape in minutes.")
                .defineInRange("tapeNetherStarMinutes", 32, 1, 480);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("networking");
        b.comment("Network cards, transceivers, and RF jamming parameters.");
        rfJammerMaxRange = b.comment("Maximum interference radius for RF Jammer in blocks.")
                .defineInRange("rfJammerMaxRange", 64, 8, 256);
        rfJammerEnergyPerSecond = b.comment("Power consumption of RF Jammer in EU per second.")
                .defineInRange("rfJammerEnergyPerSecond", 20.0, 0.0, 1000.0);
        packetSnifferMaxPacketsPerTick = b.comment("Maximum packets captured per tick to prevent buffer overflow.")
                .defineInRange("packetSnifferMaxPacketsPerTick", 100, 10, 10000);
        jumboFramesMaxPacketSize = b.comment("Maximum packet payload size for Tier 3 Network Card in bytes.")
                .defineInRange("jumboFramesMaxPacketSize", 65536, 1024, 262144);
        laserMaxDistance = b.comment("Maximum line-of-sight range for Laser Transceiver in blocks.")
                .defineInRange("laserMaxDistance", 64, 8, 256);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("self_destruct");
        b.comment("Self-destruct cards and server rack detonators (Anti-Griefing).");
        cardBoomAllowBlockDamage = b.comment("If false, the card explosion will damage entities but will NEVER break or destroy world blocks (vital for multiplayer servers!).")
                .define("cardBoomAllowBlockDamage", true);
        cardBoomExplosionPower = b.comment("Explosion power for computer Self-Destruct Card.")
                .defineInRange("cardBoomExplosionPower", 3.5, 0.5, 12.0);
        cardBoomFuseTicks = b.comment("Minimum fuse delay in ticks.")
                .defineInRange("cardBoomFuseTicks", 20, 0, 1200);

        rackBoomAllowBlockDamage = b.comment("If false, the rack detonator will damage entities but will NEVER break or destroy world blocks.")
                .define("rackBoomAllowBlockDamage", true);
        rackBoomExplosionPower = b.comment("Explosion power for server rack Detonator Board.")
                .defineInRange("rackBoomExplosionPower", 4.5, 0.5, 16.0);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("wireless_monitor");
        b.comment("Portable widescreen Wireless Monitor (handheld).");
        wirelessMonitorMaxEnergy = b.comment("Maximum internal battery capacity in EU.")
                .defineInRange("wirelessMonitorMaxEnergy", 20000.0, 1000.0, 500000.0);
        wirelessMonitorEnergyUsagePerSecond = b.comment("Power consumption while active in EU per second.")
                .defineInRange("wirelessMonitorEnergyUsagePerSecond", 1.0, 0.0, 50.0);
        wirelessMonitorDefaultPort = b.comment("Default listening network port.")
                .defineInRange("wirelessMonitorDefaultPort", 404, 1, 65535);
        wirelessMonitorAllowEEPROMFlashing = b.comment("Allow flashing firmware onto the monitor via Shift+Right-Click with an EEPROM.")
                .define("wirelessMonitorAllowEEPROMFlashing", true);
        b.pop();

        // ---------------------------------------------------------------------
        b.push("server_rack");
        b.comment("Server rack expansion board parameters.");
        rackCapacitorCapacity = b.comment("Energy storage capacity of Rack Capacitor Board in EU.")
                .defineInRange("rackCapacitorCapacity", 50000.0, 1000.0, 1000000.0);
        rackLightBoardEnergyPerTick = b.comment("Energy cost per tick per active LED on Light Board.")
                .defineInRange("rackLightBoardEnergyPerTick", 0.05, 0.0, 10.0);
        rackCoProcessorSpeedMultiplier = b.comment("Crypto Co-Processor hashing speed multiplier (1.0 = standard, 2.0 = 2x faster).")
                .defineInRange("rackCoProcessorSpeedMultiplier", 1.0, 0.1, 10.0);
        rackSwitchBoardSignalCooldown = b.comment("Tick cooldown between switch_flipped signals per switch.")
                .defineInRange("rackSwitchBoardSignalCooldown", 5, 0, 100);
        b.pop();
    }
}
