/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import com.kitfox.svg.RenderableElement;
import com.t_oster.liblasercut.EngravingProperty;
import com.t_oster.util.Tuple;

/**
 *
 * @author thommy
 */
public class EngravingImage extends Tuple<RenderableElement, EngravingProperty>{
    public EngravingProperty getProperty(){
        return this.getB();
    }
    public RenderableElement getElement(){
        return this.getA();
    }
}
