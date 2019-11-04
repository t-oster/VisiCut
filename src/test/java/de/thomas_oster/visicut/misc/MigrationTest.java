/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.thomas_oster.visicut.misc;

import de.thomas_oster.visicut.managers.LaserDeviceManager;
import de.thomas_oster.visicut.model.LaserDevice;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
public class MigrationTest {

    @Test
    public void testLasercutterMigration() throws UnsupportedEncodingException, IOException {
        LaserDeviceManager ldm = new LaserDeviceManager();
        LaserDevice ld = ldm.loadFromFile(new File("/home/thomas/.visicut/devices/Epilog_95_Zing_95_Fablab.xml"));
        assertNotNull(ld);
        assertEquals("Epilog_Zing_Fablab", ld.getName());
        ldm.save(ld, new File("/tmp/test"));
    }

}
