package pl.asie.computronics.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.*;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Computronics.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChatBox>> CHAT_BOX =
            BLOCK_ENTITIES.register("chat_box", () -> BlockEntityType.Builder.of(TileChatBox::new, ModBlocks.CHAT_BOX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRadar>> RADAR =
            BLOCK_ENTITIES.register("radar", () -> BlockEntityType.Builder.of(TileRadar::new, ModBlocks.RADAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCamera>> CAMERA =
            BLOCK_ENTITIES.register("camera", () -> BlockEntityType.Builder.of(TileCamera::new, ModBlocks.CAMERA.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTapeDrive>> TAPE_DRIVE =
            BLOCK_ENTITIES.register("tape_drive", () -> BlockEntityType.Builder.of(TileTapeDrive::new, ModBlocks.TAPE_DRIVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAcousticSensor>> ACOUSTIC_SENSOR =
            BLOCK_ENTITIES.register("acoustic_sensor", () -> BlockEntityType.Builder.of(TileAcousticSensor::new, ModBlocks.ACOUSTIC_SENSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRFJammer>> RF_JAMMER =
            BLOCK_ENTITIES.register("rf_jammer", () -> BlockEntityType.Builder.of(TileRFJammer::new, ModBlocks.RF_JAMMER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWeatherStation>> WEATHER_STATION =
            BLOCK_ENTITIES.register("weather_station", () -> BlockEntityType.Builder.of(TileWeatherStation::new, ModBlocks.WEATHER_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInductiveCharger>> INDUCTIVE_CHARGER =
            BLOCK_ENTITIES.register("inductive_charger", () -> BlockEntityType.Builder.of(TileInductiveCharger::new, ModBlocks.INDUCTIVE_CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLaserTransceiver>> LASER_TRANSCEIVER =
            BLOCK_ENTITIES.register("laser_transceiver", () -> BlockEntityType.Builder.of(TileLaserTransceiver::new, ModBlocks.LASER_TRANSCEIVER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAnalogGauge>> ANALOG_GAUGE =
            BLOCK_ENTITIES.register("analog_gauge", () -> BlockEntityType.Builder.of(TileAnalogGauge::new, ModBlocks.ANALOG_GAUGE.get()).build(null));
}
