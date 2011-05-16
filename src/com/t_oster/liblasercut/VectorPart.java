/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class VectorPart {
  
    private int curFRQ;
    private int curPWR;
    private int curSPD;
    private int maxX;
    private int maxY;
    private int minX;
    private int minY;
    
    private List<VectorCommand> commands;
    
    public VectorPart(int initialFRQ,int initialPWR, int initialSPD){
        curFRQ = initialFRQ;
        curPWR = initialPWR;
        curSPD = initialSPD;
        commands = new LinkedList<VectorCommand>();
        this.setPower(curPWR);
        this.setSpeed(curSPD);
        this.setFrequency(curFRQ);
    }
    
    public VectorCommand[] getCommandList(){
        return commands.toArray(new VectorCommand[0]);
    }
    
    public void setSpeed(int speed){
        commands.add(new VectorCommand(VectorCommand.CmdType.SETSPEED, speed));
    }
    
    public void setPower(int power){
        commands.add(new VectorCommand(VectorCommand.CmdType.SETPOWER, power));
    }
    
    public void setFrequency(int frequency){
        commands.add(new VectorCommand(VectorCommand.CmdType.SETFREQUENCY, frequency));
    }
    
    private void checkMin(int x, int y)
    {
        if (x<minX){
            minX=x;
        }
        if (y<minY){
            minY=y;
        }
    } 
    private void checkMax(int x, int y){
        if (x>maxX){
            maxX=x;
        }
        if (y>maxY){
            maxY=y;
        }
    }
    
    public void circle(int x, int y, int radius){
        commands.add(new VectorCommand(VectorCommand.CmdType.CIRCLE, x, y, radius));
        checkMin(x-radius,y-radius);
        checkMax(x+radius,y+radius);
    }
    
    public void polygon(int[] x, int[] y){
        commands.add(new VectorCommand(VectorCommand.CmdType.POLYGON, x, y));
        for (int i=0;i<x.length;i++){
            checkMin(x[i],y[i]);
            checkMax(x[i],y[i]);
        }
    }
    
    public void line(int x1, int y1, int x2, int y2){
        this.polygon(new int[]{x1,x2}, new int[]{y1,y2});
    }
    
    public int getWidth(){
        return maxX-minX;
    }
    public int getHeight(){
        return maxY-minY;
    }
}
