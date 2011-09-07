package com.t_oster.visicut.gui.beans;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JComponent;

/**
 * This class is a helper to create platform independant file
 * drop support.
 * 
 * @author thommy
 */
public class FilesDropSupport implements DropTargetListener
{

  private DropTarget dropTarget;
  private JComponent component;

  public FilesDropSupport()
  {
  }

  public void setComponent(JComponent c)
  {
    if (this.dropTarget != null)
    {
      dropTarget.removeDropTargetListener(this);
    }
    this.component = c;
    this.dropTarget = new DropTarget(this.component, this);
  }

  public JComponent getComponent()
  {
    return this.component;
  }
  protected List<File> droppedFiles = null;
  public static final String PROP_DROPPEDFILES = "droppedFiles";

  /**
   * Get the value of droppedFiles
   *
   * @return the value of droppedFiles
   */
  public List<File> getDroppedFiles()
  {
    return droppedFiles;
  }

  /**
   * Set the value of droppedFiles
   *
   * @param droppedFiles new value of droppedFiles
   */
  public void setDroppedFiles(List<File> droppedFiles)
  {
    List<File> oldDroppedFiles = this.droppedFiles;
    this.droppedFiles = droppedFiles;
    propertyChangeSupport.firePropertyChange(PROP_DROPPEDFILES, oldDroppedFiles, droppedFiles);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public void dragEnter(DropTargetDragEvent dtde)
  {
  }

  public void dragOver(DropTargetDragEvent dtde)
  {
  }

  public void dropActionChanged(DropTargetDragEvent dtde)
  {
  }

  public void dragExit(DropTargetEvent dte)
  {
  }

  public void drop(DropTargetDropEvent event)
  {
    Transferable transferable = event.getTransferable();

    event.acceptDrop(DnDConstants.ACTION_MOVE);

    DataFlavor uriListFlavor = null;
    try
    {
      uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
    }
    catch (ClassNotFoundException e)
    {
      
    }

    try
    {
      if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
      {
        List data = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        this.filesDropped(data);
      }
      else
      {
        if (transferable.isDataFlavorSupported(uriListFlavor))
        {
          String data = (String) transferable.getTransferData(uriListFlavor);
          List files = textURIListToFileList(data);
          this.filesDropped(files);
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

  private void filesDropped(List files)
  {
    List<File> result = new LinkedList<File>();
    for (Object o : files)
    {
      if (o instanceof File && ((File) o).exists())
      {
        result.add((File) o);
      }
      else
      {
        if (o instanceof String)
        {
          File f = new File((String) o);
          if (f.exists())
          {
            result.add(f);
          }
        }
      }
    }
    this.setDroppedFiles(files);
  }

  private List textURIListToFileList(String data)
  {
    List list = new ArrayList(1);
    for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();)
    {
      String s = st.nextToken();
      if (s.startsWith("#"))
      {
        // the line is a comment (as per the RFC 2483)
        continue;
      }
      try
      {
        URI uri = new URI(s);
        File file = new File(uri);
        list.add(file);
      }
      catch (URISyntaxException e)
      {
        e.printStackTrace();
      }
      catch (IllegalArgumentException e)
      {
        e.printStackTrace();
      }
    }
    return list;
  }
}
