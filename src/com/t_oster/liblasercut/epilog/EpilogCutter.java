/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.liblasercut.*;
import com.t_oster.util.Util;
import java.awt.Color;
import java.awt.image.RenderedImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import org.apache.fop.render.pcl.PCLGenerator;

/**
 *
 * @author thommy
 */
public class EpilogCutter extends LaserCutter {

    public static boolean SIMULATE_COMMUNICATION = false;
    /* Resolutions in DPI */
    private static final int[] RESOLUTIONS = new int[]{300, 500, 600, 1000};
    private static final double BED_WIDTH = 600;//Bed width in mm
    private static final double BED_HEIGHT = 300;//Bed height in mm
    private String hostname;
    private Socket connection;
    private InputStream in;
    private OutputStream out;

    public EpilogCutter(String hostname) {
        this.hostname = hostname;
    }

    private void waitForResponse(int expected) throws IOException, Exception {
        waitForResponse(expected, 3);
    }

    private void waitForResponse(int expected, int timeout) throws IOException, Exception {
        if (SIMULATE_COMMUNICATION) {
            System.out.println("Response simulated");
            return;
        }
        int result = -1;
        out.flush();
        System.out.println("Waiting for response...");
        for (int i = 0; i < timeout * 10; i++) {
            if (in.available() > 0) {
                result = in.read();
                System.out.println("Got Response: " + result);
                if (result == -1) {
                    throw new IOException("End of Stream");
                }
                if (result != expected) {
                    throw new Exception("unexpected Response: " + result);
                }
                return;
            } else {
                Thread.sleep(100 * timeout);
            }
        }
        throw new Exception("Timeout");

    }

    private void initJob(LaserJob job) throws Exception {

        String localhost = java.net.InetAddress.getLocalHost().getHostName();
        //Use PrintStream for getting prinf methotd
        //and autoflush because we're watiting for responses
        PrintStream out = new PrintStream(this.out, true, "US-ASCII");
        out.print("\002\n");
        System.out.println("sending init");
        waitForResponse(0);
        System.out.println("got response");
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        PrintStream stmp = new PrintStream(tmp, true, "US-ASCII");
        stmp.printf("H%s\n", localhost);
        stmp.printf("P%s\n", job.getUser());
        stmp.printf("J%s\n", job.getTitle());
        stmp.printf("ldfA%s%s\n", job.getName(), localhost);
        stmp.printf("UdfA%s%s\n", job.getName(), localhost);
        stmp.printf("N%s\n", job.getTitle());

        out.printf("\002%d cfA%s%s\n", tmp.toString("US-ASCII").length(), job.getName(), localhost);
        System.out.println("sending jobheader");
        waitForResponse(0);
        out.print(tmp.toString("US-ASCII"));
        out.append((char) 0);
        System.out.println("sending job");
        waitForResponse(0);
        System.out.println("got jobresponse");
    }

    private String generatePjlHeader(LaserJob job) throws UnsupportedEncodingException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");

        /* Print the printer job language header. */
        out.printf("\033%%-12345X@PJL JOB NAME=%s\r\n", job.getTitle());
        out.printf("\033E@PJL ENTER LANGUAGE=PCL\r\n");
        /* Set autofocus off. */
        out.printf("\033&y0A");
        /* FIXME unknown purpose. */
        out.printf("\033&y0C");

        /* ALSO UNKNOWN */
        out.printf("\033&y0Z");

        /* Left (long-edge) offset registration.  Adjusts the position of the
         * logical page across the width of the page.
         */
        out.printf("\033&l0U");
        /* Top (short-edge) offset registration.  Adjusts the position of the
         * logical page across the length of the page.
         */
        out.printf("\033&l0Z");

