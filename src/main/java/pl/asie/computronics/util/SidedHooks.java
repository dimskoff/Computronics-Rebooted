package pl.asie.computronics.util;

import net.minecraft.core.BlockPos;

import java.util.function.BiConsumer;

/**
 * Client-only actions callable from common code without referencing client classes.
 * The client sets these during setup; on a dedicated server they stay no-ops.
 */
public final class SidedHooks {
    private static BiConsumer<BlockPos, Integer> panelConfigOpener = (pos, id) -> { };

    private SidedHooks() {
    }

    public static void setPanelConfigOpener(BiConsumer<BlockPos, Integer> opener) {
        panelConfigOpener = opener;
    }

    public static void openPanelConfig(BlockPos pos, int componentId) {
        panelConfigOpener.accept(pos, componentId);
    }
}
