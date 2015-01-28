/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcp14.ObjectArranger;

import com.mcp14.Autoarrange.AutoArrange;
import com.kitfox.svg.*;
import java.net.MalformedURLException;
import java.net.*;
import java.util.LinkedList;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import com.kitfox.svg.app.beans.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.mcp14.Provider.HoldValues;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcel
 */
public class ObjectArranger {

    // The Button opens an OpenFileDialog and adds the chosen files to
    // the SVGFileList
    static class OpenButton extends JButton implements ActionListener{
        String outString;
        OpenButton(){
            this.setText("Import SVG Files");
            this.addActionListener(this);
            outString = null;
        }

        public void actionPerformed(ActionEvent e) { 
            JFileChooser fc = new JFileChooser();
            if(e.getSource() == this){ 
                //Create a file chooser
                fc.setMultiSelectionEnabled(true);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("SVG file", "svg");
                fc.setAcceptAllFileFilterUsed(false);
                fc.setFileFilter(filter);
                int returnVal = fc.showOpenDialog(frame);
                if(outString != null){
                    fc.setCurrentDirectory(new File(outString));
                }
                if(returnVal == JFileChooser.APPROVE_OPTION){
                    File[] files = fc.getSelectedFiles();
                    AddFilesToSVGList(files);
                    IconPanel iconPanel = new IconPanel(laserBedWidth,laserBedHeight);
                    iconPanels.add(iconPanel);
                    iconPanel.removeAll();
                    for (SVGFile svgFile : svgList){                       
                        svgFile.renderToIconPanel(iconPanel, 0,0);
                    }
                    showPreviewPanel();
                }
                    outString = fc.getSelectedFile().getPath();
                }
            }
        }
    
    static class LeftButton extends JButton implements ActionListener{
        String outString;
        LeftButton(){
            this.setText("<");
            this.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) { 
            
        }
    }
    
    static class RightButton extends JButton implements ActionListener{
    String outString;
    RightButton(){
        this.setText(">");
        this.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) { 
        showNextPanel();
    }
}

