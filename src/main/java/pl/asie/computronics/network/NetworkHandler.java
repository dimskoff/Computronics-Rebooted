package pl.asie.computronics.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import pl.asie.computronics.Computronics;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Computronics.MODID).versioned("2.0.1");
        registrar.playToServer(PanelGeometryPayload.TYPE, PanelGeometryPayload.STREAM_CODEC, PanelGeometryPayload::handle);
        registrar.playToServer(ConfigurePanelPayload.TYPE, ConfigurePanelPayload.STREAM_CODEC, ConfigurePanelPayload::handle);
        registrar.playToClient(BeepPayload.TYPE, BeepPayload.STREAM_CODEC, BeepPayload::handle);
        registrar.playToClient(AudioStreamPayload.TYPE, AudioStreamPayload.STREAM_CODEC, AudioStreamPayload::handle);
        registrar.playToClient(SpeechBoxPayload.TYPE, SpeechBoxPayload.STREAM_CODEC, SpeechBoxPayload::handle);
        Computronics.LOGGER.info("Computronics: Network payloads registered successfully.");
    }
}
