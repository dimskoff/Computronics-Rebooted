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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import pl.asie.computronics.block.BlockControlPanel;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.init.ModSounds;
import pl.asie.computronics.item.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileControlPanel extends TileEntityComputronics {

    public static final int GRID_SIZE = 8;
    public static final int TOTAL_SLOTS = GRID_SIZE * GRID_SIZE; // 64

    public enum ElementType {
        NONE,
        BUTTON,
        LAMP,
        SEVEN_SEGMENT,
        SWITCH,
        TEXT
    }

    public static class PanelElement {
        public int slot = 0;
        public ElementType type = ElementType.NONE;
        public int color = 0xCC2020; // default red
        public boolean active = false;
        public int pressTicks = 0;
        public long lastPressTime = 0;
        public int size = 1;
        public int sizeX = 1;
        public int sizeY = 1;
        public String label = "";
        public int textColor = 0xFFFFFF;
        public boolean bold = true;
        public String displayValue = "0";
        public float textScale = 1.0f;
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
            if (e.type != ElementType.NONE && (e.sizeX > 1 || e.sizeY > 1)) {
                int ex = i % GRID_SIZE;
                int ey = i / GRID_SIZE;
                if (gx >= ex && gx < ex + e.sizeX && gy >= ey && gy < ey + e.sizeY) {
                    return e;
                }
            }
        }
        return null;
    }

    public boolean resizeElement(PanelElement elem, int newSize) {
        return resizeElement(elem, newSize, newSize);
    }

    public boolean resizeElement(PanelElement elem, int newSizeX, int newSizeY) {
        if (elem == null || elem.type == ElementType.NONE) return false;
        int gx = elem.slot % GRID_SIZE;
        int gy = elem.slot / GRID_SIZE;

        if (gx + newSizeX > GRID_SIZE || gy + newSizeY > GRID_SIZE) return false;

        for (int x = gx; x < gx + newSizeX; x++) {
            for (int y = gy; y < gy + newSizeY; y++) {
                PanelElement other = findRootElement(x, y);
                if (other != null && other != elem) {
                    return false;
                }
            }
        }

        elem.sizeX = newSizeX;
        elem.sizeY = newSizeY;
        elem.size = Math.max(newSizeX, newSizeY);
        markChangedAndSync();
        return true;
    }

    public int[] calculateGridPos(Vec3 loc) {
        if (level != null) {
            return BlockControlPanel.calculateGrid(getBlockState(), worldPosition, loc);
        }
        return new int[]{0, 0};
    }

    public int[] calculateGridPos(BlockHitResult hit) {
        if (level != null) {
            return BlockControlPanel.calculateGrid(getBlockState(), worldPosition, hit);
        }
        return calculateGridPos(hit.getLocation());
    }

    public ItemInteractionResult onInteractWithItem(Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // Screwdriver opens GUI or configures
        if (held.getItem() instanceof ItemScrewdriver) {
            int[] grid = calculateGridPos(hit);
            if (grid != null) {
                PanelElement elem = findRootElement(grid[0], grid[1]);
                if (elem != null && elem.type != ElementType.NONE) {
                    if (level != null && level.isClientSide()) {
                        pl.asie.computronics.client.gui.ControlPanelConfigScreen.open(this, elem.slot);
                    }
                    return ItemInteractionResult.sidedSuccess(level != null && level.isClientSide());
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int[] grid = calculateGridPos(hit);
        if (grid == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int gx = grid[0];
        int gy = grid[1];

        if (held.getItem() instanceof ItemMiniLamp) {
            int size = ItemMiniLamp.getPlacementSize(held);
            if (mountElementWithSize(gx, gy, ElementType.LAMP, 0x20CC40, size, size)) {
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.5f);
                player.displayClientMessage(Component.literal(String.format("§a[Control Panel] §fMounted %dx%d Indicator Lamp at (%d, %d)", size, size, gx, gy)), true);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (held.getItem() instanceof ItemMiniButton) {
            int size = ItemMiniButton.getPlacementSize(held);
            if (mountElementWithSize(gx, gy, ElementType.BUTTON, 0xCC2020, size, size)) {
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);
                player.displayClientMessage(Component.literal(String.format("§e[Control Panel] §fMounted %dx%d Tactile Button at (%d, %d)", size, size, gx, gy)), true);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (held.getItem() instanceof ItemToggleSwitch) {
            int size = ItemToggleSwitch.getPlacementSize(held);
            if (mountElementWithSize(gx, gy, ElementType.SWITCH, 0xCC2020, size, size)) {
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);
                player.displayClientMessage(Component.literal(String.format("§6[Control Panel] §fMounted %dx%d Toggle Switch at (%d, %d)", size, size, gx, gy)), true);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (held.getItem() instanceof ItemSevenSegment) {
            int digits = ItemSevenSegment.getPlacementSize(held); // 1, 2, 3, or 4
            int placeX = Math.max(0, Math.min(gx, GRID_SIZE - digits));
            if (mountElementWithSize(placeX, gy, ElementType.SEVEN_SEGMENT, 0x00FFCC, digits, 1)) {
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.4f);
                player.displayClientMessage(Component.literal(String.format("§b[Control Panel] §fMounted %d-Digit 7-Segment Display (1x%d) at (%d, %d)", digits, digits, placeX, gy)), true);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (held.getItem() instanceof ItemPanelText) {
            int[] dims = ItemPanelText.getPlacementDimensions(held);
            int sx = dims[0];
            int sy = dims[1];
            int placeX = Math.max(0, Math.min(gx, GRID_SIZE - sx));
            int placeY = Math.max(0, Math.min(gy, GRID_SIZE - sy));
            if (mountElementWithSize(placeX, placeY, ElementType.TEXT, 0x00FFCC, sx, sy)) {
                PanelElement elem = findRootElement(placeX, placeY);
                if (elem != null) {
                    elem.label = "TEXT";
                    elem.textColor = 0x00FFCC;
                    elem.bold = true;
                    elem.active = true;
                    markChangedAndSync();
                }
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.3f);
                player.displayClientMessage(Component.literal(String.format("§a[Control Panel] §fMounted %dx%d Text Component at (%d, %d)", sx, sy, placeX, placeY)), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public boolean mountElementWithSize(int gx, int gy, ElementType type, int color, int sizeX, int sizeY) {
        if (gx < 0 || gx + sizeX > GRID_SIZE || gy < 0 || gy + sizeY > GRID_SIZE) return false;
        // Verify entire footprint is completely free
        for (int x = gx; x < gx + sizeX; x++) {
            for (int y = gy; y < gy + sizeY; y++) {
                if (findRootElement(x, y) != null) return false;
            }
        }
        int rootSlot = getSlotIndex(gx, gy);
        elements[rootSlot].type = type;
        elements[rootSlot].color = color;
        elements[rootSlot].sizeX = sizeX;
        elements[rootSlot].sizeY = sizeY;
        elements[rootSlot].size = Math.max(sizeX, sizeY);
        elements[rootSlot].active = (type == ElementType.LAMP || type == ElementType.SEVEN_SEGMENT);
        elements[rootSlot].label = "";
        elements[rootSlot].displayValue = (type == ElementType.SEVEN_SEGMENT) ? "0" : "";
        markChangedAndSync();
        return true;
    }

    public boolean mountElementWithSize(int gx, int gy, ElementType type, int color, int size) {
        return mountElementWithSize(gx, gy, type, color, size, size);
    }

    public boolean mountElementAt(int gx, int gy, ElementType type, int color) {
        return mountElementWithSize(gx, gy, type, color, 1, 1);
    }

    public void configureElement(int slot, int color, int sizeX, int sizeY, String label, int textColor, boolean bold, String displayValue) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return;
        PanelElement elem = elements[slot];
        if (elem.type == ElementType.NONE) return;
        elem.color = color;
        elem.label = label != null ? label : "";
        elem.textColor = textColor;
        elem.bold = bold;
        if (displayValue != null && !displayValue.isEmpty()) {
            elem.displayValue = displayValue;
        }

        // Handle resizing if valid
        if (sizeX >= 1 && sizeX <= 8 && sizeY >= 1 && sizeY <= 8) {
            resizeElement(elem, sizeX, sizeY);
        }
        markChangedAndSync();
    }

    public void configureElement(int slot, int color, int size, String label, int textColor, boolean bold, String displayValue) {
        configureElement(slot, color, size, size, label, textColor, bold, displayValue);
    }

    public void removeElementAndDrop(int slot, Player player) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return;
        PanelElement elem = elements[slot];
        if (elem.type == ElementType.NONE) return;
        ElementType oldType = elem.type;
        elem.type = ElementType.NONE;
        elem.active = false;
        elem.size = 1;
        elem.sizeX = 1;
        elem.sizeY = 1;
        elem.label = "";
        elem.displayValue = "0";
        markChangedAndSync();

        ItemStack drop = switch (oldType) {
            case BUTTON -> new ItemStack(ModItems.MINI_BUTTON.get());
            case LAMP -> new ItemStack(ModItems.MINI_LAMP.get());
            case SWITCH -> new ItemStack(ModItems.TOGGLE_SWITCH.get());
            case SEVEN_SEGMENT -> new ItemStack(ModItems.SEVEN_SEGMENT.get());
            case TEXT -> new ItemStack(ModItems.PANEL_TEXT.get());
            default -> ItemStack.EMPTY;
        };
        if (!drop.isEmpty() && player != null) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
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
                // Shift-Click: Cycle size
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
            } else if (elem != null && elem.type == ElementType.SWITCH) {
                elem.active = !elem.active;
                markChangedAndSync();
                float pitch = elem.active ? 1.3f : 0.95f;
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.9f, pitch);
                }
                if (node != null) {
                    int rx = elem.slot % GRID_SIZE;
                    int ry = elem.slot / GRID_SIZE;
                    node.sendToReachable("computer.signal", "panel_switch", elem.slot + 1, elem.active, player.getName().getString(), rx, ry);
                }
                player.displayClientMessage(Component.literal(String.format("§6[Control Panel] §fToggled Switch #%d -> %s", elem.slot + 1, elem.active ? "§a[ON]" : "§c[OFF]")), true);
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

    public void clear() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            elements[i].type = ElementType.NONE;
            elements[i].active = false;
            elements[i].size = 1;
            elements[i].sizeX = 1;
            elements[i].sizeY = 1;
            elements[i].label = "";
            elements[i].displayValue = "0";
        }
        markChangedAndSync();
    }

    // ========================================================================
    // OpenComputers Callbacks
    // ========================================================================

    @Callback(doc = "function():boolean -- Clears all elements from the panel.")
    public Object[] clear(Context context, Arguments args) {
        clear();
        return new Object[]{true};
    }

    @Callback(doc = "function(slot:number):boolean -- Gets switch toggle state (true=ON, false=OFF).")
    public Object[] getSwitch(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{false, "invalid slot"};
        PanelElement elem = elements[slot];
        if (elem.type != ElementType.SWITCH) return new Object[]{false, "element is not a switch"};
        return new Object[]{elem.active};
    }

    @Callback(doc = "function(slot:number, state:boolean):boolean -- Sets switch toggle state.")
    public Object[] setSwitch(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{false, "invalid slot"};
        PanelElement elem = elements[slot];
        if (elem.type != ElementType.SWITCH) return new Object[]{false, "element is not a switch"};
        elem.active = args.checkBoolean(1);
        markChangedAndSync();
        return new Object[]{true};
    }

    @Callback(doc = "function(x:number, y:number, state:boolean[, color:number]):boolean -- Toggles miniature lamp on/off with optional RGB color.")
    public Object[] setLamp(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        boolean state = args.checkBoolean(2);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null && elem.type == ElementType.LAMP) {
            elem.active = state;
            if (args.count() >= 4) {
                elem.color = args.checkInteger(3) & 0xFFFFFF;
            }
            markChangedAndSync();
            return new Object[]{true};
        }
        return new Object[]{false, "no lamp at coordinates"};
    }

    @Callback(doc = "function(x:number, y:number, color:number):boolean -- Sets lamp color by grid (x, y).")
    public Object[] setLampColor(Context context, Arguments args) {
        int gx = args.checkInteger(0);
        int gy = args.checkInteger(1);
        int color = args.checkInteger(2);
        PanelElement elem = findRootElement(gx, gy);
        if (elem != null && elem.type == ElementType.LAMP) {
            elem.color = color & 0xFFFFFF;
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
                map.put("state", elements[i].active);
                map.put("color", elements[i].color);
                map.put("size", elements[i].size);
                map.put("sizeX", elements[i].sizeX);
                map.put("sizeY", elements[i].sizeY);
                map.put("label", elements[i].label);
                map.put("pressed", elements[i].pressTicks > 0);
                map.put("displayValue", elements[i].displayValue);
                map.put("textScale", elements[i].textScale);
                result.add(map);
            }
        }
        return new Object[]{result};
    }

    @Callback(doc = "function(slot:number, value:any):boolean -- Sets the 7-segment display value or number.")
    public Object[] setSegment(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{false, "invalid slot"};
        PanelElement elem = elements[slot];
        if (elem.type != ElementType.SEVEN_SEGMENT) return new Object[]{false, "element is not a 7-segment display"};

        if (args.isBoolean(1)) {
            elem.active = args.checkBoolean(1);
        } else if (args.isDouble(1) || args.isInteger(1)) {
            elem.displayValue = String.valueOf(args.checkInteger(1));
            elem.active = true;
        } else {
            elem.displayValue = args.checkString(1);
            elem.active = true;
        }
        markChangedAndSync();
        return new Object[]{true};
    }

    @Callback(doc = "function(slot:number):string -- Gets the current 7-segment display value.")
    public Object[] getSegment(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{null, "invalid slot"};
        PanelElement elem = elements[slot];
        return new Object[]{elem.displayValue != null ? elem.displayValue : ""};
    }

    @Callback(doc = "function(slot:number, color:number):boolean -- Sets the color of any element.")
    public Object[] setColor(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{false, "invalid slot"};
        PanelElement elem = elements[slot];
        if (elem.type == ElementType.NONE) return new Object[]{false, "empty slot"};
        elem.color = args.checkInteger(1);
        markChangedAndSync();
        return new Object[]{true};
    }

    @Callback(doc = "function(slot:number, text:string):boolean -- Sets the text of a text component or button.")
    public Object[] setText(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{false, "invalid slot"};
        PanelElement elem = elements[slot];
        if (elem.type != ElementType.TEXT && elem.type != ElementType.BUTTON && elem.type != ElementType.LAMP) {
            return new Object[]{false, "element does not support text"};
        }
        elem.label = args.checkString(1);
        markChangedAndSync();
        return new Object[]{true};
    }

    @Callback(doc = "function(slot:number):string -- Gets the text of a text component or button.")
    public Object[] getText(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{null, "invalid slot"};
        PanelElement elem = elements[slot];
        return new Object[]{elem.label != null ? elem.label : ""};
    }

    @Callback(doc = "function(slot:number, color:number):boolean -- Sets the text color (RGB hex).")
    public Object[] setTextColor(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{false, "invalid slot"};
        PanelElement elem = elements[slot];
        elem.textColor = args.checkInteger(1) & 0xFFFFFF;
        markChangedAndSync();
        return new Object[]{true};
    }

    @Callback(doc = "function(slot:number, scale:number):boolean -- Sets the text scale factor (0.2 to 4.0).")
    public Object[] setTextScale(Context context, Arguments args) {
        int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= TOTAL_SLOTS) return new Object[]{false, "invalid slot"};
        PanelElement elem = elements[slot];
        elem.textScale = (float) Math.max(0.2, Math.min(4.0, args.checkDouble(1)));
        markChangedAndSync();
        return new Object[]{true};
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
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
                itemTag.putInt("sizeX", elements[i].sizeX);
                itemTag.putInt("sizeY", elements[i].sizeY);
                itemTag.putString("label", elements[i].label != null ? elements[i].label : "");
                itemTag.putInt("textColor", elements[i].textColor);
                itemTag.putBoolean("bold", elements[i].bold);
                itemTag.putString("displayValue", elements[i].displayValue != null ? elements[i].displayValue : "0");
                itemTag.putFloat("textScale", elements[i].textScale);
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
            elements[i].sizeX = 1;
            elements[i].sizeY = 1;
            elements[i].label = "";
            elements[i].textColor = 0xFFFFFF;
            elements[i].bold = true;
            elements[i].displayValue = "0";
        }
        if (tag.contains("elements")) {
            ListTag list = tag.getList("elements", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getInt("slot");
                if (slot >= 0 && slot < TOTAL_SLOTS) {
                    elements[slot].slot = slot;
                    try {
                        elements[slot].type = ElementType.valueOf(itemTag.getString("type"));
                    } catch (Exception e) {
                        elements[slot].type = ElementType.NONE;
                    }
                    elements[slot].color = itemTag.getInt("color");
                    elements[slot].active = itemTag.getBoolean("active");
                    int s = itemTag.contains("size") ? Math.max(1, Math.min(4, itemTag.getInt("size"))) : 1;
                    elements[slot].size = s;
                    elements[slot].sizeX = itemTag.contains("sizeX") ? Math.max(1, Math.min(4, itemTag.getInt("sizeX"))) : s;
                    elements[slot].sizeY = itemTag.contains("sizeY") ? Math.max(1, Math.min(4, itemTag.getInt("sizeY"))) : (elements[slot].type == ElementType.SEVEN_SEGMENT ? 1 : s);
                    elements[slot].label = itemTag.contains("label") ? itemTag.getString("label") : "";
                    elements[slot].textColor = itemTag.contains("textColor") ? itemTag.getInt("textColor") : 0xFFFFFF;
                    elements[slot].bold = !itemTag.contains("bold") || itemTag.getBoolean("bold");
                    elements[slot].displayValue = itemTag.contains("displayValue") ? itemTag.getString("displayValue") : "0";
                    elements[slot].textScale = itemTag.contains("textScale") ? itemTag.getFloat("textScale") : 1.0f;
                }
            }
        }
    }
}
