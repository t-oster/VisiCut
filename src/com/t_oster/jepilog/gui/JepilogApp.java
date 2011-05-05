/*
 * JepilogApp.java
 */

package com.t_oster.jepilog.gui;

import com.t_oster.jepilog.postscript.InputFile;
import com.t_oster.jepilog.postscript.PostscriptInterpreter;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.ghost4j.document.DocumentException;
import net.sf.ghost4j.renderer.RendererException;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class JepilogApp extends SingleFrameApplication {

    private PostscriptInterpreter psi = new PostscriptInterpreter();
    private InputFile inputFile;
    
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
        try {
            this.inputFile = InputFile.importFile(file);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(JepilogApp.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public Image getRasterPreview(){
        try {
            return psi.rasterizeFile(inputFile);
        } catch (Exception ex) {
            Logger.getLogger(JepilogApp.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
