package pl.asie.computronics.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * A wire from output port {@code outPort} of the owning block to input port {@code inPort} of the
 * block at {@code target}. Links are stored on the output (source) side.
 */
public record SignalLink(int outPort, BlockPos target, int inPort) {
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("o", outPort);
        t.putLong("t", target.asLong());
        t.putInt("i", inPort);
        return t;
    }

    public static SignalLink load(CompoundTag t) {
        return new SignalLink(t.getInt("o"), BlockPos.of(t.getLong("t")), t.getInt("i"));
    }
}
