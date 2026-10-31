package pl.asie.computronics.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.item.*;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Computronics.MODID);

    // Block Items
    public static final DeferredItem<BlockItem> CHAT_BOX = ITEMS.register("chat_box",
            () -> new BlockItem(ModBlocks.CHAT_BOX.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RADAR = ITEMS.register("radar",
            () -> new BlockItem(ModBlocks.RADAR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CAMERA = ITEMS.register("camera",
            () -> new BlockItem(ModBlocks.CAMERA.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TAPE_DRIVE = ITEMS.register("tape_drive",
            () -> new BlockItem(ModBlocks.TAPE_DRIVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ACOUSTIC_SENSOR = ITEMS.register("acoustic_sensor",
            () -> new BlockItem(ModBlocks.ACOUSTIC_SENSOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RF_JAMMER = ITEMS.register("rf_jammer",
            () -> new BlockItem(ModBlocks.RF_JAMMER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WEATHER_STATION = ITEMS.register("weather_station",
            () -> new BlockItem(ModBlocks.WEATHER_STATION.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> INDUCTIVE_CHARGER = ITEMS.register("inductive_charger",
            () -> new BlockItem(ModBlocks.INDUCTIVE_CHARGER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LASER_TRANSCEIVER = ITEMS.register("laser_transceiver",
            () -> new BlockItem(ModBlocks.LASER_TRANSCEIVER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ANALOG_GAUGE = ITEMS.register("analog_gauge",
            () -> new BlockItem(ModBlocks.ANALOG_GAUGE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> IRON_NOTEBLOCK = ITEMS.register("iron_noteblock",
            () -> new BlockItem(ModBlocks.IRON_NOTEBLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CONTROL_PANEL = ITEMS.register("control_panel",
            () -> new BlockItem(ModBlocks.CONTROL_PANEL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SPEECH_BOX = ITEMS.register("speech_box",
            () -> new BlockItem(ModBlocks.SPEECH_BOX.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ENTITY_DETECTOR = ITEMS.register("entity_detector",
            () -> new BlockItem(ModBlocks.ENTITY_DETECTOR.get(), new Item.Properties()));

    // Handheld Terminals
    public static final DeferredItem<ItemPocketTerminal> POCKET_TERMINAL = ITEMS.register("pocket_terminal",
            () -> new ItemPocketTerminal(new Item.Properties().rarity(Rarity.RARE)));

    // Control Panel Modular Elements
    public static final DeferredItem<ItemMiniLamp> MINI_LAMP = ITEMS.register("mini_lamp",
            () -> new ItemMiniLamp(new Item.Properties()));
    public static final DeferredItem<ItemMiniButton> MINI_BUTTON = ITEMS.register("mini_button",
            () -> new ItemMiniButton(new Item.Properties()));

    // Hardware Cards & Upgrades
    public static final DeferredItem<ItemNetworkCardTier3> NETWORK_CARD_3 = ITEMS.register("network_card_3",
            () -> new ItemNetworkCardTier3(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<ItemPacketSniffer> PACKET_SNIFFER = ITEMS.register("packet_sniffer",
            () -> new ItemPacketSniffer(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ItemCardBoom> CARD_BOOM = ITEMS.register("card_boom",
            () -> new ItemCardBoom(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<ItemRFJammerCard> RF_JAMMER_CARD = ITEMS.register("rf_jammer_card",
            () -> new ItemRFJammerCard(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ItemCardBeep> CARD_BEEP = ITEMS.register("card_beep",
            () -> new ItemCardBeep(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ItemCardFX> CARD_FX = ITEMS.register("card_fx",
            () -> new ItemCardFX(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<ItemChatUpgrade> CHAT_UPGRADE = ITEMS.register("chat_upgrade",
            () -> new ItemChatUpgrade(new Item.Properties()));
    public static final DeferredItem<ItemRadarUpgrade> RADAR_UPGRADE = ITEMS.register("radar_upgrade",
            () -> new ItemRadarUpgrade(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ItemCameraUpgrade> CAMERA_UPGRADE = ITEMS.register("camera_upgrade",
            () -> new ItemCameraUpgrade(new Item.Properties()));
    public static final DeferredItem<ItemWinchUpgrade> WINCH_UPGRADE = ITEMS.register("winch_upgrade",
            () -> new ItemWinchUpgrade(new Item.Properties()));
    public static final DeferredItem<ItemSpeechUpgrade> SPEECH_UPGRADE = ITEMS.register("speech_upgrade",
            () -> new ItemSpeechUpgrade(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Cassette Tapes
    public static final DeferredItem<ItemTape> TAPE_IRON = ITEMS.register("tape_iron",
            () -> new ItemTape(new Item.Properties(), ItemTape.CAPACITY_IRON));
    public static final DeferredItem<ItemTape> TAPE_GOLD = ITEMS.register("tape_gold",
            () -> new ItemTape(new Item.Properties(), ItemTape.CAPACITY_GOLD));
    public static final DeferredItem<ItemTape> TAPE_DIAMOND = ITEMS.register("tape_diamond",
            () -> new ItemTape(new Item.Properties(), ItemTape.CAPACITY_DIAMOND));
    public static final DeferredItem<ItemTape> TAPE_NETHER_STAR = ITEMS.register("tape_nether_star",
            () -> new ItemTape(new Item.Properties().rarity(Rarity.EPIC), ItemTape.CAPACITY_NETHER_STAR));
}
