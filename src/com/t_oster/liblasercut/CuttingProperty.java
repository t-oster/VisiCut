/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author oster
 */
public class CuttingProperty implements Cloneable{
    private int power = 20;
    private int speed = 100;
    private int frequency = 5000;
    private int focus = 0;
    
    public CuttingProperty(){
        
    }

    public CuttingProperty(int power, int speed, int frequency){
        this(power, speed, frequency, 0);
    }

    public CuttingProperty(int power, int speed, int frequency, int focus){
        this.power = power;
        this.speed = speed;
        this.frequency = frequency;
        this.focus = focus;
    }
    
    public void setPower(int power){
        this.power = power;
    }
    
    public int getPower(){
        return power;
    }
    
    public void setSpeed(int speed){
        this.speed = speed;
    }
    
    public int getSpeed(){
        return speed;
    }
    
    public void setFrequency(int frequency){
        this.frequency = frequency;
    }
    
    public int getFrequency(){
        return frequency;
    }

    public void setFocus(int focus){
        this.focus = focus;
    }

    public int getFocus(){
        return this.focus;
    }

    @Override
    public CuttingProperty clone(){
        return new CuttingProperty(power, speed, frequency, focus);
    }
}
