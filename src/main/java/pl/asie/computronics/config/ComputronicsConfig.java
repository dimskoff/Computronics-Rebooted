package pl.asie.computronics.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public class ComputronicsConfig {

    public static ComputronicsServerConfig SERVER = ComputronicsServerConfig.SERVER;
    public static ComputronicsClientConfig CLIENT = ComputronicsClientConfig.CLIENT;

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ComputronicsServerConfig.SPEC, "computronics-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ComputronicsClientConfig.SPEC, "computronics-client.toml");
    }
}
