/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.*;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.liblasercut.platform.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

/**
 *
 * @author thommy
 */
public class EpilogCutter extends LaserCutter {

    public static boolean SIMULATE_COMMUNICATION = false;
    public static final int NETWORK_TIMEOUT = 3000;
    /* Resolutions in DPI */
    private static final int[] RESOLUTIONS = new int[]{
        300, 500, 600, 1000
    };
    private static final double BED_WIDTH = 600;//Bed width in mm
    private static final double BED_HEIGHT = 300;//Bed height in mm
    private static final int MINFOCUS = -500;//Minimal focus value (not mm)
    private static final int MAXFOCUS = 500;//Maximal focus value (not mm)
    private static final double FOCUSWIDTH = 0.0252;//How much mm/unit the focus values are
    private String hostname;
    private Socket connection;
    private InputStream in;
    private OutputStream out;

    private int mm2focus(float mm) {
        return (int) (mm / FOCUSWIDTH);
    }

    private float focus2mm(int focus) {
        return (float) (focus * FOCUSWIDTH);
    }

    public EpilogCutter(String hostname) {
        this.hostname = hostname;
    }

    private void waitForResponse(int expected) throws IOException, Exception {
        waitForResponse(expected, 3);
    }

    private void waitForResponse(int expected, int timeout) throws IOException, Exception {
        if (SIMULATE_COMMUNICATION) {
            return;
        }
        int result = -1;
        out.flush();
        for (int i = 0; i < timeout * 10; i++) {
            if (in.available() > 0) {
                result = in.read();
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

    private byte[] generatePjlHeader(LaserJob job) throws UnsupportedEncodingException {
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
        return result.toByteArray();
    }

    private byte[] generatePjlFooter() throws UnsupportedEncodingException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");

        /* Footer for printer job language. */
        /* Reset */
        out.printf("\033E");
        /* Exit language. */
        out.printf("\033%%-12345X");
        /* End job. */
        out.printf("@PJL EOJ \r\n");
        return result.toByteArray();
    }

    private void sendPjlJob(LaserJob job, byte[] pjlData) throws UnknownHostException, UnsupportedEncodingException, IOException, Exception {
        String localhost = java.net.InetAddress.getLocalHost().getHostName();
        PrintStream out = new PrintStream(this.out, true, "US-ASCII");
        out.print("\002\n");
        waitForResponse(0);
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        PrintStream stmp = new PrintStream(tmp, true, "US-ASCII");
        stmp.printf("H%s\n", localhost);
        stmp.printf("P%s\n", job.getUser());
        stmp.printf("J%s\n", job.getTitle());
        stmp.printf("ldfA%s%s\n", job.getName(), localhost);
        stmp.printf("UdfA%s%s\n", job.getName(), localhost);
        stmp.printf("N%s\n", job.getTitle());
        out.printf("\002%d cfA%s%s\n", tmp.toByteArray().length, job.getName(), localhost);
        waitForResponse(0);
        out.write(tmp.toByteArray());
        out.append((char) 0);
        waitForResponse(0);
        /* Send the Job length and name to the queue */
        out.printf("\003%d dfA%s%s\n", pjlData.length, job.getName(), localhost);
        waitForResponse(0);
        /* Send the real PJL Job */
        out.write(pjlData);
        waitForResponse(0);
    }

    private void connect() throws IOException, SocketTimeoutException {
        if (SIMULATE_COMMUNICATION) {
            out = System.out;
        } else {
            connection = new Socket();
            connection.connect(new InetSocketAddress(hostname, 515), NETWORK_TIMEOUT);
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

            for (VectorCommand cmd : job.getVectorPart().getCommandList()) {
                if (cmd.getType() == VectorCommand.CmdType.SETFOCUS) {
                    if (mm2focus(cmd.getFocus()) > MAXFOCUS || (mm2focus(cmd.getFocus())) < MINFOCUS) {
                        throw new IllegalJobException("Illegal Focus value. This Lasercutter supports values between"
                                + focus2mm(MINFOCUS) + "mm to " + focus2mm(MAXFOCUS) + "mm.");
                    }
                }
            }
        }
        if (job.containsRaster()) {
            double w = Util.px2mm(job.getRasterPart().getWidth(), job.getResolution());
            double h = Util.px2mm(job.getRasterPart().getHeight(), job.getResolution());

            if (w > this.getBedWidth() || h > this.getBedHeight()) {
                throw new IllegalJobException("The Job is too big (" + w + "x" + h + ") for the Laser bed (" + this.getBedHeight() + "x" + this.getBedHeight() + ")");
            }
        }
        if (job.contains3dRaster()) {
            double w = Util.px2mm(job.getRaster3dPart().getWidth(), job.getResolution());
            double h = Util.px2mm(job.getRaster3dPart().getHeight(), job.getResolution());

            if (w > this.getBedWidth() || h > this.getBedHeight()) {
                throw new IllegalJobException("The Job is too big (" + w + "x" + h + ") for the Laser bed (" + this.getBedHeight() + "x" + this.getBedHeight() + ")");
            }
        }
    }

    //TODO: Add Timeout
    public void sendJob(LaserJob job) throws IllegalJobException, SocketTimeoutException, UnsupportedEncodingException, IOException, UnknownHostException, Exception {
        //Perform santiy checks
        checkJob(job);
        //Generate all the data
        byte[] pjlData = generatePjlData(job);
        //connect to lasercutter
        connect();
        //send job
        sendPjlJob(job, pjlData);
        //disconnect
        disconnect();
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
            p = idx + 1;
            while (p < r && p < idx + 128 && line.get(p) == line.get(idx)) {
                p++;
            }
            if (p - idx >= 2) {
                // run length
                result.add((byte) (1 - (p - idx)));
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

    private byte[] generateRaster3dPCL(LaserJob job, Raster3dPart rp) throws UnsupportedEncodingException, IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");
        LaserProperty curprop = new LaserProperty();
        if (rp != null) {
            /* Raster Orientation: Printed in current direction */
            out.printf("\033*r0F");
            /* Raster power */
            out.printf("\033&y%dP", curprop.getPower());
            /* Raster speed */
            out.printf("\033&z%dS", curprop.getSpeed());
            /* Focus */
            out.printf("\033&y%dA", mm2focus(curprop.getFocus()));

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
                LaserProperty newprop = rp.getLaserProperty(i);
                if (newprop.getPower() != curprop.getPower()) {
                    /* Raster power */
                    out.printf("\033&y%dP", curprop.getPower());
                }
                if (newprop.getSpeed() != curprop.getSpeed()) {
                    /* Raster speed */
                    out.printf("\033&z%dS", curprop.getSpeed());
                }
                if (newprop.getFocus() != curprop.getFocus()) {
                    /* Focus  */
                    out.printf("\033&y%dA", mm2focus(curprop.getFocus()));
                }
                curprop = newprop;
                Point sp = rp.getRasterStart(i);
                boolean leftToRight = true;
                for (int y = 0; y < rp.getRasterHeight(i); y++) {

                    List<Byte> line = rp.getInvertedRasterLine(i, y);
                    //Remove leading zeroes, but keep track of the offset
                    int jump = 0;

                    while (line.size() > 0 && line.get(0) == 0) {
                        line.remove(0);
                        jump++;
                    }
                    if (line.size() > 0) {
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
        }
        return result.toByteArray();
    }

    private byte[] generateRasterPCL(LaserJob job, RasterPart rp) throws UnsupportedEncodingException, IOException {

        LaserProperty curprop = new LaserProperty();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");
        /* Raster Orientation: Printed in current direction */
        out.printf("\033*r0F");
        /* Raster power */
        out.printf("\033&y%dP", curprop.getPower());
        /* Raster speed */
        out.printf("\033&z%dS", curprop.getSpeed());
        /* Focus */
        out.printf("\033&y%dA", mm2focus(curprop.getFocus()));

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
        out.printf("\033*b2M");
        /* Raster direction (1 = up, 0=down) */
        out.printf("\033&y%dO", 0);
        /* start at current position */
        out.printf("\033*r1A");

        for (int i = 0; rp != null && i < rp.getRasterCount(); i++) {
            LaserProperty newprop = rp.getLaserProperty(i);
            if (newprop.getPower() != curprop.getPower()) {
                /* Raster power */
                out.printf("\033&y%dP", curprop.getPower());
            }
            if (newprop.getSpeed() != curprop.getSpeed()) {
                /* Raster speed */
                out.printf("\033&z%dS", curprop.getSpeed());
            }
            if (newprop.getFocus() != curprop.getFocus()) {
                /* Focus  */
                out.printf("\033&y%dA", mm2focus(curprop.getFocus()));
            }
            curprop = newprop;
            Point sp = rp.getRasterStart(i);
            boolean leftToRight = true;
            for (int y = 0; y < rp.getRasterHeight(i); y++) {

                List<Byte> line = rp.getRasterLine(i, y);
                //Remove leading zeroes, but keep track of the offset
                int jump = 0;

                while (line.size() > 0 && line.get(0) == 0) {
                    line.remove(0);
                    jump++;
                }
                if (line.size() > 0) {
                    out.printf("\033*p%dX", sp.x + jump * 8);
                    out.printf("\033*p%dY", sp.y + y);
                    //TODO: vielleicht jump nach reverse???
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
        return result.toByteArray();
    }

    private byte[] generateVectorPCL(LaserJob job, VectorPart vp) throws UnsupportedEncodingException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result, true, "US-ASCII");

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
                    case SETFOCUS: {
                        out.printf("WF%d;", mm2focus(cmd.getFocus()));
                        break;
                    }
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
        }
        //Reset Focus to 0
        //out.printf("WF%d;", 0);
        return result.toByteArray();
    }

    private byte[] generatePjlData(LaserJob job) throws UnsupportedEncodingException, IOException {
        /* Generate complete PJL Job */
        ByteArrayOutputStream pjlJob = new ByteArrayOutputStream();
        PrintStream wrt = new PrintStream(pjlJob, true, "US-ASCII");

        wrt.write(generatePjlHeader(job));
        wrt.write(generateRasterPCL(job, job.getRasterPart()));
        wrt.write(generateRaster3dPCL(job, job.getRaster3dPart()));
        wrt.write(generateVectorPCL(job, job.getVectorPart()));
        wrt.write(generatePjlFooter());
        /* Pad out the remainder of the file with 0 characters. */
        for (int i = 0; i < 4096; i++) {
            wrt.append((char) 0);
        }
        wrt.flush();
        return pjlJob.toByteArray();
    }
}
