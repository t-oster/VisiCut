/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package de.thomas_oster.visicut.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * Adapted from: http://www.rbgrn.net/content/43-java-single-application-instance
 */
public class ApplicationInstanceManager {

    private static ApplicationInstanceListener subListener;

    /**
     * Registers this instance of the application.
     * The port is used to create a socket, so if you try
     * to open another  instance with same port, the message
     * will be sent and this instance will be shut down
     *
     * @return true if first instance, false if not.
     */
    public static boolean registerInstance(int port, String message) {
        // returnValueOnError should be true if lenient (allows app to run on network error) or false if strict.
        boolean returnValueOnError = true;
        // try to open network socket
        // if success, listen to socket for new instance message, return true
        // if unable to open, connect to existing and send new instance message, return false
        try {
            final ServerSocket socket = new ServerSocket(port);
            Thread instanceListenerThread = new Thread(new Runnable() {
                public void run() {
                    boolean socketClosed = false;
                    while (!socketClosed) {
                        if (socket.isClosed()) {
                            socketClosed = true;
                        } else {
                            try {
                                Socket client = socket.accept();
                                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                                String message = in.readLine();
                                fireNewInstance(message);
                                in.close();
                                client.close();
                            } catch (IOException e) {
                                socketClosed = true;
                            }
                        }
                    }
                }
            });
            instanceListenerThread.start();
            // listen
        } catch (UnknownHostException e) {
            return returnValueOnError;
        } catch (IOException e) {
            try {
                Socket clientSocket = new Socket(InetAddress.getLocalHost(), port);
                OutputStream out = clientSocket.getOutputStream();
                message += "\n";
                out.write(message.getBytes());
                out.close();
                clientSocket.close();
                return false;
            } catch (UnknownHostException e1) {
                return returnValueOnError;
            } catch (IOException e1) {
                return returnValueOnError;
            }

        }
        return true;
    }

    public static void setApplicationInstanceListener(ApplicationInstanceListener listener) {
        subListener = listener;
    }

    private static void fireNewInstance(String msg) {
      if (subListener != null) {
        subListener.newInstanceCreated(msg);
      }
  }


  /**
   * determine unique port for the single-instance-detection, even if there are multiple user sessions on the same PC
   * @return port number
   */
  public static int getSingleInstancePort()
  {
    // CAUTION: all changes to this function must be mirrored in inkscape_extension/visicut_export.py
    int port = 6543;
    if (Helper.isWindows() && System.getenv("SESSIONNAME") != null) {
        // Windows: vary port by SessionID (usually 0, except on Terminal Server with multiple users)
        // if on Windows with Terminal Services, choose a singleinstanceport unique for each session ID.
        // note: we cannot use SESSIONNAME here because it can change when disconnecting and reconnecting a session!
        // (think of SESSIONNAME like a display that can be connected to different session IDs)
        try
        {
            Process exec = Runtime.getRuntime().exec("powershell -Command (get-process -pid $pid).sessionid");
            String sessionId = new String(exec.getInputStream().readAllBytes()).strip();
            if (!sessionId.isEmpty()) {
                port += 2 + Integer.valueOf(sessionId);
            }
        }
        catch (IOException | NumberFormatException | NullPointerException ex)
        {
            Logger.getLogger(ApplicationInstanceManager.class.getName()).log(Level.WARNING, "cannot determine single-instance port", ex);
        }
    }
    else if (Helper.isLinux())
    {
        try
        {
            // Linux: vary port by DISPLAY number.
            // Extract number:
            // DISPLAY=0:1.2 -> "1"
            // DISPLAY=0:1 -> "1"
            port += Integer.valueOf((System.getenv("DISPLAY").split(":")[1]).split("\\.")[0]);
        }
        catch (NumberFormatException | NullPointerException | ArrayIndexOutOfBoundsException ex)
        {
            Logger.getLogger(ApplicationInstanceManager.class.getName()).log(Level.WARNING, "cannot determine single-instance port", ex);
        }
    } else {
        // nothing special on Mac.
    }
    return port;
  }
}