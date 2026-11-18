package pl.asie.computronics.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.*;

import java.util.function.Supplier;

/**
 * Block entity types. Each type is bound to the block of the same registry name in {@link ModBlocks}.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Computronics.MODID);

    private ModBlockEntities() {}

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        //noinspection DataFlowIssue - a null data fixer type is the norm for modded block entities
        return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChatBox>> CHAT_BOX =
            register("chat_box", TileChatBox::new, ModBlocks.CHAT_BOX);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRadar>> RADAR =
            register("radar", TileRadar::new, ModBlocks.RADAR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCamera>> CAMERA =
            register("camera", TileCamera::new, ModBlocks.CAMERA);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTapeDrive>> TAPE_DRIVE =
            register("tape_drive", TileTapeDrive::new, ModBlocks.TAPE_DRIVE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAcousticSensor>> ACOUSTIC_SENSOR =
            register("acoustic_sensor", TileAcousticSensor::new, ModBlocks.ACOUSTIC_SENSOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRFJammer>> RF_JAMMER =
            register("rf_jammer", TileRFJammer::new, ModBlocks.RF_JAMMER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWeatherStation>> WEATHER_STATION =
            register("weather_station", TileWeatherStation::new, ModBlocks.WEATHER_STATION);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInductiveCharger>> INDUCTIVE_CHARGER =
            register("inductive_charger", TileInductiveCharger::new, ModBlocks.INDUCTIVE_CHARGER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLaserTransceiver>> LASER_TRANSCEIVER =
            register("laser_transceiver", TileLaserTransceiver::new, ModBlocks.LASER_TRANSCEIVER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAnalogGauge>> ANALOG_GAUGE =
            register("analog_gauge", TileAnalogGauge::new, ModBlocks.ANALOG_GAUGE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileIronNote>> IRON_NOTEBLOCK =
            register("iron_noteblock", TileIronNote::new, ModBlocks.IRON_NOTEBLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileControlPanel>> CONTROL_PANEL =
            register("control_panel", TileControlPanel::new, ModBlocks.CONTROL_PANEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpeechBox>> SPEECH_BOX =
            register("speech_box", TileSpeechBox::new, ModBlocks.SPEECH_BOX);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileConnector>> CONNECTOR =
            register("connector", TileConnector::new, ModBlocks.CONNECTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityDetector>> ENTITY_DETECTOR =
            register("entity_detector", TileEntityDetector::new, ModBlocks.ENTITY_DETECTOR);
}
