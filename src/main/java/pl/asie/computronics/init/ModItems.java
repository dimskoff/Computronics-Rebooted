package pl.asie.computronics.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.item.*;
import pl.asie.computronics.item.rack.*;

import java.util.function.Function;

/**
 * Item registry. Registry names must never change: existing worlds reference them.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Computronics.MODID);

    private ModItems() {}

    /** Registers the {@link BlockItem} for a block, using the block's registry name. */
    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> block) {
        return ITEMS.registerSimpleBlockItem(block);
    }

    private static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory) {
        return ITEMS.registerItem(name, factory, new Item.Properties());
    }

    private static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory, Rarity rarity) {
        return ITEMS.registerItem(name, factory, new Item.Properties().rarity(rarity));
    }

    private static DeferredItem<ItemTape> tape(String name, int capacity, Rarity rarity) {
        return ITEMS.registerItem(name, props -> new ItemTape(props, capacity), new Item.Properties().rarity(rarity));
    }

    // Block Items
    public static final DeferredItem<BlockItem> CHAT_BOX = blockItem(ModBlocks.CHAT_BOX);
    public static final DeferredItem<BlockItem> RADAR = blockItem(ModBlocks.RADAR);
    public static final DeferredItem<BlockItem> CAMERA = blockItem(ModBlocks.CAMERA);
    public static final DeferredItem<BlockItem> TAPE_DRIVE = blockItem(ModBlocks.TAPE_DRIVE);
    public static final DeferredItem<BlockItem> ACOUSTIC_SENSOR = blockItem(ModBlocks.ACOUSTIC_SENSOR);
    public static final DeferredItem<BlockItem> RF_JAMMER = blockItem(ModBlocks.RF_JAMMER);
    public static final DeferredItem<BlockItem> WEATHER_STATION = blockItem(ModBlocks.WEATHER_STATION);
    public static final DeferredItem<BlockItem> INDUCTIVE_CHARGER = blockItem(ModBlocks.INDUCTIVE_CHARGER);
    public static final DeferredItem<BlockItem> LASER_TRANSCEIVER = blockItem(ModBlocks.LASER_TRANSCEIVER);
    public static final DeferredItem<BlockItem> ANALOG_GAUGE = blockItem(ModBlocks.ANALOG_GAUGE);
    public static final DeferredItem<BlockItem> IRON_NOTEBLOCK = blockItem(ModBlocks.IRON_NOTEBLOCK);
    public static final DeferredItem<BlockItem> CONTROL_PANEL = blockItem(ModBlocks.CONTROL_PANEL);
    public static final DeferredItem<BlockItem> SPEECH_BOX = blockItem(ModBlocks.SPEECH_BOX);
    public static final DeferredItem<BlockItem> ENTITY_DETECTOR = blockItem(ModBlocks.ENTITY_DETECTOR);
    public static final DeferredItem<BlockItem> CONNECTOR = blockItem(ModBlocks.CONNECTOR);

    // Handheld Terminals
    public static final DeferredItem<ItemPocketTerminal> POCKET_TERMINAL = item("pocket_terminal", ItemPocketTerminal::new, Rarity.RARE);

    // Control Panel Modular Elements
    public static final DeferredItem<ItemMiniLamp> MINI_LAMP = item("mini_lamp", ItemMiniLamp::new);
    public static final DeferredItem<ItemMiniButton> MINI_BUTTON = item("mini_button", ItemMiniButton::new);
    public static final DeferredItem<ItemToggleSwitch> TOGGLE_SWITCH = item("toggle_switch", ItemToggleSwitch::new);
    public static final DeferredItem<ItemSevenSegment> SEVEN_SEGMENT = item("seven_segment", ItemSevenSegment::new);
    public static final DeferredItem<ItemPanelText> PANEL_TEXT = item("panel_text", ItemPanelText::new);
    public static final DeferredItem<ItemHoldButton> HOLD_BUTTON = item("hold_button", ItemHoldButton::new);
    public static final DeferredItem<ItemSignalWire> SIGNAL_WIRE = item("signal_wire", ItemSignalWire::new);
    public static final DeferredItem<ItemScrewdriver> SCREWDRIVER = item("screwdriver", ItemScrewdriver::new);

    // Server Rack Mountable Boards
    public static final DeferredItem<ItemRackLightBoard> RACK_BOARD_LIGHT = item("rack_board_light", ItemRackLightBoard::new, Rarity.UNCOMMON);
    public static final DeferredItem<ItemRackSwitchBoard> RACK_BOARD_SWITCH = item("rack_board_switch", ItemRackSwitchBoard::new, Rarity.UNCOMMON);
    public static final DeferredItem<ItemRackBoomBoard> RACK_BOARD_BOOM = item("rack_board_boom", ItemRackBoomBoard::new, Rarity.RARE);
    public static final DeferredItem<ItemRackCapacitorBoard> RACK_BOARD_CAPACITOR = item("rack_board_capacitor", ItemRackCapacitorBoard::new, Rarity.UNCOMMON);
    public static final DeferredItem<ItemRackCoProcessor> RACK_BOARD_CO_PROCESSOR = item("rack_board_co_processor", ItemRackCoProcessor::new, Rarity.RARE);

    // Hardware Cards & Upgrades
    public static final DeferredItem<ItemNetworkCardTier3> NETWORK_CARD_3 = item("network_card_3", ItemNetworkCardTier3::new, Rarity.RARE);
    public static final DeferredItem<ItemPacketSniffer> PACKET_SNIFFER = item("packet_sniffer", ItemPacketSniffer::new, Rarity.UNCOMMON);
    public static final DeferredItem<ItemCardBoom> CARD_BOOM = item("card_boom", ItemCardBoom::new, Rarity.RARE);
    public static final DeferredItem<ItemRFJammerCard> RF_JAMMER_CARD = item("rf_jammer_card", ItemRFJammerCard::new, Rarity.UNCOMMON);
    public static final DeferredItem<ItemCardBeep> CARD_BEEP = item("card_beep", ItemCardBeep::new, Rarity.UNCOMMON);
    public static final DeferredItem<ItemCardFX> CARD_FX = item("card_fx", ItemCardFX::new, Rarity.RARE);

    public static final DeferredItem<ItemChatUpgrade> CHAT_UPGRADE = item("chat_upgrade", ItemChatUpgrade::new);
    public static final DeferredItem<ItemRadarUpgrade> RADAR_UPGRADE = item("radar_upgrade", ItemRadarUpgrade::new, Rarity.UNCOMMON);
    public static final DeferredItem<ItemCameraUpgrade> CAMERA_UPGRADE = item("camera_upgrade", ItemCameraUpgrade::new);
    public static final DeferredItem<ItemWinchUpgrade> WINCH_UPGRADE = item("winch_upgrade", ItemWinchUpgrade::new);
    public static final DeferredItem<ItemSpeechUpgrade> SPEECH_UPGRADE = item("speech_upgrade", ItemSpeechUpgrade::new, Rarity.UNCOMMON);

    // Cassette Tapes
    public static final DeferredItem<ItemTape> TAPE_IRON = tape("tape_iron", ItemTape.CAPACITY_IRON, Rarity.COMMON);
    public static final DeferredItem<ItemTape> TAPE_GOLD = tape("tape_gold", ItemTape.CAPACITY_GOLD, Rarity.COMMON);
    public static final DeferredItem<ItemTape> TAPE_DIAMOND = tape("tape_diamond", ItemTape.CAPACITY_DIAMOND, Rarity.COMMON);
    public static final DeferredItem<ItemTape> TAPE_NETHER_STAR = tape("tape_nether_star", ItemTape.CAPACITY_NETHER_STAR, Rarity.EPIC);
}
