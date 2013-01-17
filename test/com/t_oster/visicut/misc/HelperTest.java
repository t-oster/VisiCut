package com.t_oster.visicut.misc;

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class HelperTest
{
  
  public HelperTest()
  {
  }

  /**
   * Test of toHtmlRGB method, of class Helper.
   */
  @Test
  public void testToHtmlRGB()
  {
    assertEquals("#ff0000", Helper.toHtmlRGB(Color.RED));
    assertEquals("#00ff00", Helper.toHtmlRGB(Color.GREEN));
    assertEquals("#0000ff", Helper.toHtmlRGB(Color.BLUE));
  }

  /**
   * Test of fromHtmlRGB method, of class Helper.
   */
  @Test
  public void testFromHtmlRGB()
  {
    assertEquals(new Color(16, 16, 16), Helper.fromHtmlRGB("#101010"));
    assertEquals(Color.BLACK, Helper.fromHtmlRGB("#000000"));
    assertEquals(Color.BLUE, Helper.fromHtmlRGB("#0000ff"));
    assertEquals(Color.RED, Helper.fromHtmlRGB("#ff0000"));
    assertEquals(Color.GREEN, Helper.fromHtmlRGB("#00ff00"));
    assertEquals(Color.WHITE, Helper.fromHtmlRGB("#ffffff"));
  }
  
  @Test
  public void testPathnameConversion()
  {
    String[] examples = new String[]{
      "ha_-:,;ß!§$%&/()=\"'",
      " asd  asd 343q2 ()",
      "___&&&__&&$%__"
    };
    for (String s: examples)
    {
      String enc = Helper.toPathName(s);
      for (char c : enc.toCharArray())
      {
        assertTrue("s contains "+c, c == '_' || Helper.allowedChars.contains(""+c));
      }
      assertEquals(s, Helper.fromPathName(enc));
    }
  }

}
