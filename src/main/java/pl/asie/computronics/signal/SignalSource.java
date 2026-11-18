package pl.asie.computronics.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Remembered on an input port: where its signal comes from (so it can be unplugged from either end). */
public record SignalSource(BlockPos pos, int outPort) {
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("p", pos.asLong());
        t.putInt("o", outPort);
        return t;
    }

    public static SignalSource load(CompoundTag t) {
        return new SignalSource(BlockPos.of(t.getLong("p")), t.getInt("o"));
    }
}
