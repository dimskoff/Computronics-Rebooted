package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.service.ChatService;

public class TileChatBox extends TileEntityComputronics implements ChatService.Listener {
    private final ChatService.ChatState chat = new ChatService.ChatState();

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
            ChatService.addListener(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ChatService.removeListener(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ChatService.removeListener(this);
    }

    // ChatService.Listener
    @Override
    public Level chatLevel() {
        return level;
    }

    @Override
    public Vec3 chatPosition() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public ChatService.ChatState chatState() {
        return chat;
    }

    @Override
    public Node chatNode() {
        return node;
    }

    private static boolean enabled() {
        try {
            return ComputronicsServerConfig.SERVER.enableChatBox.get();
        } catch (Exception e) {
            return true;
        }
    }

    @Callback(doc = "function(message:string[, distance:number]):boolean -- Broadcasts a chat message to players within range.")
    public Object[] say(Context context, Arguments args) {
        if (!enabled()) {
            return result(false, "chat box is disabled in server config");
        }
        String message = args.checkString(0);
        Integer dist = args.count() > 1 && (args.isInteger(1) || args.isDouble(1)) ? args.checkInteger(1) : null;
        return ChatService.say(chat, level, chatPosition(), message, dist);
    }

    @Callback(doc = "function(player:string, message:string):boolean -- Sends a private message to a player within range.")
    public Object[] tell(Context context, Arguments args) {
        if (!enabled()) {
            return result(false, "chat box is disabled in server config");
        }
        return ChatService.tell(chat, level, chatPosition(), args.checkString(0), args.checkString(1));
    }

    @Callback(direct = true, doc = "function():number -- Gets the chat broadcast distance.")
    public Object[] getDistance(Context context, Arguments args) {
        int d = chat.getDistance();
        return result(d == ChatService.UNLIMITED ? -1 : d);
    }

    @Callback(doc = "function(distance:number):number -- Sets the chat broadcast distance (clamped to the server maximum).")
    public Object[] setDistance(Context context, Arguments args) {
        int d = chat.setDistance(args.checkInteger(0));
        setChanged();
        return result(d == ChatService.UNLIMITED ? -1 : d);
    }

    @Callback(direct = true, doc = "function():string -- Gets the name shown in chat.")
    public Object[] getName(Context context, Arguments args) {
        return result(chat.getName());
    }

    @Callback(doc = "function(name:string):string -- Sets the name shown in chat (max 32 chars, formatting codes removed).")
    public Object[] setName(Context context, Arguments args) {
        String n = chat.setName(args.checkString(0));
        setChanged();
        return result(n);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        chat.save(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        chat.load(tag);
    }

    /** Registered on the NeoForge event bus by Computronics; dispatches to every chat listener. */
    public static class ChatEventHandler {
        public static final ChatEventHandler INSTANCE = new ChatEventHandler();

        @SubscribeEvent
        public void onServerChat(ServerChatEvent event) {
            ChatService.onPlayerChat(event.getPlayer(), event.getRawText());
        }
    }
}
