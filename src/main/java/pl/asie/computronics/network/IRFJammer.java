package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IRFJammer {
    Level getJammerLevel();
    BlockPos getJammerPos();
    /** 0 = broad spectrum (jams everything). */
    int getFrequency();
    double getEffectiveRadius();
    /** True only while the jammer is switched on AND powered. */
    boolean isEnabled();
}
