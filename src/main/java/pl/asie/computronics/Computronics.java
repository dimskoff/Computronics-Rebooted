package pl.asie.computronics;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModBlocks;
import pl.asie.computronics.init.ModCreativeTabs;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.oc.ComputronicsOCDrivers;
import pl.asie.computronics.network.RFJammerManager;
import pl.asie.computronics.block.entity.TileChatBox;

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

        // Register game event listeners
        NeoForge.EVENT_BUS.register(TileChatBox.ChatEventHandler.INSTANCE);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ComputronicsOCDrivers.init();
            LOGGER.info("Computronics: Rebooted OpenComputers drivers initialized successfully.");
        });
    }
}
