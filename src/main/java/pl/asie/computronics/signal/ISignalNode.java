package pl.asie.computronics.signal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A block entity with Redstone-Control-style signal ports that wires can connect.
 * Port numbers are positive; inputs and outputs are separate namespaces.
 */
public interface ISignalNode {
    /** A port picked by the player: number + direction. */
    record PortRef(int port, boolean output) {
    }

    /** Port under the player's crosshair, or null. */
    @Nullable
    PortRef pickPort(Player player, Vec3 hitLocation);

    boolean hasPort(int port, boolean output);

    /** World-space position where a wire attaches to the port. */
    Vec3 portPosition(int port, boolean output);

    /** Current value of an output port. */
    int getOutput(int port);

    /** Called when the signal arriving at an input port changes. */
    void setInput(int port, int value);

    /** Outgoing wires (stored on the output side). Mutable. */
    List<SignalLink> links();

    @Nullable
    SignalSource sourceOf(int inPort);

    void setSource(int inPort, @Nullable SignalSource source);

    /** Persist + sync after links/sources changed. */
    void markSignalDirty();

    /** Short human-readable port name for HUD, e.g. "IN 3" or "OUT #12". */
    String describePort(PortRef ref);
}
