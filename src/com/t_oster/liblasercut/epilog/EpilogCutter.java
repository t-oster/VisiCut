/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.RasterPart;
import com.t_oster.liblasercut.VectorPart;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thommy
 */
public class EpilogCutter implements LaserCutter {

    public static final boolean SIMULATE_COMMUNICATION = true;
    private static final int[] RESOLUTIONS = new int[]{300, 600};
    private static final int BED_WIDTH = 1000;
    private static final int BED_HEIGHT = 500;
    private String hostname;
    private Socket connection;
    private InputStream in;
    private OutputStream out;

    public EpilogCutter(String hostname) {
    }

    public void waitForResponse(int expected) throws IOException, Exception {
        if (SIMULATE_COMMUNICATION) {
            System.out.println("Response: 0");
            return;
        }
        int result;
        result = in.read();
        if (result == -1) {
            throw new IOException("End of Stream");
        }
        if (result != expected) {
            throw new Exception("unexpected Response");
        }
    }

    private void initJob(LaserJob job) throws Exception {

        String localhost = java.net.InetAddress.getLocalHost().getHostName();
        //Use PrintStream for getting prinf methotd
        //and autoflush because we're watiting for responses
        PrintStream out = new PrintStream(this.out, true);
        out.print("\002\n");
        waitForResponse(0);
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        PrintStream stmp = new PrintStream(tmp, true);
        stmp.printf("H%s\n", localhost);
        stmp.printf("P%s\n", job.getUser());
        stmp.printf("J%s\n", job.getTitle());
        stmp.printf("ldfA%s%s\n", job.getName(), localhost);
        stmp.printf("UdfA%s%s\n", job.getName(), localhost);
        stmp.printf("N%s\n", job.getTitle());

        out.printf("\002%d cfA%s%s\n", tmp.toString("US-ASCII").length(), job.getName(), localhost);
        waitForResponse(0);
        out.print(tmp.toString("US-ASCII"));
        waitForResponse(0);
    }

    private String generatePjlHeader(LaserJob job) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result);

        /* Print the printer job language header. */
        out.printf("\027%%-12345X@PJL JOB NAME=%s\r\n", job.getTitle());
        out.printf("\027E@PJL ENTER LANGUAGE=PCL\r\n");
        /* Set autofocus off. */
        out.printf("\027&y0A");
        /* Left (long-edge) offset registration.  Adjusts the position of the
         * logical page across the width of the page.
         */
        out.printf("\027&l0U");
        /* Top (short-edge) offset registration.  Adjusts the position of the
         * logical page across the length of the page.
         */
        out.printf("\027&l0Z");

        /* Resolution of the print. */
        out.printf("\027&u%dD", job.getResolution());
        /* X position = 0 */
        out.printf("\027*p0X");
        /* Y position = 0 */
        out.printf("\027*p0Y");
        /* PCL resolution. */
        out.printf("\027*t%dR", job.getResolution());
        try {
            return result.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
            return result.toString();
        }
    }

    private String generatePjlFooter() {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result);

        /* Footer for printer job language. */
        /* Reset */
        out.printf("\027E");
        /* Exit language. */
        out.printf("\027%%-12345X");
        /* End job. */
        out.printf("@PJL EOJ \r\n");
        try {
            return result.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
            return result.toString();
        }
    }

    private void sendPjlJob(LaserJob job) throws UnknownHostException {

        String localhost = java.net.InetAddress.getLocalHost().getHostName();
        StringBuffer pjlJob = new StringBuffer();
        pjlJob.append(generatePjlHeader(job));
        if (job.containsRaster()) {
            pjlJob.append(generateRasterPCL(job.getRasterPart()));
        }
        if (job.containsVector()) {
            pjlJob.append(generateVectorPCL(job.getVectorPart()));
        }
        pjlJob.append(generatePjlFooter());
        //Use PrintStream for getting prinf methotd
        //and autoflush because we're watiting for responses
        PrintStream out = new PrintStream(this.out, true);
        out.printf("\003%d dfA%s%s\n", pjlJob.length(), job.getName(), localhost);
        out.print(pjlJob);
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
        return false;
    }

    public void sendJob(LaserJob job) {
        try {
            boolean wasConnected = isConnected();
            if (!wasConnected) {
                connect();
            }
            initJob(job);
            sendPjlJob(job);
            if (!wasConnected) {
                disconnect();
            }
        } catch (Exception ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    private String generateRasterPCL(RasterPart job) {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result);
        /* FIXME unknown purpose. */
        out.printf("\027&y0C");

        /* We're going to perform a raster print. */

        //TODO: translate method
        //generate_raster(pjl_file, bitmap_file);
        try {
            return result.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
            return result.toString();
        }
    }

    private String generateVectorPCL(VectorPart job) {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(result);

        out.printf("\027E@PJL ENTER LANGUAGE=PCL\r\n");
        /* Page Orientation */
        out.printf("\027*r0F");
        out.printf("\027*r%dT", 7016);// if not dummy, then job.getHeight());
        out.printf("\027*r%dS", 4958);// if not dummy then job.getWidth());
        out.printf("\027*r1A");
        out.printf("\027*rC");
        out.printf("\027%%1B");

        /* We're going to perform a vector print. */
        //TODO: Translate Method
        //generate_vector(pjl_file, vector_file);

        //Dummy Data from captured printjob
        out.print("IN;XR5000;YP100;ZS010;PU1224,6476;"
                + "PD1944,6476,1944,5116,1224,5116,1224,6476;PU1652,5625;PD1641,5624,1631,5621,1622"
                + ",5618,1614,5613,1607,5607,1602,5600,1599,5592,1598,5584,1599,5575,1602,5568,1607"
                + ",5561,1614,5555,1622,5549,1631,5546,1641,5543,1652,5542,1663,5543,1673,5546,1683"
                + ",5549,1691,5555,1697,5561,1702,5568,1705,5575,1706,5584,1705,5592,1702,5600,1697"
                + ",5607,1691,5613,1683,5618,1673,5621,1663,5624,1652,5625;");


        out.printf("\027%%0B");// end HLGL
        out.printf("\027%%1BPU");  // start HLGL, and pen up, end
        try {
            return result.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EpilogCutter.class.getName()).log(Level.SEVERE, null, ex);
            return result.toString();
        }
    }
}
