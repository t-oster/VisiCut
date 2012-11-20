/**
 * This file is part of VisiCut. Copyright (C) 2012 Thomas Oster
 * <thomas.oster@rwth-aachen.de> RWTH Aachen University - 52062 Aachen, Germany
 *
 * VisiCut is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * VisiCut is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VisiCut. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.t_oster.visicut.gui;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.beans.EditRectangle;
import com.t_oster.visicut.gui.beans.EditRectangle.Button;
import com.t_oster.visicut.gui.beans.EditRectangle.ParameterField;
import com.t_oster.visicut.gui.beans.PreviewPanel;
import com.t_oster.visicut.misc.DialogHelper;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/**
 * This class handles the transformations to the background and the selected
 * graphics set, which are applyable via mouse in the preview panel (Main View)
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PreviewPanelKeyboardMouseHandler implements MouseListener, MouseMotionListener, KeyListener
{

  private PreviewPanel previewPanel;
  private DialogHelper dialogHelper;
  private JPopupMenu menu = new JPopupMenu();
  private JMenuItem optionsmenu = new JMenuItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("PROFILE_OPTIONS"));
  private JMenuItem resetMenuItem = new JMenuItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("RESET TRANSFORMATION"));
  //TODO: i10n
  private JMenuItem flipHorizMenuItem = new JMenuItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("FLIP_HORIZONTALLY"));
  private JMenuItem flipVertMenuItem = new JMenuItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("FLIP_VERTICALLY"));

  public PreviewPanelKeyboardMouseHandler(PreviewPanel panel)
  {
    this.previewPanel = panel;
    this.dialogHelper = new DialogHelper(panel, "VisiCut");
    this.previewPanel.addMouseListener(this);
    this.previewPanel.addMouseMotionListener(this);
    this.previewPanel.addKeyListener(this);
    this.buildMenu();
  }

  private void buildMenu()
  {
    menu.add(resetMenuItem);
    resetMenuItem.addActionListener(new ActionListener()
    {

      public void actionPerformed(ActionEvent ae)
      {
        PreviewPanelKeyboardMouseHandler.this.getGraphicObjects().setTransform(
          PreviewPanelKeyboardMouseHandler.this.getGraphicObjects().getBasicTransform());
        VisicutModel.getInstance().fitMaterialIntoBed();
        PreviewPanelKeyboardMouseHandler.this.previewPanel.setEditRectangle(new EditRectangle(getGraphicObjects().getBoundingBox()));
        PreviewPanelKeyboardMouseHandler.this.previewPanel.repaint();
      }
    });
    optionsmenu.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ae)
      {
        MainView.getInstance().editCurrentProfile();
      }
    });
    menu.add(optionsmenu);
    flipHorizMenuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        flip(true);
      }
    });
    menu.add(flipHorizMenuItem);
    flipVertMenuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        flip(false);
      }
    });
    menu.add(flipVertMenuItem);
  }

  private void flip(boolean horizontal)
  {
    Rectangle2D bb = previewPanel.getGraphicObjects().getBoundingBox();
    double mx = bb.getX()+bb.getWidth()/2;
    double my = bb.getY()+bb.getHeight()/2;
    AffineTransform flipX = AffineTransform.getTranslateInstance(mx, my);
    flipX.scale(horizontal ? -1 : 1, horizontal ? 1 : -1);
    flipX.translate(-mx, -my);
    AffineTransform cur = previewPanel.getGraphicObjects().getTransform();
    cur.preConcatenate(flipX);
    previewPanel.getGraphicObjects().setTransform(cur);
    previewPanel.setEditRectangle(null);
    previewPanel.ClearCache();
    previewPanel.repaint();
  }

  private EditRectangle getEditRect()
  {
    return this.previewPanel.getEditRectangle();
  }

  private GraphicSet getGraphicObjects()
  {
    return this.previewPanel.getGraphicObjects();
  }

  /**
   * For now this just returns all objects, but later maybe multiple input files
   * are supported and this should only return the currently selected one
   *
   * @return
   */
  private GraphicSet getSelectedSet()
  {
    return this.previewPanel.getGraphicObjects();
  }

  public void keyTyped(KeyEvent key)
  {
  }

  public void keyPressed(KeyEvent ke)
  {
    if (this.getEditRect() != null)
    {
      double diffx = 0;
      double diffy = 0;
      switch (ke.getKeyCode())
      {
        case KeyEvent.VK_LEFT:
          diffx -= 1;
          break;
        case KeyEvent.VK_RIGHT:
          diffx += 1;
          break;
        case KeyEvent.VK_UP:
          diffy -= 1;
          break;
        case KeyEvent.VK_DOWN:
          diffy += 1;
          break;
      }
      if (ke.isShiftDown())
      {
        this.previewPanel.setFastPreview(true);
        this.getEditRect().width += diffx;
        this.getEditRect().height += diffy;
        this.previewPanel.repaint();
      }
      else
      {
        this.moveSet(diffx, diffy);
      }
      ke.consume();
    }
  }

  private void applyEditRectoToSet()
  {
    //Apply changes to the EditRectangle to the getSelectedSet()
    Rectangle2D src = getSelectedSet().getBoundingBox();
    AffineTransform t = getSelectedSet().getTransform();
    t.preConcatenate(Helper.getTransform(src, getEditRect()));
    getSelectedSet().setTransform(t);
    this.previewPanel.repaint();
  }

  public void keyReleased(KeyEvent ke)
  {
    if (ke.getKeyCode() == KeyEvent.VK_SHIFT)
    {
      this.previewPanel.setFastPreview(false);
      this.applyEditRectoToSet();
    }
  }

  private enum MouseAction
  {
    movingViewport,
    movingSet,
    resizingSet,
    rotatingSet
  };
  private Point lastMousePosition = null;
  private Point lastMousePositionInViewport = null;
  private MouseAction currentAction = null;
  private Button currentButton = null;

  private boolean checkParameterFieldClick(MouseEvent me)
  {
    //TODO: Bug
    /*
     * If this transormation is applied, everything is correct, but the
     * setCursor position and the whole mouse event things are on the wrong
     * place. Deselecting the rectangle and selecting again resolves that.
     */
    if (me.getButton() == MouseEvent.BUTTON1 && this.getEditRect() != null)
    {//Check if clicked on one of the parameters Button
      try
      {
        if (this.getEditRect().isRotateMode())
        {
          if (this.getEditRect().getParameterFieldBounds(EditRectangle.ParameterField.ANGLE).contains(me.getPoint()))
          {
            Double a = dialogHelper.askDouble(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("ANGLE"), this.getEditRect().getRotationAngleInDegree());
            if (a == null)
            {
              return true;
            }
            this.rotateTo(-Math.PI*a/180d);
            return true;
          }
        }
        else
        {
          if (this.getEditRect().getParameterFieldBounds(EditRectangle.ParameterField.X).contains(me.getPoint()))
          {
            Double x = dialogHelper.askDouble(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("LEFT OFFSET"), this.getEditRect().x / 10);
            if (x == null)
            {
              return true;
            }
            if (x < 0 || x*10+this.getEditRect().width > previewPanel.getAreaSize().x)
            {
              dialogHelper.showErrorMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("OUT_OF_BOUNDS"));
              return true;
            }
            this.getEditRect().x = x * 10;
            this.applyEditRectoToSet();
            return true;
          }
          if (this.getEditRect().getParameterFieldBounds(EditRectangle.ParameterField.Y).contains(me.getPoint()))
          {
            Double y = dialogHelper.askDouble(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("TOP OFFSET"), this.getEditRect().y / 10);
            if (y == null)
            {
              return true;
            }
            if (y < 0 || y*10+this.getEditRect().height > previewPanel.getAreaSize().y)
            {
              dialogHelper.showErrorMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("OUT_OF_BOUNDS"));
              return true;
            }
            this.getEditRect().y = y * 10;
            this.applyEditRectoToSet();
            return true;
          }
          if (this.getEditRect().getParameterFieldBounds(EditRectangle.ParameterField.WIDTH).contains(me.getPoint()))
          {
            Double w = dialogHelper.askDouble(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("WIDTH"), this.getEditRect().width / 10);
            if (w == null)
            {
              return true;
            }
            if (w <= 0 || w*10+this.getEditRect().x > previewPanel.getAreaSize().x)
            {
              dialogHelper.showErrorMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("OUT_OF_BOUNDS"));
              return true;
            }
            this.getEditRect().width = w * 10;
            this.applyEditRectoToSet();
            return true;
          }
          if (this.getEditRect().getParameterFieldBounds(EditRectangle.ParameterField.HEIGHT).contains(me.getPoint()))
          {
            Double h = dialogHelper.askDouble(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("HEIGHT"), this.getEditRect().height / 10);
            if (h == null)
            {
              return true;
            }
            if (h <= 0 || h*10+this.getEditRect().y > previewPanel.getAreaSize().y)
            {
              dialogHelper.showErrorMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/PreviewPanelKeyboardMouseHandler").getString("OUT_OF_BOUNDS"));
              return true;
            }
            this.getEditRect().height = h * 10;
            this.applyEditRectoToSet();
            return true;
          }
        }
      }
      catch (Exception e)
      {
      }
    }
    return false;
  }

  public void mouseClicked(MouseEvent me)
  {
    this.previewPanel.requestFocus();
    if (this.checkParameterFieldClick(me))
    {
      return;
    }
    if (me.getButton() == MouseEvent.BUTTON1)
    {
      boolean onGraphic = false;
      if (getGraphicObjects() != null && getGraphicObjects().getBoundingBox() != null)
      {
        Rectangle2D bb = getGraphicObjects().getBoundingBox();
        Rectangle2D e = Helper.transform(bb, this.previewPanel.getMmToPxTransform());
        onGraphic = e.contains(me.getPoint());
      }
      if (onGraphic)
      {//clicked on the graphic
        if (getEditRect() != null)
        {//Already selected => toggle rotate/scale mode
          if (getEditRect().isRotateMode())
          {//we need to resize the rectangle after rotation
            this.previewPanel.setEditRectangle(new EditRectangle(getGraphicObjects().getBoundingBox()));
          }
          else
          {
            getEditRect().setRotateMode(true);
            getEditRect().setRotationAngle(Helper.getRotationAngle(this.previewPanel.getGraphicObjects().getTransform()));
            this.previewPanel.repaint();
          }
        }
        else
        {//not yet select => select in scale mode
          this.previewPanel.setEditRectangle(new EditRectangle(getGraphicObjects().getBoundingBox()));
        }
      }
      else
      {//clicked next to graphic => clear selection
        this.previewPanel.setEditRectangle(null);
      }
    }
    else if (getEditRect() != null && me.getButton() == MouseEvent.BUTTON3)
    {
      Rectangle2D bb = getGraphicObjects().getBoundingBox();
      Rectangle2D e = Helper.transform(bb, this.previewPanel.getMmToPxTransform());
      if (e.contains(me.getPoint()))
      {
        try
        {
          this.optionsmenu.setVisible(VisicutModel.getInstance().getMappings().size() == 1);
        }
        catch (NullPointerException ex)
        {
          this.optionsmenu.setVisible(false);
        }
        this.menu.show(this.previewPanel, me.getX(), me.getY());
      }
    }
  }

  public void mousePressed(MouseEvent evt)
  {
    lastMousePosition = evt.getPoint();
    lastMousePositionInViewport = SwingUtilities.convertMouseEvent(evt.getComponent(), evt, previewPanel.getParent()).getPoint();
    currentAction = MouseAction.movingViewport;
    if (getEditRect() != null)
    {//something selected
      Rectangle2D curRect = Helper.transform(getEditRect(), this.previewPanel.getMmToPxTransform());
      Button b = getEditRect().getButtonByPoint(lastMousePosition, this.previewPanel.getMmToPxTransform());
      if (b != null)
      {//a button selected
        currentButton = b;
        currentAction = getEditRect().isRotateMode() ? MouseAction.rotatingSet : MouseAction.resizingSet;
        previewPanel.setFastPreview(true);
      }
      else
      {//no button selected
        if (curRect.contains(lastMousePosition))
        {//selection in the rectangle
          currentAction = MouseAction.movingSet;
        }
        else
        {
          currentAction = MouseAction.movingViewport;
        }
      }
    }
  }

  public void mouseReleased(MouseEvent evt)
  {
    if (currentAction == MouseAction.resizingSet || currentAction == MouseAction.rotatingSet)
    {
      this.previewPanel.setFastPreview(false);
      this.previewPanel.repaint();
    }
    if (currentAction == MouseAction.resizingSet)
    {
      this.applyEditRectoToSet();
    }
    lastMousePosition = evt.getPoint();
  }

  public void mouseEntered(MouseEvent me)
  {
  }

  public void mouseExited(MouseEvent me)
  {
  }

  private void rotateTo(double angle)
  {
    Rectangle2D bb = getGraphicObjects().getBoundingBox();
    //move back
    AffineTransform tr = AffineTransform.getTranslateInstance(bb.getCenterX(), bb.getCenterY());
    //rotate
    tr.concatenate(AffineTransform.getRotateInstance(angle-getEditRect().getRotationAngle()));
    //center
    tr.concatenate(AffineTransform.getTranslateInstance(-bb.getCenterX(), -bb.getCenterY()));
    //apply current
    tr.concatenate(getGraphicObjects().transform);
    getGraphicObjects().setTransform(tr);
    getEditRect().setRotationAngle(Helper.getRotationAngle(tr));
    this.previewPanel.repaint();
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
          case rotatingSet:
          {
            Rectangle2D bb = getGraphicObjects().getBoundingBox();
            Point2D middle = previewPanel.getMmToPxTransform().transform(new Point.Double(bb.getCenterX(), bb.getCenterY()), null);
            double angle = Math.atan2(evt.getPoint().y-middle.getY(), evt.getPoint().x-middle.getX());
            this.rotateTo(angle);
            break;
          }
          case resizingSet:
          {
            this.previewPanel.getMmToPxTransform().createInverse().deltaTransform(diff, diff);
            switch (currentButton)
            {
              case BOTTOM_RIGHT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : diff.y;
                getEditRect().height += (offset * getEditRect().height / getEditRect().width);
                getEditRect().width += offset;
                break;
              }
              case BOTTOM_LEFT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : diff.y;
                getEditRect().height -= (offset * getEditRect().height / getEditRect().width);
                getEditRect().x += offset;
                getEditRect().width -= offset;
                break;
              }
              case TOP_RIGHT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : -diff.y;
                getEditRect().y -= (offset * getEditRect().height / getEditRect().width);
                getEditRect().height += (offset * getEditRect().height / getEditRect().width);
                getEditRect().width += offset;
                break;
              }
              case TOP_LEFT:
              {
                int offset = Math.abs(diff.x) > Math.abs(diff.y) ? diff.x : diff.y;
                getEditRect().y += (offset * getEditRect().height / getEditRect().width);
                getEditRect().height -= (offset * getEditRect().height / getEditRect().width);
                getEditRect().x += offset;
                getEditRect().width -= offset;
                break;
              }
              case CENTER_RIGHT:
              {
                this.getEditRect().width += diff.x;
                break;
              }
              case TOP_CENTER:
              {
                this.getEditRect().y += diff.y;
                this.getEditRect().height -= diff.y;
                break;
              }
              case BOTTOM_CENTER:
              {
                this.getEditRect().height += diff.y;
                break;
              }
              case CENTER_LEFT:
              {
                this.getEditRect().x += diff.x;
                this.getEditRect().width -= diff.x;
                break;
              }
            }
            if (getEditRect().width < 0.1)
            {
              getEditRect().width = 0.1;
            }
            if (getEditRect().height < 0.1)
            {
              getEditRect().height = 0.1;
            }
            if (getEditRect().x < 0)
            {
              getEditRect().x = 0;
            }
            if (getEditRect().y < 0)
            {
              getEditRect().y = 0;
            }
            if (getEditRect().x + getEditRect().width > previewPanel.getAreaSize().x)
            {
              getEditRect().width = previewPanel.getAreaSize().x - getEditRect().x;
            }
            if (getEditRect().y + getEditRect().height > previewPanel.getAreaSize().y)
            {
              getEditRect().height = previewPanel.getAreaSize().y - getEditRect().y;
            }
            this.applyEditRectoToSet();
            break;
          }
          case movingSet:
          {
            AffineTransform tr = this.previewPanel.getMmToPxTransform();
            Point2D.Double d = new Point2D.Double(diff.x, diff.y);
            tr.createInverse().deltaTransform(d, d);
            this.moveSet(d.x, d.y);
            this.previewPanel.repaint();
            break;
          }
          case movingViewport:
          {
            JViewport vp = (JViewport) this.previewPanel.getParent();
            Point loc = vp.getViewPosition();
            MouseEvent cur = SwingUtilities.convertMouseEvent(evt.getComponent(), evt, vp);
            loc.translate(lastMousePositionInViewport.x-cur.getX(), lastMousePositionInViewport.y-cur.getY());
            lastMousePositionInViewport = cur.getPoint();
            this.previewPanel.scrollRectToVisible(new Rectangle(loc, vp.getSize()));
            break;
          }
        }
      }
      catch (NoninvertibleTransformException ex)
      {
        Logger.getLogger(PreviewPanelKeyboardMouseHandler.class.getName()).log(Level.SEVERE, null, ex);
      }
      lastMousePosition = evt.getPoint();
    }
  }

  private void moveSet(double mmDiffX, double mmDiffY)
  {
    Rectangle2D bb = getSelectedSet().getBoundingBox();
    //make sure, we're not moving the bb out of the laser-area
    if (bb.getX() + mmDiffX < 0)
    {
      mmDiffX = -bb.getX();
    }
    if (bb.getY() + mmDiffY < 0)
    {
      mmDiffY = -bb.getY();
    }
    if (bb.getX() + bb.getWidth() + mmDiffX > previewPanel.getAreaSize().x)
    {
      mmDiffX = previewPanel.getAreaSize().x - (bb.getX() + bb.getWidth());
    }
    if (bb.getY() + bb.getHeight() + mmDiffY > previewPanel.getAreaSize().y)
    {
      mmDiffY = previewPanel.getAreaSize().y - (bb.getY() + bb.getHeight());
    }
    if (mmDiffX == 0 && mmDiffY == 0)
    {
      return;
    }
    AffineTransform tr = AffineTransform.getTranslateInstance(mmDiffX, mmDiffY);
    if (getSelectedSet().getTransform() != null)
    {
      tr.concatenate(getSelectedSet().getTransform());
    }
    getSelectedSet().setTransform(tr);
    this.previewPanel.setEditRectangle(new EditRectangle(bb));
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
        if (getEditRect() != null)
        {
          //Check for text cursor
          if (getEditRect().isRotateMode())
          {
            if (getEditRect().getParameterFieldBounds(ParameterField.ANGLE).contains(p))
            {
              cursor = Cursor.TEXT_CURSOR;
              break cursorcheck;
            }
          }
          else
          {
            for (ParameterField param : EditRectangle.ParameterField.values())
            {
              if (param != ParameterField.ANGLE && getEditRect().getParameterFieldBounds(param).contains(p))
              {
                cursor = Cursor.TEXT_CURSOR;
                break cursorcheck;
              }
            }
          }
          Button b = getEditRect().getButtonByPoint(p, this.previewPanel.getMmToPxTransform());
          if (b != null)
          {
            if (getEditRect().isRotateMode() && b == Button.ROTATE_BUTTON)
            {
              //TODO: Create rotate cursor
              cursor = Cursor.CROSSHAIR_CURSOR;
              break cursorcheck;
            }
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
          Rectangle2D e = Helper.transform(bb, this.previewPanel.getMmToPxTransform());
          if (e.contains(p))
          {
            cursor = this.getEditRect() == null ? Cursor.HAND_CURSOR : Cursor.MOVE_CURSOR;
            break cursorcheck;
          }
        }
      }
    }
    this.previewPanel.setCursor(Cursor.getPredefinedCursor(cursor));
  }
}
