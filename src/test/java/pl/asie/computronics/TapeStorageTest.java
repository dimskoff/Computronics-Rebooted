package pl.asie.computronics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.asie.computronics.item.ItemTape;
import pl.asie.computronics.tape.TapeStorage;

import java.nio.charset.StandardCharsets;

public class TapeStorageTest {

    @Test
    public void testTapeReadWriteSeek() {
        TapeStorage storage = TapeStorage.get("test-storage-1", ItemTape.CAPACITY_IRON);
        Assertions.assertNotNull(storage);
        Assertions.assertEquals(ItemTape.CAPACITY_IRON, storage.getCapacity());
        Assertions.assertEquals(0, storage.getPosition());

        byte[] hello = "Hello Computronics-Rebooted!".getBytes(StandardCharsets.UTF_8);
        int written = storage.write(hello);
        Assertions.assertEquals(hello.length, written);
        Assertions.assertEquals(hello.length, storage.getPosition());

        // Seek back to beginning
        int seekDiff = storage.seek(-hello.length);
        Assertions.assertEquals(-hello.length, seekDiff);
        Assertions.assertEquals(0, storage.getPosition());

        // Read bytes back
        byte[] readBack = storage.read(hello.length);
        Assertions.assertArrayEquals(hello, readBack);
        Assertions.assertEquals(hello.length, storage.getPosition());
    }

    @Test
    public void testTapeCapacityBounds() {
        TapeStorage storage = TapeStorage.get("test-storage-bounds", 100);
        storage.seek(-500);
        Assertions.assertEquals(0, storage.getPosition());

        storage.seek(500);
        Assertions.assertEquals(100, storage.getPosition());

        byte[] overflow = new byte[10];
        int written = storage.write(overflow);
        Assertions.assertEquals(0, written);
    }
}
