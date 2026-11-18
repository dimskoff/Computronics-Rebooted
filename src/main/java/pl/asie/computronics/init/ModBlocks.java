package pl.asie.computronics.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.*;

import java.util.function.Function;

/**
 * Block registry. Registry names must never change: existing worlds reference them.
 * Each block gets its {@link net.minecraft.world.item.BlockItem} via {@code ModItems.blockItem(...)},
 * and its block entity type in {@link ModBlockEntities}.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Computronics.MODID);

    private ModBlocks() {}

    /** Shared properties for all machine blocks: metal, pickaxe-friendly, blast resistant like iron blocks. */
    public static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f, 6.0f)
                .sound(SoundType.METAL);
    }

    private static <B extends Block> DeferredBlock<B> machine(String name, Function<BlockBehaviour.Properties, B> factory) {
        return BLOCKS.registerBlock(name, factory, machineProperties());
    }

    private static <B extends Block> DeferredBlock<B> machine(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        return BLOCKS.registerBlock(name, factory, properties);
    }

    public static final DeferredBlock<BlockChatBox> CHAT_BOX = machine("chat_box", BlockChatBox::new);
    public static final DeferredBlock<BlockRadar> RADAR = machine("radar", BlockRadar::new);
    public static final DeferredBlock<BlockCamera> CAMERA = machine("camera", BlockCamera::new);
    public static final DeferredBlock<BlockTapeDrive> TAPE_DRIVE = machine("tape_drive", BlockTapeDrive::new);
    public static final DeferredBlock<BlockAcousticSensor> ACOUSTIC_SENSOR = machine("acoustic_sensor", BlockAcousticSensor::new,
            machineProperties().sound(SoundType.SCULK_SENSOR));
    public static final DeferredBlock<BlockRFJammer> RF_JAMMER = machine("rf_jammer", BlockRFJammer::new);
    public static final DeferredBlock<BlockWeatherStation> WEATHER_STATION = machine("weather_station", BlockWeatherStation::new);
    public static final DeferredBlock<BlockInductiveCharger> INDUCTIVE_CHARGER = machine("inductive_charger", BlockInductiveCharger::new);
    public static final DeferredBlock<BlockLaserTransceiver> LASER_TRANSCEIVER = machine("laser_transceiver", BlockLaserTransceiver::new);
    public static final DeferredBlock<BlockAnalogGauge> ANALOG_GAUGE = machine("analog_gauge", BlockAnalogGauge::new);
    public static final DeferredBlock<BlockIronNote> IRON_NOTEBLOCK = machine("iron_noteblock", BlockIronNote::new);
    public static final DeferredBlock<BlockControlPanel> CONTROL_PANEL = machine("control_panel", BlockControlPanel::new);
    public static final DeferredBlock<BlockSpeechBox> SPEECH_BOX = machine("speech_box", BlockSpeechBox::new);
    public static final DeferredBlock<BlockConnector> CONNECTOR = machine("connector", BlockConnector::new);
    public static final DeferredBlock<BlockEntityDetector> ENTITY_DETECTOR = machine("entity_detector", BlockEntityDetector::new);
}
