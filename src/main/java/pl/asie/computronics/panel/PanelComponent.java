package pl.asie.computronics.panel;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * One mounted component. {@link #id} is the 1-based index of its top-left cell
 * ({@code y * GRID + x + 1}), which is stable for the component's lifetime and is what Lua uses.
 */
public final class PanelComponent {
    public static final int MAX_LABEL = 64;
    public static final int MAX_VALUE = 16;

    public final PanelComponentType type;
    public final int x;
    public final int y;
    public int width;
    public int height;
    public int color;
    public String label = "";
    /** lamp lit / switch on */
    public boolean state;
    /** seven-segment text */
    public String value = "";
    /** game time until which a button is drawn pressed */
    public long pressedUntil;

    public PanelComponent(PanelComponentType type, int x, int y, int width, int height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = type.defaultColor;
        if (type == PanelComponentType.SEGMENT) value = "0";
    }

    public int id() {
        return y * PanelLayout.GRID + x + 1;
    }

    public boolean contains(int cx, int cy) {
        return cx >= x && cy >= y && cx < x + width && cy < y + height;
    }

    public int digits() {
        return Math.max(1, width / 2);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("type", type.id());
        t.putByte("x", (byte) x);
        t.putByte("y", (byte) y);
        t.putByte("w", (byte) width);
        t.putByte("h", (byte) height);
        t.putInt("color", color);
        if (!label.isEmpty()) t.putString("label", label);
        if (state) t.putBoolean("state", true);
        if (!value.isEmpty()) t.putString("value", value);
        if (pressedUntil != 0) t.putLong("pressed", pressedUntil);
        return t;
    }

    public static PanelComponent load(CompoundTag t) {
        PanelComponentType type = PanelComponentType.byName(t.getString("type"));
        if (type == null) return null;
        PanelComponent c = new PanelComponent(type, t.getByte("x"), t.getByte("y"),
                Math.max(1, t.getByte("w")), Math.max(1, t.getByte("h")));
        c.color = t.getInt("color") & 0xFFFFFF;
        c.label = clip(t.getString("label"), MAX_LABEL);
        c.state = t.getBoolean("state");
        c.value = clip(t.getString("value"), MAX_VALUE);
        c.pressedUntil = t.getLong("pressed");
        return c;
    }

    /** Lua view of this component. */
    public Map<String, Object> describe(long gameTime) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id());
        m.put("type", type.id());
        m.put("x", x + 1);
        m.put("y", y + 1);
        m.put("width", width);
        m.put("height", height);
        m.put("color", color);
        if (type.hasLabel()) m.put("label", label);
        if (type.hasState()) m.put("state", state);
        if (type == PanelComponentType.BUTTON) m.put("pressed", gameTime < pressedUntil);
        if (type == PanelComponentType.SEGMENT) {
            m.put("value", value);
            m.put("digits", digits());
        }
        return m;
    }

    static String clip(String s, int max) {
        if (s == null) return "";
        // strip formatting codes so labels can't fake colored chat-like text
        s = s.replace("§", "");
        return s.length() > max ? s.substring(0, max) : s;
    }
}
