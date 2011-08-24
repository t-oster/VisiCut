package com.t_oster.visicut.gui.beans;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JPanel;

/**
 * This class is a helper to create platform independant file
 * drop support.
 * Extend this class and implement the fileDropped method how you
 * like
 * 
 * @author thommy
 */
public abstract class FilesDropPanel extends JPanel implements DropTargetListener {

    private DropTarget dropTarget = new DropTarget(this, this);

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void drop(DropTargetDropEvent event) {
        Transferable transferable = event.getTransferable();

        event.acceptDrop(DnDConstants.ACTION_MOVE);

        DataFlavor uriListFlavor = null;
        try {
            uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List data = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                this.filesDropped(data);
            } else if (transferable.isDataFlavorSupported(uriListFlavor)) {
                String data = (String) transferable.getTransferData(uriListFlavor);
                List files = textURIListToFileList(data);
                this.filesDropped(files);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public abstract void filesDropped(List files);

    private List textURIListToFileList(String data) {
        List list = new ArrayList(1);
        for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
            String s = st.nextToken();
            if (s.startsWith("#")) {
                // the line is a comment (as per the RFC 2483)
                continue;
            }
            try {
                URI uri = new URI(s);
                File file = new File(uri);
                list.add(file);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
