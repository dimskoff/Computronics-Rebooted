package pl.asie.computronics;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModBlocks;
import pl.asie.computronics.init.ModCreativeTabs;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.oc.ComputronicsOCDrivers;
import pl.asie.computronics.block.entity.TileChatBox;
import pl.asie.computronics.block.entity.TileEntityComputronics;

@Mod(Computronics.MODID)
public class Computronics {
    public static final String MODID = "computronics";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Computronics(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        pl.asie.computronics.init.ModSounds.SOUNDS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(pl.asie.computronics.client.ComputronicsClient::onClientSetup);
        }

        // Register game event listeners
        NeoForge.EVENT_BUS.register(TileChatBox.ChatEventHandler.INSTANCE);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ComputronicsOCDrivers.init();
            LOGGER.info("Computronics: Rebooted OpenComputers drivers initialized successfully.");
        });
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        try {
            BlockCapability<Environment, Direction> envCap = Capabilities.EnvironmentCapability();
            BlockCapability<SidedEnvironment, Direction> sidedCap = Capabilities.SidedEnvironmentCapability();

            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.CHAT_BOX.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.RADAR.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.CAMERA.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.TAPE_DRIVE.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.ACOUSTIC_SENSOR.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.RF_JAMMER.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.WEATHER_STATION.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.INDUCTIVE_CHARGER.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.LASER_TRANSCEIVER.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.ANALOG_GAUGE.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.IRON_NOTEBLOCK.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.CONTROL_PANEL.get());
            registerBlockCaps(event, envCap, sidedCap, ModBlockEntities.SPEECH_BOX.get());
            LOGGER.info("Computronics: OpenComputers block capabilities registered successfully.");
        } catch (Throwable t) {
            LOGGER.warn("Computronics: Could not register OpenComputers capabilities: {}", t.getMessage());
        }
    }

    private <T extends TileEntityComputronics> void registerBlockCaps(
            RegisterCapabilitiesEvent event,
            BlockCapability<Environment, Direction> envCap,
            BlockCapability<SidedEnvironment, Direction> sidedCap,
            BlockEntityType<T> type) {
        event.registerBlockEntity(envCap, type, (be, dir) -> be);
        event.registerBlockEntity(sidedCap, type, (be, dir) -> be);
    }
}
