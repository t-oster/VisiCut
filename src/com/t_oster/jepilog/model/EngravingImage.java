/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import com.t_oster.liblasercut.EngravingProperty;

/**
 *
 * @author oster
 */
public class EngravingImage {
    private Rectangle rect;
    private EngravingProperty prop = null;
    
    public EngravingImage(Rectangle rect, EngravingProperty prop){
        this.rect = rect;
        this.prop = prop;
    }
   
    public EngravingImage(Rectangle rect){
        this(rect, null);
    }
    
    public Rectangle getRectangle(){
        return this.rect;
    }
    
    public EngravingProperty getEngravingProperty(){
        return this.prop;
    }
}
