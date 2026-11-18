package pl.asie.computronics;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.asie.computronics.audio.DFPWM;
import pl.asie.computronics.tape.TapeStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TapeStorageTest {
    private static Path dir;

    @BeforeAll
    static void setup() throws IOException {
        dir = Files.createTempDirectory("ctapes");
        TapeStorage.setStorageDirectory(dir);
    }

    @AfterAll
    static void teardown() {
        TapeStorage.shutdown();
    }

    @Test
    public void readWriteAtPosition() {
        TapeStorage storage = TapeStorage.get("rw", 1024);
        Assertions.assertNotNull(storage);
        Assertions.assertEquals(1024, storage.getCapacity());

        byte[] hello = "Hello Computronics!".getBytes(StandardCharsets.UTF_8);
        Assertions.assertEquals(hello.length, storage.write(10, hello));
        Assertions.assertArrayEquals(hello, storage.read(10, hello.length));
        Assertions.assertEquals('H', storage.readByte(10));
    }

    @Test
    public void boundsAreRespected() {
        TapeStorage storage = TapeStorage.get("bounds", 100);
        Assertions.assertEquals(5, storage.write(95, new byte[10]));
        Assertions.assertEquals(0, storage.write(100, new byte[10]));
        Assertions.assertEquals(0, storage.read(100, 10).length);
        Assertions.assertEquals(-1, storage.readByte(-1));
    }

    @Test
    public void persistsOnSaveAndNeverTruncates() throws IOException {
        TapeStorage storage = TapeStorage.get("persist", 64);
        storage.write(0, new byte[]{1, 2, 3});
        TapeStorage.saveAll();
        Path file = dir.resolve("tape_persist.bin");
        Assertions.assertTrue(Files.exists(file));
        Assertions.assertEquals(64, Files.size(file));

        TapeStorage.shutdown();
        TapeStorage.setStorageDirectory(dir);
        TapeStorage reloaded = TapeStorage.get("persist", 16); // capacity shrank in config
        Assertions.assertEquals(64, reloaded.getCapacity());
        Assertions.assertArrayEquals(new byte[]{1, 2, 3}, reloaded.read(0, 3));
    }

    @Test
    public void maliciousIdCannotEscapeFolder() {
        TapeStorage storage = TapeStorage.get("../../evil", 8);
        storage.write(0, new byte[]{1});
        TapeStorage.saveAll();
        Assertions.assertTrue(Files.exists(dir.resolve("tape_______evil.bin")));
    }

    @Test
    public void dfpwmRoundTripTracksSignal() {
        // 1 second of a 440 Hz sine at 32768 Hz
        byte[] pcm = new byte[32768];
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (byte) Math.round(Math.sin(2 * Math.PI * 440 * i / 32768.0) * 100);
        }
        byte[] encoded = new DFPWM().encode(pcm);
        Assertions.assertEquals(4096, encoded.length);
        byte[] decoded = new DFPWM().decode(encoded);
        Assertions.assertEquals(pcm.length, decoded.length);

        // decoded signal must correlate strongly with the source (allowing for filter delay)
        double best = 0;
        for (int lag = 0; lag < 32; lag++) {
            double dot = 0, a = 0, b = 0;
            for (int i = 1000; i < pcm.length - 64; i++) {
                dot += pcm[i] * decoded[i + lag];
                a += pcm[i] * pcm[i];
                b += decoded[i + lag] * decoded[i + lag];
            }
            best = Math.max(best, dot / Math.sqrt(a * b));
        }
        Assertions.assertTrue(best > 0.9, "correlation too low: " + best);
    }

    @Test
    public void dfpwmSilenceIsQuiet() {
        byte[] alternating = new byte[512];
        java.util.Arrays.fill(alternating, (byte) 0x55); // 0101... = silence in DFPWM
        byte[] out = new DFPWM().decode(alternating);
        int max = 0;
        for (int i = 256; i < out.length; i++) max = Math.max(max, Math.abs(out[i]));
        Assertions.assertTrue(max < 20, "silence too loud: " + max);
    }
}
