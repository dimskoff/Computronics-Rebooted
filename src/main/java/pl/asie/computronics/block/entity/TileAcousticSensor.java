package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import pl.asie.computronics.init.ModBlockEntities;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class TileAcousticSensor extends TileEntityComputronics implements GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem {
    private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private final VibrationSystem.User vibrationUser = new AcousticVibrationUser();
    private final VibrationSystem.Listener vibrationListener = new VibrationSystem.Listener(this);

    private int sensitivity = 16;
    private Map<String, Object> lastVibration = null;

    public TileAcousticSensor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACOUSTIC_SENSOR.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("acoustic_sensor", Visibility.Network)
                .create();
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return vibrationUser;
    }

    @Override
    public VibrationSystem.Listener getListener() {
        return vibrationListener;
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (level instanceof ServerLevel serverLevel) {
            VibrationSystem.Ticker.tick(serverLevel, this.getVibrationData(), this.getVibrationUser());
        }
    }

    @Callback(direct = true, doc = "function():number -- Gets sensor sensitivity/range.")
    public Object[] getSensitivity(Context context, Arguments args) {
        return result(sensitivity);
    }

    @Callback(doc = "function(range:number):number -- Sets sensor sensitivity/range (1-32).")
    public Object[] setSensitivity(Context context, Arguments args) {
        int val = args.checkInteger(0);
        this.sensitivity = Math.max(1, Math.min(val, 32));
        return result(this.sensitivity);
    }

    @Callback(direct = true, doc = "function():table -- Returns information about the last detected vibration.")
    public Object[] getLastVibration(Context context, Arguments args) {
        return result(lastVibration);
    }

    private class AcousticVibrationUser implements VibrationSystem.User {
        private final PositionSource positionSource = new BlockPositionSource(worldPosition);

        @Override
        public int getListenerRadius() {
            return sensitivity;
        }

        @Override
        public PositionSource getPositionSource() {
            return positionSource;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, GameEvent.Context context) {
            if (context.sourceEntity() != null && context.sourceEntity().isSteppingCarefully()) {
                return false;
            }
            return true;
        }

        @Override
        public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, @Nullable Entity entity, @Nullable Entity projectileOwner, float distance) {
            String eventName = gameEvent.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
            int frequency = VibrationSystem.getGameEventFrequency(gameEvent);

            Map<String, Object> vib = new HashMap<>();
            vib.put("event", eventName);
            vib.put("distance", (double) distance);
            vib.put("frequency", frequency);
            vib.put("x", pos.getX());
            vib.put("y", pos.getY());
            vib.put("z", pos.getZ());
            if (entity != null) {
                vib.put("entity", entity.getName().getString());
            }

            lastVibration = vib;

            if (node != null) {
                node.sendToReachable("computer.signal", "vibration", eventName, (double) distance, frequency, pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }
}
