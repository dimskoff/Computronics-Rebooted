package pl.asie.computronics.panel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The grid of components on one control panel. Pure data + rules, no world access, so it can be
 * unit tested and shared by server logic, the renderer and the config screen.
 * <p>
 * All methods are synchronized: OpenComputers "direct" callbacks read it from the OC executor thread.
 */
public final class PanelLayout {
    public static final int GRID = 8;

    private final List<PanelComponent> components = new ArrayList<>();

    public synchronized List<PanelComponent> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(components));
    }

    public synchronized boolean isEmpty() {
        return components.isEmpty();
    }

    public synchronized PanelComponent at(int cx, int cy) {
        for (PanelComponent c : components) {
            if (c.contains(cx, cy)) return c;
        }
        return null;
    }

    public synchronized PanelComponent byId(int id) {
        for (PanelComponent c : components) {
            if (c.id() == id) return c;
        }
        return null;
    }

    /** True if a w*h rectangle at (x,y) is inside the grid and free (ignoring {@code except}). */
    public synchronized boolean fits(int x, int y, int w, int h, PanelComponent except) {
        if (w < 1 || h < 1 || x < 0 || y < 0 || x + w > GRID || y + h > GRID) return false;
        for (PanelComponent c : components) {
            if (c == except) continue;
            if (x < c.x + c.width && c.x < x + w && y < c.y + c.height && c.y < y + h) return false;
        }
        return true;
    }

    /**
     * Places a component with its top-left at the clicked cell, shifting it left/up if it would
     * stick out of the grid. Returns null if there's no room.
     */
    public synchronized PanelComponent place(PanelComponentType type, int cx, int cy, int w, int h) {
        w = Math.min(w, Math.min(type.maxWidth, GRID));
        h = Math.min(h, Math.min(type.maxHeight, GRID));
        int x = Math.max(0, Math.min(cx, GRID - w));
        int y = Math.max(0, Math.min(cy, GRID - h));
        if (!fits(x, y, w, h, null)) return null;
        PanelComponent c = new PanelComponent(type, x, y, w, h);
        components.add(c);
        return c;
    }

    public synchronized boolean remove(PanelComponent c) {
        return components.remove(c);
    }

    public synchronized boolean resize(PanelComponent c, int w, int h) {
        if (w > c.type.maxWidth || h > c.type.maxHeight) return false;
        if (!fits(c.x, c.y, w, h, c)) return false;
        c.width = w;
        c.height = h;
        return true;
    }

    public synchronized List<PanelComponent> clear() {
        List<PanelComponent> removed = new ArrayList<>(components);
        components.clear();
        return removed;
    }

    // ------------------------------------------------------------------ NBT

    public synchronized ListTag save() {
        ListTag list = new ListTag();
        for (PanelComponent c : components) list.add(c.save());
        return list;
    }

    public synchronized void load(ListTag list) {
        components.clear();
        for (int i = 0; i < list.size(); i++) {
            PanelComponent c = PanelComponent.load(list.getCompound(i));
            if (c != null && fits(c.x, c.y, c.width, c.height, null)) components.add(c);
        }
    }

    /** Reads the 2.0.0-beta.1 "elements" format (slot based) so old worlds keep their panels. */
    public synchronized void loadLegacy(ListTag list) {
        components.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            PanelComponentType type = PanelComponentType.byName(t.getString("type"));
            if (type == null) continue;
            int slot = t.getInt("slot");
            int x = slot % GRID, y = slot / GRID;
            int w = Math.max(1, t.contains("sizeX") ? t.getInt("sizeX") : t.getInt("size"));
            int h = Math.max(1, t.contains("sizeY") ? t.getInt("sizeY") : t.getInt("size"));
            if (type == PanelComponentType.SEGMENT) {
                w = Math.min(8, w * 2);
                h = 3;
            }
            w = Math.min(w, type.maxWidth);
            h = Math.min(h, type.maxHeight);
            if (!fits(x, y, w, h, null)) continue;
            PanelComponent c = new PanelComponent(type, x, y, w, h);
            c.color = t.getInt("color") & 0xFFFFFF;
            c.label = PanelComponent.clip(t.getString("label"), PanelComponent.MAX_LABEL);
            c.state = t.getBoolean("active");
            if (t.contains("displayValue", Tag.TAG_STRING)) {
                c.value = PanelComponent.clip(t.getString("displayValue"), PanelComponent.MAX_VALUE);
            }
            components.add(c);
        }
    }
}
