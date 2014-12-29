/*
 * ThingiverseLoginDialog.java
 *
 * Created on 23.12.2014, 22:55:29
 */
package com.t_oster.visicut.gui;

// We use JavaFX for its browser control.
// This requires a recent Java version, e.g. Java 8.
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import static javafx.concurrent.Worker.State.FAILED;

/**
 * @author Patrick Schmidt
 */
public class ThingiverseLoginDialog extends javax.swing.JDialog
{
  // JavaFX
  private final JFXPanel jfxPanel = new JFXPanel();
  private WebEngine webEngine;

  // Swing
  private final JPanel swingPanel = new JPanel(new BorderLayout());
  private final JLabel lblStatus = new JLabel();
  private final JProgressBar progressBar = new JProgressBar();

  private String browserCode = null;
  
  public String getBrowserCode()
  {
    return browserCode;
  }
  
  public ThingiverseLoginDialog(java.awt.Frame parent, boolean modal, String url)
  {
    super(parent, modal);
    initComponents();

    loadURL(url);
  }

  private void initComponents()
  {
    // For JavaFX/Swing interop see:
    // http://docs.oracle.com/javafx/2/swing/SimpleSwingBrowser.java.htm
    createScene();

    progressBar.setPreferredSize(new Dimension(1000, 18));
    progressBar.setStringPainted(true);

    JPanel statusBar = new JPanel(new BorderLayout(5, 0));
    statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    statusBar.add(lblStatus, BorderLayout.CENTER);
    statusBar.add(progressBar, BorderLayout.CENTER);

    swingPanel.add(jfxPanel, BorderLayout.CENTER);
    swingPanel.add(statusBar, BorderLayout.SOUTH);

    getContentPane().add(swingPanel);
    pack();

    setTitle("Thingiverse Login");
    setSize(1024, 600);
    setLocationRelativeTo(null);
  }

  private void createScene()
  {
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        WebView view = new WebView();
        webEngine = view.getEngine();

        // Update status label
        webEngine.setOnStatusChanged(new EventHandler<WebEvent<String>>()
        {
          @Override
          public void handle(final WebEvent<String> event)
          {
            SwingUtilities.invokeLater(new Runnable()
            {
              @Override
              public void run()
              {
                lblStatus.setText(event.getData());
              }
            });
          }
        });

        // Update progress bar
        webEngine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>()
        {
          @Override
          public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue)
          {
            SwingUtilities.invokeLater(new Runnable()
            {
              @Override
              public void run()
              {
                progressBar.setValue(newValue.intValue());
              }
            });
          }
        });
        
        // Close dialog on success
        webEngine.locationProperty().addListener(new ChangeListener<String>()
        {
          @Override
          public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue)
          {
            SwingUtilities.invokeLater(new Runnable()
            {
              @Override
              public void run()
              {
                String prefix = "http://hci.rwth-aachen.de/visicut?code=";
                if (newValue.startsWith(prefix))
                {
                  browserCode = newValue.substring(prefix.length());
                  
                  // Close dialog
                  ThingiverseLoginDialog.this.dispose();
                }
              }
            });
          }
        });

        // Handle exceptions
        webEngine.getLoadWorker()
          .exceptionProperty()
          .addListener(new ChangeListener<Throwable>()
            {
              public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value)
              {
                if (webEngine.getLoadWorker().getState() == FAILED)
                {
                  SwingUtilities.invokeLater(new Runnable()
                    {
                      @Override
                      public void run()
                      {
                        JOptionPane.showMessageDialog(
                          swingPanel,
                          (value != null)
                            ? webEngine.getLocation() + "\n" + value.getMessage()
                            : webEngine.getLocation() + "\nUnexpected error.",
                          "Loading error...",
                          JOptionPane.ERROR_MESSAGE);
                      }
                  });
                }
              }
          });

        jfxPanel.setScene(new Scene(view));
      }
    });
  }

  private void loadURL(final String url)
  {
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        String tmp = toURL(url);

        if (tmp == null)
        {
          tmp = toURL("http://" + url);
        }

        webEngine.load(tmp);
      }
    });
  }

  private static String toURL(String str)
  {
    try
    {
      return new URL(str).toExternalForm();
    }
    catch (MalformedURLException exception)
    {
      return null;
    }
  }
}