        /* Resolution of the print. Number of Units/Inch*/
        out.printf("\033&u%dD", job.getResolution());
        /* X position = 0 */
        out.printf("\033*p0X");
        /* Y position = 0 */
        out.printf("\033*p0Y");
        /* PCL/RasterGraphics resolution. */
        out.printf("\033*t%dR", job.getResolution());

        
        try {
            return result.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
            return result.toString();
        }
    }

    private String generatePjlFooter() throws UnsupportedEncodingException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");

        /* Footer for printer job language. */
        /* Reset */
        out.printf("\033E");
        /* Exit language. */
        out.printf("\033%%-12345X");
        /* End job. */
        out.printf("@PJL EOJ \r\n");
        try {
            return result.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
            return result.toString();
        }
    }

    private void sendPjlJob(LaserJob job) throws UnknownHostException, UnsupportedEncodingException, IOException, Exception {

        String localhost = java.net.InetAddress.getLocalHost().getHostName();
        /* Generate complete PJL Job */
        ByteArrayOutputStream pjlJob = new ByteArrayOutputStream();
        PrintStream wrt = new PrintStream(pjlJob, true, "US-ASCII");

        wrt.append(generatePjlHeader(job));
        //if (job.containsRaster()) {
        wrt.write(generateRasterPCL(job, job.getRasterPart()));
        //}
        //if (job.containsVector()) {
        wrt.append(generateVectorPCL(job, job.getVectorPart()));
        //}
        wrt.append(generatePjlFooter());
        /* Pad out the remainder of the file with 0 characters. */
        for (int i = 0; i < 4096; i++) {
            wrt.append((char) 0);
        }
        wrt.flush();

        //dump pjl into file for debugging
        //new PrintStream(new FileOutputStream(new File("/tmp/last.pjl")), true, "US-ASCII").print(pjlJob.toString("US-ASCII"));

        /*
        pjlJob = new ByteArrayOutputStream();
        FileInputStream is = new FileInputStream(new File("/tmp/working.pjl"));
        while(is.available()>0){
        pjlJob.write(is.read());
        }
        is.close
         * 
         */
        //Use PrintStream for getting prinf methotd
        //and autoflush because we're watiting for responses
        PrintStream out = new PrintStream(this.out, true, "US-ASCII");
        /* Send the Job length and name to the queue */
        out.printf("\003%d dfA%s%s\n", pjlJob.toString("US-ASCII").length(), job.getName(), localhost);
        System.out.println("Sending dataFileHeader");
        waitForResponse(0);
        System.out.println("Accepted. Sending Data File");
        /* Send the real PJL Job */
        out.write(pjlJob.toByteArray());
        //out.append((char) 0);
        System.out.println("Data file sent");
        waitForResponse(0);
    }

    private void connect() throws IOException {
        if (SIMULATE_COMMUNICATION) {
            out = System.out;
        } else {
            connection = new Socket(hostname, 515);
            in = new BufferedInputStream(connection.getInputStream());
            out = new BufferedOutputStream(connection.getOutputStream());
        }
    }

    private void disconnect() throws IOException {
        if (!SIMULATE_COMMUNICATION) {
            in.close();
            out.close();
        }
    }

    private boolean isConnected() {
        return (connection != null && connection.isConnected());
    }

    private void checkJob(LaserJob job) throws IllegalJobException {
        boolean pass = false;
        for (int i : this.getResolutions()) {
            if (i == job.getResolution()) {
                pass = true;
                break;
            }
        }
        if (!pass) {
            throw new IllegalJobException("Resoluiton of " + job.getResolution() + " is not supported");
        }
        if (job.containsVector()) {
            double w = Util.px2mm(job.getVectorPart().getWidth(), job.getResolution());
            double h = Util.px2mm(job.getVectorPart().getHeight(), job.getResolution());

            if (w > this.getBedWidth() || h > this.getBedHeight()) {
                throw new IllegalJobException("The Job is too big (" + w + "x" + h + ") for the Laser bed (" + this.getBedHeight() + "x" + this.getBedHeight() + ")");
            }
        }
    }

    //TODO: Add Timeout
    public void sendJob(LaserJob job) throws IllegalJobException, Exception {
        checkJob(job);
        boolean wasConnected = isConnected();
        if (!wasConnected) {
            connect();
        }
        initJob(job);
        sendPjlJob(job);
        if (!wasConnected) {
            disconnect();
        }
        System.out.println("Successfully disconnected");
    }

    public List<Integer> getResolutions() {
        List<Integer> result = new LinkedList();
        for (int r : RESOLUTIONS) {
            result.add(r);
        }
        return result;
    }

    public double getBedWidth() {
        return BED_WIDTH;
    }

    public double getBedHeight() {
        return BED_HEIGHT;
    }

    /**
     * Encodes the given line of the given image in TIFF Packbyte encoding
     */
    public List<Byte> encode(List<Byte> line) {
        int idx = 0;
        int r = line.size();
        List<Byte> result = new LinkedList<Byte>();
        while (idx < r) {
            int p;
            p = idx+1;
            while (p < r && p < idx + 128 && line.get(p) == line.get(idx)) {
                p++;
            }
            if (p - idx >= 2) {
                // run length
                result.add((byte) (1-(p - idx)));
                result.add((byte) line.get(idx));
                idx = p;
            } else {
                p = idx;
                while (p < r && p < idx + 127
                        && (p + 1 == r || line.get(p)
                        != line.get(p + 1))) {
                    p++;
                }
                result.add((byte) (p - idx - 1));
                while (idx < p) {
                    result.add((byte) (line.get(idx++)));
                }
            }
        }
        return result;
    }

    private byte[] generateRasterPCL(LaserJob job, RasterPart rp) throws UnsupportedEncodingException, IOException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");
        /* Raster Orientation: Printed in current direction */
        out.printf("\033*r0F");
        /* Raster power */
        out.printf("\033&y%dP", 100);//Full power, scaling is done seperate
        /* Raster speed */
        out.printf("\033&z%dS", 100);//TODO real speed
        /* unknown */
        out.printf("\033&y0A");

        out.printf("\033*r%dT", rp != null ? rp.getHeight() : 10);//height);
        out.printf("\033*r%dS", rp != null ? rp.getWidth() : 10);//width);
        /* Raster compression:
         *  2 = TIFF encoding
         *  7 = TIFF encoding, 3d-mode,
         *
         * Wahrscheinlich:
         * 2M = Bitweise, also 1=dot 0=nodot (standard raster)
         * 7MLT = Byteweise 0= no power 100=full power (3d raster)
         */
        out.printf("\033*b%dMLT", 7);
        /* Raster direction (1 = up, 0=down) */
        out.printf("\033&y%dO", 0);
        /* start at current position */
        out.printf("\033*r1A");

        for (int i = 0; rp != null && i < rp.getRasterCount(); i++) {
            Point sp = rp.getRasterStart(i);
            boolean leftToRight = true;
            for (int y = 0; y < rp.getRasterHeight(i); y++) {
                
                List<Byte> line = rp.getRasterLine(i, y);
                //Remove leading zeroes, but keep track of the offset
                int jump = 0;
                
                while (line.size() > 0 && line.get(0) == 0){
                    line.remove(0);
                    jump++;
                }
                if (line.size()>0){
                    out.printf("\033*p%dX", sp.x + jump);
                    out.printf("\033*p%dY", sp.y + y);
                    if (leftToRight) {
                        out.printf("\033*b%dA", line.size());
                    } else {
                        out.printf("\033*b%dA", -line.size());
                        Collections.reverse(line);
                    }
                    line = encode(line);
                    int len = line.size();
                    int pcks = len / 8;
                    if (len % 8 > 0) {
                        pcks++;
                    }
                    out.printf("\033*b%dW", pcks * 8);
                    for (byte s : line) {
                        out.write(s);
                    }
                    for (int k = 0; k < 8 - (len % 8); k++) {
                        out.write((byte) 128);
                    }
                    leftToRight = !leftToRight;
                }
            }

        }
        out.printf("\033*rC");       // end raster
        out.write((char) 26);
        out.write((char) 4); // some end of file markers
        //PrintStream ps = new PrintStream((new FileOutputStream(new File("/tmp/rasterdump.hex"))));
        //ps.write(result.toByteArray());
        //ps.close();
        return result.toByteArray();
    }

    private String generateVectorPCL(LaserJob job, VectorPart vp) throws UnsupportedEncodingException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");

        out.printf("\033E@PJL ENTER LANGUAGE=PCL\r\n");
        /* Page Orientation */
        out.printf("\033*r0F");
        out.printf("\033*r%dT", vp == null ? 500 : vp.getHeight());// if not dummy, then job.getHeight());
        out.printf("\033*r%dS", vp == null ? 500 : vp.getWidth());// if not dummy then job.getWidth());
        out.printf("\033*r1A");
        out.printf("\033*rC");
        out.printf("\033%%1B");// Start HLGL
        out.printf("IN;PU0,0;");

        if (vp != null) {
            int sx = job.getStartX();
            int sy = job.getStartY();
            VectorCommand.CmdType lastType = null;
            for (VectorCommand cmd : vp.getCommandList()) {
                if (lastType != null && lastType == VectorCommand.CmdType.LINETO && cmd.getType() != VectorCommand.CmdType.LINETO) {
                    out.print(";");
                }
                switch (cmd.getType()) {
                    case SETFREQUENCY: {
                        out.printf("XR%04d;", cmd.getFrequency());
                        break;
                    }
                    case SETPOWER: {
                        out.printf("YP%03d;", cmd.getPower());
                        break;
                    }
                    case SETSPEED: {
                        out.printf("ZS%03d;", cmd.getSpeed());
                        break;
                    }
                    case MOVETO: {
                        out.printf("PU%d,%d;", cmd.getX() - sx, cmd.getY() - sy);
                        break;
                    }
                    case LINETO: {
                        if (lastType == null || lastType != VectorCommand.CmdType.LINETO) {
                            out.printf("PD%d,%d", cmd.getX() - sx, cmd.getY() - sy);
                        } else {
                            out.printf(",%d,%d", cmd.getX() - sx, cmd.getY() - sy);
                        }
                        break;
                    }
                }
                lastType = cmd.getType();
            }

            //Pen up and goto 0,0
            out.printf("PU0,0;");  // start HLGL, and pen up, end
        }

        System.out.append(result.toString());
        try {
            return result.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
            return result.toString();
        }
    }
}
