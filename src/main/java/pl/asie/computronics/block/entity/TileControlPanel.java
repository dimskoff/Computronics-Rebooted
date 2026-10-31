package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.init.ModSounds;
import pl.asie.computronics.item.ItemMiniButton;
import pl.asie.computronics.item.ItemMiniLamp;
import pl.asie.computronics.item.ItemScrewdriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileControlPanel extends TileEntityComputronics {
    public static final int GRID_SIZE = 8;
    public static final int TOTAL_SLOTS = GRID_SIZE * GRID_SIZE; // 64 slots

    public enum ElementType {
        NONE,
        BUTTON,
        LAMP
    }

    public static class PanelElement {
        public int slot = 0;
        public ElementType type = ElementType.NONE;
        public int color = 0xCC2020; // default red
        public boolean active = false;
        public int pressTicks = 0;
        public long lastPressTime = 0;
        public int size = 1; // 1, 2, 3, 4
        public String label = "";
        public int textColor = 0xFFFFFF;
        public boolean bold = true;
    }

    private final PanelElement[] elements = new PanelElement[TOTAL_SLOTS];

    public TileControlPanel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROL_PANEL.get(), pos, state);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            elements[i] = new PanelElement();
            elements[i].slot = i;
        }
        node = Network.newNode(this, Visibility.Network)
                .withComponent("control_panel", Visibility.Network)
                .create();
    }

    public Direction getHorizontalFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    public boolean isTopHalf() {
        if (getBlockState().hasProperty(BlockStateProperties.HALF)) {
            return getBlockState().getValue(BlockStateProperties.HALF) == Half.TOP;
        }
        return false;
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (elements[i].pressTicks > 0) {
                elements[i].pressTicks--;
                if (elements[i].pressTicks == 0) {
                    if (level != null) {
                        level.playSound(null, worldPosition, ModSounds.BUTTON_RELEASE.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
                    }
                    markChangedAndSync();
                }
            }
        }
    }

    public void markChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getSlotIndex(int x, int y) {
        x = Math.max(0, Math.min(x, GRID_SIZE - 1));
        y = Math.max(0, Math.min(y, GRID_SIZE - 1));
        return y * GRID_SIZE + x;
    }

    public PanelElement findRootElement(int gx, int gy) {
        int slot = getSlotIndex(gx, gy);
        if (elements[slot].type != ElementType.NONE) {
            return elements[slot];
        }
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            PanelElement e = elements[i];
            if (e.type != ElementType.NONE && e.size > 1) {
                int ex = i % GRID_SIZE;
                int ey = i / GRID_SIZE;
                if (gx >= ex && gx < ex + e.size && gy >= ey && gy < ey + e.size) {
                    return e;
                }
            }
        }
        return null;
    }

    public boolean resizeElement(PanelElement elem, int newSize) {
        int ex = elem.slot % GRID_SIZE;
        int ey = elem.slot / GRID_SIZE;
        int maxSize = Math.min(4, Math.min(GRID_SIZE - ex, GRID_SIZE - ey));
        newSize = Math.max(1, Math.min(newSize, maxSize));
        elem.size = newSize;
        markChangedAndSync();
        return true;
    }

    public int[] calculateGridPos(Vec3 loc) {
        Direction facing = getHorizontalFacing();

        double lx = Math.max(0.0, Math.min(0.999, loc.x - worldPosition.getX()));
        double lz = Math.max(0.0, Math.min(0.999, loc.z - worldPosition.getZ()));

        double u;
        double v;

        switch (facing) {
            case NORTH -> { u = lx; v = lz; }
            case SOUTH -> { u = 1.0 - lx; v = 1.0 - lz; }
            case WEST  -> { u = 1.0 - lz; v = lx; }
            case EAST  -> { u = lz; v = 1.0 - lx; }
            default    -> { u = lx; v = lz; }
        }

        int gx = Math.max(0, Math.min(GRID_SIZE - 1, (int) (u * GRID_SIZE)));
        int gy = Math.max(0, Math.min(GRID_SIZE - 1, (int) (v * GRID_SIZE)));
        return new int[]{gx, gy};
    }

    public int[] calculateGridPos(BlockHitResult hit) {
        return calculateGridPos(hit.getLocation());
    }

    public ItemInteractionResult onInteractWithItem(Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // Screwdriver has its own UseOnContext handler
        if (held.getItem() instanceof ItemScrewdriver) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int[] grid = calculateGridPos(hit);
        if (grid == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int gx = grid[0];
        int gy = grid[1];
        int slot = getSlotIndex(gx, gy);

        if (held.getItem() instanceof ItemMiniLamp) {
            PanelElement existing = findRootElement(gx, gy);
            if (existing == null) {
                elements[slot].type = ElementType.LAMP;
                elements[slot].color = 0x20CC40; // Green
                elements[slot].active = true;
                elements[slot].size = 1;
                elements[slot].label = "";
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.5f);
                markChangedAndSync();
                player.displayClientMessage(Component.literal(String.format("§a[Control Panel] §fInstalled Mini Lamp at (%d, %d)", gx, gy)), true);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (held.getItem() instanceof ItemMiniButton) {
            PanelElement existing = findRootElement(gx, gy);
            if (existing == null) {
                elements[slot].type = ElementType.BUTTON;
                elements[slot].color = 0xCC2020; // Red
                elements[slot].active = false;
                elements[slot].size = 1;
                elements[slot].label = "";
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);
                markChangedAndSync();
                player.displayClientMessage(Component.literal(String.format("§e[Control Panel] §fInstalled Mini Button at (%d, %d)", gx, gy)), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public boolean mountElementAt(int gx, int gy, ElementType type, int color) {
        if (gx < 0 || gx >= GRID_SIZE || gy < 0 || gy >= GRID_SIZE) return false;
        PanelElement existing = findRootElement(gx, gy);
        if (existing != null) return false;
        int slot = getSlotIndex(gx, gy);
        elements[slot].type = type;
        elements[slot].color = color;
        elements[slot].active = (type == ElementType.LAMP);
        elements[slot].size = 1;
        elements[slot].label = "";
        markChangedAndSync();
        return true;
    }

    public InteractionResult onInteractEmptyHand(Player player, BlockHitResult hit) {
        int[] grid = calculateGridPos(hit);
        if (grid == null) {
            return InteractionResult.PASS;
        }

        int gx = grid[0];
        int gy = grid[1];
        PanelElement elem = findRootElement(gx, gy);

        if (player.isShiftKeyDown()) {
            if (elem != null && elem.type != ElementType.NONE) {
                // Shift-Click: Cycle size (1x1 -> 2x2 -> 3x3 -> 4x4 -> 1x1)
                int newSize = (elem.size % 4) + 1;
                resizeElement(elem, newSize);
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
                }
                player.displayClientMessage(Component.literal(String.format("§e[Control Panel] §fResized component to §b%dx%d", newSize, newSize)), true);
                return InteractionResult.SUCCESS;
            }
        } else {
            // Normal click
            if (elem != null && elem.type == ElementType.BUTTON) {
                elem.pressTicks = 10;
                elem.lastPressTime = System.currentTimeMillis();
                if (level != null) {
                    level.playSound(null, worldPosition, ModSounds.BUTTON_PRESS.get(), SoundSource.BLOCKS, 0.85f, 1.0f);
                    markChangedAndSync();
                }
                if (node != null) {
                    int rx = elem.slot % GRID_SIZE;
                    int ry = elem.slot / GRID_SIZE;
                    node.sendToReachable("computer.signal", "panel_button", elem.slot + 1, player.getName().getString(), rx, ry);
                }
                player.displayClientMessage(Component.literal(String.format("§6[Control Panel] §fPressed Button #%d %s", elem.slot + 1, elem.label.isEmpty() ? "" : "(" + elem.label + ")")), true);
                return InteractionResult.SUCCESS;
            } else if (elem != null && elem.type == ElementType.LAMP) {
                String colorHex = String.format("#%06X", elem.color);
                player.displayClientMessage(Component.literal(String.format("§b[Control Panel] §fMini Lamp: %s (Color: %s)", elem.active ? "ON" : "OFF", colorHex)), true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    public PanelElement getElement(int slot) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            return elements[slot];
        }
        return null;
    }

    // =========================================================================
    // OpenComputers Callbacks
    // =========================================================================

    @Callback(direct = true, doc = "function():number -- Gets grid dimension (8).")
    public Object[] getGridSize(Context context, Arguments args) {
        return new Object[]{GRID_SIZE};
    }

    @Callback(doc = "function(x:number, y:number, color:number):boolean -- Sets miniature lamp RGB color.")
    public Object[] setLampColor(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        int color = args.checkInteger(2);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null && elem.type == ElementType.LAMP) {
            elem.color = color & 0xFFFFFF;
            elem.active = (color != 0);
            markChangedAndSync();
            return new Object[]{true};
        }
        return new Object[]{false, "no lamp at coordinates"};
    }

    @Callback(doc = "function(x:number, y:number, state:boolean):boolean -- Toggles miniature lamp on/off.")
    public Object[] setLamp(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        boolean state = args.checkBoolean(2);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null && elem.type == ElementType.LAMP) {
            elem.active = state;
            markChangedAndSync();
            return new Object[]{true};
        }
        return new Object[]{false, "no lamp at coordinates"};
    }

    @Callback(doc = "function(x:number, y:number):boolean -- Software button press.")
    public Object[] pressButton(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null && elem.type == ElementType.BUTTON) {
            elem.pressTicks = 10;
            elem.lastPressTime = System.currentTimeMillis();
            if (level != null) {
                level.playSound(null, worldPosition, ModSounds.BUTTON_PRESS.get(), SoundSource.BLOCKS, 0.85f, 1.0f);
                markChangedAndSync();
            }
            if (node != null) {
                int rx = elem.slot % GRID_SIZE;
                int ry = elem.slot / GRID_SIZE;
                node.sendToReachable("computer.signal", "panel_button", elem.slot + 1, "SYSTEM", rx, ry);
            }
            return new Object[]{true};
        }
        return new Object[]{false, "no button at coordinates"};
    }

    @Callback(doc = "function(x:number, y:number, size:number):boolean -- Sets component size (1-4).")
    public Object[] setElementSize(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        int size = args.checkInteger(2);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null) {
            return new Object[]{resizeElement(elem, size)};
        }
        return new Object[]{false, "no element at coordinates"};
    }

    @Callback(doc = "function(x:number, y:number, label:string[, textColor:number, bold:boolean]):boolean -- Sets button text label.")
    public Object[] setButtonLabel(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        String label = args.checkString(2);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null) {
            elem.label = label;
            if (args.count() >= 4) elem.textColor = args.checkInteger(3);
            if (args.count() >= 5) elem.bold = args.checkBoolean(4);
            markChangedAndSync();
            return new Object[]{true};
        }
        return new Object[]{false, "no element at coordinates"};
    }

    @Callback(doc = "function(x:number, y:number, color:number):boolean -- Sets button/lamp base color.")
    public Object[] setElementColor(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        int color = args.checkInteger(2);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null) {
            elem.color = color & 0xFFFFFF;
            markChangedAndSync();
            return new Object[]{true};
        }
        return new Object[]{false, "no element at coordinates"};
    }

    @Callback(direct = true, doc = "function():table -- Returns all components on the panel.")
    public Object[] getElements(Context context, Arguments args) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (elements[i].type != ElementType.NONE) {
                Map<String, Object> map = new HashMap<>();
                map.put("slot", i + 1);
                map.put("x", i % GRID_SIZE);
                map.put("y", i / GRID_SIZE);
                map.put("type", elements[i].type.name().toLowerCase());
                map.put("active", elements[i].active);
                map.put("color", elements[i].color);
                map.put("size", elements[i].size);
                map.put("label", elements[i].label);
                map.put("pressed", elements[i].pressTicks > 0);
                result.add(map);
            }
        }
        return new Object[]{result};
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (elements[i].type != ElementType.NONE) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("slot", i);
                itemTag.putString("type", elements[i].type.name());
                itemTag.putInt("color", elements[i].color);
                itemTag.putBoolean("active", elements[i].active);
                itemTag.putInt("size", elements[i].size);
                itemTag.putString("label", elements[i].label);
                itemTag.putInt("textColor", elements[i].textColor);
                itemTag.putBoolean("bold", elements[i].bold);
                list.add(itemTag);
            }
        }
        tag.put("elements", list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            elements[i].type = ElementType.NONE;
            elements[i].active = false;
            elements[i].size = 1;
            elements[i].label = "";
            elements[i].textColor = 0xFFFFFF;
            elements[i].bold = true;
        }
        if (tag.contains("elements")) {
            ListTag list = tag.getList("elements", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getInt("slot");
                if (slot >= 0 && slot < TOTAL_SLOTS) {
                    elements[slot].slot = slot;
                    elements[slot].type = ElementType.valueOf(itemTag.getString("type"));
                    elements[slot].color = itemTag.getInt("color");
                    elements[slot].active = itemTag.getBoolean("active");
                    elements[slot].size = itemTag.contains("size") ? Math.max(1, Math.min(4, itemTag.getInt("size"))) : 1;
                    elements[slot].label = itemTag.contains("label") ? itemTag.getString("label") : "";
                    elements[slot].textColor = itemTag.contains("textColor") ? itemTag.getInt("textColor") : 0xFFFFFF;
                    elements[slot].bold = !itemTag.contains("bold") || itemTag.getBoolean("bold");
                }
            }
        }
    }
}
