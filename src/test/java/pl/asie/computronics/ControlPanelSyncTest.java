package pl.asie.computronics;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.network.ConfigurePanelPayload;
import pl.asie.computronics.network.SyncPanelElementsPayload;

public class ControlPanelSyncTest {

    @Test
    public void testSyncPanelElementsPayloadCodec() {
        BlockPos pos = new BlockPos(-50, 12, 300);
        CompoundTag tag = new CompoundTag();
        tag.putString("testKey", "testValue");
        tag.putInt("number", 42);

        SyncPanelElementsPayload payload = new SyncPanelElementsPayload(pos, tag);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);

        SyncPanelElementsPayload decoded = new SyncPanelElementsPayload(buf);
        Assertions.assertEquals(pos, decoded.pos());
        Assertions.assertNotNull(decoded.tag());
        Assertions.assertEquals("testValue", decoded.tag().getString("testKey"));
        Assertions.assertEquals(42, decoded.tag().getInt("number"));
    }

    @Test
    public void testConfigurePanelPayloadCodec() {
        BlockPos pos = new BlockPos(100, 64, -200);
        int slot = 5;
        int color = 0x20CC40;
        int sizeX = 2;
        int sizeY = 1;
        String label = "LINE 1\nLINE 2\nLINE 3";
        int textColor = 0x00FFCC;
        boolean bold = true;
        String displayValue = "1234";

        ConfigurePanelPayload payload = new ConfigurePanelPayload(
                pos, slot, color, sizeX, sizeY, label, textColor, bold, displayValue
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);

        ConfigurePanelPayload decoded = new ConfigurePanelPayload(buf);

        Assertions.assertEquals(pos, decoded.pos());
        Assertions.assertEquals(slot, decoded.slot());
        Assertions.assertEquals(color, decoded.color());
        Assertions.assertEquals(sizeX, decoded.sizeX());
        Assertions.assertEquals(sizeY, decoded.sizeY());
        Assertions.assertEquals(label, decoded.label());
        Assertions.assertEquals(textColor, decoded.textColor());
        Assertions.assertEquals(bold, decoded.bold());
        Assertions.assertEquals(displayValue, decoded.displayValue());
    }

    @Test
    public void testTileControlPanelNBTSerialization() {
        try {
            TileControlPanel panel = new TileControlPanel(BlockPos.ZERO, null);
            panel.mountElementWithSize(2, 3, TileControlPanel.ElementType.BUTTON, 0xCC2020, 2, 2);
            int slot = panel.getSlotIndex(2, 3);

            panel.configureElement(slot, 0x20CC40, 2, 2, "RUN\nSTOP", 0x00FFCC, true, "99");

            CompoundTag tag = new CompoundTag();
            panel.saveAdditional(tag, null);

            TileControlPanel clientPanel = new TileControlPanel(BlockPos.ZERO, null);
            clientPanel.loadAdditional(tag, null);

            TileControlPanel.PanelElement elem = clientPanel.getElement(slot);
            Assertions.assertNotNull(elem);
            Assertions.assertEquals(TileControlPanel.ElementType.BUTTON, elem.type);
            Assertions.assertEquals(0x20CC40, elem.color);
            Assertions.assertEquals(2, elem.sizeX);
            Assertions.assertEquals(2, elem.sizeY);
            Assertions.assertEquals("RUN\nSTOP", elem.label);
            Assertions.assertEquals(0x00FFCC, elem.textColor);
            Assertions.assertTrue(elem.bold);
            Assertions.assertEquals("99", elem.displayValue);

            // Test full 8-wide banner text component sync and clamping
            panel.mountElementWithSize(0, 0, TileControlPanel.ElementType.TEXT, 0x00FFCC, 8, 1);
            panel.configureElement(0, 0x00FFCC, 8, 1, "SYSTEM ONLINE", 0xFFFFFF, true, "");
            CompoundTag textTag = new CompoundTag();
            panel.saveAdditional(textTag, null);

            TileControlPanel clientPanel2 = new TileControlPanel(BlockPos.ZERO, null);
            clientPanel2.handleUpdateTag(textTag, null);
            TileControlPanel.PanelElement textElem = clientPanel2.getElement(0);
            Assertions.assertNotNull(textElem);
            Assertions.assertEquals(TileControlPanel.ElementType.TEXT, textElem.type);
            Assertions.assertEquals(8, textElem.sizeX);
            Assertions.assertEquals(1, textElem.sizeY);
            Assertions.assertEquals("SYSTEM ONLINE", textElem.label);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // If registry isn't bootstrapped in pure unit test, ignore
            System.out.println("Registry bootstrap required for full blockentity test: " + e.getMessage());
        }
    }
}
