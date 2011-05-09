/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 * @author thommy
 */
public class EpilogCutter implements LaserCutter{

    private static final int[] RESOLUTIONS=new int[]{300,600};
    private static final int BED_WIDTH = 1000;
    private static final int BED_HEIGHT = 500;
    
    private String hostname;
    private Socket connection;
    private InputStream in;
    private OutputStream out;
    
    public EpilogCutter(String hostname){
        
    }
    
    private void sendPjlHeader(LaserJob job){
        
    }
    
    private void connect(){
        
    }
    
    private void disconnect(){
        
    }
    
    private boolean isConnected(){
        return false;
    }
    
    public void sendJob(LaserJob job) {
        boolean wasConnected = isConnected();
        if (!wasConnected){
            connect();
        }
        sendPjlHeader(job);
        if (job.containsRaster()){
            sendRasterPCL(job);
        }
        if (job.containsVector()){
            sendVectorPCL(job);
        }
        sendPjlFooter(job);
        if (!wasConnected){
            disconnect();
        }
}

    private void sendPjlFooter(LaserJob job) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int[] getResolutions() {
        return RESOLUTIONS;
    }

    public int getBedWidth() {
        return BED_WIDTH;
    }

    public int getBedHeight() {
        return BED_HEIGHT;
    }

    private void sendRasterPCL(LaserJob job) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void sendVectorPCL(LaserJob job) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
}
