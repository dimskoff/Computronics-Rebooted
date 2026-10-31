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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.item.ItemMiniButton;
import pl.asie.computronics.item.ItemMiniLamp;

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
        public ElementType type = ElementType.NONE;
        public int color = 0x00FF00; // default green
        public boolean active = false;
        public int pressTicks = 0;
    }

    private final PanelElement[] elements = new PanelElement[TOTAL_SLOTS];

    public TileControlPanel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROL_PANEL.get(), pos, state);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            elements[i] = new PanelElement();
        }
        node = Network.newNode(this, Visibility.Network)
                .withComponent("control_panel", Visibility.Network)
                .create();
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (elements[i].pressTicks > 0) {
                elements[i].pressTicks--;
                if (elements[i].pressTicks == 0) {
                    setChanged();
                }
            }
        }
    }

    private int getSlotIndex(int x, int y) {
        x = Math.max(0, Math.min(x, GRID_SIZE - 1));
        y = Math.max(0, Math.min(y, GRID_SIZE - 1));
        return y * GRID_SIZE + x;
    }

    private int parseSlot(Arguments args) {
        if (args.count() >= 2 && args.isInteger(1)) {
            int x = args.checkInteger(0);
            int y = args.checkInteger(1);
            return getSlotIndex(x, y);
        }
        int index = args.checkInteger(0);
        return Math.floorMod(index - 1, TOTAL_SLOTS);
    }

    private int[] calculateGridPos(BlockHitResult hit) {
        Direction facing = getFacing();
        if (hit.getDirection() != facing) {
            return null; // clicked a side other than the front panel
        }

        Vec3 loc = hit.getLocation();
        double lx = loc.x - worldPosition.getX();
        double ly = loc.y - worldPosition.getY();
        double lz = loc.z - worldPosition.getZ();

        double u = 0.0;
        double v = Math.max(0.0, Math.min(0.999, ly));

        switch (facing) {
            case NORTH -> u = Math.max(0.0, Math.min(0.999, 1.0 - lx));
            case SOUTH -> u = Math.max(0.0, Math.min(0.999, lx));
            case WEST  -> u = Math.max(0.0, Math.min(0.999, lz));
            case EAST  -> u = Math.max(0.0, Math.min(0.999, 1.0 - lz));
            default    -> u = 0.5;
        }

        int gx = (int) (u * GRID_SIZE);
        int gy = (int) (v * GRID_SIZE);
        return new int[]{gx, gy};
    }

    public ItemInteractionResult onInteractWithItem(Player player, InteractionHand hand, BlockHitResult hit) {
        int[] grid = calculateGridPos(hit);
        if (grid == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int gx = grid[0];
        int gy = grid[1];
        int slot = getSlotIndex(gx, gy);
        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() instanceof ItemMiniLamp) {
            if (elements[slot].type == ElementType.NONE) {
                elements[slot].type = ElementType.LAMP;
                elements[slot].color = 0x00FF00;
                elements[slot].active = true;
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.5f);
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                player.displayClientMessage(Component.literal(String.format("§a[Control Panel] §fInstalled Mini Lamp at (%d, %d) [Slot #%d]", gx, gy, slot + 1)), true);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (held.getItem() instanceof ItemMiniButton) {
            if (elements[slot].type == ElementType.NONE) {
                elements[slot].type = ElementType.BUTTON;
                elements[slot].active = false;
                if (!player.isCreative()) held.shrink(1);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                player.displayClientMessage(Component.literal(String.format("§e[Control Panel] §fInstalled Mini Button at (%d, %d) [Slot #%d]", gx, gy, slot + 1)), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public InteractionResult onInteractEmptyHand(Player player, BlockHitResult hit) {
        int[] grid = calculateGridPos(hit);
        if (grid == null) {
            return InteractionResult.PASS;
        }

        int gx = grid[0];
        int gy = grid[1];
        int slot = getSlotIndex(gx, gy);
        PanelElement elem = elements[slot];

        if (player.isShiftKeyDown()) {
            // Shift-Click: Remove element and drop item
            if (elem.type == ElementType.LAMP) {
                elem.type = ElementType.NONE;
                elem.active = false;
                if (level != null) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(ModItems.MINI_LAMP.get()));
                    level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                setChanged();
                player.displayClientMessage(Component.literal(String.format("§c[Control Panel] §fDismantled Mini Lamp at (%d, %d)", gx, gy)), true);
                return InteractionResult.SUCCESS;
            } else if (elem.type == ElementType.BUTTON) {
                elem.type = ElementType.NONE;
                elem.active = false;
                if (level != null) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(ModItems.MINI_BUTTON.get()));
                    level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                setChanged();
                player.displayClientMessage(Component.literal(String.format("§c[Control Panel] §fDismantled Mini Button at (%d, %d)", gx, gy)), true);
                return InteractionResult.SUCCESS;
            }
        } else {
            // Normal click
            if (elem.type == ElementType.BUTTON) {
                elem.pressTicks = 10;
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.8f, 1.4f);
                }
                if (node != null) {
                    node.sendToReachable("computer.signal", "panel_button", slot + 1, player.getName().getString(), gx, gy);
                }
                setChanged();
                player.displayClientMessage(Component.literal(String.format("§6[Control Panel] §fPressed Button at (%d, %d) [Slot #%d]", gx, gy, slot + 1)), true);
                return InteractionResult.SUCCESS;
            } else if (elem.type == ElementType.LAMP) {
                String colorHex = String.format("#%06X", elem.color);
                player.displayClientMessage(Component.literal(String.format("§b[Control Panel] §fMini Lamp (%d, %d): %s (Color: %s)", gx, gy, elem.active ? "ON" : "OFF", colorHex)), true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    // =========================================================================
    // OpenComputers Callbacks
    // =========================================================================

    @Callback(direct = true, doc = "function():number -- Gets the grid dimension (8 for 8x8).")
    public Object[] getGridSize(Context context, Arguments args) {
        return new Object[]{GRID_SIZE};
    }

    @Callback(doc = "function(indexOrX:number[, y:number], color:number):boolean -- Sets miniature lamp RGB color.")
    public Object[] setLampColor(Context context, Arguments args) {
        int slot;
        int color;
        if (args.count() >= 3) {
            slot = getSlotIndex(args.checkInteger(0), args.checkInteger(1));
            color = args.checkInteger(2);
        } else {
            slot = Math.floorMod(args.checkInteger(0) - 1, TOTAL_SLOTS);
            color = args.checkInteger(1);
        }
        elements[slot].color = color & 0xFFFFFF;
        elements[slot].active = (color != 0);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return new Object[]{true};
    }

    @Callback(doc = "function(indexOrX:number[, y:number], state:boolean):boolean -- Toggles miniature lamp on/off.")
    public Object[] setLamp(Context context, Arguments args) {
        int slot;
        boolean state;
        if (args.count() >= 3) {
            slot = getSlotIndex(args.checkInteger(0), args.checkInteger(1));
            state = args.checkBoolean(2);
        } else {
            slot = Math.floorMod(args.checkInteger(0) - 1, TOTAL_SLOTS);
            state = args.checkBoolean(1);
        }
        elements[slot].active = state;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(indexOrX:number[, y:number]):number -- Gets miniature lamp color.")
    public Object[] getLampColor(Context context, Arguments args) {
        int slot = parseSlot(args);
        return new Object[]{elements[slot].color};
    }

    @Callback(direct = true, doc = "function(indexOrX:number[, y:number]):boolean -- Checks if lamp is currently on.")
    public Object[] isLampActive(Context context, Arguments args) {
        int slot = parseSlot(args);
        return new Object[]{elements[slot].active};
    }

    @Callback(doc = "function(indexOrX:number[, y:number]):boolean -- Software button press.")
    public Object[] pressButton(Context context, Arguments args) {
        int slot = parseSlot(args);
        elements[slot].pressTicks = 10;
        int gx = slot % GRID_SIZE;
        int gy = slot / GRID_SIZE;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.8f, 1.4f);
        }
        if (node != null) {
            node.sendToReachable("computer.signal", "panel_button", slot + 1, "SYSTEM", gx, gy);
        }
        setChanged();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(indexOrX:number[, y:number]):boolean -- Checks if button is currently pressed.")
    public Object[] isButtonPressed(Context context, Arguments args) {
        int slot = parseSlot(args);
        return new Object[]{elements[slot].pressTicks > 0};
    }

    @Callback(direct = true, doc = "function():table -- Returns all installed components on the panel.")
    public Object[] getElements(Context context, Arguments args) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (elements[i].type != ElementType.NONE) {
                Map<String, Object> map = new HashMap<>();
                map.put("index", i + 1);
                map.put("x", i % GRID_SIZE);
                map.put("y", i / GRID_SIZE);
                map.put("type", elements[i].type.name().toLowerCase());
                map.put("active", elements[i].active);
                map.put("color", elements[i].color);
                map.put("pressed", elements[i].pressTicks > 0);
                result.add(map);
            }
        }
        return new Object[]{result};
    }

    @Callback(doc = "function():boolean -- Clears and turns off all miniature lamps.")
    public Object[] clear(Context context, Arguments args) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            elements[i].active = false;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return new Object[]{true};
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
        }
        if (tag.contains("elements")) {
            ListTag list = tag.getList("elements", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getInt("slot");
                if (slot >= 0 && slot < TOTAL_SLOTS) {
                    elements[slot].type = ElementType.valueOf(itemTag.getString("type"));
                    elements[slot].color = itemTag.getInt("color");
                    elements[slot].active = itemTag.getBoolean("active");
                }
            }
        }
    }
}
