/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.t_oster.util.Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thommy
 */
public class UtilTest {
    
    public UtilTest() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Test
    public void testDiffer(){
        assertTrue(Util.differ("bla", "blubb"));
        assertTrue(Util.differ("a", null));
        assertFalse(Util.differ(null,null));
        assertFalse(Util.differ("abc", "abc"));
    }
}
