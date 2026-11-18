package pl.asie.computronics.terminal;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import pl.asie.computronics.item.ItemPocketTerminal;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PocketTerminalManager {
    private static final Map<String, TerminalEndpoint> ENDPOINTS = new ConcurrentHashMap<>();

    private static boolean registered;

    /** Registers logout / server-stop / stale-endpoint cleanup listeners (idempotent; also called lazily). */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> removeAllOf(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> clearAll());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 20 == 0) sweepStale(e.getServer().getTickCount());
        });
    }

    public static void updateTerminal(ServerPlayer player, String terminalId, ItemStack stack) {
        if (terminalId == null || terminalId.isEmpty()) return;
        init();
        int now = player.getServer() != null ? player.getServer().getTickCount() : 0;

        TerminalEndpoint endpoint = ENDPOINTS.get(terminalId);
        if (endpoint == null) {
            endpoint = new TerminalEndpoint(player, terminalId, stack);
            endpoint.lastSeenTick = now;
            endpoint.lastNetworkUpdateTick = now;
            ENDPOINTS.put(terminalId, endpoint);
            try {
                Network.joinWirelessNetwork(endpoint);
            } catch (Throwable ignored) {}
        } else {
            boolean changedPlayer = endpoint.playerRef.get() != player;
            endpoint.updateState(player, stack);
            endpoint.lastSeenTick = now;
            // Only re-index in the wireless network when moved more than a block or every 20 ticks.
            boolean moved = endpoint.lastNetworkPos.distToCenterSqr(player.position()) > 1.0 || changedPlayer;
            if (moved || now - endpoint.lastNetworkUpdateTick >= 20) {
                endpoint.lastNetworkPos = player.blockPosition();
                endpoint.lastNetworkUpdateTick = now;
                try {
                    Network.updateWirelessNetwork(endpoint);
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void removeAllOf(UUID playerId) {
        ENDPOINTS.values().removeIf(ep -> {
            if (playerId.equals(ep.playerId)) {
                leave(ep);
                return true;
            }
            return false;
        });
    }

    public static void clearAll() {
        ENDPOINTS.values().forEach(PocketTerminalManager::leave);
        ENDPOINTS.clear();
    }

    private static void sweepStale(int now) {
        ENDPOINTS.values().removeIf(ep -> {
            ServerPlayer p = ep.playerRef.get();
            if (p == null || p.isRemoved() || now - ep.lastSeenTick > 40) {
                leave(ep);
                return true;
            }
            return false;
        });
    }

    private static void leave(TerminalEndpoint ep) {
        try {
            Network.leaveWirelessNetwork(ep);
        } catch (Throwable ignored) {}
    }

    public static void removeTerminal(String terminalId) {
        if (terminalId == null || terminalId.isEmpty()) return;
        TerminalEndpoint endpoint = ENDPOINTS.remove(terminalId);
        if (endpoint != null) {
            try {
                Network.leaveWirelessNetwork(endpoint);
            } catch (Throwable ignored) {}
        }
    }

    public static class TerminalEndpoint implements WirelessEndpoint {
        private WeakReference<ServerPlayer> playerRef;
        private final UUID playerId;
        private int lastSeenTick;
        private int lastNetworkUpdateTick;
        private BlockPos lastNetworkPos;
        private final String terminalId;
        private WeakReference<ItemStack> stackRef;
        private BlockPos lastPos;

        public TerminalEndpoint(ServerPlayer player, String terminalId, ItemStack stack) {
            this.playerRef = new WeakReference<>(player);
            this.terminalId = terminalId;
            this.stackRef = new WeakReference<>(stack);
            this.lastPos = player.blockPosition();
            this.playerId = player.getUUID();
            this.lastNetworkPos = lastPos;
        }

        public void updateState(ServerPlayer player, ItemStack stack) {
            this.playerRef = new WeakReference<>(player);
            this.stackRef = new WeakReference<>(stack);
            this.lastPos = player.blockPosition();
        }

        @Override
        public int x() {
            ServerPlayer p = playerRef.get();
            return p != null ? p.getBlockX() : lastPos.getX();
        }

        @Override
        public int y() {
            ServerPlayer p = playerRef.get();
            return p != null ? p.getBlockY() : lastPos.getY();
        }

        @Override
        public int z() {
            ServerPlayer p = playerRef.get();
            return p != null ? p.getBlockZ() : lastPos.getZ();
        }

        @Override
        public Level getWirelessLevel() {
            ServerPlayer p = playerRef.get();
            return p != null ? p.level() : null;
        }

        @Override
        public void receivePacket(Packet packet, WirelessEndpoint sender) {
            ItemStack stack = stackRef.get();
            ServerPlayer player = playerRef.get();
            if (player == null || stack == null || !ItemPocketTerminal.isActive(stack)) return;

            // Strict wireless port check!
            int listenPort = ItemPocketTerminal.getListeningPort(stack);
            if (packet.port() != listenPort) {
                return;
            }

            Object[] data = packet.data();
            if (data == null || data.length < 2) return;

            int argOffset = 0;
            String target = "all";

            String first = String.valueOf(data[0]).toLowerCase(Locale.ROOT);
            if ("pt".equals(first) || "pocket".equals(first) || "vram".equals(first) || "pager".equals(first)) {
                if (data.length < 3) return;
                target = String.valueOf(data[1]);
                argOffset = 2;
            } else {
                target = String.valueOf(data[0]);
                argOffset = 1;
            }

            boolean matches = "all".equalsIgnoreCase(target)
                    || terminalId.equalsIgnoreCase(target)
                    || (target.length() >= 4 && terminalId.toLowerCase(Locale.ROOT).startsWith(target.toLowerCase(Locale.ROOT)));

            if (!matches) return;

            String cmd = String.valueOf(data[argOffset]).toLowerCase(Locale.ROOT);

            switch (cmd) {
                case "clear" -> {
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        for (int i = 0; i < ItemPocketTerminal.SCREEN_HEIGHT; i++) {
                            tag.remove("line_" + i);
                            tag.remove("fg_line_" + i);
                        }
                    });
                }
                case "set" -> {
                    if (data.length >= argOffset + 4) {
                        int x = parseNumber(data[argOffset + 1], 1);
                        int y = parseNumber(data[argOffset + 2], 1);
                        String text = String.valueOf(data[argOffset + 3]);
                        int fg = data.length >= argOffset + 5 ? parseNumber(data[argOffset + 4], -1) : -1;
                        setLineText(stack, x, y, text, fg);
                    }
                }
                case "fill" -> {
                    if (data.length >= argOffset + 6) {
                        int x = parseNumber(data[argOffset + 1], 1);
                        int y = parseNumber(data[argOffset + 2], 1);
                        int w = parseNumber(data[argOffset + 3], 1);
                        int h = parseNumber(data[argOffset + 4], 1);
                        String ch = String.valueOf(data[argOffset + 5]);
                        if (ch.isEmpty()) ch = " ";
                        char fillChar = ch.charAt(0);
                        int fg = data.length >= argOffset + 7 ? parseNumber(data[argOffset + 6], -1) : -1;

                        for (int row = y; row < y + h && row <= ItemPocketTerminal.SCREEN_HEIGHT; row++) {
                            StringBuilder sb = new StringBuilder();
                            for (int col = 0; col < w && (x + col) <= ItemPocketTerminal.SCREEN_WIDTH; col++) {
                                sb.append(fillChar);
                            }
                            setLineText(stack, x, row, sb.toString(), fg);
                        }
                    }
                }
                case "print" -> {
                    if (data.length >= argOffset + 2) {
                        String text = String.valueOf(data[argOffset + 1]);
                        int fg = data.length >= argOffset + 3 ? parseNumber(data[argOffset + 2], -1) : -1;
                        scrollPrint(stack, text, fg);
                    }
                }
                case "color", "palette" -> {
                    if (data.length >= argOffset + 3) {
                        int fg = parseNumber(data[argOffset + 1], 0x00FF66);
                        int bg = parseNumber(data[argOffset + 2], 0x08150A);
                        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                            tag.putInt("fg_col", fg);
                            tag.putInt("bg_col", bg);
                        });
                    }
                }
                case "port" -> {
                    if (data.length >= argOffset + 2) {
                        int newPort = parseNumber(data[argOffset + 1], ItemPocketTerminal.defaultPort());
                        ItemPocketTerminal.setListeningPort(stack, newPort);
                    }
                }
                case "title" -> {
                    if (data.length >= argOffset + 2) {
                        String title = String.valueOf(data[argOffset + 1]);
                        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                            tag.putString("title", title);
                        });
                    }
                }
                case "beep" -> {
                    float pitch = 1.0f;
                    if (data.length >= argOffset + 2) {
                        pitch = (float) Math.max(0.5, Math.min(2.0, parseDouble(data[argOffset + 1], 1.0)));
                    }
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, pitch);
                }
            }
        }

        private static int parseNumber(Object obj, int fallback) {
            if (obj instanceof Number n) return n.intValue();
            try {
                return (int) Double.parseDouble(String.valueOf(obj));
            } catch (Exception e) {
                return fallback;
            }
        }

        private static double parseDouble(Object obj, double fallback) {
            if (obj instanceof Number n) return n.doubleValue();
            try {
                return Double.parseDouble(String.valueOf(obj));
            } catch (Exception e) {
                return fallback;
            }
        }

        private static void setLineText(ItemStack stack, int x, int y, String text, int fgColor) {
            if (y < 1 || y > ItemPocketTerminal.SCREEN_HEIGHT) return;
            int rowIndex = y - 1;

            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                String existing = tag.contains("line_" + rowIndex) ? tag.getString("line_" + rowIndex) : "";
                StringBuilder sb = new StringBuilder(existing);
                while (sb.length() < ItemPocketTerminal.SCREEN_WIDTH) {
                    sb.append(' ');
                }

                int start = Math.max(0, x - 1);
                for (int i = 0; i < text.length() && (start + i) < ItemPocketTerminal.SCREEN_WIDTH; i++) {
                    sb.setCharAt(start + i, text.charAt(i));
                }

                tag.putString("line_" + rowIndex, sb.toString());
                if (fgColor >= 0) {
                    tag.putInt("fg_line_" + rowIndex, fgColor);
                }
            });
        }

        private static void scrollPrint(ItemStack stack, String text, int fgColor) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                for (int i = 0; i < ItemPocketTerminal.SCREEN_HEIGHT - 1; i++) {
                    String next = tag.contains("line_" + (i + 1)) ? tag.getString("line_" + (i + 1)) : "";
                    tag.putString("line_" + i, next);
                    if (tag.contains("fg_line_" + (i + 1))) {
                        tag.putInt("fg_line_" + i, tag.getInt("fg_line_" + (i + 1)));
                    } else {
                        tag.remove("fg_line_" + i);
                    }
                }
                String lineText = text.length() > ItemPocketTerminal.SCREEN_WIDTH
                        ? text.substring(0, ItemPocketTerminal.SCREEN_WIDTH)
                        : text;
                tag.putString("line_" + (ItemPocketTerminal.SCREEN_HEIGHT - 1), lineText);
                if (fgColor >= 0) {
                    tag.putInt("fg_line_" + (ItemPocketTerminal.SCREEN_HEIGHT - 1), fgColor);
                }
            });
        }
    }
}
