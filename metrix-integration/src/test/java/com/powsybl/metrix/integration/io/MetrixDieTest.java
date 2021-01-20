package com.powsybl.metrix.integration.io;

import com.google.common.collect.Sets;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixDieTest extends AbstractConverterTest {

    @Test
    public void testInvalidAttrSize() {
        MetrixDie die = new MetrixDie();
        try {
            die.setIntArray("AAA", new int[]{1});
            fail();
        } catch (Exception ignored) {
        }
    }

    @Test
    public void loadSaveLoadTest() throws IOException {
        Path dirTmp1 = tmpDir.resolve("tmp1");
        Path dirTmp2 = tmpDir.resolve("tmp2");
        Files.createDirectories(dirTmp1);
        Files.createDirectories(dirTmp2);
        MetrixDie die = new MetrixDie();
        int[] aaaaaaaa = new int[] {1, 2, 3};
        float[] bbbbbbbb = new float[] {1.1f, 2.2f, 3.3f};
        double[] cccccccc = new double[] {1.11f, 2.22f, 3.33f};
        String[] dddddddd = new String[] {"S1", "S2_", "S3", "S4"};
        boolean[] eeeeeeee = new boolean[] {true, false, false, true};
        die.setIntArray("AAAAAAAA", aaaaaaaa);
        die.setFloatArray("BBBBBBBB", bbbbbbbb);
        die.setDoubleArray("CCCCCCCC", cccccccc);
        die.setStringArray("DDDDDDDD", dddddddd);
        die.setBooleanArray("EEEEEEEE", eeeeeeee);
        die.save(dirTmp1);
        assertTrue(Files.exists(dirTmp1.resolve("fort.2")));
        assertTrue(Files.exists(dirTmp1.resolve("fort.44_BIN")));
        assertTrue(Files.exists(dirTmp1.resolve("fort.45_BIN")));
        assertTrue(Files.exists(dirTmp1.resolve("fort.46_BIN")));
        assertTrue(Files.exists(dirTmp1.resolve("fort.47_BIN")));
        assertTrue(Files.exists(dirTmp1.resolve("fort.48_BIN")));
        MetrixDie die2 = new MetrixDie();
        die2.load(dirTmp1);
        assertArrayEquals(die2.getIntArray("AAAAAAAA"), aaaaaaaa);
        assertArrayEquals(die2.getFloatArray("BBBBBBBB"), bbbbbbbb, 0f);
        assertArrayEquals(die2.getDoubleArray("CCCCCCCC"), cccccccc, 0);
        assertArrayEquals(die2.getStringArray("DDDDDDDD"), dddddddd);
        assertArrayEquals(die2.getBooleanArray("EEEEEEEE"), eeeeeeee);
        assertEquals(die2.getAttributeNames(), Sets.newHashSet("AAAAAAAA", "BBBBBBBB", "CCCCCCCC", "DDDDDDDD", "EEEEEEEE"));
        die2.save(dirTmp2);
        assertTrue(Files.exists(dirTmp2.resolve("fort.2")));
        assertTrue(Files.exists(dirTmp2.resolve("fort.44_BIN")));
        assertTrue(Files.exists(dirTmp2.resolve("fort.45_BIN")));
        assertTrue(Files.exists(dirTmp2.resolve("fort.46_BIN")));
        assertTrue(Files.exists(dirTmp2.resolve("fort.47_BIN")));
        assertTrue(Files.exists(dirTmp2.resolve("fort.48_BIN")));
        assertArrayEquals(Files.readAllBytes(dirTmp1.resolve("fort.2")), Files.readAllBytes(dirTmp2.resolve("fort.2")));
        assertArrayEquals(Files.readAllBytes(dirTmp1.resolve("fort.44_BIN")), Files.readAllBytes(dirTmp2.resolve("fort.44_BIN")));
        assertArrayEquals(Files.readAllBytes(dirTmp1.resolve("fort.45_BIN")), Files.readAllBytes(dirTmp2.resolve("fort.45_BIN")));
        assertArrayEquals(Files.readAllBytes(dirTmp1.resolve("fort.46_BIN")), Files.readAllBytes(dirTmp2.resolve("fort.46_BIN")));
        assertArrayEquals(Files.readAllBytes(dirTmp1.resolve("fort.47_BIN")), Files.readAllBytes(dirTmp2.resolve("fort.47_BIN")));
        assertArrayEquals(Files.readAllBytes(dirTmp1.resolve("fort.48_BIN")), Files.readAllBytes(dirTmp2.resolve("fort.48_BIN")));
    }

    @Test
    public void jsonLoadSaveTest() throws IOException, URISyntaxException {
        MetrixDie die = new MetrixDie();
        Path inputFile =  Paths.get(getClass().getResource("/simpleNetwork.json").toURI());
        die.loadFromJson(inputFile);
        Path outputFile = fileSystem.getPath("output.json");
        die.saveToJson(outputFile);
        compareTxt(Files.newInputStream(inputFile), Files.newInputStream(outputFile));
    }
}
