package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.*;

public class EnvironmentNetworkCardTier3 extends AbstractManagedEnvironment {
    public static final int MAX_PACKET_SIZE = 65536; // 64 KB Jumbo Frames
    public static final int MAX_OPEN_PORTS = 64;

    protected final EnvironmentHost host;
    protected final Set<Integer> openPorts = new HashSet<>();
    protected String wakeMessage = null;
    protected boolean wakeMessageFuzzy = false;

    public EnvironmentNetworkCardTier3(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("modem", Visibility.Neighbors)
                .create());
    }

    protected int checkPort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("invalid port number");
        }
        return port;
    }

    @Callback(doc = "function(port:number):boolean -- Opens the specified port (up to 64).")
    public Object[] open(Context context, Arguments args) {
        int port = checkPort(args.checkInteger(0));
        if (openPorts.contains(port)) {
            return new Object[]{false};
        }
        if (openPorts.size() >= MAX_OPEN_PORTS) {
            throw new IllegalStateException("too many open ports");
        }
        return new Object[]{openPorts.add(port)};
    }

    @Callback(doc = "function([port:number]):boolean -- Closes the specified port (default: all ports).")
    public Object[] close(Context context, Arguments args) {
        if (args.count() == 0) {
            boolean closed = !openPorts.isEmpty();
            openPorts.clear();
            return new Object[]{closed};
        }
        int port = checkPort(args.checkInteger(0));
        return new Object[]{openPorts.remove(port)};
    }

    @Callback(direct = true, doc = "function(port:number):boolean -- Whether the specified port is open.")
    public Object[] isOpen(Context context, Arguments args) {
        int port = checkPort(args.checkInteger(0));
        return new Object[]{openPorts.contains(port)};
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wireless networking capability.")
    public Object[] isWireless(Context context, Arguments args) {
        return new Object[]{false};
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wired networking capability.")
    public Object[] isWired(Context context, Arguments args) {
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Gets maximum packet size in bytes.")
    public Object[] maxPacketSize(Context context, Arguments args) {
        return new Object[]{MAX_PACKET_SIZE};
    }

    @Callback(direct = true, doc = "function():number -- Gets maximum number of open ports.")
    public Object[] maxOpenPorts(Context context, Arguments args) {
        return new Object[]{MAX_OPEN_PORTS};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns true indicating source address spoofing is supported.")
    public Object[] isSpoofingSupported(Context context, Arguments args) {
        return new Object[]{true};
    }

    @Callback(doc = "function(target:string, [source:string], port:number, data...):boolean -- Sends data to target with optional spoofed source.")
    public Object[] send(Context context, Arguments args) {
        String target = args.checkString(0);
        String sourceAddress;
        int port;
        Object[] payload;

        // Flexible argument parsing:
        // Case 1: send(target, spoofedSource, port, data...)
        // Case 2: send(target, port, data...)
        if (args.isString(1)) {
            sourceAddress = args.checkString(1);
            port = checkPort(args.checkInteger(2));
            payload = extractPayload(args, 3);
        } else {
            sourceAddress = node().address();
            port = checkPort(args.checkInteger(1));
            payload = extractPayload(args, 2);
        }

        Packet packet = Network.newPacket(sourceAddress, target, port, payload);
        if (packet != null) {
            node().sendToReachable("network.message", packet);
            return new Object[]{true};
        }
        return new Object[]{false};
    }

    @Callback(doc = "function([source:string], port:number, data...):boolean -- Broadcasts data with optional spoofed source.")
    public Object[] broadcast(Context context, Arguments args) {
        String sourceAddress;
        int port;
        Object[] payload;

        // Flexible argument parsing:
        // Case 1: broadcast(spoofedSource, port, data...)
        // Case 2: broadcast(port, data...)
        if (args.isString(0)) {
            sourceAddress = args.checkString(0);
            port = checkPort(args.checkInteger(1));
            payload = extractPayload(args, 2);
        } else {
            sourceAddress = node().address();
            port = checkPort(args.checkInteger(0));
            payload = extractPayload(args, 1);
        }

        Packet packet = Network.newPacket(sourceAddress, null, port, payload);
        if (packet != null) {
            node().sendToReachable("network.message", packet);
            return new Object[]{true};
        }
        return new Object[]{false};
    }

    private Object[] extractPayload(Arguments args, int startIndex) {
        int count = args.count() - startIndex;
        if (count <= 0) return new Object[0];
        Object[] arr = new Object[count];
        for (int i = 0; i < count; i++) {
            arr[i] = args.checkAny(startIndex + i);
        }
        return arr;
    }

    @Callback(direct = true, doc = "function():string, boolean -- Gets the wake-up message and fuzzy flag.")
    public Object[] getWakeMessage(Context context, Arguments args) {
        return new Object[]{wakeMessage, wakeMessageFuzzy};
    }

    @Callback(doc = "function(message:string[, fuzzy:boolean]):string -- Sets the wake-up message.")
    public Object[] setWakeMessage(Context context, Arguments args) {
        String old = wakeMessage;
        wakeMessage = args.optString(0, null);
        wakeMessageFuzzy = args.optBoolean(1, wakeMessageFuzzy);
        return new Object[]{old, wakeMessageFuzzy};
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            openPorts.clear();
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) && node().isNeighborOf(message.source())) {
            openPorts.clear();
        }

        if ("network.message".equals(message.name()) && message.data() != null && message.data().length > 0) {
            if (message.data()[0] instanceof Packet packet) {
                receivePacket(packet);
            }
        }
    }

    protected void receivePacket(Packet packet) {
        if (node() == null) return;

        boolean isForMe = packet.destination() == null || packet.destination().equals(node().address());
        if (isForMe && openPorts.contains(packet.port())) {
            List<Object> signal = new ArrayList<>();
            signal.add("modem_message");
            signal.add(node().address());
            signal.add(packet.source());
            signal.add(packet.port());
            signal.add(0.0); // distance
            if (packet.data() != null) {
                signal.addAll(Arrays.asList(packet.data()));
            }
            node().sendToReachable("computer.signal", signal.toArray());
        }

        // Check wake message
        if (wakeMessage != null && packet.data() != null && packet.data().length > 0) {
            Object first = packet.data()[0];
            String str = first instanceof byte[] bytes ? new String(bytes) : String.valueOf(first);
            if (wakeMessage.equals(str) || (wakeMessageFuzzy && str.startsWith(wakeMessage))) {
                if (host instanceof Context ctx) {
                    ctx.start();
                }
            }
        }
    }
}
