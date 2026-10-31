package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class EnvironmentPacketSniffer extends AbstractManagedEnvironment {
    protected final EnvironmentHost host;

    private boolean promiscuous = true;
    private int filterPort = 0;
    private String filterSender = null;

    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong totalPackets = new AtomicLong(0);

    private long windowBytes = 0;
    private long windowPackets = 0;
    private long currentBps = 0;
    private long currentPps = 0;
    private int tickCount = 0;

    public EnvironmentPacketSniffer(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("packet_sniffer", Visibility.Neighbors)
                .create());
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        tickCount++;
        if (tickCount >= 20) {
            currentBps = windowBytes;
            currentPps = windowPackets;
            windowBytes = 0;
            windowPackets = 0;
            tickCount = 0;
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("network.message".equals(message.name()) && message.data() != null && message.data().length > 0) {
            if (message.data()[0] instanceof Packet packet) {
                sniffPacket(packet);
            }
        }
    }

    private void sniffPacket(Packet packet) {
        if (!promiscuous) {
            if (filterPort > 0 && packet.port() != filterPort) return;
            if (filterSender != null && !packet.source().startsWith(filterSender)) return;
        }

        int size = packet.size();
        totalBytes.addAndGet(size);
        totalPackets.incrementAndGet();
        windowBytes += size;
        windowPackets++;

        if (node() != null) {
            String dest = packet.destination() != null ? packet.destination() : "broadcast";
            String summary = packet.data() != null ? Arrays.toString(packet.data()) : "[]";
            if (summary.length() > 64) {
                summary = summary.substring(0, 61) + "...";
            }
            node().sendToReachable("computer.signal", "packet_sniffed", packet.source(), dest, packet.port(), size, summary);
        }
    }

    @Callback(direct = true, doc = "function():number -- Gets the current bandwidth in bytes per second.")
    public Object[] getBandwidth(Context context, Arguments args) {
        return new Object[]{currentBps};
    }

    @Callback(direct = true, doc = "function():number -- Gets current packets per second.")
    public Object[] getPacketsPerSecond(Context context, Arguments args) {
        return new Object[]{currentPps};
    }

    @Callback(direct = true, doc = "function():number -- Gets total bytes recorded since start/reset.")
    public Object[] getTotalBytes(Context context, Arguments args) {
        return new Object[]{totalBytes.get()};
    }

    @Callback(direct = true, doc = "function():number -- Gets total packets recorded since start/reset.")
    public Object[] getTotalPackets(Context context, Arguments args) {
        return new Object[]{totalPackets.get()};
    }

    @Callback(doc = "function() -- Resets traffic and packet counters.")
    public Object[] resetCounters(Context context, Arguments args) {
        totalBytes.set(0);
        totalPackets.set(0);
        windowBytes = 0;
        windowPackets = 0;
        currentBps = 0;
        currentPps = 0;
        return new Object[]{true};
    }

    @Callback(doc = "function(port:number[, senderPrefix:string]):boolean -- Sets packet sniffer hardware filter.")
    public Object[] setFilter(Context context, Arguments args) {
        this.filterPort = args.checkInteger(0);
        this.filterSender = args.optString(1, null);
        this.promiscuous = false;
        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Clears sniffer filter and resumes promiscuous mode.")
    public Object[] clearFilter(Context context, Arguments args) {
        this.filterPort = 0;
        this.filterSender = null;
        this.promiscuous = true;
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if sniffer is running in promiscuous mode.")
    public Object[] isPromiscuous(Context context, Arguments args) {
        return new Object[]{promiscuous};
    }

    @Callback(doc = "function(state:boolean):boolean -- Enables or disables promiscuous mode.")
    public Object[] setPromiscuous(Context context, Arguments args) {
        this.promiscuous = args.checkBoolean(0);
        return new Object[]{promiscuous};
    }
}
