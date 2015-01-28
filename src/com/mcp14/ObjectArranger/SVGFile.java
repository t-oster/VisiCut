package com.mcp14.ObjectArranger;


import com.mcp14.ObjectArranger.ObjectArranger;
import com.kitfox.svg.app.beans.*;
import java.awt.Point;
import java.net.URI;
import java.util.LinkedList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Marcel
 */
    // The SVGFile Code representation
    public class SVGFile
    {
        public final URI uri;
        public LinkedList<Point> positions;
        public int width;
        public int height;
        public LinkedList<SVGLabel> labels;
        private SVGIcon icon;

        public SVGFile(URI svgUri)
        {
            uri = svgUri;
            positions = new LinkedList<Point>();
            labels = new LinkedList<SVGLabel>();
            positions.add(new Point(0,0));

            icon = new SVGIcon(); 
            icon.setSvgURI(uri);
            SVGLabel label = new SVGLabel(icon,this);
            label.setBounds(0, 0, icon.getIconWidth(), icon.getIconHeight());
            labels.add(label);
            
            width = icon.getIconWidth();
            height = icon.getIconHeight();
        }
        
        public void updateLabelList(){
            for (Point position : positions){
                SVGLabel label = new SVGLabel(icon, this);
                label.setBounds(position.x, position.y, width, height);
                labels.add(label);
            }
        }
        
        public void setPosition(int x, int y){
            SVGLabel sLabel = new SVGLabel(icon, this);
            sLabel.setBounds(x,y,width,height);
            labels.add(sLabel);
        }
        
        public int getFileArea(){
            return (width*height);
        }
        
        public void renderToIconPanel(ObjectArranger.IconPanel iconPanel, int x, int y){
            SVGLabel label = new SVGLabel(icon, this);
            label.setBounds(x, y, width, height);
            iconPanel.add(label);
        }
    }
