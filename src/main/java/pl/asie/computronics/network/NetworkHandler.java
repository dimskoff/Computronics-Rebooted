package pl.asie.computronics.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import pl.asie.computronics.Computronics;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Computronics.MODID).versioned("1.0.0");
        registrar.playToServer(ConfigurePanelPayload.TYPE, ConfigurePanelPayload.STREAM_CODEC, ConfigurePanelPayload::handle);
        registrar.playToServer(RemovePanelElementPayload.TYPE, RemovePanelElementPayload.STREAM_CODEC, RemovePanelElementPayload::handle);
        registrar.playToClient(SpeechBoxPayload.TYPE, SpeechBoxPayload.STREAM_CODEC, SpeechBoxPayload::handle);
        Computronics.LOGGER.info("Computronics: Network payloads registered successfully.");
    }
}
