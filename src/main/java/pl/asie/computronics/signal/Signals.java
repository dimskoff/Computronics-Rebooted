package pl.asie.computronics.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Iterator;

/**
 * Wiring rules and signal propagation shared by every {@link ISignalNode}.
 * Signals are integers (buttons/switches send 15 when active, 0 otherwise).
 */
public final class Signals {
    public static final int ON = 15;
    public static final double MAX_WIRE_LENGTH = 16.0;

    private Signals() {
    }

    public static ISignalNode node(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof ISignalNode n ? n : null;
    }

    public enum Result { OK, SAME_DIRECTION, TOO_FAR, NO_PORT, SAME_PORT }

    /**
     * Connects two ports (in any order). The input's previous wire, if any, is replaced.
     */
    public static Result connect(Level level, BlockPos aPos, ISignalNode.PortRef a, BlockPos bPos, ISignalNode.PortRef b) {
        if (a.output() == b.output()) return Result.SAME_DIRECTION;
        BlockPos outPos = a.output() ? aPos : bPos, inPos = a.output() ? bPos : aPos;
        int outPort = a.output() ? a.port() : b.port(), inPort = a.output() ? b.port() : a.port();
        if (outPos.equals(inPos) && outPort == inPort) return Result.SAME_PORT;
        ISignalNode out = node(level, outPos), in = node(level, inPos);
        if (out == null || in == null || !out.hasPort(outPort, true) || !in.hasPort(inPort, false)) return Result.NO_PORT;
        if (out.portPosition(outPort, true).distanceTo(in.portPosition(inPort, false)) > MAX_WIRE_LENGTH) return Result.TOO_FAR;

        disconnectInput(level, inPos, inPort);
        out.links().add(new SignalLink(outPort, inPos.immutable(), inPort));
        in.setSource(inPort, new SignalSource(outPos.immutable(), outPort));
        in.setInput(inPort, out.getOutput(outPort));
        out.markSignalDirty();
        in.markSignalDirty();
        return Result.OK;
    }

    /** Removes the wire feeding an input. Returns true if there was one. */
    public static boolean disconnectInput(Level level, BlockPos inPos, int inPort) {
        ISignalNode in = node(level, inPos);
        if (in == null) return false;
        SignalSource src = in.sourceOf(inPort);
        if (src == null) return false;
        ISignalNode out = node(level, src.pos());
        if (out != null) {
            out.links().removeIf(l -> l.outPort() == src.outPort() && l.target().equals(inPos) && l.inPort() == inPort);
            out.markSignalDirty();
        }
        in.setSource(inPort, null);
        in.setInput(inPort, 0);
        in.markSignalDirty();
        return true;
    }

    /** Removes every wire leaving an output. Returns how many were removed. */
    public static int disconnectOutput(Level level, BlockPos outPos, int outPort) {
        ISignalNode out = node(level, outPos);
        if (out == null) return 0;
        int removed = 0;
        for (Iterator<SignalLink> it = out.links().iterator(); it.hasNext(); ) {
            SignalLink l = it.next();
            if (l.outPort() != outPort) continue;
            it.remove();
            removed++;
            ISignalNode in = node(level, l.target());
            if (in != null) {
                in.setSource(l.inPort(), null);
                in.setInput(l.inPort(), 0);
                in.markSignalDirty();
            }
        }
        if (removed > 0) out.markSignalDirty();
        return removed;
    }

    /** Pushes the current value of one output (or all outputs if port &lt;= 0) along its wires. */
    public static void propagate(Level level, ISignalNode out, int port) {
        for (SignalLink l : out.links()) {
            if (port > 0 && l.outPort() != port) continue;
            ISignalNode in = node(level, l.target());
            if (in != null && in.hasPort(l.inPort(), false)) in.setInput(l.inPort(), out.getOutput(l.outPort()));
        }
    }

    /** Called when a node is broken: unplug everything on both ends. Returns the number of wires removed. */
    public static int disconnectAll(Level level, BlockPos pos, ISignalNode node) {
        int removed = 0;
        for (SignalLink l : node.links().toArray(SignalLink[]::new)) {
            removed += disconnectOutput(level, pos, l.outPort());
        }
        node.links().clear();
        return removed;
    }

    /** Drops links whose target no longer accepts them (target removed or component taken off). */
    public static boolean pruneDeadLinks(Level level, ISignalNode node) {
        return node.links().removeIf(l -> level.isLoaded(l.target()) && (node(level, l.target()) == null || !node(level, l.target()).hasPort(l.inPort(), false)));
    }
}
