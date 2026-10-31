package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.HashMap;
import java.util.Map;

public class TileWeatherStation extends TileEntityComputronics {

    public TileWeatherStation(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEATHER_STATION.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("weather_station", Visibility.Network)
                .create();
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if it is raining.")
    public Object[] isRaining(Context context, Arguments args) {
        if (level == null) return result(false);
        return result(level.isRaining());
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if there is a thunderstorm.")
    public Object[] isThundering(Context context, Arguments args) {
        if (level == null) return result(false);
        return result(level.isThundering());
    }

    @Callback(direct = true, doc = "function():number -- Gets the rain level (0.0 to 1.0).")
    public Object[] getRainStrength(Context context, Arguments args) {
        if (level == null) return result(0.0);
        return result((double) level.getRainLevel(1.0f));
    }

    @Callback(direct = true, doc = "function():number -- Gets the thunder level (0.0 to 1.0).")
    public Object[] getThunderStrength(Context context, Arguments args) {
        if (level == null) return result(0.0);
        return result((double) level.getThunderLevel(1.0f));
    }

    @Callback(direct = true, doc = "function():number -- Gets the temperature at the station.")
    public Object[] getTemperature(Context context, Arguments args) {
        if (level == null) return result(0.0);
        Holder<Biome> biome = level.getBiome(worldPosition);
        return result((double) biome.value().getBaseTemperature());
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if biome has precipitation.")
    public Object[] hasPrecipitation(Context context, Arguments args) {
        if (level == null) return result(false);
        Holder<Biome> biome = level.getBiome(worldPosition);
        return result(biome.value().hasPrecipitation());
    }

    @Callback(direct = true, doc = "function():number -- Gets the current light level.")
    public Object[] getLightLevel(Context context, Arguments args) {
        if (level == null) return result(0);
        return result(level.getMaxLocalRawBrightness(worldPosition.above()));
    }

    @Callback(direct = true, doc = "function():number -- Gets the lunar phase (0-7).")
    public Object[] getMoonPhase(Context context, Arguments args) {
        if (level == null) return result(0);
        return result(level.getMoonPhase());
    }

    @Callback(doc = "function():table -- Predicts storm arrival / duration.")
    public Object[] predictStorm(Context context, Arguments args) {
        if (level instanceof ServerLevel serverLevel) {
            Map<String, Object> forecast = new HashMap<>();
            forecast.put("isRaining", serverLevel.isRaining());
            forecast.put("isThundering", serverLevel.isThundering());
            if (serverLevel.getLevelData() instanceof ServerLevelData sld) {
                forecast.put("clearWeatherTicks", sld.getClearWeatherTime());
                forecast.put("rainTicks", sld.getRainTime());
                forecast.put("thunderTicks", sld.getThunderTime());
            }
            return result(forecast);
        }
        return result(null, "server level unavailable");
    }
}