    static class ArrangeButton extends JButton implements ActionListener{
        String outString;
        ArrangeButton(){
            this.setText("Arrange");
            this.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) { 
            try {
                SVGFile largeFile = getLargestSVGFile(svgList);
                AutoArrange.start(svgList, laserBedWidth, laserBedHeight);
                for(Map.Entry<AutoArrange.BinNumber, Set<HoldValues>> entry : AutoArrange.allValues.entrySet()) {
                    AutoArrange.BinNumber bn = entry.getKey();
                    Set<HoldValues> hValues = entry.getValue();
                    System.out.println("Printing in OBJARR Bin = " + bn.getBinNumber() + " List = ");
                    IconPanel iconPanel = new IconPanel(laserBedWidth,laserBedHeight);
                    iconPanels.add(iconPanel);
                    for (HoldValues hv : hValues){
                        System.out.println("Object ID : " + hv.getObjectID() + " CoordX : " + hv.getX() + " CoordY : " + hv.getY() + " Rotation :" + hv.getObjectRotation());
                        SVGFile currentSVG = svgList.get(hv.getObjectID()-1);
                        currentSVG.renderToIconPanel(iconPanel, (int) hv.getX(),(int) hv.getY());
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ObjectArranger.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            showNextPanel();
            this.setEnabled(false);
        }
        
        private SVGFile getLargestSVGFile(LinkedList<SVGFile> svgList){
            SVGFile largestFile = null;
            for ( int i = 0; i < svgList.size(); i++){
                SVGFile iFile = svgList.get(i);
                for (int j = i; j < svgList.size(); j++){
                    SVGFile jFile = svgList.get(j);
                    if (iFile.getFileArea() > jFile.getFileArea())
                        largestFile = iFile;
                    else
                        largestFile = jFile;        
                }
            }
            return largestFile;
        }
    }

    // The panel that renders the SVGFiles and implements the dragging
    // of the SVGFiles
    public static class IconPanel extends JPanel implements MouseMotionListener, MouseListener
    {
        public static final long serialVersionUID = 0;
        boolean select = false;
        Component component;
        int xDifference;
        int yDifference;


        public IconPanel(int width, int height)
        {   

            setPreferredSize(new Dimension(width, height));
            this.setLayout(null);
            this.setBounds(10,10,width, height);
            this.addMouseMotionListener(this);
            this.addMouseListener(this);
            xDifference = 0;
            yDifference = 0;
            component = null;

        }

        public void paintComponent(Graphics g)
        {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                if( select ){
                    Graphics2D g2 = (Graphics2D) g;
                    final float dash1[] = {10.0f};
                    final BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.f, dash1, 0.0f);
                    g2.setColor(Color.BLACK);
                    g2.setStroke(dashed);
//                    g2.fillRect(component.getWidth()/2, component.getY(), 10,10);
//                    g2.fillRect(component.getX(), component.getHeight()/2, 10,10);
//                    g2.fillRect(component.getX() + component.getWidth(), component.getHeight()/2, 10,10);
//                    g2.drawRect(component.getWidth()/2, component.getY()+component.getHeight(), 10,10);
                    g2.drawRoundRect(component.getX(),component.getY(),component.getWidth(), component.getHeight(), 10, 10);
                }
        }


        public void mouseDragged(MouseEvent e) {
            if (component != this && component != null){
                
                if (select){
                    select = false;
                    repaint();
                }
                
                component.setLocation(e.getX()-xDifference, e.getY()-yDifference);
                //x and y condition for the border of the canvas
                if (isXInBounds(component) == 0 && isYInBounds(component) == 0 )
                    component.setLocation(0, 0);
                if (isXInBounds(component) == 1 && isYInBounds(component) == 1 )
                    component.setLocation(this.getWidth() - component.getWidth(), this.getHeight() - component.getHeight());
                if (isXInBounds(component) == 0 && isYInBounds(component) == 1 )
                    component.setLocation(0, this.getHeight() - component.getHeight());
                if (isXInBounds(component) == 1 && isYInBounds(component) == 0 )
                    component.setLocation(this.getWidth() - component.getWidth(), 0);
                if (isXInBounds(component) == 0 && isYInBounds(component) == -1 )
                    component.setLocation(0, component.getY());
                if (isXInBounds(component) == 1 && isYInBounds(component) == -1 )
                    component.setLocation(this.getWidth() - component.getWidth(), component.getY());
                if (isXInBounds(component) == -1 && isYInBounds(component) == 0 )
                    component.setLocation(component.getX(), 0);
                if (isXInBounds(component) == -1 && isYInBounds(component) == 1 )
                    component.setLocation(component.getX(), this.getHeight() - component.getHeight());
            }
        }
        //function to determine if component is in canvas X boundary
        private int isXInBounds(Component component){
            if(component.getX() < 0)
                return 0;
            else if ((component.getX() + component.getWidth()) > this.getWidth()) {
                return 1;
            } 
            else {
                return -1;
            }
        }
        //function to determine if component is in canvas Y boundary
        private int isYInBounds(Component component){
            if(component.getY() < 0)
                return 0;
            else if ((component.getY() + component.getHeight()) > this.getHeight()) {
                return 1;
            } 
            else {
                return -1;
            }
        }
        @Override
        public void mouseMoved(MouseEvent e) {

        }
        @Override
        public void mousePressed(MouseEvent e){
            component = this.findComponentAt(e.getPoint());
            xDifference = e.getX()-component.getX();
            yDifference = e.getY()-component.getY();
            if (component != null && component != this) {
                select = true;
                this.repaint();
            }
        }
        @Override
        public void mouseClicked(MouseEvent e){

        }
        @Override
        public void mouseEntered(MouseEvent e){

        }
        @Override
        public void mouseExited(MouseEvent e){

        }
        @Override
        public void mouseReleased(MouseEvent e){
            if (component != null && component != this){
                SVGLabel label = (SVGLabel)component;
                //label.svgFile.positions.get(positionCount).x = label.getX();
                //label.svgFile.positions.get(positionCount).y = label.getY();
                select = false;
            }
        }
    }

    // The frame for the whole App
    static class SVGFrame extends javax.swing.JFrame
    {
        public static final long serialVersionUID = 0;

        /** Creates new form SVGIconDemo */
        public SVGFrame(JPanel iconPanel)
        {

            this.getContentPane().add(iconPanel, BorderLayout.CENTER);
            JPanel btnPanel = new JPanel();
            btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.LINE_AXIS));
            btnPanel.setVisible(true);

            OpenButton oButton = new OpenButton();
            ArrangeButton aButton = new ArrangeButton();
            LeftButton lButton = new LeftButton();
            RightButton rButton = new RightButton();
            
            btnPanel.add(oButton,BorderLayout.WEST);
            btnPanel.add(aButton,BorderLayout.WEST);
            btnPanel.add(lButton,BorderLayout.EAST);
            btnPanel.add(rButton,BorderLayout.EAST);
            this.getContentPane().add(btnPanel,BorderLayout.SOUTH);
        }
    }

    // Method that adds the Files to the static Linked List of the class
    // by taking the filePath and using the SVGSalamander Lib
    static void AddFilesToSVGList(File[] files){
        SVGUniverse svgUniverse = new SVGUniverse();
        if (svgList == null){
            svgList = new LinkedList<SVGFile>();
        }
        for(File file : files){
            URL url = null;
            try{
            url = new URL("file:"+file.getAbsolutePath());}
             catch (MalformedURLException e){
                e.printStackTrace();
            }
            if(url != null){
            URI uri = svgUniverse.loadSVG(url);
            SVGFile svgFile = new SVGFile(uri);
            svgList.add(svgFile);
            }
        }

    }
    
    static void showPreviewPanel(){
        IconContainerPanel.add(iconPanels.get(0));
        currentPanel = iconPanels.get(0);
        frame.repaint();
    }
    
    static void showNextPanel(){
        IconContainerPanel.removeAll();
        positionCount = (positionCount + 1) % iconPanels.size();
        IconContainerPanel.add(iconPanels.get(positionCount));
        frame.repaint();
    }

    static JFrame frame;
    static JPanel IconContainerPanel;
    static LinkedList<SVGFile> svgList;
    static int positionCount, laserBedWidth, laserBedHeight;
    static LinkedList<IconPanel> iconPanels;
    static IconPanel currentPanel;

    /**
     * @param args the command line arguments
     */
    public static HashMap<AutoArrange.BinNumber, Set<HoldValues>> objectArrange(int width, int height) {
        positionCount = 0;
        laserBedWidth = width;
        laserBedHeight = height;
        IconContainerPanel = new JPanel();
        IconContainerPanel.setLayout(null);
        iconPanels = new LinkedList<IconPanel>();
        SVGFrame svgFrame = new SVGFrame(IconContainerPanel);
        frame = svgFrame;
        svgFrame.setSize(900, 700);
        svgFrame.setVisible(true);
        return AutoArrange.allValues;
    }
}
