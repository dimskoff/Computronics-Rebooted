package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.config.ComputronicsConfig;
import pl.asie.computronics.oc.OCData;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Tier 3 ("jumbo frame") wired network card, component "modem". */
public class EnvironmentNetworkCardTier3 extends AbstractManagedEnvironment {
    public static final int MAX_OPEN_PORTS = 64;
    /** Hardcoded energy cost: base per packet plus per KiB of payload. */
    public static final double ENERGY_PER_PACKET = 0.2;
    public static final double ENERGY_PER_KIB = 0.1;

    protected final EnvironmentHost host;
    protected final Set<Integer> openPorts = new HashSet<>();
    protected String wakeMessage = null;
    protected boolean wakeMessageFuzzy = false;

    public EnvironmentNetworkCardTier3(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("modem", Visibility.Neighbors)
                .withConnector()
                .create());
    }

    private static int maxPacketSize() {
        return ComputronicsConfig.SERVER.jumboFramesMaxPacketSize.get();
    }

    protected int checkPort(int port) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("invalid port number");
        return port;
    }

    @Callback(doc = "function(port:number):boolean -- Opens the specified port.")
    public Object[] open(Context context, Arguments args) {
        int port = checkPort(args.checkInteger(0));
        if (openPorts.contains(port)) return new Object[]{false};
        if (openPorts.size() >= MAX_OPEN_PORTS) throw new IllegalArgumentException("too many open ports");
        return new Object[]{openPorts.add(port)};
    }

    @Callback(doc = "function([port:number]):boolean -- Closes the specified port (default: all ports).")
    public Object[] close(Context context, Arguments args) {
        if (args.count() == 0) {
            boolean closed = !openPorts.isEmpty();
            openPorts.clear();
            return new Object[]{closed};
        }
        return new Object[]{openPorts.remove(checkPort(args.checkInteger(0)))};
    }

    @Callback(direct = true, doc = "function(port:number):boolean -- Whether the specified port is open.")
    public Object[] isOpen(Context context, Arguments args) {
        return new Object[]{openPorts.contains(checkPort(args.checkInteger(0)))};
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wireless networking capability.")
    public Object[] isWireless(Context context, Arguments args) {
        return new Object[]{false};
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wired networking capability.")
    public Object[] isWired(Context context, Arguments args) {
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum packet size in bytes.")
    public Object[] maxPacketSize(Context context, Arguments args) {
        return new Object[]{maxPacketSize()};
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum number of open ports.")
    public Object[] maxOpenPorts(Context context, Arguments args) {
        return new Object[]{MAX_OPEN_PORTS};
    }

    @Callback(doc = "function(address:string, port:number, data...):boolean -- Sends data to the specified address.")
    public Object[] send(Context context, Arguments args) {
        String target = args.checkString(0);
        int port = checkPort(args.checkInteger(1));
        return transmit(target, port, extractPayload(args, 2));
    }

    @Callback(doc = "function(port:number, data...):boolean -- Broadcasts data on the specified port.")
    public Object[] broadcast(Context context, Arguments args) {
        int port = checkPort(args.checkInteger(0));
        return transmit(null, port, extractPayload(args, 1));
    }

    private Object[] transmit(String target, int port, Object[] payload) {
        // Source is always this card's own address: no spoofing.
        Packet packet = Network.newPacket(node().address(), target, port, payload);
        int size = packet.size();
        if (size > maxPacketSize()) {
            throw new IllegalArgumentException("packet too big (max " + maxPacketSize() + ")");
        }
        if (!OCData.tryConsume(node(), ENERGY_PER_PACKET + ENERGY_PER_KIB * size / 1024.0)) {
            return new Object[]{false, "not enough energy"};
        }
        node().sendToReachable("network.message", packet);
        return new Object[]{true};
    }

    private Object[] extractPayload(Arguments args, int startIndex) {
        int count = args.count() - startIndex;
        if (count <= 0) return new Object[0];
        Object[] arr = new Object[count];
        for (int i = 0; i < count; i++) arr[i] = args.checkAny(startIndex + i);
        return arr;
    }

    @Callback(direct = true, doc = "function():string, boolean -- Gets the wake-up message and fuzzy flag.")
    public Object[] getWakeMessage(Context context, Arguments args) {
        return new Object[]{wakeMessage, wakeMessageFuzzy};
    }

    @Callback(doc = "function(message:string[, fuzzy:boolean]):string -- Sets the wake-up message (nil to disable).")
    public Object[] setWakeMessage(Context context, Arguments args) {
        String old = wakeMessage;
        wakeMessage = args.optString(0, null);
        wakeMessageFuzzy = args.optBoolean(1, wakeMessageFuzzy);
        return new Object[]{old, wakeMessageFuzzy};
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) openPorts.clear();
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        String name = message.name();
        if (("computer.stopped".equals(name) || "computer.started".equals(name)) && node().isNeighborOf(message.source())) {
            openPorts.clear();
        }
        if ("network.message".equals(name) && message.data() != null && message.data().length > 0
                && message.data()[0] instanceof Packet packet) {
            receivePacket(packet);
        }
    }

    protected void receivePacket(Packet packet) {
        if (node() == null || node().address() == null) return;
        if (node().address().equals(packet.source())) return;
        if (packet.destination() != null && !packet.destination().equals(node().address())) return;

        if (openPorts.contains(packet.port())) {
            List<Object> signal = new ArrayList<>();
            signal.add("modem_message");
            signal.add(node().address());
            signal.add(packet.source());
            signal.add(packet.port());
            signal.add(0.0);
            if (packet.data() != null) signal.addAll(Arrays.asList(packet.data()));
            node().sendToReachable("computer.signal", signal.toArray());
        }

        if (wakeMessage != null && packet.data() != null && packet.data().length > 0) {
            Object first = packet.data()[0];
            String str = first instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8)
                    : first instanceof String s ? s : null;
            if (str != null && (wakeMessage.equals(str) || (wakeMessageFuzzy && str.contains(wakeMessage)))) {
                wake();
            }
        }
    }

    /** Same approach as OC's network card: start the hosting machine, or ask neighbors to start. */
    private void wake() {
        if (host instanceof Context ctx) {
            ctx.start();
        } else if (host instanceof MachineHost mh && mh.machine() != null) {
            mh.machine().start();
        } else {
            node().sendToNeighbors("computer.start");
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        CompoundTag tag = OCData.read(holder);
        openPorts.clear();
        for (int p : tag.getIntArray("openPorts")) {
            if (p >= 1 && p <= 65535 && openPorts.size() < MAX_OPEN_PORTS) openPorts.add(p);
        }
        wakeMessage = tag.contains("wakeMessage") ? tag.getString("wakeMessage") : null;
        wakeMessageFuzzy = tag.getBoolean("wakeMessageFuzzy");
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        OCData.write(holder, tag -> {
            tag.putIntArray("openPorts", openPorts.stream().mapToInt(Integer::intValue).toArray());
            if (wakeMessage != null) tag.putString("wakeMessage", wakeMessage);
            else tag.remove("wakeMessage");
            tag.putBoolean("wakeMessageFuzzy", wakeMessageFuzzy);
        });
    }
}
