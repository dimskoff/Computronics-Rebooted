package pl.asie.computronics.util;

import net.minecraft.core.BlockPos;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Client-only actions callable from common code without referencing client classes.
 * The client sets these during setup; on a dedicated server they stay no-ops.
 */
public final class SidedHooks {
    private static BiConsumer<BlockPos, Integer> panelConfigOpener = (pos, id) -> { };
    private static Consumer<BlockPos> panelGeometryOpener = pos -> { };

    private SidedHooks() {
    }

    public static void setPanelConfigOpener(BiConsumer<BlockPos, Integer> opener) {
        panelConfigOpener = opener;
    }

    public static void setPanelGeometryOpener(Consumer<BlockPos> opener) {
        panelGeometryOpener = opener;
    }

    public static void openPanelConfig(BlockPos pos, int componentId) {
        panelConfigOpener.accept(pos, componentId);
    }

    public static void openPanelGeometry(BlockPos pos) {
        panelGeometryOpener.accept(pos);
    }
}
