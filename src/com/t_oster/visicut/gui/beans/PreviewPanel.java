package com.t_oster.visicut.gui.beans;

import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the Panel which provides the Preview
 * of the current LaserJob
 * 
 * @author thommy
 */
public class PreviewPanel extends FilesDropPanel {

    protected SVGRoot SVGRootElement = null;
    protected File droppedFile = null;
    public static final String PROP_DROPPEDFILE = "droppedFile";


    /**
     * Get the value of droppedFile
     *
     * @return the value of droppedFile
     */
    public File getDroppedFile() {
        return droppedFile;
    }

    /**
     * Set the value of droppedFiles
     *
     * @param droppedFiles new value of droppedFiles
     */
    public void setDroppedFile(File droppedFile) {
        File oldDroppedFile = this.droppedFile;
        this.droppedFile = droppedFile;
        this.firePropertyChange(PROP_DROPPEDFILE, oldDroppedFile, droppedFile);
    }


    /**
     * Get the value of SVGRootElement
     *
     * @return the value of SVGRootElement
     */
    public SVGRoot getSVGRootElement() {
        return SVGRootElement;
    }

    /**
     * Set the value of SVGRootElement
     *
     * @param SVGRootElement new value of SVGRootElement
     */
    public void setSVGRootElement(SVGRoot SVGRootElement) {
        this.SVGRootElement = SVGRootElement;
        this.repaint();
    }

    @Override
    public void filesDropped(List files) {
        if (files != null && !files.isEmpty())
        {
            Object o = files.get(0);
            File f = null;
            if (o instanceof File)
            {
                f = (File) o;
            }
            else if (o instanceof String)
            {
                f = new File((String) o);
            }
            if (f != null && f.exists())
            {
                this.setDroppedFile(f);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        SVGRoot root = this.getSVGRootElement();
        if (g instanceof Graphics2D) {
            Graphics2D gg = (Graphics2D) g;
            if (root != null) {
                try {
                    root.render(gg);
                } catch (SVGException ex) {
                    Logger.getLogger(PreviewPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
