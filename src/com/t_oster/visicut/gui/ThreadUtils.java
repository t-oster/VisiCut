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
package com.t_oster.visicut.gui;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;

/**
 * Utilities for thread safety in the GUI context.
 *
 * Suggested reading:
 * https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html
 *
 * @author Max Gaukler <development@maxgaukler.de>
 */
public class ThreadUtils
{
  /**
   * Runs the given method in the GUI (AWT Event Dispatcher Thread) context.
   * This must be used if the GUI elements are accessed from other threads.
   * This function is a wrapper for SwingUtilities.invokeAndWait.
   *
   * Example usage:
   * ThreadUtils.runIn
   *
   * @param r lambda expression or Runnable
   */
  public static void runInGUIThread(Runnable r) {
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
    } else {
      try
      {
        SwingUtilities.invokeAndWait(r);
      }
      catch (InterruptedException ex)
      {
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex);
      }
      catch (InvocationTargetException ex)
      {
        // uncaught exception inside the runnable
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex.getCause());
      }
    }
  }

  /**
   * warn if we are not in the GUI thread
   */
  public static void assertInGUIThread() {
    try {
      if (!SwingUtilities.isEventDispatchThread()) {
        throw new Exception("Warning: A GUI function was called from the non-GUI thread " + Thread.currentThread().getName() + ". This may cause sporadic errors and should therefore be fixed.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
