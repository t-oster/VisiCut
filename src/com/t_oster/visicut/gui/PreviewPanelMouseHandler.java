/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.gui;

import com.t_oster.visicut.gui.beans.EditRectangle;
import com.t_oster.visicut.gui.beans.EditRectangle.Button;
import com.t_oster.visicut.gui.beans.PreviewPanel;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles the transformations to the background and the
 * selected graphics set, which are applyable via mouse in the preview panel
 * (Main View)
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PreviewPanelMouseHandler implements MouseListener, MouseMotionListener
{

  private PreviewPanel previewPanel;

  public PreviewPanelMouseHandler(PreviewPanel panel)
  {
    this.previewPanel = panel;
    this.previewPanel.addMouseListener(this);
    this.previewPanel.addMouseMotionListener(this);
  }

  private enum MouseAction
  {

    movingBackground,
    movingSet,
    resizingSet,
  };
  private Point lastMousePosition = null;
  private MouseAction currentAction = null;
  private Button currentButton = null;
  private GraphicSet selectedSet = null;
  private EditRectangle editRect = null;

  public void mouseClicked(MouseEvent me)
  {
  }

  public void mousePressed(MouseEvent evt)
  {
    lastMousePosition = evt.getPoint();
    currentAction = MouseAction.movingBackground;
    if (editRect != null)
    {
      Rectangle2D curRect = Helper.transform(editRect, this.previewPanel.getLastDrawnTransform());
      Button b = editRect.getButtonByPoint(lastMousePosition, this.previewPanel.getLastDrawnTransform());
      if (b != null)
      {
        currentButton = b;
        currentAction = MouseAction.resizingSet;
      }
      else
      {
        if (curRect.contains(lastMousePosition))
        {
          currentAction = MouseAction.movingSet;
        }
      }
    }
    setCursor(evt.getPoint());
  }

  public void mouseReleased(MouseEvent evt)
  {
    if (currentAction == MouseAction.resizingSet)
    {
      //Apply changes to the EditRectangle to the selectedSet
      Rectangle2D src = selectedSet.getOriginalBoundingBox();
      selectedSet.setTransform(Helper.getTransform(src, editRect));
      this.previewPanel.repaint();
    }
    else
    {//not resizing
      if (this.selectedSet != null)
      {//something selected before
        if (this.currentAction != MouseAction.movingSet)
        {
          editRect = null;
          this.previewPanel.setEditRectangle(null);
          selectedSet = null;
        }
        else
        {
          Rectangle2D bb = selectedSet.getBoundingBox();
          editRect = new EditRectangle(bb);
          this.previewPanel.setEditRectangle(editRect);
        }
      }
      else
      {//nothing selected before
        if (this.previewPanel.getGraphicObjects() != null)
        {
          Rectangle2D bb = this.previewPanel.getGraphicObjects().getBoundingBox();
          Rectangle2D e = Helper.transform(bb, this.previewPanel.getLastDrawnTransform());
          if (e.contains(evt.getPoint()))
          {
            selectedSet = this.previewPanel.getGraphicObjects();
            editRect = new EditRectangle(bb);
            this.previewPanel.setEditRectangle(editRect);
          }
          else
          {
            selectedSet = null;
            editRect = null;
            this.previewPanel.setEditRectangle(null);
          }
        }
      }
    }
    lastMousePosition = evt.getPoint();
    setCursor(evt.getPoint());
  }

  public void mouseEntered(MouseEvent me)
  {
  }

  public void mouseExited(MouseEvent me)
  {
  }

  public void mouseDragged(MouseEvent evt)
  {
    if (lastMousePosition != null)
    {
      Point diff = new Point(evt.getPoint().x - lastMousePosition.x, evt.getPoint().y - lastMousePosition.y);
      try
      {
        switch (currentAction)
        {
          case resizingSet:
          {
            this.previewPanel.getLastDrawnTransform().createInverse().deltaTransform(diff, diff);
            switch (currentButton)
            {
              case BOTTOM_RIGHT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : diff.y;
                editRect.height += (offset * editRect.height / editRect.width);
                editRect.width += offset;
                break;
              }
              case BOTTOM_LEFT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : diff.y;
                editRect.height -= (offset * editRect.height / editRect.width);
                editRect.x += offset;
                editRect.width -= offset;
                break;
              }
              case TOP_RIGHT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : -diff.y;
                editRect.y -= (offset * editRect.height / editRect.width);
                editRect.height += (offset * editRect.height / editRect.width);
                editRect.width += offset;
                break;
              }
              case TOP_LEFT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : diff.y;
                editRect.y += (offset * editRect.height / editRect.width);
                editRect.height -= (offset * editRect.height / editRect.width);
                editRect.x += offset;
                editRect.width -= offset;
                break;
              }
              case CENTER_RIGHT:
              {
                this.editRect.width += diff.x;
                break;
              }
              case TOP_CENTER:
              {
                this.editRect.y += diff.y;
                this.editRect.height -= diff.y;
                break;
              }
              case BOTTOM_CENTER:
              {
                this.editRect.height += diff.y;
                break;
              }
              case CENTER_LEFT:
              {
                this.editRect.x += diff.x;
                this.editRect.width -= diff.x;
                break;
              }
            }
            this.previewPanel.setEditRectangle(editRect);
            break;
          }
          case movingSet:
          {
            this.previewPanel.getLastDrawnTransform().createInverse().deltaTransform(diff, diff);
            if (selectedSet.getTransform() != null)
            {
              AffineTransform tr = AffineTransform.getTranslateInstance(diff.x, diff.y);
              tr.concatenate(selectedSet.getTransform());
              selectedSet.setTransform(tr);
            }
            else
            {
              selectedSet.setTransform(AffineTransform.getTranslateInstance(diff.x, diff.y));
            }
            Rectangle2D bb = selectedSet.getBoundingBox();
            editRect = new EditRectangle(bb);
            this.previewPanel.setEditRectangle(editRect);
            break;
          }
          case movingBackground:
          {
            Point center = this.previewPanel.getCenter();
            center.translate(-diff.x * 1000 / this.previewPanel.getZoom(), -diff.y * 1000 / this.previewPanel.getZoom());
            this.previewPanel.setCenter(center);
            break;
          }
        }
        this.previewPanel.repaint();
      }
      catch (NoninvertibleTransformException ex)
      {
        Logger.getLogger(PreviewPanelMouseHandler.class.getName()).log(Level.SEVERE, null, ex);
      }
      lastMousePosition = evt.getPoint();
    }
  }

  public void mouseMoved(MouseEvent evt)
  {
    setCursor(evt.getPoint());
  }

  private void setCursor(Point p)
  {
    int cursor = Cursor.DEFAULT_CURSOR;
    cursorcheck:
    {
      if (this.previewPanel.getGraphicObjects() != null)
      {
        if (editRect != null)
        {
          Button b = editRect.getButtonByPoint(p, this.previewPanel.getLastDrawnTransform());
          if (b != null)
          {
            switch (b)
            {
              case TOP_RIGHT:
                cursor = Cursor.NE_RESIZE_CURSOR;
                break cursorcheck;
              case CENTER_RIGHT:
                cursor = Cursor.E_RESIZE_CURSOR;
                break cursorcheck;
              case BOTTOM_RIGHT:
                cursor = Cursor.SE_RESIZE_CURSOR;
                break cursorcheck;
              case BOTTOM_CENTER:
                cursor = Cursor.S_RESIZE_CURSOR;
                break cursorcheck;
              case BOTTOM_LEFT:
                cursor = Cursor.SW_RESIZE_CURSOR;
                break cursorcheck;
              case CENTER_LEFT:
                cursor = Cursor.W_RESIZE_CURSOR;
                break cursorcheck;
              case TOP_LEFT:
                cursor = Cursor.NW_RESIZE_CURSOR;
                break cursorcheck;
              case TOP_CENTER:
                cursor = Cursor.N_RESIZE_CURSOR;
                break cursorcheck;
            }
          }
        }
        Rectangle2D bb = this.previewPanel.getGraphicObjects().getBoundingBox();
        if (bb != null)
        {
          Rectangle2D e = Helper.transform(bb, this.previewPanel.getLastDrawnTransform());
          if (e.contains(p))
          {
            cursor = this.editRect == null ? Cursor.HAND_CURSOR : Cursor.MOVE_CURSOR;
            break cursorcheck;
          }
        }
      }
    }
    this.previewPanel.setCursor(Cursor.getPredefinedCursor(cursor));
  }
}
