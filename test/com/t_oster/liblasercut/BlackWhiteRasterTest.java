/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thommy
 */
public class BlackWhiteRasterTest
{

  public BlackWhiteRasterTest()
  {
  }

  @BeforeClass
  public static void setUpClass() throws Exception
  {
  }

  @AfterClass
  public static void tearDownClass() throws Exception
  {
  }

  @Before
  public void setUp()
  {
  }

  @After
  public void tearDown()
  {
  }

  @Test
  public void testByteRepresentation()
  {
    BlackWhiteRaster ras = new BlackWhiteRaster(500, 600);
    assertEquals(500, ras.getWidth());
    assertEquals(600, ras.getHeight());
    for (int x = 0; x < ras.getWidth(); x++)
    {
      for (int y = 0; y < ras.getHeight(); y++)
      {
        ras.setBlack(x, y, true);
      }
    }
    for (int x = 0; x < ras.getWidth(); x++)
    {
      for (int y = 0; y < ras.getHeight(); y++)
      {
        assertTrue(ras.isBlack(x,y));
      }
    }
    for (int x = 0; x < ras.getWidth(); x++)
    {
      for (int y = 0; y < ras.getHeight(); y++)
      {
        ras.setBlack(x, y, false);
      }
    }
    for (int x = 0; x < ras.getWidth(); x++)
    {
      for (int y = 0; y < ras.getHeight(); y++)
      {
        assertFalse(ras.isBlack(x,y));
      }
    }
    for (int x = 0; x < ras.getWidth(); x++)
    {
      for (int y = 0; y < ras.getHeight(); y++)
      {
        boolean black = ((Math.random() * 10) % 2 == 1);
        ras.setBlack(x, y, black);
        assertEquals(black, ras.isBlack(x, y));
      }
    }
  }
}
