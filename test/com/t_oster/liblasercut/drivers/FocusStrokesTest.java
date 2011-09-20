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
package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.*;
import com.t_oster.liblasercut.platform.Point;
import org.junit.Test;

/**
 * This test is a cutting job which detects the line behavior
 * at different focus distances
 * @author oster
 */
public class FocusStrokesTest
{

  @Test
  public void focuslines() throws IllegalJobException, Exception
  {
    RasterPart rp = new RasterPart(new LaserProperty(20,10,5000,10));
    BlackWhiteRaster bwr = new BlackWhiteRaster(10000,50);
    for (int y=0;y<50;y++)
    {
      for (int x=0;x<10000;x++)
      {
        bwr.setBlack(x, y, true);
      }
    }
    rp.addImage(bwr, new Point(0,0));
    LaserJob job = new LaserJob("focus", "bla", "bla", 500, null, null, rp);
    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    instance.sendJob(job);
  }
}
