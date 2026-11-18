package pl.asie.computronics.panel;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Maps between block space and panel space.
 * <p>
 * Panel space: {@code u} goes right and {@code v} goes down as seen by a player looking at the
 * panel, {@code w} points out of the front face towards that player. (u, v) in [0, 1] covers the
 * whole front face; {@code w = 0} is the front face itself. Rendering and click hit-testing both use
 * this single definition, so what you see is exactly what you click.
 */
public final class PanelOrientation {
    /** Plate thickness in blocks (4 px). */
    public static final float THICKNESS = 4 / 16f;
    /** Border around the usable grid, in panel units. */
    public static final float MARGIN = 1 / 16f;
    public static final float CELL = (1 - 2 * MARGIN) / PanelLayout.GRID;

    public final Vec3 origin, u, v, w;

    private PanelOrientation(Vec3 origin, Vec3 u, Vec3 v, Vec3 w) {
        this.origin = origin;
        this.u = u;
        this.v = v;
        this.w = w;
    }

    public static PanelOrientation of(BlockState state) {
        AttachFace face = state.hasProperty(BlockStateProperties.ATTACH_FACE)
                ? state.getValue(BlockStateProperties.ATTACH_FACE) : AttachFace.FLOOR;
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        return of(face, facing);
    }

    /**
     * @param facing for walls: the direction the front faces; for floor/ceiling: towards the player who placed it
     */
    public static PanelOrientation of(AttachFace face, Direction facing) {
        Vec3 right = vec(facing.getCounterClockWise());
        Vec3 u, v, w;
        switch (face) {
            case FLOOR -> {
                w = new Vec3(0, 1, 0);
                u = right;
                v = vec(facing);
            }
            case CEILING -> {
                w = new Vec3(0, -1, 0);
                u = right;
                v = vec(facing.getOpposite());
            }
            default -> {
                w = vec(facing);
                u = right;
                v = new Vec3(0, -1, 0);
            }
        }
        Vec3 center = new Vec3(0.5, 0.5, 0.5);
        Vec3 frontCenter = center.add(w.scale(-0.5 + THICKNESS));
        Vec3 origin = frontCenter.subtract(u.scale(0.5)).subtract(v.scale(0.5));
        return new PanelOrientation(origin, u, v, w);
    }

    /** Block-local (0..1) hit location -> panel (u, v). */
    public double[] toPanel(Vec3 local) {
        Vec3 d = local.subtract(origin);
        return new double[]{d.dot(u), d.dot(v)};
    }

    /** Block-local hit location -> grid cell {x, y}, or null if outside the usable area. */
    public int[] toCell(Vec3 local) {
        double[] p = toPanel(local);
        double gx = (p[0] - MARGIN) / CELL;
        double gy = (p[1] - MARGIN) / CELL;
        if (gx < 0 || gy < 0 || gx >= PanelLayout.GRID || gy >= PanelLayout.GRID) return null;
        return new int[]{(int) gx, (int) gy};
    }

    /** Transform from panel space to block-local space, for renderers. */
    public Matrix4f matrix() {
        return new Matrix4f(
                (float) u.x, (float) u.y, (float) u.z, 0,
                (float) v.x, (float) v.y, (float) v.z, 0,
                (float) w.x, (float) w.y, (float) w.z, 0,
                (float) origin.x, (float) origin.y, (float) origin.z, 1);
    }

    private static Vec3 vec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }
}
