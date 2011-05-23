/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.util;

import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.xml.StyleAttribute;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author oster
 */
public class Util {
    public static double inch2mm(double inch){
        return inch*25.4;
    }
    public static double mm2inch(double mm){
        return mm/25.4;
    }
    public static double px2mm(double px, double dpi){
        return inch2mm(px/dpi);
    }
    public static double mm2px(double mm, double dpi){
        return mm2inch(mm)*dpi;
    }
    /**
     * Returns true iff the given objects are not equal
     * This method is used to avoid null checks
     * @param a
     * @param b
     * @return 
     */
    public static boolean differ(Object a, Object b){
        if (a==null ^ b==null){
            return true;
        }
        else if (a==null && b==null){
            return false;
        }
        else{
            return !a.equals(b);
        }
    }

    /**
     * This 
     * applies all transformations in the Path of the SVGShape
     * and returns the Transformed Shape, which can be displayed
     * or printed on the position it appears in the original image.
     * @param selectedSVGElement
     * @return 
     */
    public static Shape extractTransformedShape(ShapeElement s) throws SVGException {
        if (s != null){
            List first = s.getPath(null); 
            //Track all Transformations on the Path of the Elemenent
            AffineTransform tr = new AffineTransform();
            Object elem = first.get(first.size() - 1);
            for (Object o:first){
                if (o instanceof SVGElement){
                    Object sty = ((SVGElement) o).getPresAbsolute("transform");
                    if (sty != null && sty instanceof StyleAttribute){
                        StyleAttribute style = (StyleAttribute) sty;
                        tr.concatenate(SVGElement.parseSingleTransform(style.getStringValue()));
                    }
                }
            }
            return tr.createTransformedShape(s.getShape());
        }
        return null;
    }
}
