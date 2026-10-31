package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class EnvironmentChatUpgrade extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    private int distance = 32;
    private String customPrefix = "[RobotChat]";

    public EnvironmentChatUpgrade(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("chat_box", Visibility.Neighbors)
                .create());
    }

    @Callback(doc = "function(message:string[, distance:number, prefix:string]):boolean -- Broadcasts a chat message from the robot/drone.")
    public Object[] say(Context context, Arguments args) {
        String message = args.checkString(0);
        int range = args.optInteger(1, this.distance);
        String prefix = args.optString(2, this.customPrefix);

        Level level = host.getEnvironmentLevel();
        if (level instanceof ServerLevel serverLevel) {
            Component formatted = Component.literal(prefix + " ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(message).withStyle(ChatFormatting.WHITE));

            double x = host.xPosition();
            double y = host.yPosition();
            double z = host.zPosition();
            double rangeSq = range * range;

            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (player.level() == level) {
                    if (range <= 0 || player.distanceToSqr(x, y, z) <= rangeSq) {
                        player.sendSystemMessage(formatted);
                    }
                }
            }
            return new Object[]{true};
        }
        return new Object[]{false};
    }

    @Callback(doc = "function(player:string, message:string[, prefix:string]):boolean -- Sends a whisper to a player.")
    public Object[] tell(Context context, Arguments args) {
        String targetName = args.checkString(0);
        String message = args.checkString(1);
        String prefix = args.optString(2, this.customPrefix);

        Level level = host.getEnvironmentLevel();
        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayerByName(targetName);
            if (player != null) {
                Component formatted = Component.literal(prefix + " -> You: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(message).withStyle(ChatFormatting.ITALIC));
                player.sendSystemMessage(formatted);
                return new Object[]{true};
            }
        }
        return new Object[]{false, "Player not found"};
    }

    @Callback(direct = true, doc = "function():number -- Gets distance.")
    public Object[] getDistance(Context context, Arguments args) {
        return new Object[]{distance};
    }

    @Callback(doc = "function(distance:number):number -- Sets distance.")
    public Object[] setDistance(Context context, Arguments args) {
        this.distance = Math.max(0, Math.min(args.checkInteger(0), 64));
        return new Object[]{distance};
    }
}
