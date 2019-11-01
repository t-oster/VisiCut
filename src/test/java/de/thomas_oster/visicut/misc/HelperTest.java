/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package de.thomas_oster.visicut.misc;

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
