package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.signal.ISignalNode;
import pl.asie.computronics.signal.SignalLink;
import pl.asie.computronics.signal.SignalSource;
import pl.asie.computronics.signal.Signals;

import java.util.*;

/**
 * Signal connector: bridges wired panel signals and OpenComputers.
 * <ul>
 *   <li>8 inputs on the top face (green): read with {@code getInput(n)}, changes fire {@code connector_input}.</li>
 *   <li>8 outputs on the bottom face (red): driven by {@code setOutput(n, value)}.</li>
 * </ul>
 * Ports are laid out 4 x 2 on each face, numbered 1-4 along +X in the north row, 5-8 in the south row.
 */
public class TileConnector extends TileEntityComputronics implements ISignalNode {
    public static final int PORTS = 8;

    private final int[] inputs = new int[PORTS];
    private final int[] outputs = new int[PORTS];
    private final List<SignalLink> links = new ArrayList<>();
    private final Map<Integer, SignalSource> sources = new HashMap<>();
    private volatile boolean outputsDirty = true;
    private volatile boolean syncPending;

    public TileConnector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONNECTOR.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("connector", Visibility.Network)
                .create();
    }

    public int input(int port) {
        return port >= 1 && port <= PORTS ? inputs[port - 1] : 0;
    }

    public int output(int port) {
        return port >= 1 && port <= PORTS ? outputs[port - 1] : 0;
    }

    public boolean isWired(int inPort) {
        return sources.containsKey(inPort);
    }

    /** Port index (1..8) for a point on a face, given block-local x/z. */
    public static int portAt(double x, double z) {
        int col = Math.max(0, Math.min(3, (int) (x * 4)));
        int row = Math.max(0, Math.min(1, (int) (z * 2)));
        return row * 4 + col + 1;
    }

    public static double portX(int port) {
        return (((port - 1) % 4) + 0.5) / 4.0;
    }

    public static double portZ(int port) {
        return (((port - 1) / 4) + 0.5) / 2.0;
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        boolean refresh = level.getGameTime() % 20 == (pos.hashCode() & 15);
        if (outputsDirty || refresh) {
            outputsDirty = false;
            Signals.propagate(level, this, 0);
        }
        if (refresh && Signals.pruneDeadLinks(level, this)) syncPending = true;
        if (syncPending) {
            syncPending = false;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ ISignalNode

    @Override
    public @Nullable PortRef pickPort(Player player, Vec3 hitLocation) {
        BlockPos p = getBlockPos();
        Vec3 l = hitLocation.subtract(p.getX(), p.getY(), p.getZ());
        if (l.y > 0.99) return new PortRef(portAt(l.x, l.z), false);
        if (l.y < 0.01) return new PortRef(portAt(l.x, l.z), true);
        return null;
    }

    @Override
    public boolean hasPort(int port, boolean output) {
        return port >= 1 && port <= PORTS;
    }

    @Override
    public Vec3 portPosition(int port, boolean output) {
        BlockPos p = getBlockPos();
        return new Vec3(p.getX() + portX(port), p.getY() + (output ? -0.02 : 1.02), p.getZ() + portZ(port));
    }

    @Override
    public int getOutput(int port) {
        return output(port);
    }

    @Override
    public void setInput(int port, int value) {
        if (port < 1 || port > PORTS || inputs[port - 1] == value) return;
        inputs[port - 1] = value;
        if (node != null && node.network() != null) {
            node.sendToReachable("computer.signal", "connector_input", port, value);
        }
        syncPending = true;
    }

    @Override
    public List<SignalLink> links() {
        return links;
    }

    @Override
    public @Nullable SignalSource sourceOf(int inPort) {
        return sources.get(inPort);
    }

    @Override
    public void setSource(int inPort, @Nullable SignalSource source) {
        if (source == null) sources.remove(inPort);
        else sources.put(inPort, source);
    }

    @Override
    public void markSignalDirty() {
        syncPending = true;
        outputsDirty = true;
    }

    @Override
    public String describePort(PortRef ref) {
        return (ref.output() ? "OUT " : "IN ") + ref.port();
    }

    /** Unplugs every wire on both sides, returns how many were removed. */
    public int unplugAll() {
        if (level == null) return 0;
        int n = 0;
        for (int port = 1; port <= PORTS; port++) {
            if (Signals.disconnectInput(level, getBlockPos(), port)) n++;
            n += Signals.disconnectOutput(level, getBlockPos(), port);
        }
        return n;
    }

    // ------------------------------------------------------------------ Lua API

    private static int port(Arguments args, int i) {
        int p = args.checkInteger(i);
        if (p < 1 || p > PORTS) throw new IllegalArgumentException("port must be 1-" + PORTS);
        return p;
    }

    @Callback(direct = true, doc = "function(port:number):number -- Signal on input port 1-8 (top face).")
    public Object[] getInput(Context context, Arguments args) {
        return new Object[]{inputs[port(args, 0) - 1]};
    }

    @Callback(direct = true, doc = "function():table -- All 8 input values.")
    public Object[] getInputs(Context context, Arguments args) {
        Map<Integer, Integer> m = new HashMap<>();
        for (int i = 0; i < PORTS; i++) m.put(i + 1, inputs[i]);
        return new Object[]{m};
    }

    @Callback(direct = true, doc = "function(port:number):number -- Current value of output port 1-8 (bottom face).")
    public Object[] getOutput(Context context, Arguments args) {
        return new Object[]{outputs[port(args, 0) - 1]};
    }

    @Callback(direct = true, limit = 64, doc = "function(port:number, value:number or boolean) -- Drives output port 1-8 (true = 15).")
    public Object[] setOutput(Context context, Arguments args) {
        int p = port(args, 0);
        int v = args.isBoolean(1) ? (args.checkBoolean(1) ? Signals.ON : 0) : args.checkInteger(1);
        if (outputs[p - 1] != v) {
            outputs[p - 1] = v;
            outputsDirty = true;
            syncPending = true;
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(port:number):boolean -- Whether a wire is plugged into input port 1-8.")
    public Object[] isWired(Context context, Arguments args) {
        return new Object[]{isWired(port(args, 0))};
    }

    // ------------------------------------------------------------------ persistence & sync

    private void writeShared(CompoundTag tag) {
        tag.putIntArray("in", inputs);
        tag.putIntArray("out", outputs);
        ListTag l = new ListTag();
        for (SignalLink link : links) l.add(link.save());
        tag.put("links", l);
        ListTag s = new ListTag();
        for (Map.Entry<Integer, SignalSource> e : sources.entrySet()) {
            CompoundTag t = e.getValue().save();
            t.putInt("port", e.getKey());
            s.add(t);
        }
        tag.put("sources", s);
    }

    private void readShared(CompoundTag tag) {
        copy(tag.getIntArray("in"), inputs);
        copy(tag.getIntArray("out"), outputs);
        links.clear();
        for (Tag t : tag.getList("links", Tag.TAG_COMPOUND)) links.add(SignalLink.load((CompoundTag) t));
        sources.clear();
        for (Tag t : tag.getList("sources", Tag.TAG_COMPOUND)) {
            sources.put(((CompoundTag) t).getInt("port"), SignalSource.load((CompoundTag) t));
        }
    }

    private static void copy(int[] from, int[] to) {
        Arrays.fill(to, 0);
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeShared(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readShared(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeShared(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readShared(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        readShared(pkt.getTag());
    }
}
