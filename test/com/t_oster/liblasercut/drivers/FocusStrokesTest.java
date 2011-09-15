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
    int min = -126;
    int max = 126;
    int step = 20;
    VectorPart vp = new VectorPart(new LaserProperty(20, 100, 5000));
    for (int focus = min; focus <= max; focus+=step)
    {
      vp.setFocus(focus);
      vp.moveto(0, 100*focus/step);
      vp.lineto(500, 100*focus/step);
    }
    vp.setPower(50);
    for (int focus = min; focus <= max; focus+=step)
    {
      vp.setFocus(focus);
      vp.moveto(600, 100*focus/step);
      vp.lineto(1100, 100*focus/step);
    }
    vp.setPower(100);
    for (int focus = min; focus <= max; focus+=step)
    {
      vp.setFocus(focus);
      vp.moveto(1200, 100*focus/step);
      vp.lineto(1700, 100*focus/step);
    }
    LaserJob job = new LaserJob("focus", "bla", "bla", 500, null, vp, null);
    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    instance.sendJob(job);
  }
}
