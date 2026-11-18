package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.service.RadarService;

public class EnvironmentRadarUpgrade extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    private final RadarService.RadarState radar = new RadarService.RadarState();

    public EnvironmentRadarUpgrade(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("radar", Visibility.Neighbors)
                .withConnector(500)
                .create());
    }

    private Object[] scan(Arguments args, RadarService.Filter filter) {
        if (host == null) return new Object[]{null, "world unavailable"};
        return RadarService.scan(radar, node(), host.getEnvironmentLevel(),
                new Vec3(host.xPosition(), host.yPosition(), host.zPosition()), args.optInteger(0, 16), filter);
    }

    @Callback(doc = "function([radius:number]):table -- Scans living entities around robot/drone.")
    public Object[] getEntities(Context context, Arguments args) {
        return scan(args, RadarService.Filter.ALL);
    }

    @Callback(doc = "function([radius:number]):table -- Scans players around robot/drone.")
    public Object[] getPlayers(Context context, Arguments args) {
        return scan(args, RadarService.Filter.PLAYERS);
    }

    @Callback(doc = "function([radius:number]):table -- Scans mobs around robot/drone.")
    public Object[] getMobs(Context context, Arguments args) {
        return scan(args, RadarService.Filter.MOBS);
    }
}
