package pl.asie.computronics.panel;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps between block space and panel space.
 * <p>
 * Panel space: {@code u} goes right and {@code v} goes down as seen by a player looking at the
 * panel, {@code w} points out of the front face towards that player. (u, v) in [0, 1] covers the
 * whole front face, {@code w = 0} is the front face and {@code w = -THICKNESS} the back face.
 * <p>
 * Like a slab, the plate can sit anywhere along its mounting axis ({@link #OFFSETS} positions,
 * 0 = against the block it was placed on, max = the opposite side), and it can be tilted so the far
 * edge rises ({@link #TILTS}). Rendering, clicking and the collision shape all derive from here.
 */
public final class PanelOrientation {
    public static final float THICKNESS = 3 / 16f;
    public static final float MARGIN = 1 / 16f;
    public static final float CELL = (1 - 2 * MARGIN) / PanelLayout.GRID;
    public static final int OFFSETS = 4;
    public static final float[] TILTS = {0f, 22.5f, 45f};

    public final Vec3 origin, u, v, w;
    /** centre of the plate (between front and back face) */
    public final Vec3 center;

    private PanelOrientation(Vec3 origin, Vec3 u, Vec3 v, Vec3 w, Vec3 center) {
        this.origin = origin;
        this.u = u;
        this.v = v;
        this.w = w;
        this.center = center;
    }

    public static PanelOrientation of(BlockState state, int offset, int tilt) {
        AttachFace face = state.hasProperty(BlockStateProperties.ATTACH_FACE)
                ? state.getValue(BlockStateProperties.ATTACH_FACE) : AttachFace.FLOOR;
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        return of(face, facing, offset, tilt);
    }

    public static PanelOrientation of(AttachFace face, Direction facing) {
        return of(face, facing, 0, 0);
    }

    /**
     * @param facing for walls: the direction the front faces; for floor/ceiling: towards the player who placed it
     * @param offset 0..OFFSETS-1, 0 = against the mounting surface
     * @param tilt   index into {@link #TILTS}
     */
    public static PanelOrientation of(AttachFace face, Direction facing, int offset, int tilt) {
        Vec3 right = vec(facing.getCounterClockWise());
        Vec3 u, v, n;
        switch (face) {
            case FLOOR -> {
                n = new Vec3(0, 1, 0);
                u = right;
                v = vec(facing);
            }
            case CEILING -> {
                n = new Vec3(0, -1, 0);
                u = right;
                v = vec(facing.getOpposite());
            }
            default -> {
                n = vec(facing);
                u = right;
                v = new Vec3(0, -1, 0);
            }
        }
        // tilt around u: the far (v = 0) edge lifts towards the viewer
        double a = Math.toRadians(TILTS[Math.max(0, Math.min(TILTS.length - 1, tilt))]);
        Vec3 vt = v.scale(Math.cos(a)).subtract(n.scale(Math.sin(a)));
        Vec3 wt = v.scale(Math.sin(a)).add(n.scale(Math.cos(a)));

        // how far the plate reaches along the mounting normal, so it always stays inside the block
        double half = (Math.sin(a) + Math.cos(a) * THICKNESS) / 2;
        double lo = -0.5 + half, hi = 0.5 - half;
        int o = Math.max(0, Math.min(OFFSETS - 1, offset));
        double c = lo + (hi - lo) * o / (OFFSETS - 1);

        Vec3 center = new Vec3(0.5, 0.5, 0.5).add(n.scale(c));
        Vec3 front = center.add(wt.scale(THICKNESS / 2));
        Vec3 origin = front.subtract(u.scale(0.5)).subtract(vt.scale(0.5));
        return new PanelOrientation(origin, u, vt, wt, center);
    }

    /** Block-local point -> panel (u, v, w). */
    public double[] toPanel(Vec3 local) {
        Vec3 d = local.subtract(origin);
        return new double[]{d.dot(u), d.dot(v), d.dot(w)};
    }

    public Vec3 toBlock(double pu, double pv, double pw) {
        return origin.add(u.scale(pu)).add(v.scale(pv)).add(w.scale(pw));
    }

    /** Block-local point on (or near) the front face -> grid cell {x, y}, or null if outside. */
    public int[] toCell(Vec3 local) {
        double[] p = toPanel(local);
        return cellOf(p[0], p[1]);
    }

    public static int[] cellOf(double pu, double pv) {
        double gx = (pu - MARGIN) / CELL;
        double gy = (pv - MARGIN) / CELL;
        if (gx < 0 || gy < 0 || gx >= PanelLayout.GRID || gy >= PanelLayout.GRID) return null;
        return new int[]{(int) gx, (int) gy};
    }

    /**
     * Intersects a ray (block-local) with the front ({@code back = false}) or back face and returns
     * panel (u, v), or null if the ray misses that side of the plate.
     */
    public double[] raycast(Vec3 localEye, Vec3 dir, boolean back) {
        double planeW = back ? -THICKNESS : 0;
        double[] e = toPanel(localEye);
        double dw = dir.dot(w);
        // the viewer must be on the side of the face they are clicking
        if (back ? (e[2] > planeW || dw <= 1e-6) : (e[2] < planeW || dw >= -1e-6)) return null;
        double t = (planeW - e[2]) / dw;
        if (t < 0 || t > 8) return null;
        double pu = e[0] + dir.dot(u) * t, pv = e[1] + dir.dot(v) * t;
        if (pu < 0 || pv < 0 || pu > 1 || pv > 1) return null;
        return new double[]{pu, pv};
    }

    /** Transform from panel space to block-local space, for renderers. */
    public Matrix4f matrix() {
        return new Matrix4f(
                (float) u.x, (float) u.y, (float) u.z, 0,
                (float) v.x, (float) v.y, (float) v.z, 0,
                (float) w.x, (float) w.y, (float) w.z, 0,
                (float) origin.x, (float) origin.y, (float) origin.z, 1);
    }

    // ------------------------------------------------------------------ shapes

    private static final Map<Long, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    /** Collision/selection shape: the plate sliced into boxes (exact when not tilted). */
    public static VoxelShape shape(AttachFace face, Direction facing, int offset, int tilt) {
        long key = ((long) face.ordinal() << 16) | ((long) facing.ordinal() << 8) | ((long) offset << 4) | tilt;
        return SHAPES.computeIfAbsent(key, k -> {
            PanelOrientation o = of(face, facing, offset, tilt);
            int slices = tilt == 0 ? 1 : 6;
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < slices; i++) {
                double v0 = (double) i / slices, v1 = (double) (i + 1) / slices;
                AABB box = null;
                for (double pu : new double[]{0, 1}) for (double pv : new double[]{v0, v1}) for (double pw : new double[]{0, -THICKNESS}) {
                    Vec3 p = o.toBlock(pu, pv, pw);
                    AABB pt = new AABB(p, p);
                    box = box == null ? pt : box.minmax(pt);
                }
                box = clamp(box);
                shape = Shapes.joinUnoptimized(shape, Shapes.create(box), BooleanOp.OR);
            }
            return shape.optimize();
        });
    }

    private static AABB clamp(AABB b) {
        return new AABB(Math.max(0, b.minX), Math.max(0, b.minY), Math.max(0, b.minZ),
                Math.min(1, Math.max(b.maxX, b.minX + 1e-3)), Math.min(1, Math.max(b.maxY, b.minY + 1e-3)),
                Math.min(1, Math.max(b.maxZ, b.minZ + 1e-3)));
    }

    private static Vec3 vec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }
}
