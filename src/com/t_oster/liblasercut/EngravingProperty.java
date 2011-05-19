/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author oster
 */
public class EngravingProperty {
    private int speed = 100;
    private int power = 50;
    
    public EngravingProperty(){
        
    }
    
    public EngravingProperty(int speed, int power){
        this.speed = speed;
        this.power = power;
    }
    
    public void setSpeed(int speed){
        this.speed = speed;
    }
    
    public int getSpeed(){
        return this.speed;
    }
    
    public void setPower(int power){
        this.power = power;
    }
    
    public int getPower(){
        return this.power;
    }
    
}
