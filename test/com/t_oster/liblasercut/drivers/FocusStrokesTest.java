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
    int min = -120;
    int max = 150;
    int step = 20;
    VectorPart vp = new VectorPart(new LaserProperty(10, 100, 5000));
    for (int focus = min; focus <= max; focus+=step)
    {
      vp.setFocus(focus);
      vp.moveto(0, 30*focus/step);
      vp.lineto(500, 30*focus/step);
    }
    LaserJob job = new LaserJob("focus", "bla", "bla", 500, null, vp, null);
    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    instance.sendJob(job);
  }
}
