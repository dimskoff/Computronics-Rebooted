package pl.asie.computronics.service;

import li.cil.oc.api.network.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.network.RFJammerManager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Shared logic for the Chat Box block and the Chat Box upgrade.
 * All mutating / world-touching methods must be called on the server thread
 * (i.e. from non-direct OC callbacks or tick/event handlers).
 */
public final class ChatService {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_MESSAGE_LENGTH = 256;
    public static final int UNLIMITED = Integer.MAX_VALUE;
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9A-FK-ORX]?");

    /** Something that hears player chat and is told about it via the "chat_message" signal. */
    public interface Listener {
        Level chatLevel();
        Vec3 chatPosition();
        ChatState chatState();
        Node chatNode();
    }

    private static final Set<Listener> LISTENERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ChatService() {}

    public static void addListener(Listener l) {
        if (l != null) LISTENERS.add(l);
    }

    public static void removeListener(Listener l) {
        if (l != null) LISTENERS.remove(l);
    }

    /** Per-device persisted state (distance, name) and transient cooldown. */
    public static final class ChatState {
        private volatile int distance;
        private volatile String name;
        private long lastSendTick = Long.MIN_VALUE;

        public ChatState() {
            this.distance = configMaxDistance() < 0 ? 40 : configMaxDistance();
            this.name = defaultName();
        }

        public int getDistance() {
            return distance;
        }

        /** Stores the clamped distance; returns the stored value. */
        public int setDistance(int d) {
            this.distance = clampDistance(d);
            return this.distance;
        }

        public String getName() {
            return name;
        }

        public String setName(String n) {
            this.name = sanitizeName(n);
            return this.name;
        }

        public void save(CompoundTag tag) {
            tag.putInt("chatDistance", distance);
            tag.putString("chatName", name);
        }

        public void load(CompoundTag tag) {
            if (tag.contains("chatDistance")) distance = clampDistance(tag.getInt("chatDistance"));
            if (tag.contains("chatName")) name = sanitizeName(tag.getString("chatName"));
        }
    }

    // ----------------------------------------------------------------- config

    public static int configMaxDistance() {
        try {
            return ComputronicsServerConfig.SERVER.chatBoxMaxDistance.get();
        } catch (Exception e) {
            return 40;
        }
    }

    public static boolean allowInterdimensional() {
        try {
            return ComputronicsServerConfig.SERVER.chatBoxAllowInterdimensional.get();
        } catch (Exception e) {
            return false;
        }
    }

    public static int cooldownTicks() {
        try {
            return ComputronicsServerConfig.SERVER.chatBoxCooldownTicks.get();
        } catch (Exception e) {
            return 20;
        }
    }

    public static String defaultName() {
        String n;
        try {
            n = ComputronicsServerConfig.SERVER.chatBoxDefaultPrefix.get();
        } catch (Exception e) {
            n = "ChatBox";
        }
        // the config default is "[ChatBox]"; brackets are added by the formatter
        n = stripFormatting(n).trim();
        if (n.startsWith("[") && n.endsWith("]") && n.length() >= 2) {
            n = n.substring(1, n.length() - 1);
        }
        n = limit(n.trim(), MAX_NAME_LENGTH);
        return n.isEmpty() ? "ChatBox" : n;
    }

    // ----------------------------------------------------------------- sanitizing

    public static String stripFormatting(String s) {
        if (s == null) return "";
        return FORMATTING.matcher(s).replaceAll("").replace("§", "");
    }

    private static String limit(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    public static String sanitizeName(String n) {
        String s = stripFormatting(n).replaceAll("[\\p{Cntrl}]", "").trim();
        s = limit(s, MAX_NAME_LENGTH);
        return s.isEmpty() ? defaultName() : s;
    }

    public static String sanitizeMessage(String m) {
        String s = stripFormatting(m).replaceAll("[\\p{Cntrl}&&[^\\t]]", " ");
        return limit(s, MAX_MESSAGE_LENGTH);
    }

    /** Clamp to [1, configMax]; with configMax == -1, values <= 0 mean unlimited. */
    public static int clampDistance(int requested) {
        int max = configMaxDistance();
        if (max < 0) {
            return requested <= 0 ? UNLIMITED : requested;
        }
        if (requested <= 0) return max;
        return Math.max(1, Math.min(requested, max));
    }

    public static MutableComponent format(String name, String message) {
        MutableComponent prefix = Component.literal("[" + sanitizeName(name) + "] ")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        return prefix.append(Component.literal(sanitizeMessage(message)).withStyle(ChatFormatting.RESET));
    }

    // ----------------------------------------------------------------- sending

    private static boolean inReach(ServerPlayer player, Level level, Vec3 pos, int range) {
        if (player.level() != level) {
            // no cross-dimension distance math
            return allowInterdimensional();
        }
        if (range == UNLIMITED) return true;
        double r = range;
        return player.distanceToSqr(pos) <= r * r;
    }

    /**
     * Checks cooldown / jamming. Returns null when sending is allowed, otherwise an error message.
     * On success the cooldown is consumed.
     */
    private static String precheck(ChatState state, Level level, Vec3 pos) {
        if (!(level instanceof ServerLevel)) return "world unavailable";
        if (RFJammerManager.INSTANCE.isJammed(level, pos)) return "signal jammed";
        long now = level.getGameTime();
        int cd = cooldownTicks();
        if (cd > 0 && state.lastSendTick != Long.MIN_VALUE && now - state.lastSendTick < cd && now >= state.lastSendTick) {
            return "cooldown";
        }
        state.lastSendTick = now;
        return null;
    }

    /** say(text[, distance]) */
    public static Object[] say(ChatState state, Level level, Vec3 pos, String message, Integer distanceArg) {
        String err = precheck(state, level, pos);
        if (err != null) return new Object[]{false, err};
        int range = clampDistance(distanceArg != null ? distanceArg : state.getDistance());
        Component formatted = format(state.getName(), message);
        ServerLevel sl = (ServerLevel) level;
        for (ServerPlayer player : sl.getServer().getPlayerList().getPlayers()) {
            if (inReach(player, level, pos, range)) {
                player.sendSystemMessage(formatted);
            }
        }
        return new Object[]{true};
    }

    /** tell(player, text) -- same range/dimension rules as say(). */
    public static Object[] tell(ChatState state, Level level, Vec3 pos, String playerName, String message) {
        if (!(level instanceof ServerLevel sl)) return new Object[]{false, "world unavailable"};
        ServerPlayer player = sl.getServer().getPlayerList().getPlayerByName(playerName);
        if (player == null || !inReach(player, level, pos, clampDistance(state.getDistance()))) {
            return new Object[]{false, "player not in range"};
        }
        String err = precheck(state, level, pos);
        if (err != null) return new Object[]{false, err};
        player.sendSystemMessage(format(state.getName(), message));
        return new Object[]{true};
    }

    // ----------------------------------------------------------------- receiving

    /** Called from ServerChatEvent (server thread). */
    public static void onPlayerChat(ServerPlayer player, String message) {
        for (Listener l : LISTENERS) {
            Node node = l.chatNode();
            Level level = l.chatLevel();
            Vec3 pos = l.chatPosition();
            if (node == null || node.network() == null || level == null || pos == null) continue;
            int range = clampDistance(l.chatState().getDistance());
            if (!inReach(player, level, pos, range)) continue;
            if (RFJammerManager.INSTANCE.isJammed(level, pos)) continue;
            // OC prepends the component address: chat_message(address, username, message)
            node.sendToReachable("computer.signal", "chat_message", player.getName().getString(), message);
        }
    }

    public static Vec3 center(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }
}
