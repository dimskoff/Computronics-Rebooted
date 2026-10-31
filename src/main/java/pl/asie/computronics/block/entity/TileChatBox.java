package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TileChatBox extends TileEntityComputronics {
    public static final Set<TileChatBox> ACTIVE_CHAT_BOXES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private int distance = 64;
    private String customPrefix = "[ChatBox]";

    public TileChatBox(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHAT_BOX.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("chat_box", Visibility.Network)
                .create();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ACTIVE_CHAT_BOXES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ACTIVE_CHAT_BOXES.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ACTIVE_CHAT_BOXES.remove(this);
    }

    @Callback(doc = "function(message:string[, distance:number, prefix:string]):boolean -- Broadcasts a chat message.")
    public Object[] say(Context context, Arguments args) {
        String message = args.checkString(0);
        int range = args.optInteger(1, this.distance);
        String prefix = args.optString(2, this.customPrefix);

        if (level instanceof ServerLevel serverLevel) {
            Component formatted = Component.literal(prefix + " ").withStyle(ChatFormatting.DARK_PURPLE)
                    .append(Component.literal(message).withStyle(ChatFormatting.WHITE));

            double x = getBlockPos().getX() + 0.5;
            double y = getBlockPos().getY() + 0.5;
            double z = getBlockPos().getZ() + 0.5;
            double rangeSq = range * range;

            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (player.level() == level) {
                    if (range <= 0 || player.distanceToSqr(x, y, z) <= rangeSq) {
                        player.sendSystemMessage(formatted);
                    }
                }
            }
            return result(true);
        }
        return result(false);
    }

    @Callback(doc = "function(player:string, message:string[, prefix:string]):boolean -- Sends a private whisper to a player.")
    public Object[] tell(Context context, Arguments args) {
        String targetName = args.checkString(0);
        String message = args.checkString(1);
        String prefix = args.optString(2, this.customPrefix);

        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayerByName(targetName);
            if (player != null) {
                Component formatted = Component.literal(prefix + " -> You: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(message).withStyle(ChatFormatting.ITALIC));
                player.sendSystemMessage(formatted);
                return result(true);
            }
        }
        return result(false, "Player not found");
    }

    @Callback(direct = true, doc = "function():number -- Gets the default chat broadcast distance.")
    public Object[] getDistance(Context context, Arguments args) {
        return result(distance);
    }

    @Callback(doc = "function(distance:number):number -- Sets the default chat broadcast distance.")
    public Object[] setDistance(Context context, Arguments args) {
        int newDist = args.checkInteger(0);
        this.distance = Math.max(0, Math.min(newDist, 128));
        return result(this.distance);
    }

    @Callback(direct = true, doc = "function():string -- Gets the custom prefix name.")
    public Object[] getName(Context context, Arguments args) {
        return result(customPrefix);
    }

    @Callback(doc = "function(name:string):string -- Sets the custom prefix name.")
    public Object[] setName(Context context, Arguments args) {
        this.customPrefix = args.checkString(0);
        return result(customPrefix);
    }

    public void onPlayerChat(ServerPlayer player, String message) {
        if (node != null && level != null && player.level() == level) {
            double dist = Math.sqrt(player.distanceToSqr(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5
            ));
            if (distance <= 0 || dist <= distance) {
                node.sendToReachable("computer.signal", "chat_message", player.getName().getString(), message, dist);
            }
        }
    }

    public static class ChatEventHandler {
        public static final ChatEventHandler INSTANCE = new ChatEventHandler();

        @SubscribeEvent
        public void onServerChat(ServerChatEvent event) {
            ServerPlayer player = event.getPlayer();
            String rawMessage = event.getMessage().getString();
            for (TileChatBox box : ACTIVE_CHAT_BOXES) {
                box.onPlayerChat(player, rawMessage);
            }
        }
    }
}
