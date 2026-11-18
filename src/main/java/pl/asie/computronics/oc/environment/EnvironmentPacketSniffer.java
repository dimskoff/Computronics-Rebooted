package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.config.ComputronicsConfig;
import pl.asie.computronics.oc.OCData;

import java.nio.charset.StandardCharsets;

/** Packet sniffer card, component "packet_sniffer". */
public class EnvironmentPacketSniffer extends AbstractManagedEnvironment {
    protected final EnvironmentHost host;

    private boolean promiscuous = true;
    private int filterPort = 0;
    private String filterSender = null;

    private long totalBytes, totalPackets;
    private long windowBytes, windowPackets, currentBps, currentPps;
    private int tickCount, signalsThisTick;

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
        signalsThisTick = 0;
        if (++tickCount >= 20) {
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
        if ("network.message".equals(message.name()) && message.data() != null && message.data().length > 0
                && message.data()[0] instanceof Packet packet) {
            sniffPacket(packet);
        }
    }

    private static String describe(Object o) {
        if (o instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
        return String.valueOf(o);
    }

    private void sniffPacket(Packet packet) {
        if (!promiscuous) {
            if (filterPort > 0 && packet.port() != filterPort) return;
            if (filterSender != null && (packet.source() == null || !packet.source().startsWith(filterSender))) return;
        }
        int size = packet.size();
        totalBytes += size;
        totalPackets++;
        windowBytes += size;
        windowPackets++;

        if (node() == null || signalsThisTick >= ComputronicsConfig.SERVER.packetSnifferMaxPacketsPerTick.get()) return;
        signalsThisTick++;
        String dest = packet.destination() != null ? packet.destination() : "broadcast";
        StringBuilder sb = new StringBuilder();
        Object[] data = packet.data();
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(describe(data[i]));
                if (sb.length() > 64) break;
            }
        }
        String summary = sb.length() > 64 ? sb.substring(0, 61) + "..." : sb.toString();
        node().sendToReachable("computer.signal", "packet_sniffed", packet.source(), dest, packet.port(), size, summary);
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
        return new Object[]{totalBytes};
    }

    @Callback(direct = true, doc = "function():number -- Gets total packets recorded since start/reset.")
    public Object[] getTotalPackets(Context context, Arguments args) {
        return new Object[]{totalPackets};
    }

    @Callback(doc = "function():boolean -- Resets traffic and packet counters.")
    public Object[] resetCounters(Context context, Arguments args) {
        totalBytes = totalPackets = windowBytes = windowPackets = currentBps = currentPps = 0;
        return new Object[]{true};
    }

    @Callback(doc = "function(port:number[, senderPrefix:string]):boolean -- Sets the hardware filter.")
    public Object[] setFilter(Context context, Arguments args) {
        filterPort = args.checkInteger(0);
        filterSender = args.optString(1, null);
        promiscuous = false;
        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Clears the filter and resumes promiscuous mode.")
    public Object[] clearFilter(Context context, Arguments args) {
        filterPort = 0;
        filterSender = null;
        promiscuous = true;
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if the sniffer is in promiscuous mode.")
    public Object[] isPromiscuous(Context context, Arguments args) {
        return new Object[]{promiscuous};
    }

    @Callback(doc = "function(state:boolean):boolean -- Enables or disables promiscuous mode.")
    public Object[] setPromiscuous(Context context, Arguments args) {
        promiscuous = args.checkBoolean(0);
        return new Object[]{promiscuous};
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        CompoundTag tag = OCData.read(holder);
        if (tag.contains("snifferPromiscuous")) promiscuous = tag.getBoolean("snifferPromiscuous");
        filterPort = tag.getInt("snifferFilterPort");
        filterSender = tag.contains("snifferFilterSender") ? tag.getString("snifferFilterSender") : null;
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        OCData.write(holder, tag -> {
            tag.putBoolean("snifferPromiscuous", promiscuous);
            tag.putInt("snifferFilterPort", filterPort);
            if (filterSender != null) tag.putString("snifferFilterSender", filterSender);
            else tag.remove("snifferFilterSender");
        });
    }
}
