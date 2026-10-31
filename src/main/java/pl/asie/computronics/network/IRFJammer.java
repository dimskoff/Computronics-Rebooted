package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IRFJammer {
    Level getJammerLevel();
    BlockPos getJammerPos();
    int getFrequency();
    double getEffectiveRadius();
    boolean isEnabled();
}
