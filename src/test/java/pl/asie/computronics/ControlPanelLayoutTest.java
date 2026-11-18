package pl.asie.computronics;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;
import pl.asie.computronics.panel.PanelLayout;
import pl.asie.computronics.panel.PanelOrientation;

public class ControlPanelLayoutTest {

    @Test
    public void placementOverlapAndClamp() {
        PanelLayout l = new PanelLayout();
        PanelComponent a = l.place(PanelComponentType.BUTTON, 0, 0, 2, 2);
        Assertions.assertNotNull(a);
        Assertions.assertEquals(1, a.id());
        Assertions.assertNull(l.place(PanelComponentType.LAMP, 1, 1, 1, 1), "overlap must fail");
        // clicked at the right edge: shifted left to fit
        PanelComponent seg = l.place(PanelComponentType.SEGMENT, 7, 5, 4, 3);
        Assertions.assertNotNull(seg);
        Assertions.assertEquals(4, seg.x);
        Assertions.assertEquals(2, seg.digits());
        Assertions.assertSame(seg, l.at(7, 7));
        Assertions.assertTrue(l.remove(a));
        Assertions.assertNull(l.at(0, 0));
    }

    @Test
    public void resizeRespectsNeighbours() {
        PanelLayout l = new PanelLayout();
        PanelComponent a = l.place(PanelComponentType.LAMP, 0, 0, 1, 1);
        l.place(PanelComponentType.LAMP, 2, 0, 1, 1);
        Assertions.assertTrue(l.resize(a, 2, 2));
        Assertions.assertFalse(l.resize(a, 3, 3));
        Assertions.assertFalse(l.resize(a, 5, 5), "beyond type max");
    }

    @Test
    public void nbtRoundTrip() {
        PanelLayout l = new PanelLayout();
        PanelComponent b = l.place(PanelComponentType.BUTTON, 3, 4, 2, 2);
        b.label = "GO\nNOW";
        b.color = 0x123456;
        PanelComponent s = l.place(PanelComponentType.SWITCH, 0, 0, 1, 1);
        s.state = true;

        PanelLayout copy = new PanelLayout();
        copy.load(l.save());
        PanelComponent b2 = copy.byId(b.id());
        Assertions.assertEquals("GO\nNOW", b2.label);
        Assertions.assertEquals(0x123456, b2.color);
        Assertions.assertEquals(2, b2.width);
        Assertions.assertTrue(copy.byId(1).state);
    }

    @Test
    public void legacyFormatMigrates() {
        ListTag old = new ListTag();
        CompoundTag e = new CompoundTag();
        e.putInt("slot", 9);
        e.putString("type", "SEVEN_SEGMENT");
        e.putInt("sizeX", 3);
        e.putInt("sizeY", 1);
        e.putString("displayValue", "42");
        old.add(e);
        PanelLayout l = new PanelLayout();
        l.loadLegacy(old);
        PanelComponent c = l.byId(10);
        Assertions.assertNotNull(c);
        Assertions.assertEquals(PanelComponentType.SEGMENT, c.type);
        Assertions.assertEquals(3, c.digits());
        Assertions.assertEquals("42", c.value);
    }

    @Test
    public void orientationIsReadableAndClickable() {
        for (AttachFace face : AttachFace.values()) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                PanelOrientation o = PanelOrientation.of(face, d);
                // u x v must point away from the viewer (text not mirrored, same as GUI space)
                Vec3 uv = o.u.cross(o.v);
                Assertions.assertTrue(uv.dot(o.w) < -0.99, face + "/" + d);
                // a click at the centre of cell (2, 5) maps back to that cell
                Vec3 p = o.origin
                        .add(o.u.scale(PanelOrientation.MARGIN + 2.5 * PanelOrientation.CELL))
                        .add(o.v.scale(PanelOrientation.MARGIN + 5.5 * PanelOrientation.CELL));
                int[] cell = o.toCell(p);
                Assertions.assertArrayEquals(new int[]{2, 5}, cell, face + "/" + d);
                // the front face lies inside the block
                Assertions.assertTrue(p.x >= 0 && p.x <= 1 && p.y >= 0 && p.y <= 1 && p.z >= 0 && p.z <= 1);
            }
        }
    }

    @Test
    public void tiltedAndRaisedPlatesStayInsideBlockAndAreClickable() {
        for (AttachFace face : AttachFace.values()) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int off = 0; off < PanelOrientation.OFFSETS; off++) {
                    for (int tilt = 0; tilt < PanelOrientation.TILTS.length; tilt++) {
                        PanelOrientation o = PanelOrientation.of(face, d, off, tilt);
                        String id = face + "/" + d + "/" + off + "/" + tilt;
                        Assertions.assertTrue(o.u.cross(o.v).dot(o.w) < -0.99, id);
                        // every corner of the plate is inside the block
                        for (double pu : new double[]{0, 1}) for (double pv : new double[]{0, 1}) for (double pw : new double[]{0, -PanelOrientation.THICKNESS}) {
                            Vec3 p = o.toBlock(pu, pv, pw);
                            Assertions.assertTrue(p.x > -1e-6 && p.x < 1 + 1e-6 && p.y > -1e-6 && p.y < 1 + 1e-6 && p.z > -1e-6 && p.z < 1 + 1e-6, id + " corner " + p);
                        }
                        // a ray from the viewer's side through the centre of cell (6, 2) hits that cell
                        Vec3 target = o.toBlock(PanelOrientation.MARGIN + 6.5 * PanelOrientation.CELL, PanelOrientation.MARGIN + 2.5 * PanelOrientation.CELL, 0);
                        Vec3 eye = target.add(o.w.scale(1.5)).add(o.v.scale(0.4));
                        double[] uv = o.raycast(eye, target.subtract(eye).normalize(), false);
                        Assertions.assertNotNull(uv, id);
                        Assertions.assertArrayEquals(new int[]{6, 2}, PanelOrientation.cellOf(uv[0], uv[1]), id);
                        // the same ray does not count as a click on the back
                        Assertions.assertNull(o.raycast(eye, target.subtract(eye).normalize(), true), id);
                        // from behind, the back face is hit
                        Vec3 back = o.toBlock(0.5, 0.5, -PanelOrientation.THICKNESS);
                        Vec3 eyeB = back.subtract(o.w.scale(1.2));
                        Assertions.assertNotNull(o.raycast(eyeB, back.subtract(eyeB).normalize(), true), id);
                    }
                }
            }
        }
    }

    @Test
    public void floorTopOffsetSitsAtTheTopOfTheBlock() {
        PanelOrientation o = PanelOrientation.of(AttachFace.FLOOR, Direction.NORTH, PanelOrientation.OFFSETS - 1, 0);
        Assertions.assertEquals(1.0, o.origin.y, 1e-6);
        PanelOrientation flush = PanelOrientation.of(AttachFace.FLOOR, Direction.NORTH, 0, 0);
        Assertions.assertEquals(PanelOrientation.THICKNESS, flush.origin.y, 1e-6);
    }

    @Test
    public void wallPanelTextReadsLeftToRight() {
        // wall panel facing south: viewer stands south looking north, so "right" is east (+x)
        PanelOrientation o = PanelOrientation.of(AttachFace.WALL, Direction.SOUTH);
        Assertions.assertEquals(new Vec3(1, 0, 0), o.u);
        Assertions.assertEquals(new Vec3(0, -1, 0), o.v);
        Assertions.assertEquals(PanelOrientation.THICKNESS, o.origin.z, 1e-6);
    }
}
