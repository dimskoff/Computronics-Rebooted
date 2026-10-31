package pl.asie.computronics.init;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.*;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Computronics.MODID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .strength(2.0f, 6.0f)
                .sound(SoundType.METAL);
    }

    public static final DeferredBlock<BlockChatBox> CHAT_BOX =
            BLOCKS.register("chat_box", () -> new BlockChatBox(metal()));

    public static final DeferredBlock<BlockRadar> RADAR =
            BLOCKS.register("radar", () -> new BlockRadar(metal()));

    public static final DeferredBlock<BlockCamera> CAMERA =
            BLOCKS.register("camera", () -> new BlockCamera(metal()));

    public static final DeferredBlock<BlockTapeDrive> TAPE_DRIVE =
            BLOCKS.register("tape_drive", () -> new BlockTapeDrive(metal()));

    public static final DeferredBlock<BlockAcousticSensor> ACOUSTIC_SENSOR =
            BLOCKS.register("acoustic_sensor", () -> new BlockAcousticSensor(metal().sound(SoundType.SCULK_SENSOR)));

    public static final DeferredBlock<BlockRFJammer> RF_JAMMER =
            BLOCKS.register("rf_jammer", () -> new BlockRFJammer(metal()));

    public static final DeferredBlock<BlockWeatherStation> WEATHER_STATION =
            BLOCKS.register("weather_station", () -> new BlockWeatherStation(metal()));

    public static final DeferredBlock<BlockInductiveCharger> INDUCTIVE_CHARGER =
            BLOCKS.register("inductive_charger", () -> new BlockInductiveCharger(metal()));

    public static final DeferredBlock<BlockLaserTransceiver> LASER_TRANSCEIVER =
            BLOCKS.register("laser_transceiver", () -> new BlockLaserTransceiver(metal()));

    public static final DeferredBlock<BlockAnalogGauge> ANALOG_GAUGE =
            BLOCKS.register("analog_gauge", () -> new BlockAnalogGauge(metal()));

    public static final DeferredBlock<BlockIronNote> IRON_NOTEBLOCK =
            BLOCKS.register("iron_noteblock", () -> new BlockIronNote(metal()));

    public static final DeferredBlock<BlockControlPanel> CONTROL_PANEL =
            BLOCKS.register("control_panel", () -> new BlockControlPanel(metal()));

    public static final DeferredBlock<BlockSpeechBox> SPEECH_BOX =
            BLOCKS.register("speech_box", () -> new BlockSpeechBox(metal()));

    public static final DeferredBlock<BlockEntityDetector> ENTITY_DETECTOR =
            BLOCKS.register("entity_detector", () -> new BlockEntityDetector(metal()));
}
