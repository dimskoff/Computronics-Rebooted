package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.service.ChatService;

public class EnvironmentChatUpgrade extends AbstractManagedEnvironment implements ChatService.Listener {
    private static final String DATA_KEY = "computronics_chat";

    private final EnvironmentHost host;
    private final ChatService.ChatState chat = new ChatService.ChatState();

    public EnvironmentChatUpgrade(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("chat_box", Visibility.Neighbors)
                .create());
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node() && host != null && host.getEnvironmentLevel() != null
                && !host.getEnvironmentLevel().isClientSide) {
            ChatService.addListener(this);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            ChatService.removeListener(this);
        }
    }

    // ChatService.Listener
    @Override
    public Level chatLevel() {
        return host != null ? host.getEnvironmentLevel() : null;
    }

    @Override
    public Vec3 chatPosition() {
        return host != null ? new Vec3(host.xPosition(), host.yPosition(), host.zPosition()) : null;
    }

    @Override
    public ChatService.ChatState chatState() {
        return chat;
    }

    @Override
    public Node chatNode() {
        return node();
    }

    @Callback(doc = "function(message:string[, distance:number]):boolean -- Broadcasts a chat message from the robot/drone.")
    public Object[] say(Context context, Arguments args) {
        String message = args.checkString(0);
        Integer dist = args.count() > 1 && (args.isInteger(1) || args.isDouble(1)) ? args.checkInteger(1) : null;
        return ChatService.say(chat, chatLevel(), chatPosition(), message, dist);
    }

    @Callback(doc = "function(player:string, message:string):boolean -- Sends a private message to a player within range.")
    public Object[] tell(Context context, Arguments args) {
        return ChatService.tell(chat, chatLevel(), chatPosition(), args.checkString(0), args.checkString(1));
    }

    @Callback(direct = true, doc = "function():number -- Gets the chat broadcast distance.")
    public Object[] getDistance(Context context, Arguments args) {
        int d = chat.getDistance();
        return new Object[]{d == ChatService.UNLIMITED ? -1 : d};
    }

    @Callback(doc = "function(distance:number):number -- Sets the chat broadcast distance (clamped to the server maximum).")
    public Object[] setDistance(Context context, Arguments args) {
        int d = chat.setDistance(args.checkInteger(0));
        if (host != null) host.markChanged();
        return new Object[]{d == ChatService.UNLIMITED ? -1 : d};
    }

    @Callback(direct = true, doc = "function():string -- Gets the name shown in chat.")
    public Object[] getName(Context context, Arguments args) {
        return new Object[]{chat.getName()};
    }

    @Callback(doc = "function(name:string):string -- Sets the name shown in chat (max 32 chars, formatting codes removed).")
    public Object[] setName(Context context, Arguments args) {
        String n = chat.setName(args.checkString(0));
        if (host != null) host.markChanged();
        return new Object[]{n};
    }

    @Override
    public void loadData(DataComponentHolder holder) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(holder);
        CustomData data = holder.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag root = data.copyTag();
            if (root.contains(DATA_KEY)) {
                chat.load(root.getCompound(DATA_KEY));
            }
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        CustomData existing = holder.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = existing != null ? existing.copyTag() : new CompoundTag();
        CompoundTag mine = new CompoundTag();
        chat.save(mine);
        root.put(DATA_KEY, mine);
        holder.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
