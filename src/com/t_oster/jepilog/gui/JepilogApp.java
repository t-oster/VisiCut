/*
 * JepilogApp.java
 */

package com.t_oster.jepilog.gui;

import java.awt.Image;
import java.io.File;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class JepilogApp extends SingleFrameApplication {
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new JepilogView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of JepilogApp
     */
    public static JepilogApp getApplication() {
        return Application.getInstance(JepilogApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(JepilogApp.class, args);
    }
    
    public boolean importFile(File file){
       return false;
    }
    
    public Image getRasterPreview(){
        return null;
    }
}
