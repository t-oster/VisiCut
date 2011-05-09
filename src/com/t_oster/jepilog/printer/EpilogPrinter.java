/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.printer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author thommy
 */
public class EpilogPrinter {
    
    private int port = 515;
    private String ipAddress;
    private Socket connection;
    private InputStream in;
    private OutputStream out;
    
    private void connect() throws UnknownHostException, IOException{
        connection = new Socket(ipAddress, port);
        in = new BufferedInputStream(connection.getInputStream());
	out = new BufferedOutputStream(connection.getOutputStream());
    }
    private void disconnect() throws IOException{
        connection.close();
    }
    private void initJob(String user, String title, String name, String hostname){
    /**
        // talk to printer
        out.write("\002"+queue+"\n");
    sprintf(buf, "\002%s\n", queue);
    write(socket_descriptor, (char *)buf, strlen(buf));
    read(socket_descriptor, &lpdres, 1);
    if (lpdres) {
        fprintf (stderr, "Bad response from %s, %u\n", host, lpdres);
        return false;
    }
    sprintf(buf, "H%s\n", localhost);
    sprintf(buf + strlen(buf) + 1, "P%s\n", job_user);
    sprintf(buf + strlen(buf) + 1, "J%s\n", job_title);
    sprintf(buf + strlen(buf) + 1, "ldfA%s%s\n", job_name, localhost);
    sprintf(buf + strlen(buf) + 1, "UdfA%s%s\n", job_name, localhost);
    sprintf(buf + strlen(buf) + 1, "N%s\n", job_title);
    sprintf(buf + strlen(buf) + 1, "\002%d cfA%s%s\n", (int)strlen(buf), job_name, localhost);
    write(socket_descriptor, buf + strlen(buf) + 1, strlen(buf + strlen(buf) + 1));
    read(socket_descriptor, &lpdres, 1);
    if (lpdres) {
        fprintf(stderr, "Bad response from %s, %u\n", host, lpdres);
        return false;
    }
    write(socket_descriptor, (char *)buf, strlen(buf) + 1);
    read(socket_descriptor, &lpdres, 1);
    if (lpdres) {
        fprintf(stderr, "Bad response from %s, %u\n", host, lpdres);
        return false;
    }
     */
    }
    
    public void sendToPrinter(PJLFile pjl) throws UnknownHostException, IOException{
        String hostname =  java.net.InetAddress.getLocalHost().getHostName();
        this.connect();
        this.initJob("john", "testjob", "test1",  hostname);
       /* 
    
    {
        {
            struct stat file_stat;
            if (fstat(fileno(pjl_file), &file_stat)) {
                perror(buf);
                return false;
            }
            sprintf((char *) buf, "\003%u dfA%s%s\n", (int) file_stat.st_size, job_name, localhost);
        }
        write(socket_descriptor, (char *)buf, strlen(buf));
        read(socket_descriptor, &lpdres, 1);
        if (lpdres) {
            fprintf(stderr, "Bad response from %s, %u\n", host, lpdres);
            return false;
        }
        {
            int l;
            while ((l = fread((char *)buf, 1, sizeof (buf), pjl_file)) > 0) {
                write(socket_descriptor, (char *)buf, l);
            }
        }
    }
    // dont wait for a response...
    printer_disconnect(socket_descriptor);
    return true;
    }*/
    }
}
