package pl.asie.computronics.panel;

import net.minecraft.world.item.Item;
import pl.asie.computronics.init.ModItems;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Kinds of components that can be mounted on a control panel.
 * Sizes are in grid cells (the panel is {@link PanelLayout#GRID} x {@link PanelLayout#GRID}).
 */
public enum PanelComponentType {
    /** Momentary push button, fires {@code panel_button}. */
    BUTTON(0xCC2020, 4, 4, true, () -> ModItems.MINI_BUTTON.get()),
    /** Push button that stays pressed while the player holds right-click, fires {@code panel_hold}. */
    HOLD_BUTTON(0xE08020, 4, 4, true, () -> ModItems.HOLD_BUTTON.get()),
    /** Toggle switch, fires {@code panel_switch}. */
    SWITCH(0xD0D0D0, 4, 4, true, () -> ModItems.TOGGLE_SWITCH.get()),
    /** Indicator lamp driven by the computer. */
    LAMP(0x20CC40, 4, 4, true, () -> ModItems.MINI_LAMP.get()),
    /** Seven segment display, width = digit count, 2 cells tall. */
    SEGMENT(0xFF3020, 8, 4, false, () -> ModItems.SEVEN_SEGMENT.get()),
    /** Free text label. */
    LABEL(0xFFFFFF, 8, 8, false, () -> ModItems.PANEL_TEXT.get());

    public final int defaultColor;
    public final int maxWidth;
    public final int maxHeight;
    /** square components resize uniformly */
    public final boolean square;
    private final Supplier<Item> item;

    PanelComponentType(int defaultColor, int maxWidth, int maxHeight, boolean square, Supplier<Item> item) {
        this.defaultColor = defaultColor;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.square = square;
        this.item = item;
    }

    public Item item() {
        return item.get();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "panel.computronics.component." + id();
    }

    public boolean hasLabel() {
        return this != SEGMENT;
    }

    /** Has a signal output on the back of the panel (wire to a connector input). */
    public boolean hasOutputPort() {
        return this == BUTTON || this == HOLD_BUTTON || this == SWITCH;
    }

    /** Has a signal input on the back of the panel (wire from a connector output). */
    public boolean hasInputPort() {
        return this == LAMP || this == SEGMENT;
    }

    public boolean isButton() {
        return this == BUTTON || this == HOLD_BUTTON;
    }

    public boolean hasState() {
        return this == SWITCH || this == LAMP;
    }

    /** Default footprint for a placement "size" chosen on the item (1..4). */
    public int[] footprint(int size) {
        size = Math.max(1, Math.min(4, size));
        return switch (this) {
            case SEGMENT -> new int[]{size * 2, 3};      // size = digit count, 2x3 cells per digit
            case LABEL -> new int[]{Math.min(8, size * 2), size}; // 2:1 text plate
            default -> new int[]{size, size};
        };
    }

    public static PanelComponentType byName(String name) {
        if (name == null) return null;
        String n = name.toUpperCase(Locale.ROOT);
        // legacy names from 2.0.0-beta.1
        if (n.equals("SEVEN_SEGMENT")) return SEGMENT;
        if (n.equals("TEXT")) return LABEL;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
