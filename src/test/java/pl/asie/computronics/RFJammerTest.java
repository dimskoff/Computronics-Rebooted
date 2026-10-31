package pl.asie.computronics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.asie.computronics.network.RFJammerManager;

public class RFJammerTest {

    @Test
    public void testJammerManagerInstance() {
        RFJammerManager manager = RFJammerManager.INSTANCE;
        Assertions.assertNotNull(manager);

        // Without active jammers, data passes uncorrupted
        Object[] original = new Object[]{"Ping 123", 42};
        Object[] result = manager.applyJamming(null, null, 100, original);
        Assertions.assertArrayEquals(original, result);
    }
}
