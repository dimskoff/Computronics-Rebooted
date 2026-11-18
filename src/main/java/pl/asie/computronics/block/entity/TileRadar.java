package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.service.RadarService;

public class TileRadar extends TileEntityComputronics {
    private final RadarService.RadarState radar = new RadarService.RadarState();

    public TileRadar(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("radar", Visibility.Network)
                .withConnector(1000)
                .create();
    }

    private Object[] scan(Arguments args, RadarService.Filter filter) {
        return RadarService.scan(radar, node, level, Vec3.atCenterOf(worldPosition), args.optInteger(0, 16), filter);
    }

    @Callback(doc = "function([radius:number]):table -- Returns living entities within range.")
    public Object[] getEntities(Context context, Arguments args) {
        return scan(args, RadarService.Filter.ALL);
    }

    @Callback(doc = "function([radius:number]):table -- Returns players within range.")
    public Object[] getPlayers(Context context, Arguments args) {
        return scan(args, RadarService.Filter.PLAYERS);
    }

    @Callback(doc = "function([radius:number]):table -- Returns mobs within range.")
    public Object[] getMobs(Context context, Arguments args) {
        return scan(args, RadarService.Filter.MOBS);
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum scanning radius.")
    public Object[] getMaxRadius(Context context, Arguments args) {
        return result(RadarService.maxRadius());
    }
}
