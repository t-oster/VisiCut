package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.*;
import javax.swing.JOptionPane;
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
