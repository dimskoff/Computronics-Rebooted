package pl.asie.computronics.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ComputronicsClientConfig {

    public static final ComputronicsClientConfig CLIENT;
    public static final ModConfigSpec SPEC;

    static {
        final Pair<ComputronicsClientConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(ComputronicsClientConfig::new);
        CLIENT = pair.getLeft();
        SPEC = pair.getRight();
    }

    // =========================================================================
    // Rendering & Visuals
    // =========================================================================
    public final ModConfigSpec.BooleanValue renderHandheldWirelessMonitor;
    public final ModConfigSpec.BooleanValue renderControlPanelWorldText;
    public final ModConfigSpec.IntValue controlPanelTextRenderDistance;
    public final ModConfigSpec.BooleanValue renderRackOverlays;
    public final ModConfigSpec.BooleanValue enableScrewdriverGuiBlur;

    // =========================================================================
    // Audio & Effects
    // =========================================================================
    public final ModConfigSpec.DoubleValue tapeDriveMasterVolume;
    public final ModConfigSpec.DoubleValue speechBoxVolumeMultiplier;
    public final ModConfigSpec.BooleanValue ironNoteblockParticles;

    public ComputronicsClientConfig(ModConfigSpec.Builder b) {
        b.comment("Computronics Rebooted - Client Graphics, Rendering & Audio Settings");

        b.push("rendering");
        renderHandheldWirelessMonitor = b.comment("Render 3D widescreen tablet model when holding Wireless Monitor in first-person.")
                .define("renderHandheldWirelessMonitor", true);

        renderControlPanelWorldText = b.comment("Render 3D font text and labels on Modular Control Panels in the world.")
                .define("renderControlPanelWorldText", true);

        controlPanelTextRenderDistance = b.comment("Maximum distance in blocks to render 3D panel text (distance culling for FPS optimization).")
                .defineInRange("controlPanelTextRenderDistance", 32, 4, 128);

        renderRackOverlays = b.comment("Render glowing dynamic LEDs and interactive switches on Server Racks.")
                .define("renderRackOverlays", true);

        enableScrewdriverGuiBlur = b.comment("Whether to enable background world blur when opening the Screwdriver configuration GUI (false = sharp/crisp world).")
                .define("enableScrewdriverGuiBlur", false);
        b.pop();

        b.push("audio");
        tapeDriveMasterVolume = b.comment("Client master volume multiplier for tape audio playback (0.0 to 1.0).")
                .defineInRange("tapeDriveMasterVolume", 1.0, 0.0, 1.0);

        speechBoxVolumeMultiplier = b.comment("Client master volume multiplier for Speech Box voices (0.0 to 2.0).")
                .defineInRange("speechBoxVolumeMultiplier", 1.0, 0.0, 2.0);

        ironNoteblockParticles = b.comment("Spawn custom metal chime note particles around Iron Note Block.")
                .define("ironNoteblockParticles", true);
        b.pop();
    }
}
