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
    
    public void moveto(int x, int y){
        commands.add(new VectorCommand(VectorCommand.CmdType.MOVETO, x, y));
        checkMin(x,y);
        checkMax(x,y);
    }
    
    public void lineto(int x, int y){
        commands.add(new VectorCommand(VectorCommand.CmdType.LINETO, x, y));
        checkMin(x,y);
        checkMax(x,y);
    }
    
    public int getWidth(){
        return maxX-minX;
    }
    public int getHeight(){
        return maxY-minY;
    }
}
