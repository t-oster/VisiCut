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
package com.t_oster.visicut.gui;

// Do not add this import!
// It is loaded dynamically iff JavaFX is available:
// import com.tur0kk.thingiverse.gui.ThingiverseLoginDialog;

import com.tur0kk.thingiverse.gui.ThingiverseDialog;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.QuitResponse;
import com.frochr123.fabqr.FabQRFunctions;
import com.frochr123.fabqr.gui.FabQRUploadDialog;
import com.frochr123.gui.QRWebcamScanDialog;
import com.frochr123.helper.QRCodeInfo;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.uicomponents.PlatformIcon;
import com.t_oster.uicomponents.Ruler;
import com.t_oster.uicomponents.warnings.Message;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.VisicutModel.Modification;
import com.t_oster.visicut.gui.beans.CreateNewMaterialDialog;
import com.t_oster.visicut.gui.beans.CreateNewThicknessDialog;
import com.t_oster.visicut.gui.parameterpanel.ParameterPanel;
import com.t_oster.visicut.managers.LaserDeviceManager;
import com.t_oster.visicut.managers.LaserPropertyManager;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.MaterialManager;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.misc.DialogHelper;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.PlfFile;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.graphicelements.psvgsupport.ParametricPlfPart;
import com.t_oster.visicut.model.mapping.MappingSet;
import com.tur0kk.facebook.FacebookManager;
import com.tur0kk.facebook.gui.FacebookDialog;
import com.tur0kk.thingiverse.ThingiverseManager;
import com.frochr123.periodictasks.RefreshCameraThread;
import com.frochr123.periodictasks.RefreshProjectorThread;
import com.frochr123.periodictasks.RefreshQRCodesTask;
import com.t_oster.uicomponents.warnings.WarningPanel;
import com.t_oster.visicut.Preferences;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.filechooser.FileFilter;
import org.jdesktop.application.Action;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MainView extends javax.swing.JFrame
{
  private static MainView instance = null;
  private ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView");
  private ThingiverseDialog thingiverseDialog = null;
  private ParameterPanel parameterPanel = new ParameterPanel();
  private boolean cameraActive = false;
  private boolean cameraCapturing = false;
  private String cameraCapturingError = "";
  private RefreshCameraThread cameraThread = null;
  private boolean projectorActive = false;
  private RefreshProjectorThread projectorThread = null;
  private RefreshQRCodesTask qrCodesTask = null;
  private boolean editGuiForQRCodesDisabled = false;
  private boolean laserJobInProgress = false;
  private boolean isFabqrUploadDialogOpened = false;
  
  public static MainView getInstance()
  {
    return instance;
  }

  final protected DialogHelper dialog = new DialogHelper(this, this.getTitle())
  {

    @Override
    public void showWarningMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Warning", text, Message.Type.WARNING, null));
    }

    @Override
    public void showWarningMessageOnce(String text, String messageId, int timeout)
    {
      // use timeout=-1 to disable timeout
      MainView.this.warningPanel.addMessageOnce(new Message("Warning", text, Message.Type.WARNING, null, timeout), messageId);
    }

    @Override
    public void showSuccessMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Success", text, Message.Type.SUCCESS, null, 10000));
    }

    @Override
    public void showInfoMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Info", text, Message.Type.INFO, null));
    }

    @Override
    public void showErrorMessage(Exception ex)
    {
      Throwable cause = ex;
      while ((cause.getMessage() == null || "".equals(cause.getMessage())) && cause.getCause() != null)
      {
        cause = cause.getCause();
      }
      cause.printStackTrace();
      MainView.this.warningPanel.addMessage(new Message("Error", "Exception: "+cause.getLocalizedMessage(), Message.Type.ERROR, null));
    }

    @Override
    public void showErrorMessage(Exception cause, String text)
    {
      cause.printStackTrace();
      MainView.this.warningPanel.addMessage(new Message("Error", text+": "+cause.getLocalizedMessage(), Message.Type.ERROR, null));
    }

    @Override
    public void showErrorMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Error", text, Message.Type.ERROR, null));
    }
    
    @Override
    public void removeMessageWithId(String messageId) {
      MainView.this.warningPanel.removeMessageWithId(messageId);
    }
  };
  private boolean ignoreLaserCutterComboBoxUpdates;

  public MainView(File loadedFile)
  {
    this();
    this.loadFileReal(loadedFile, true);
  }

  /**
   * Shows the according EditLaserProfile dialog for the given
   * laser-profile and returns the altered clone of the profile if ok
   * was pressed, and null else. The given LaserProfile is not touched
   * @param lp
   * @return 
   */
  public LaserProfile editLaserProfile(LaserProfile profile)
  {
    if (profile instanceof VectorProfile)
    {
      EditVectorProfileDialog d = new EditVectorProfileDialog(null, true);
      d.setVectorProfile((VectorProfile) ((VectorProfile) profile).clone());
      d.setOnlyEditParameters(true);
      d.setVisible(true);
      profile = d.isOkPressed() ? d.getVectorProfile() : null;
    }
    else if (profile instanceof RasterProfile)
    {
      EditRasterProfileDialog d = new EditRasterProfileDialog(null, true);
      d.setRasterProfile((RasterProfile) profile);
      d.setOnlyEditParameters(true);
      d.setVisible(true);
      profile = d.getRasterProfile();
    }
    else if (profile instanceof Raster3dProfile)
    {
      EditRaster3dProfileDialog d = new EditRaster3dProfileDialog(null, true);
      d.setRasterProfile((Raster3dProfile) profile);
      d.setOnlyEditParameters(true);
      d.setVisible(true);
      profile = d.getRasterProfile();
    }
    return profile;
  }
  
  /** Creates new form MainView */
  public MainView()
  {
    try
    {
      Image i = ImageIO.read(this.getClass().getResourceAsStream("resources/visicut.png"));
      this.setIconImage(i);
    }
    catch (Exception e)
    {
      System.err.println("Error reading image: "+e);
    }
    instance = this;
    initComponents();
    jScrollPane2.setColumnHeaderView(new Ruler(this.previewPanel, Ruler.HORIZONTAL));
    jScrollPane2.setRowHeaderView(new Ruler(this.previewPanel, Ruler.VERTICAL));
    jScrollPane2.setCorner(JScrollPane.UPPER_LEFT_CORNER, new JLabel("cm"));
    //fixes slow scrolling issue on Mac OS X
    JViewport viewPort = jScrollPane2.getViewport();
    viewPort.setScrollMode(JViewport.BLIT_SCROLL_MODE);
    viewPort.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
    viewPort.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    jScrollPane2.addComponentListener(new ComponentListener(){
      public void componentResized(ComponentEvent ce)
      {
        MainView.this.previewPanel.resizeToFitZoomAndArea();
      }
      public void componentMoved(ComponentEvent ce){}
      public void componentShown(ComponentEvent ce){}
      public void componentHidden(ComponentEvent ce){}
    });
    if (Helper.isMacOS())
    {//Mac OS has its own exit menu and different Keybindings
      fileMenu.remove(exitMenuItem);
      openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
      newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.META_MASK));
      saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
      executeJobMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.META_MASK));
      zoomInMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ADD, java.awt.event.InputEvent.META_MASK));
      zoomOutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.META_MASK));
    }
    
    fixMaterialThicknesses();    
    fillComboBoxes();
    refreshMaterialThicknessesComboBox();

    if (Helper.isMacOS())
    {
      com.apple.eawt.Application macApplication = com.apple.eawt.Application.getApplication();
      jmPreferences.setVisible(false);
      macApplication.setPreferencesHandler(new com.apple.eawt.PreferencesHandler() {

        public void handlePreferences(PreferencesEvent pe)
        {
          MainView.this.jmPreferencesActionPerformed(null);
        }
      });
      exitMenuItem.setVisible(false);
      macApplication.setQuitHandler(new com.apple.eawt.QuitHandler() {

        public void handleQuitRequestWith(QuitEvent qe, QuitResponse qr)
        {
          MainView.this.exitMenuItemActionPerformed(null);
        }
      });
      aboutMenuItem.setVisible(false);
      macApplication.setAboutHandler(new com.apple.eawt.AboutHandler() {

        public void handleAbout(AboutEvent ae)
        {
          MainView.this.aboutMenuItemActionPerformed(null);
        }
      });
      macApplication.setOpenFileHandler(new com.apple.eawt.OpenFilesHandler() {

        public void openFiles(OpenFilesEvent ofe)
        {
          for (File f : ofe.getFiles())
          {
            MainView.this.loadFile(f, false);
          }
        }
      });
    }
    //Window listener for capturing close and save preferences before exiting
    this.addWindowListener(new WindowListener()
    {

      public void windowOpened(WindowEvent e)
      {
      }

      public void windowClosing(WindowEvent e)
      {
        MainView.this.exitMenuItemActionPerformed(null);
      }

      public void windowClosed(WindowEvent e)
      {
      }

      public void windowIconified(WindowEvent e)
      {
      }

      public void windowDeiconified(WindowEvent e)
      {
      }

      public void windowActivated(WindowEvent e)
      {
      }

      public void windowDeactivated(WindowEvent e)
      {
      }
    });
    this.refreshRecentFilesMenu();
    this.jmInstallInkscape.setEnabled(Helper.isInkscapeExtensionInstallable());
    this.jmInstallIllustrator.setEnabled(Helper.isIllustratorScriptInstallable());
    if (!Helper.isInkscapeExtensionInstallable() && !Helper.isIllustratorScriptInstallable())
    {
      this.jmExtras.setVisible(false);
    }
    this.refreshExampleMenu();
    //initialize states coorectly
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_SELECTEDLASERDEVICE, null, null));
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_SELECTEDPART, null, null));
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_PREFERENCES, null, null));

    PositionPanelController c = new PositionPanelController(positionPanel, visicutModel1);
    this.warningPanel.setVisible(false);
    LaserDeviceManager.getInstance().addPropertyChangeListener(new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent pce)
      {
        refreshLaserDeviceComboBox();
      }
    });
    //apply the saved window size and position, if in current screen size
    Rectangle lastBounds = PreferencesManager.getInstance().getPreferences().getWindowBounds();
    if (lastBounds != null && this.getGraphicsConfiguration().getBounds().contains(lastBounds))
    {
      this.setExtendedState(JFrame.NORMAL);
      this.setPreferredSize(new Dimension(lastBounds.width, lastBounds.height));
      this.setBounds(lastBounds);
    }
    
    // all GUI parts are now initialised.
    if (LaserDeviceManager.getInstance().getAll().isEmpty()) {
      this.jmDownloadSettingsActionPerformed(null);
    }
    
    // Cleanup old temporary files, which might not have been deleted correctly
    FileUtils.cleanupOldTempFilesAtStartup();
  }

  private ActionListener exampleItemClicked = new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        if (!"".equals(ae.getActionCommand()))
        {
          MainView.this.loadFile(new File(ae.getActionCommand()), false);
        }
      }
    };

  private void fillMenu(JMenu parent, Map<String, Object> map)
  {
    for (Entry<String, Object> e: map.entrySet())
    {
      if (e.getValue() instanceof File)
      {
        JMenuItem item = new JMenuItem(e.getKey());
        item.setActionCommand(((File) e.getValue()).getAbsolutePath());
        item.addActionListener(exampleItemClicked);
        parent.add(item);
      }
      else if (e.getValue() instanceof Map)
      {
        JMenu m = new JMenu(e.getKey());
        fillMenu(m, (Map) e.getValue());
        parent.add(m);
      }
    }
  }
  
  private JMenuItem openExamples;
  private void refreshExampleMenu()
  {
    jmExamples.removeAll();
    JMenu builtin = new JMenu(bundle.getString("BUILTIN"));
    this.fillMenu(builtin, PreferencesManager.getInstance().getBuiltinExampleFiles());
    jmExamples.add(builtin);
    this.fillMenu(jmExamples, PreferencesManager.getInstance().getExampleFiles());
    if (openExamples == null)
    {
      openExamples = new JMenuItem(bundle.getString("EDIT"));
      openExamples.addActionListener(new ActionListener(){

        public void actionPerformed(ActionEvent ae)
        {
          dialog.openInFilebrowser(new File(Helper.getBasePath(), "examples"));
          //TODO refresh menu on next click (menu and action-listener don't work)
        }
      });
    }
    jmExamples.add(openExamples);
  }

  /**
   * Fills the recent files menu from the current
   * list in preferences
   */
  private void refreshRecentFilesMenu()
  {
    this.recentFilesMenu.removeAll();
    for (String p: this.visicutModel1.getPreferences().getRecentFiles())
    {
      final File f = new File(p);
      if (f.isFile())
      {
        JMenuItem i = new JMenuItem(f.getName());
        i.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent ae)
          {
            loadFile(f, false);
          }
        });
        this.recentFilesMenu.add(i);
      }
    }
  }

  private void refreshMaterialComboBox()
  {
    this.ignoreMaterialComboBoxChanges = true;
    String sp = this.visicutModel1.getMaterial() != null ? this.visicutModel1.getMaterial().getName() : null;
    this.materialComboBox.removeAllItems();
    for (MaterialProfile mp : MaterialManager.getInstance().getAll())
    {
      this.materialComboBox.addItem(mp);
      if (mp.getName().equals(sp))
      {
        this.materialComboBox.setSelectedItem(mp);
      }
    }
    this.ignoreMaterialComboBoxChanges = false;
    //hide material combo box if only one material available
    boolean materialUiVisible = true;
    if (this.materialComboBox.getItemCount() == 1 && MaterialManager.getInstance().getAll().get(0).getMaterialThicknesses().size() == 1)
    {
      this.materialComboBox.setSelectedIndex(0);
      materialUiVisible = false;
    }
    this.materialComboBox.setVisible(materialUiVisible);
    this.btAddMaterial.setVisible(materialUiVisible);
    this.cbMaterialThickness.setVisible(materialUiVisible);
    this.btAddMaterialThickness.setVisible(materialUiVisible);
    this.jLabel1.setVisible(materialUiVisible);
    this.jLabel5.setVisible(materialUiVisible);
  }
  
  // add a "0.0mm" material thickness if the material has an empty list of thicknesses
  private void fixMaterialThicknesses() {
    List<MaterialProfile> materials = MaterialManager.getInstance().getAll();
    for (MaterialProfile mp: materials) {
      if (mp.getMaterialThicknesses().isEmpty()) {
        LinkedList<Float> l = new LinkedList<Float>();
        l.add((float) 0.0);
        mp.setMaterialThicknesses(l);
        System.err.println("Found material \"" + mp.getName() + "\" without a thickness entry - this should not happen! Adding a thickness 0.0 for this material. Please report if you find a way to create materials without a thickness.");
        try {
          MaterialManager.getInstance().save(mp);
        } catch(IOException e) {
          System.err.println("Failed to fix because of exception " + e);
        }
        
      }
    }
  }

  /*
   * Initially fills LaserCutter, Material, Object and Mapping ComboBox with all possible Elements
   */
  private void fillComboBoxes()
  {
    this.refreshLaserDeviceComboBox();
    this.refreshMaterialComboBox();
    this.refreshObjectComboBox();
  }
  
  private void refreshLaserDeviceComboBox()
  {
    String sld = this.visicutModel1.getSelectedLaserDevice() != null ? this.visicutModel1.getSelectedLaserDevice().getName() : null;
    ignoreLaserCutterComboBoxUpdates = true;
    this.laserCutterComboBox.removeAllItems();
    for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
    {
      this.laserCutterComboBox.addItem(ld);
      if (ld.getName().equals(sld))
      {
        this.laserCutterComboBox.setSelectedItem(ld);
      }
    }
    //hide lasercutter combo box if only one lasercutter available
    if (this.laserCutterComboBox.getItemCount() == 1)
    {
      this.laserCutterComboBox.setSelectedIndex(0);
      this.laserCutterComboBox.setVisible(false);
      this.jLabel9.setVisible(false);
    }
    else
    {
      this.laserCutterComboBox.setVisible(true);
      this.jLabel9.setVisible(true);
    }
    ignoreLaserCutterComboBoxUpdates = false;
  }
   /**
   * update entries of objectComboBox, then update selection
   * @param forceUpdate even update if the list of PlfParts has not changed
   * @throws RuntimeException if forceUpdate==false and PlfParts have not changed
   */
  public void refreshObjectComboBox() {        
    this.ignoreObjectComboBoxEvents = true;
    // fill new list of PlfItems
    this.objectComboBox.removeAllItems();
    if (VisicutModel.getInstance().getSelectedPart() == null) {
      // add default "nothing selected" item if nothing is selected
      this.objectComboBox.addItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("(nothing selected)"));
    }
    for (PlfPart p: VisicutModel.getInstance().getPlfFile()) {
      if (p == null || p.getSourceFile() == null) { // necessary? 
        continue;
      }
      // add regular item
      this.objectComboBox.addItem(p);
    }
    
    // now set the correct selection
    if (VisicutModel.getInstance().getSelectedPart() != null) {
      // something is selected, also select this in the combobox
      this.objectComboBox.setSelectedItem(VisicutModel.getInstance().getSelectedPart());
    } else {
      // no PlfPart is selected, so select the pseudo-item "nothing selected"
      this.objectComboBox.setSelectedIndex(0);
    }
    //gradually hide complexity. Issue #71
    int files = this.visicutModel1.getPlfFile().size();
    boolean selected = this.visicutModel1.getSelectedPart() != null;
    if (files == 1 && selected)
    {
      this.jLabel2.setText("<html>"+this.bundle.getString("jLabel2.text")+" <b>"+this.visicutModel1.getSelectedPart().getSourceFile().getName()+"</b></html>");
    }
    else
    {
      this.jLabel2.setText(this.bundle.getString("jLabel2.text") + (files == 1 && !selected ? " "+this.bundle.getString("(nothing selected)") : ""));
    }

    // Avoid some frequent visible changes if GUI is disabled for QR code detection
    // Once this mode turns on / off again, the correct state is restored
    if (!isEditGuiForQRCodesDisabled())
    {
      this.objectComboBox.setVisible(files > 1);
    }

    this.btRemoveObject.setVisible(files > 0 && selected);
    this.ignoreObjectComboBoxEvents = false;
  }
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        visicutModel1 = VisicutModel.getInstance();
        filesDropSupport1 = new com.t_oster.uicomponents.FilesDropSupport();
        saveFileChooser = new javax.swing.JFileChooser();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane2 = new javax.swing.JScrollPane();
        previewPanel = new com.t_oster.visicut.gui.beans.PreviewPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        materialComboBox = new com.t_oster.uicomponents.ImageComboBox();
        jLabel9 = new javax.swing.JLabel();
        laserCutterComboBox = new com.t_oster.uicomponents.ImageComboBox();
        jLabel10 = new javax.swing.JLabel();
        calculateTimeButton = new javax.swing.JButton();
        timeLabel = new javax.swing.JLabel();
        mappingTabbedPane = new javax.swing.JTabbedPane();
        mappingPanel = new com.t_oster.visicut.gui.mapping.MappingPanel();
        positionPanel = new com.t_oster.uicomponents.PositionPanel();
        propertyPanelContainer = new javax.swing.JScrollPane();
        propertiesPanel = new com.t_oster.visicut.gui.propertypanel.PropertiesPanel();
        btAddMaterial = new javax.swing.JButton();
        cbMaterialThickness = new javax.swing.JComboBox();
        btAddMaterialThickness = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        executeJobButton = new javax.swing.JButton();
        objectComboBox = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        btRemoveObject = new javax.swing.JButton();
        btAddObject = new javax.swing.JButton();
        warningPanel = new com.t_oster.uicomponents.warnings.WarningPanel();
        jPanel1 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        btFitScreen = new javax.swing.JButton();
        bt1to1 = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(null, new java.awt.Dimension(35, 35), new java.awt.Dimension(35, 35));
        btFacebook = new javax.swing.JButton();
        btThingiverse = new javax.swing.JButton();
        btQRWebcamScan = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        importMenuItem = new javax.swing.JMenuItem();
        recentFilesMenu = new javax.swing.JMenu();
        jmExamples = new javax.swing.JMenu();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        executeJobMenuItem = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        zoomOutMenuItem = new javax.swing.JMenuItem();
        zoomInMenuItem = new javax.swing.JMenuItem();
        zoomWindowMenuItem = new javax.swing.JMenuItem();
        zoomRealMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        cameraActiveMenuItem = new javax.swing.JCheckBoxMenuItem();
        projectorActiveMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        showGridMenuItem = new javax.swing.JCheckBoxMenuItem();
        actionsMenu = new javax.swing.JMenu();
        facebookMenuItem = new javax.swing.JMenuItem();
        thingiverseMenuItem = new javax.swing.JMenuItem();
        webcamQRCodeMenuItem = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        calibrateCameraMenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jmImportSettings = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jmExportSettings = new javax.swing.JMenuItem();
        editMappingMenuItem = new javax.swing.JMenuItem();
        jmManageLaserprofiles = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        materialMenuItem = new javax.swing.JMenuItem();
        jmPreferences = new javax.swing.JMenuItem();
        jmExtras = new javax.swing.JMenu();
        jmInstallInkscape = new javax.swing.JMenuItem();
        jmInstallIllustrator = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        visicutModel1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                visicutModel1PropertyChange(evt);
            }
        });

        filesDropSupport1.setComponent(previewPanel);
        filesDropSupport1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                filesDropSupport1PropertyChange(evt);
            }
        });

        saveFileChooser.setAcceptAllFileFilterUsed(false);
        saveFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        saveFileChooser.setFileFilter(new ExtensionFilter(".plf", java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("VISICUT PORTABLE LASER FILE")));
        saveFileChooser.setName("saveFileChooser"); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(MainView.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        jScrollPane2.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
        jScrollPane2.setName("jScrollPane2"); // NOI18N
        jScrollPane2.setWheelScrollingEnabled(false);

        previewPanel.setName("previewPanel"); // NOI18N
        com.t_oster.visicut.gui.PreviewPanelKeyboardMouseHandler ppMouseHandler = new com.t_oster.visicut.gui.PreviewPanelKeyboardMouseHandler(this.previewPanel);

        javax.swing.GroupLayout previewPanelLayout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanelLayout);
        previewPanelLayout.setHorizontalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 605, Short.MAX_VALUE)
        );
        previewPanelLayout.setVerticalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 457, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(previewPanel);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        materialComboBox.setName("materialComboBox"); // NOI18N
        materialComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                materialComboBoxActionPerformed(evt);
            }
        });

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        laserCutterComboBox.setName("laserCutterComboBox"); // NOI18N
        laserCutterComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                laserCutterComboBoxActionPerformed(evt);
            }
        });

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        calculateTimeButton.setText(resourceMap.getString("calculateTimeButton.text")); // NOI18N
        calculateTimeButton.setEnabled(false);
        calculateTimeButton.setName("calculateTimeButton"); // NOI18N
        calculateTimeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calculateTimeButtonActionPerformed(evt);
            }
        });

        timeLabel.setText(resourceMap.getString("timeLabel.text")); // NOI18N
        timeLabel.setName("timeLabel"); // NOI18N

        mappingTabbedPane.setName("Custom"); // NOI18N

        mappingPanel.setName("mappingPanel"); // NOI18N
        mappingTabbedPane.addTab(resourceMap.getString("mappingPanel.TabConstraints.tabTitle"), mappingPanel); // NOI18N

        positionPanel.setName("positionPanel"); // NOI18N
        mappingTabbedPane.addTab(resourceMap.getString("positionPanel.TabConstraints.tabTitle"), positionPanel); // NOI18N

        propertyPanelContainer.setName("propertyPanelContainer"); // NOI18N

        propertiesPanel.setName("propertiesPanel"); // NOI18N
        propertiesPanel.setLayout(new javax.swing.BoxLayout(propertiesPanel, javax.swing.BoxLayout.Y_AXIS));
        propertyPanelContainer.setViewportView(propertiesPanel);

        mappingTabbedPane.addTab(resourceMap.getString("propertyPanelContainer.TabConstraints.tabTitle"), propertyPanelContainer); // NOI18N

        btAddMaterial.setIcon(PlatformIcon.get(PlatformIcon.ADD));
        btAddMaterial.setName("btAddMaterial"); // NOI18N
        btAddMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAddMaterialActionPerformed(evt);
            }
        });

        cbMaterialThickness.setName("cbMaterialThickness"); // NOI18N
        cbMaterialThickness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbMaterialThicknessActionPerformed(evt);
            }
        });

        btAddMaterialThickness.setIcon(PlatformIcon.get(PlatformIcon.ADD));
        btAddMaterialThickness.setName("btAddMaterialThickness"); // NOI18N
        btAddMaterialThickness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAddMaterialThicknessActionPerformed(evt);
            }
        });

        jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
        jCheckBox1.setToolTipText(resourceMap.getString("jCheckBox1.toolTipText")); // NOI18N
        jCheckBox1.setName("jCheckBox1"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${useThicknessAsFocusOffset}"), jCheckBox1, org.jdesktop.beansbinding.BeanProperty.create("selected"), "cbUseThickness");
        bindingGroup.addBinding(binding);

        executeJobButton.setText(resourceMap.getString("executeJobButton.text")); // NOI18N
        executeJobButton.setName("executeJobButton"); // NOI18N
        executeJobButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeJobButtonActionPerformed(evt);
            }
        });

        objectComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        objectComboBox.setName("objectComboBox"); // NOI18N
        objectComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                objectComboBoxChangeHandler(evt);
            }
        });

        jSeparator1.setName("jSeparator1"); // NOI18N

        btRemoveObject.setIcon(PlatformIcon.get(PlatformIcon.REMOVE_FILE));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/EditableTablePanel"); // NOI18N
        btRemoveObject.setToolTipText(bundle.getString("-")); // NOI18N
        btRemoveObject.setName("btRemoveObject"); // NOI18N
        btRemoveObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btRemoveObjectActionPerformed(evt);
            }
        });

        btAddObject.setIcon(PlatformIcon.get(PlatformIcon.ADD_FILE));
        btAddObject.setToolTipText(bundle.getString("+")); // NOI18N
        btAddObject.setName("btAddObject"); // NOI18N
        btAddObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAddObjectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(executeJobButton, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 199, Short.MAX_VALUE)
                                .addComponent(calculateTimeButton)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(mappingTabbedPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(objectComboBox, 0, 416, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btAddObject, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btRemoveObject, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addComponent(laserCutterComboBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(materialComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btAddMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(cbMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btAddMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 158, Short.MAX_VALUE)
                                .addComponent(jCheckBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING))
                                .addGap(0, 454, Short.MAX_VALUE)))
                        .addGap(20, 20, 20))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(laserCutterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btAddMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(materialComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cbMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btAddMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jCheckBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(objectComboBox)
                        .addComponent(jLabel2))
                    .addComponent(btRemoveObject, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btAddObject, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mappingTabbedPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(calculateTimeButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(executeJobButton)
                .addContainerGap())
        );

        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView"); // NOI18N
        mappingTabbedPane.getAccessibleContext().setAccessibleName(bundle1.getString("MAPPING")); // NOI18N

        jScrollPane1.setViewportView(jPanel2);

        warningPanel.setName("warningPanel"); // NOI18N
        warningPanel.setPreferredSize(new java.awt.Dimension(276, 123));

        jPanel1.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(0));

        jButton2.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_OUT));
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setToolTipText(resourceMap.getString("jButton2.toolTipText")); // NOI18N
        jButton2.setMaximumSize(new java.awt.Dimension(30, 30));
        jButton2.setMinimumSize(new java.awt.Dimension(30, 30));
        jButton2.setName("jButton2"); // NOI18N
        jButton2.setPreferredSize(new java.awt.Dimension(35, 35));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton2);

        jButton1.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_IN));
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setToolTipText(resourceMap.getString("jButton1.toolTipText")); // NOI18N
        jButton1.setMaximumSize(new java.awt.Dimension(30, 30));
        jButton1.setMinimumSize(new java.awt.Dimension(30, 30));
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setPreferredSize(new java.awt.Dimension(35, 35));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1);

        btFitScreen.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_LASERBED));
        btFitScreen.setText(resourceMap.getString("btFitScreen.text")); // NOI18N
        btFitScreen.setToolTipText(resourceMap.getString("btFitScreen.toolTipText")); // NOI18N
        btFitScreen.setMaximumSize(new java.awt.Dimension(35, 35));
        btFitScreen.setMinimumSize(new java.awt.Dimension(35, 35));
        btFitScreen.setName("btFitScreen"); // NOI18N
        btFitScreen.setPreferredSize(new java.awt.Dimension(35, 35));
        btFitScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btFitScreenActionPerformed(evt);
            }
        });
        jPanel1.add(btFitScreen);

        bt1to1.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_ACTUAL));
        bt1to1.setText(resourceMap.getString("bt1to1.text")); // NOI18N
        bt1to1.setToolTipText(resourceMap.getString("bt1to1.toolTipText")); // NOI18N
        bt1to1.setMaximumSize(new java.awt.Dimension(35, 35));
        bt1to1.setMinimumSize(new java.awt.Dimension(35, 35));
        bt1to1.setName("bt1to1"); // NOI18N
        bt1to1.setPreferredSize(new java.awt.Dimension(35, 35));
        bt1to1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt1to1ActionPerformed(evt);
            }
        });
        jPanel1.add(bt1to1);

        filler2.setName("filler2"); // NOI18N
        jPanel1.add(filler2);

        btFacebook.setIcon(com.frochr123.icons.IconLoader.loadIcon(com.frochr123.icons.IconLoader.ICON_FACEBOOK));
        btFacebook.setText(resourceMap.getString("btFacebook.text")); // NOI18N
        btFacebook.setToolTipText(resourceMap.getString("btFacebook.toolTipText")); // NOI18N
        btFacebook.setMaximumSize(new java.awt.Dimension(35, 35));
        btFacebook.setMinimumSize(new java.awt.Dimension(35, 35));
        btFacebook.setName("btFacebook"); // NOI18N
        btFacebook.setPreferredSize(new java.awt.Dimension(35, 35));
        btFacebook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btFacebookActionPerformed(evt);
            }
        });
        jPanel1.add(btFacebook);
        btFacebook.getAccessibleContext().setAccessibleDescription(resourceMap.getString("btFacebook.AccessibleContext.accessibleDescription")); // NOI18N

        btThingiverse.setIcon(com.frochr123.icons.IconLoader.loadIcon(com.frochr123.icons.IconLoader.ICON_THINGIVERSE));
        btThingiverse.setText(resourceMap.getString("btThingiverse.text")); // NOI18N
        btThingiverse.setToolTipText(resourceMap.getString("btThingiverse.toolTipText")); // NOI18N
        btThingiverse.setMaximumSize(new java.awt.Dimension(35, 35));
        btThingiverse.setMinimumSize(new java.awt.Dimension(35, 35));
        btThingiverse.setName("btThingiverse"); // NOI18N
        btThingiverse.setPreferredSize(new java.awt.Dimension(35, 35));
        btThingiverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btThingiverseActionPerformed(evt);
            }
        });
        jPanel1.add(btThingiverse);
        btThingiverse.getAccessibleContext().setAccessibleDescription(resourceMap.getString("btThingiverse.AccessibleContext.accessibleDescription")); // NOI18N

        btQRWebcamScan.setIcon(com.frochr123.icons.IconLoader.loadIcon(com.frochr123.icons.IconLoader.ICON_QRCODE));
        btQRWebcamScan.setToolTipText(resourceMap.getString("btQRWebcamScan.toolTipText")); // NOI18N
        btQRWebcamScan.setMaximumSize(new java.awt.Dimension(35, 35));
        btQRWebcamScan.setMinimumSize(new java.awt.Dimension(35, 35));
        btQRWebcamScan.setName("btQRWebcamScan"); // NOI18N
        btQRWebcamScan.setPreferredSize(new java.awt.Dimension(35, 35));
        btQRWebcamScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btQRWebcamScanActionPerformed(evt);
            }
        });
        jPanel1.add(btQRWebcamScan);

        jPanel3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        progressBar.setAlignmentY(0.52F);
        progressBar.setMaximumSize(new java.awt.Dimension(140, 33));
        progressBar.setMinimumSize(new java.awt.Dimension(140, 33));
        progressBar.setName("progressBar"); // NOI18N
        jPanel3.add(progressBar);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        newMenuItem.setText(resourceMap.getString("newMenuItem.text")); // NOI18N
        newMenuItem.setName("newMenuItem"); // NOI18N
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newMenuItem);

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setText(resourceMap.getString("openMenuItem.text")); // NOI18N
        openMenuItem.setName("openMenuItem"); // NOI18N
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        importMenuItem.setText(resourceMap.getString("importMenuItem.text")); // NOI18N
        importMenuItem.setName("importMenuItem"); // NOI18N
        importMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importMenuItem);

        recentFilesMenu.setText(resourceMap.getString("recentFilesMenu.text")); // NOI18N
        recentFilesMenu.setName("recentFilesMenu"); // NOI18N
        fileMenu.add(recentFilesMenu);

        jmExamples.setText(resourceMap.getString("jmExamples.text")); // NOI18N
        jmExamples.setName("jmExamples"); // NOI18N
        fileMenu.add(jmExamples);

        jSeparator5.setName("jSeparator5"); // NOI18N
        fileMenu.add(jSeparator5);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('s');
        saveMenuItem.setText(resourceMap.getString("saveMenuItem.text")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('a');
        saveAsMenuItem.setText(resourceMap.getString("saveAsMenuItem.text")); // NOI18N
        saveAsMenuItem.setName("saveAsMenuItem"); // NOI18N
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);

        jSeparator4.setName("jSeparator4"); // NOI18N
        fileMenu.add(jSeparator4);

        executeJobMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        executeJobMenuItem.setText(resourceMap.getString("executeJobMenuItem.text")); // NOI18N
        executeJobMenuItem.setName("executeJobMenuItem"); // NOI18N
        executeJobMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeJobMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(executeJobMenuItem);

        jSeparator6.setName("jSeparator6"); // NOI18N
        fileMenu.add(jSeparator6);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getActionMap(MainView.class, this);
        zoomOutMenuItem.setAction(actionMap.get("zoomOut")); // NOI18N
        zoomOutMenuItem.setText(resourceMap.getString("zoomOutMenuItem.text")); // NOI18N
        zoomOutMenuItem.setToolTipText(resourceMap.getString("zoomOutMenuItem.toolTipText")); // NOI18N
        zoomOutMenuItem.setName("zoomOutMenuItem"); // NOI18N
        viewMenu.add(zoomOutMenuItem);

        zoomInMenuItem.setAction(actionMap.get("zoomIn")); // NOI18N
        zoomInMenuItem.setText(resourceMap.getString("zoomInMenuItem.text")); // NOI18N
        zoomInMenuItem.setToolTipText(resourceMap.getString("zoomInMenuItem.toolTipText")); // NOI18N
        zoomInMenuItem.setName("zoomInMenuItem"); // NOI18N
        viewMenu.add(zoomInMenuItem);

        zoomWindowMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.event.InputEvent.CTRL_MASK));
        zoomWindowMenuItem.setText(resourceMap.getString("zoomWindowMenuItem.text")); // NOI18N
        zoomWindowMenuItem.setToolTipText(resourceMap.getString("zoomWindowMenuItem.toolTipText")); // NOI18N
        zoomWindowMenuItem.setName("zoomWindowMenuItem"); // NOI18N
        zoomWindowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomWindowMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomWindowMenuItem);

        zoomRealMenuItem.setText(resourceMap.getString("zoomRealMenuItem.text")); // NOI18N
        zoomRealMenuItem.setToolTipText(resourceMap.getString("zoomRealMenuItem.toolTipText")); // NOI18N
        zoomRealMenuItem.setName("zoomRealMenuItem"); // NOI18N
        zoomRealMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomRealMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomRealMenuItem);

        jSeparator2.setName("jSeparator2"); // NOI18N
        viewMenu.add(jSeparator2);

        cameraActiveMenuItem.setText(resourceMap.getString("cameraActiveMenuItem.text")); // NOI18N
        cameraActiveMenuItem.setToolTipText(resourceMap.getString("cameraActiveMenuItem.toolTipText")); // NOI18N
        cameraActiveMenuItem.setName("cameraActiveMenuItem"); // NOI18N
        cameraActiveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cameraActiveMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(cameraActiveMenuItem);

        projectorActiveMenuItem.setText(resourceMap.getString("projectorActiveMenuItem.text")); // NOI18N
        projectorActiveMenuItem.setToolTipText(resourceMap.getString("projectorActiveMenuItem.toolTipText")); // NOI18N
        projectorActiveMenuItem.setName("projectorActiveMenuItem"); // NOI18N
        projectorActiveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projectorActiveMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(projectorActiveMenuItem);

        jSeparator7.setName("jSeparator7"); // NOI18N
        viewMenu.add(jSeparator7);

        showGridMenuItem.setText(resourceMap.getString("showGridMenuItem.text")); // NOI18N
        showGridMenuItem.setName("showGridMenuItem"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, previewPanel, org.jdesktop.beansbinding.ELProperty.create("${showGrid}"), showGridMenuItem, org.jdesktop.beansbinding.BeanProperty.create("selected"), "ShowGrid");
        bindingGroup.addBinding(binding);

        viewMenu.add(showGridMenuItem);

        menuBar.add(viewMenu);

        actionsMenu.setText(resourceMap.getString("actionsMenu.text")); // NOI18N
        actionsMenu.setName("actionsMenu"); // NOI18N

        facebookMenuItem.setText(resourceMap.getString("facebookMenuItem.text")); // NOI18N
        facebookMenuItem.setName("facebookMenuItem"); // NOI18N
        facebookMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                facebookMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(facebookMenuItem);

        thingiverseMenuItem.setText(resourceMap.getString("thingiverseMenuItem.text")); // NOI18N
        thingiverseMenuItem.setName("thingiverseMenuItem"); // NOI18N
        thingiverseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thingiverseMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(thingiverseMenuItem);

        webcamQRCodeMenuItem.setText(resourceMap.getString("webcamQRCodeMenuItem.text")); // NOI18N
        webcamQRCodeMenuItem.setName("webcamQRCodeMenuItem"); // NOI18N
        webcamQRCodeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                webcamQRCodeMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(webcamQRCodeMenuItem);

        menuBar.add(actionsMenu);

        optionsMenu.setText(resourceMap.getString("optionsMenu.text")); // NOI18N
        optionsMenu.setName("optionsMenu"); // NOI18N

        calibrateCameraMenuItem.setText(resourceMap.getString("calibrateCameraMenuItem.text")); // NOI18N
        calibrateCameraMenuItem.setName("calibrateCameraMenuItem"); // NOI18N
        calibrateCameraMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calibrateCameraMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(calibrateCameraMenuItem);

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setName("jMenu1"); // NOI18N

        jmImportSettings.setText(resourceMap.getString("jmImportSettings.text")); // NOI18N
        jmImportSettings.setName("jmImportSettings"); // NOI18N
        jmImportSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmImportSettingsActionPerformed(evt);
            }
        });
        jMenu1.add(jmImportSettings);

        jMenuItem1.setText(resourceMap.getString("jmDownloadSettings.text")); // NOI18N
        jMenuItem1.setName("jmDownloadSettings"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmDownloadSettingsActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jmExportSettings.setText(resourceMap.getString("jmExportSettings.text")); // NOI18N
        jmExportSettings.setName("jmExportSettings"); // NOI18N
        jmExportSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmExportSettingsActionPerformed(evt);
            }
        });
        jMenu1.add(jmExportSettings);

        editMappingMenuItem.setText(resourceMap.getString("editMappingMenuItem.text")); // NOI18N
        editMappingMenuItem.setName("editMappingMenuItem"); // NOI18N
        editMappingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editMappingMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(editMappingMenuItem);

        jmManageLaserprofiles.setText(resourceMap.getString("jmManageLaserprofiles.text")); // NOI18N
        jmManageLaserprofiles.setName("jmManageLaserprofiles"); // NOI18N
        jmManageLaserprofiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmManageLaserprofilesActionPerformed(evt);
            }
        });
        jMenu1.add(jmManageLaserprofiles);

        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        materialMenuItem.setText(resourceMap.getString("materialMenuItem.text")); // NOI18N
        materialMenuItem.setName("materialMenuItem"); // NOI18N
        materialMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                materialMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(materialMenuItem);

        optionsMenu.add(jMenu1);

        jmPreferences.setText(bundle1.getString("PREFERENCES")); // NOI18N
        jmPreferences.setName("jmPreferences"); // NOI18N
        jmPreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmPreferencesActionPerformed(evt);
            }
        });
        optionsMenu.add(jmPreferences);

        menuBar.add(optionsMenu);

        jmExtras.setText(resourceMap.getString("jmExtras.text")); // NOI18N
        jmExtras.setName("jmExtras"); // NOI18N

        jmInstallInkscape.setText(resourceMap.getString("jmInstallInkscape.text")); // NOI18N
        jmInstallInkscape.setName("jmInstallInkscape"); // NOI18N
        jmInstallInkscape.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmInstallInkscapeActionPerformed(evt);
            }
        });
        jmExtras.add(jmInstallInkscape);

        jmInstallIllustrator.setText(resourceMap.getString("jmInstallIllustrator.text")); // NOI18N
        jmInstallIllustrator.setName("jmInstallIllustrator"); // NOI18N
        jmInstallIllustrator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmInstallIllustratorActionPerformed(evt);
            }
        });
        jmExtras.add(jmInstallIllustrator);

        menuBar.add(jmExtras);

        helpMenu.setAction(actionMap.get("showAboutDialog")); // NOI18N
        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutDialog")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(warningPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 563, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(8, 8, 8)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(warningPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
      PreferencesManager.getInstance().getPreferences().setWindowBounds(MainView.this.getBounds());
      this.visicutModel1.updatePreferences();
      System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

  public void loadFile(File file, final boolean discardCurrent)
  {
    final File fileToLoad = file;
    lastDirectory = file.getParentFile();
    //refresh recent files
    List<String> recent = this.visicutModel1.getPreferences().getRecentFiles();
    recent.remove(file.getAbsolutePath());
    recent.add(0, file.getAbsolutePath());
    if (recent.size() > 5)
    {
      for (int i=recent.size()-1;i>=5;i--)
      {
        recent.remove(i);
      }
    }
    this.refreshRecentFilesMenu();
    try
    {
      PreferencesManager.getInstance().savePreferences();
    }
    catch (Exception ex)
    {
      Logger.getLogger(MainView.class.getName()).log(Level.SEVERE, null, ex);
    }
    new Thread()
    {

      @Override
      public void run()
      {
        MainView.this.loadFileReal(fileToLoad, discardCurrent);
      }
    }.start();
  }

  private void fitObjectsIntoBed()
  {
    final Map<PlfPart, AffineTransform> backup = new LinkedHashMap<PlfPart, AffineTransform>();
    for (PlfPart p : visicutModel1.getPlfFile())
    {
      backup.put(p, new AffineTransform(p.getGraphicObjects().getTransform()));
    }
    String text = "";
    Modification modification = this.visicutModel1.fitObjectsIntoBed();
    switch (modification.type)
    {
      case MOVE:
      {
        text = bundle.getString("NEEDED_MOVE");
        break;
      }
      case ROTATE:
      {
        text = bundle.getString("NEEDED_ROTATE");
        break;
      }
      case RESIZE:
      {
        text = bundle.getString("NEEDED_REFIT");
        break;
      }
      case NONE:
      {
        return;
      }
    }
    
    if( (modification.oldX != modification.newX) || (modification.oldY != modification.newY) )
    {
      text += "\n"+bundle.getString("OLDXY")+": (" + modification.oldX+","+ modification.oldY +")";
      text += "\n"+bundle.getString("CHANGED_TO")+": (" + modification.newX+","+ modification.newY +")";
    }    
    
    if( (modification.oldWidth != modification.newWidth) || (modification.oldHeight != modification.newHeight) )
    {
      text += "\n"+bundle.getString("OLDWH")+": (" +  modification.oldWidth+","+modification.oldHeight+")";
      text += "\n"+bundle.getString("CHANGED_TO")+": (" + modification.newWidth+","+modification.newHeight+")";
      text += "\n scaling factor: "+modification.factor;
    }
    
    
    warningPanel.addMessage(new Message("Info", text, Message.Type.INFO, new com.t_oster.uicomponents.warnings.Action[]
    {
      new com.t_oster.uicomponents.warnings.Action(bundle.getString("UNDO"))
      {
        @Override
        public boolean clicked()
        {
          for (Entry<PlfPart, AffineTransform> e : backup.entrySet())
          {
            e.getKey().getGraphicObjects().setTransform(e.getValue());
            VisicutModel.getInstance().firePartUpdated(e.getKey());
          }
          return true;
        } 
      }
    }
    ));
  }
  
  private void loadFileReal(File file, boolean discardCurrent)
  {
    // bring window to front - needed when VisiCut was called from a plugin
    this.toFront();
    this.requestFocus();
    
    // remove old error messages, they are no longer relevant (or for multiple files it is too confusing which one refers to which file)
    warningPanel.removeAllWarnings();

    try
    {
      this.progressBar.setIndeterminate(true);
      LinkedList<String> warnings = new LinkedList<String>();
      this.visicutModel1.loadFile(MappingManager.getInstance(), file, warnings, discardCurrent);
      if (!warnings.isEmpty())
        {
          dialog.showWarningMessage(warnings);
        }
      //if the image is too big, fit it a nd notify the user
      this.fitObjectsIntoBed();
      this.progressBar.setIndeterminate(false);
      this.refreshButtonStates(VisicutModel.PROP_PLF_PART_ADDED);
    }
    catch (Exception e)
    {
      this.progressBar.setIndeterminate(false);
      dialog.showErrorMessage(e, bundle.getString("ERROR WHILE OPENING '") + file.getName() + "'");
    }
  }

  /**
   * Sets all Buttons to their correct state (disabled/enabled)
   */
  public void refreshButtonStates(String action)
  {
    // Is called at application start up as well
    if (action != null && action.equals(VisicutModel.PROP_SELECTEDLASERDEVICE))
    {
      boolean cam = (!getVisiCam().isEmpty());
      boolean projector = this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getProjectorURL() != null && !this.visicutModel1.getSelectedLaserDevice().getProjectorURL().isEmpty();

      this.calibrateCameraMenuItem.setEnabled(cam);
      this.cameraActiveMenuItem.setEnabled(cam);
      previewPanel.setShowBackgroundImage(cam);
      setCameraActive(cam);

      this.projectorActiveMenuItem.setEnabled(projector);
      setProjectorActive(projector);
    }

    boolean estimateSupported = this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getLaserCutter().canEstimateJobDuration();
    this.calculateTimeButton.setVisible(estimateSupported);
    this.timeLabel.setVisible(estimateSupported);
    this.jLabel10.setVisible(estimateSupported);
    //check for focus-property in at least one profile type
    boolean focusSupported = false;
    if (this.visicutModel1.getSelectedLaserDevice() != null)
    {
      LaserCutter lc = this.visicutModel1.getSelectedLaserDevice().getLaserCutter();
      for (LaserProperty p : new LaserProperty[]{
        lc.getLaserPropertyForVectorPart(),
        lc.getLaserPropertyForRasterPart(),
        lc.getLaserPropertyForRaster3dPart()})
      {
        if (p != null && Arrays.asList(p.getPropertyKeys()).contains("focus"))
        {
          focusSupported = true;
          break;
        }
      }
    }
    if (!focusSupported || (MaterialManager.getInstance().getAll().size() == 1 && MaterialManager.getInstance().getAll().get(0).getMaterialThicknesses().size() == 1))
    {
      this.jCheckBox1.setSelected(false);
      this.jCheckBox1.setVisible(false);
      this.jSeparator1.setVisible(this.laserCutterComboBox.isVisible());
    }
    else
    {
      this.jCheckBox1.setVisible(true);
      this.jSeparator1.setVisible(true);
    }
    
    refreshExecuteButtons(false);
  }
  
  public void refreshExecuteButtons(boolean skipLockCheck)
  {
    boolean execute = this.visicutModel1.getMaterial() != null
      && this.visicutModel1.getSelectedLaserDevice() != null
      && this.visicutModel1.getPlfFile().size() > 0
      && (skipLockCheck || !isEditGuiForQRCodesDisabled());
    if (execute)
    {
      boolean jobEmpty = true;
      for(PlfPart p:this.visicutModel1.getPlfFile())
      {
        if (p.getMapping() != null && p.getMapping().size() > 0)
        {
          jobEmpty = false;
          break;
        }
      }
      if (jobEmpty)
      {
        execute = false;
      }
    }
    this.calculateTimeButton.setEnabled(execute);
    this.executeJobButton.setEnabled(execute);
    this.executeJobMenuItem.setEnabled(execute);
  }
  
  private File lastDirectory = null;

  private void openFileDialog(boolean discardCurrent)
  {
    final FileFilter allFilter = VisicutModel.getInstance().getAllFileFilter();
    //On Mac os, awt.FileDialog looks more native
    if (Helper.isMacOS())
    {
      FileDialog openFileChooser = new FileDialog(this, bundle.getString("PLEASE SELECT A FILE"));
      openFileChooser.setMode(FileDialog.LOAD);
      if (lastDirectory != null)
      {
        openFileChooser.setDirectory(lastDirectory.getAbsolutePath());
      }
      openFileChooser.setFilenameFilter(new FilenameFilter()
      {
        public boolean accept(File dir, String file)
        {
          return allFilter.accept(new File(dir, file));
        }
      });
      openFileChooser.setVisible(true);
      if (openFileChooser.getFile() != null)
      {
        File file = new File(new File(openFileChooser.getDirectory()), openFileChooser.getFile());
        loadFile(file, discardCurrent);
      }
    }
    else
    {
      JFileChooser openFileChooser = new JFileChooser();
      openFileChooser.setAcceptAllFileFilterUsed(false);
      for (FileFilter f : this.visicutModel1.getGraphicFileImporter().getFileFilters())
      {
        openFileChooser.addChoosableFileFilter(f);
      }
      openFileChooser.addChoosableFileFilter(allFilter);
      openFileChooser.setFileFilter(allFilter);
      openFileChooser.setCurrentDirectory(lastDirectory);
      int returnVal = openFileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION)
      {
        File file = openFileChooser.getSelectedFile();
        loadFile(file, discardCurrent);
      }
    }
  }

private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
  this.openFileDialog(true);
}//GEN-LAST:event_openMenuItemActionPerformed

  private void editMappings() throws FileNotFoundException, IOException
  {
    List<MappingSet> mappingsets = new LinkedList<MappingSet>();
    for (MappingSet m : MappingManager.getInstance().getAll())
    {
      mappingsets.add(m.clone());
    }
    EditMappingsDialog d = new EditMappingsDialog(this, true);
    d.setMappingSets(mappingsets);
    d.setVisible(true);
    mappingsets = d.getMappingSets();
    if (mappingsets != null)
    {
      MappingManager.getInstance().setAll(mappingsets);
      this.fillComboBoxes();
      this.previewPanel.repaint();
    }
  }



private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
  VisicutAboutBox box = new VisicutAboutBox(this);
  box.setModal(true);
  box.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed
  private int jobnumber = 0;

  private synchronized void executeJob()
  {
    try
    {
      final Map<LaserProfile, List<LaserProperty>> cuttingSettings = this.getPropertyMapForCurrentJob();
      if (cuttingSettings == null)
      {
        return;
      }     
      if (VisicutModel.getInstance().getStartPoint() != null)
      {
        if (!dialog.showYesNoQuestion(bundle.getString("STARTPOINTWARNING")))
        {
          return;
        }
      }
      
      setLaserJobInProgress(true);
      laserJobStarted();
      
      new Thread()
      {
        @Override
        public void run()
        {
          ProgressListener pl = new ProgressListener()
          {
            public void progressChanged(Object o, int i)
            {
              MainView.this.progressBar.setValue(i);
              MainView.this.progressBar.repaint();
            }
            public void taskChanged(Object o, String string)
            {
              MainView.this.progressBar.setString(string);
            }
          };
          MainView.this.progressBar.setMinimum(0);
          MainView.this.progressBar.setMaximum(100);
          MainView.this.progressBar.setValue(1);
          MainView.this.progressBar.setStringPainted(true);
          MainView.this.executeJobButton.setEnabled(false);
          MainView.this.executeJobMenuItem.setEnabled(false);
          try
          {
            MainView.this.warningPanel.removeAllWarnings();
            jobnumber++;
            String prefix = MainView.this.visicutModel1.getSelectedLaserDevice().getJobPrefix();
            String jobname = prefix+jobnumber;
            if (PreferencesManager.getInstance().getPreferences().isUseFilenamesForJobs())
            {
              //use filename of the PLF file or any part with a filename as job name
              PlfFile plf = MainView.this.visicutModel1.getPlfFile();
              List<PlfPart> plfParts = MainView.this.visicutModel1.getPlfFile().getPartsCopy();
              File f = plf.getFile();
              if (f == null)
              {
                for (PlfPart p : plfParts)
                {
                  if (p.getSourceFile() != null)
                  {
                    f = p.getSourceFile();
                    break;
                  }
                }
              }
              if (f != null)
              {
                jobname = f.getName();
              }
            }
            List<String> warnings = new LinkedList<String>();
            MainView.this.visicutModel1.sendJob(jobname, pl, cuttingSettings, warnings);
            for (String w : warnings)
            {
              dialog.showWarningMessage(w);
            }
            MainView.this.progressBar.setValue(0);
            MainView.this.progressBar.setString("");
            MainView.this.progressBar.setStringPainted(false);
            String txt = MainView.this.visicutModel1.getSelectedLaserDevice().getJobSentText();
            txt = txt.replace("$jobname", jobname).replace("$name", MainView.this.visicutModel1.getSelectedLaserDevice().getName());
            dialog.showSuccessMessage(txt);
          }
          catch (Exception ex)
          {
            if (ex instanceof IllegalJobException && ex.getMessage().startsWith("Illegal Focus value"))
            {
              dialog.showWarningMessage(bundle.getString("YOU MATERIAL IS TOO HIGH FOR AUTOMATIC FOCUSSING.PLEASE FOCUS MANUALLY AND SET THE TOTAL HEIGHT TO 0."));
            }
            else if (ex instanceof java.net.SocketTimeoutException)
            {
              dialog.showErrorMessage(ex, bundle.getString("SOCKETTIMEOUT")+" "+bundle.getString("CHECKSWITCHEDON"));
            }
            else if (ex instanceof java.net.UnknownHostException)
            {
              dialog.showErrorMessage(ex, bundle.getString("UNKNOWNHOST")+" "+bundle.getString("CHECKSWITCHEDON"));
            }
            else
            {
              dialog.showErrorMessage(ex);
            }
          }
          MainView.this.progressBar.setString("");
          MainView.this.progressBar.setValue(0);
          MainView.this.executeJobButton.setEnabled(true);
          MainView.this.executeJobMenuItem.setEnabled(true);
          setLaserJobInProgress(false);
          laserJobStopped();
        }
      }.start();
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex);
    }
  }

private void executeJobButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeJobButtonActionPerformed
    this.executeJob();
}//GEN-LAST:event_executeJobButtonActionPerformed

private void filesDropSupport1PropertyChange(java.beans.PropertyChangeEvent evt)//GEN-FIRST:event_filesDropSupport1PropertyChange
{//GEN-HEADEREND:event_filesDropSupport1PropertyChange
  if (this.filesDropSupport1.getDroppedFiles() != null && this.filesDropSupport1.getDroppedFiles().size() > 0)
  {
    for (File f : this.filesDropSupport1.getDroppedFiles())
    {
      this.loadFile(f, false);
    }
  }
}//GEN-LAST:event_filesDropSupport1PropertyChange

  private void save()
  {
    File file = null;
    //On Mac os, awt.FileDialog looks more native
    if (Helper.isMacOS())
    {
      FileDialog fdialog = new java.awt.FileDialog(this);
      fdialog.setMode(FileDialog.SAVE);
      if (lastDirectory != null)
      {
        fdialog.setDirectory(lastDirectory.getAbsolutePath());
      }
      fdialog.setVisible(true);
      if (fdialog.getFile() != null)
      {
        file = new File(new File(fdialog.getDirectory()), fdialog.getFile());
      }
    }
    else
    {
      saveFileChooser.setCurrentDirectory(lastDirectory);
      if (saveFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
      {
        file = saveFileChooser.getSelectedFile();
      }
    }
    if (file != null)
    {
      lastDirectory = file.getParentFile();
      if (!file.getName().endsWith("plf"))
      {
        file = new File(file.getAbsolutePath() + ".plf");
      }
      try
      {
        this.visicutModel1.saveToFile(MaterialManager.getInstance(), MappingManager.getInstance(), file);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex, bundle.getString("ERROR SAVING FILE"));
      }
    }
    else
    {
      //File access cancelled by user.
    }
  }

private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
  this.save();
}//GEN-LAST:event_saveAsMenuItemActionPerformed

private void visicutModel1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_visicutModel1PropertyChange
  if (evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_ADDED)
    ||evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_REMOVED)
    ||evt.getPropertyName().equals(VisicutModel.PROP_SELECTEDPART)) {
    // regenerate list of parts, update selection in ComboBox
    this.refreshObjectComboBox();
  }
    
  if (evt.getPropertyName().equals(VisicutModel.PROP_PLF_FILE_CHANGED))
  {
    MainView.this.timeLabel.setText("");
    if (this.visicutModel1.getPlfFile().getFile() != null)
    {
      this.setTitle("VisiCut - " + this.visicutModel1.getPlfFile().getFile().getName());
    }
    else
    {
      this.setTitle("VisiCut - Unnamed PLF");
    }
    this.refreshButtonStates(evt.getPropertyName());
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_SELECTEDLASERDEVICE)
    ||evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_UPDATED)
    ||evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_REMOVED))
  {
    MainView.this.timeLabel.setText("");
    this.refreshButtonStates(evt.getPropertyName());
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_SELECTEDPART))
  {
    PlfPart p = this.visicutModel1.getSelectedPart();
    this.mappingTabbedPane.setVisible(p != null);
    if (p != null)
    {
      if (p instanceof ParametricPlfPart)
      {
        if (this.mappingTabbedPane.indexOfTabComponent(this.parameterPanel) == -1)
        {
          this.mappingTabbedPane.add(bundle.getString("PARAMETERS"), this.parameterPanel);
        }
      }
      else
      {
        if (this.mappingTabbedPane.indexOfTabComponent(this.parameterPanel) == -1)
        {
          this.mappingTabbedPane.remove(this.parameterPanel);
        }
      }
    }
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_MATERIAL))
  {
    MainView.this.timeLabel.setText("");
    this.refreshMaterialThicknessesComboBox();
    this.refreshButtonStates(evt.getPropertyName());
  }
  // Called on application start (= loading preferences) and change preferences
  else if (evt.getPropertyName().equals(VisicutModel.PROP_PREFERENCES))
  {
    Preferences p = null;
    
    if (evt.getNewValue() != null)
    {
      p = (Preferences)(evt.getNewValue());
    }
    else if (PreferencesManager.getInstance() != null && PreferencesManager.getInstance().getPreferences() != null)
    {
      p = PreferencesManager.getInstance().getPreferences();
    }

    if (p != null)
    {
      btQRWebcamScan.setVisible(p.isEnableQRCodes());
      webcamQRCodeMenuItem.setVisible(p.isEnableQRCodes());
    }
  }
}//GEN-LAST:event_visicutModel1PropertyChange

private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
  if (this.visicutModel1.getPlfFile().getFile() == null || !this.visicutModel1.getPlfFile().getFile().exists())
  {//File is not PLF or no file loaded yet
    this.saveAsMenuItemActionPerformed(evt);
  }
  else
  {
    try
    {
      this.visicutModel1.saveToFile(MaterialManager.getInstance(), MappingManager.getInstance(), this.visicutModel1.getPlfFile().getFile());
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex, bundle.getString("ERROR SAVING FILE"));
    }
  }
}//GEN-LAST:event_saveMenuItemActionPerformed

private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
  this.previewPanel.setZoom(100d);
  this.visicutModel1.newPlfFile();
}//GEN-LAST:event_newMenuItemActionPerformed

private void calibrateCameraMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_calibrateCameraMenuItemActionPerformed
  if (this.visicutModel1.getBackgroundImage() == null)
  {
    dialog.showErrorMessage(bundle.getString("THE CAMERA DOESN'T SEEM TO BE WORKING. PLEASE CHECK THE URL IN THE LASERCUTTER SETTINGS"));
    return;
  }
  List<VectorProfile> profiles = ProfileManager.getInstance().getVectorProfiles();
  if (profiles.isEmpty())
  {
    dialog.showErrorMessage(bundle.getString("NEED_VECTOR_PROFILE"));
    return;
  }
  VectorProfile p = dialog.askElement(profiles, bundle.getString("SELECT_VECTOR_PROFILE"));
  if (p == null)
  {
    return;
  }
  //TODO ask user for VectorProfile and make sure the properties for current
  //material and cutter are available
  CamCalibrationDialog ccd = new CamCalibrationDialog(this, true);
  ccd.setVectorProfile(p);
  ccd.setBackgroundImage(this.visicutModel1.getBackgroundImage());
  ccd.setImageURL(getVisiCam());
  ccd.setResultingTransformation(this.visicutModel1.getSelectedLaserDevice().getCameraCalibration());
  ccd.setVisible(true);
  this.visicutModel1.getSelectedLaserDevice().setCameraCalibration(ccd.getResultingTransformation());
  try
  {
    LaserDeviceManager.getInstance().save(this.visicutModel1.getSelectedLaserDevice());
  }
  catch (Exception ex)
  {
    dialog.showErrorMessage(ex, bundle.getString("ERROR WHILE SAVING SETTINGS"));
  }
  this.previewPanel.repaint();
}//GEN-LAST:event_calibrateCameraMenuItemActionPerformed

private void executeJobMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeJobMenuItemActionPerformed
  this.executeJob();
}//GEN-LAST:event_executeJobMenuItemActionPerformed

  public synchronized void captureImage()
  {
    if (!cameraCapturing)
    {
      cameraCapturing = true;

      new Thread()
      {
        @Override
        public void run()
        {
          URLConnection conn=null;
          try
          {
            URL src = new URL(getVisiCam());
            conn = src.openConnection();
            conn.setConnectTimeout(5*1000); // 5s connect timeout
            conn.setReadTimeout(30*1000); // 30s read timeout after connecting

            // HTTP authentication
            if (VisicutModel.getInstance() != null && VisicutModel.getInstance().getSelectedLaserDevice() != null)
            {
              String encodedCredentials = Helper.getEncodedCredentials(VisicutModel.getInstance().getSelectedLaserDevice().getURLUser(), VisicutModel.getInstance().getSelectedLaserDevice().getURLPassword());
              if (!encodedCredentials.isEmpty())
              {
                conn.setRequestProperty("Authorization", "Basic " + encodedCredentials);
              }
            }

            ImageInputStream stream=new MemoryCacheImageInputStream(conn.getInputStream());
            BufferedImage back = ImageIO.read(stream);
            if (back != null && MainView.this.visicutModel1.getBackgroundImage() == null)
            {//First Time Image is Captured => resize View
              // TODO also called when image reappears after error, while the user is moving some object around -- not so good. Put this call somewhere else.
              // MainView.this.previewPanel.setZoom(100d);
            }
            
            // Check again if camera and background are active, might have changed in the meantime, because of threading
            if (back != null && isCameraActive() && isPreviewPanelShowBackgroundImage())
            {
              MainView.this.visicutModel1.setBackgroundImage(back);
            }
            cameraCapturingError = "";
          }
          catch (Exception ex)
          {
            MainView.this.visicutModel1.setBackgroundImage(null);
            ex.printStackTrace();
            if (ex instanceof IOException && conn instanceof HttpURLConnection) {
              // possible HTTP error - if the server sent a message, display it
              // This can be used by VisiCam to show an error message like "error capturing image"
              String msg="";
              int responseCode=0;
              try {
                responseCode=((HttpURLConnection) conn).getResponseCode();
                if (responseCode != 200) {
                  // received a text error message, display it
                  InputStream stream=((HttpURLConnection) conn).getErrorStream();
                  if (stream == null) {
                    msg="(no message sent)";
                  } else {
                    InputStreamReader errorReader=new InputStreamReader(stream);
                    StringBuilder buffer=new StringBuilder();
                    int c;
                    while ((c=errorReader.read()) != -1) {
                      // read until finished
                      buffer.append((char) c);
                      if (buffer.length() > 200) {
                        buffer.append("...");
                        break;
                      }
                    }
                    msg=buffer.toString();
                  }
                } else {
                  // server sent HTTP OK, so the exception is not a HTTP problem
                  msg=ex.toString();
                }
              } catch(Exception e) {
                e.printStackTrace();
                if (e instanceof java.net.SocketException) {
                    // Network errors like "port not found" have meaningful error messages
                    msg=e.getLocalizedMessage();
                } else {
                  // Most other exceptions are not easy to understand without the class name
                  // (e.g. 'java.net.UnknownHostException: foo.example.com')
                  msg = ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
                }
              }
              if (responseCode != 0) {
                msg = msg + "\n(HTTP " + responseCode + ")";
              }
              if (responseCode == 503 && conn.getHeaderFields().containsKey("Retry-After")) {
                    // For temporary errors like 'marker not found' that should not cause an error message,
                    // the VisiCam server can send status 503 with a Retry-After header.
                    // Then we don't show an error message.
                    cameraCapturingError = "";
                    System.err.println("ignoring camera error (as signaled by HTTP 503 + Retry-After).");
              } else  {
                cameraCapturingError = bundle.getString("ERROR CAPTURING PHOTO") + ": "+ msg;
              }
            } else {
              cameraCapturingError = bundle.getString("ERROR CAPTURING PHOTO") + "\nError (" + ex.getClass().getSimpleName() + "): " + ex.getLocalizedMessage();
            }
          }

          MainView.this.cameraCapturing = false;
        }
      }.start();
    }
  }
  
  public boolean isVisiCamDetected(){
    try{
      String url = MainView.this.visicutModel1.getSelectedLaserDevice().getCameraURL();
      return url != null && !"".equals(url);
    }catch(Exception e){
      return false;
    }
  }
  
  public String getVisiCam()
  {
    LaserDevice dev = MainView.this.visicutModel1.getSelectedLaserDevice();
    return (dev != null && dev.getCameraURL() != null) ? dev.getCameraURL() : "";
  }

  @Action
  public void zoomIn()
  {
    previewPanel.setZoom(previewPanel.getZoom() - (-2 * previewPanel.getZoom() / 32));
  }

  @Action
  public void zoomOut()
  {
    previewPanel.setZoom(previewPanel.getZoom() - (2 * previewPanel.getZoom() / 32));
  }

private void editMappingMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editMappingMenuItemActionPerformed
    try
    {
      this.editMappings();
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex);
    }
}//GEN-LAST:event_editMappingMenuItemActionPerformed

private boolean ignoreMaterialComboBoxChanges = false;
private void materialComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_materialComboBoxActionPerformed
  if (ignoreMaterialComboBoxChanges)
  {
    return;
  }
  MaterialProfile newMaterial = this.materialComboBox.getSelectedItem() instanceof MaterialProfile ? (MaterialProfile) this.materialComboBox.getSelectedItem() : null;
  if (!Util.differ(newMaterial, visicutModel1.getMaterial()))
  {
    return;
  }
  this.visicutModel1.setMaterial(newMaterial);
}//GEN-LAST:event_materialComboBoxActionPerformed

  private void materialMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_materialMenuItemActionPerformed
  {//GEN-HEADEREND:event_materialMenuItemActionPerformed
    EditMaterialsDialog d = new EditMaterialsDialog(this, true);
    d.setMaterials(MaterialManager.getInstance().getAll());
    d.setVisible(true);
    List<MaterialProfile> result = d.getMaterials();
    if (result != null)
    {
      try
      {
        MaterialManager.getInstance().setAll(result);
        fixMaterialThicknesses();
        this.refreshMaterialComboBox();
        this.visicutModel1.setMaterial(this.materialComboBox.getSelectedItem() instanceof MaterialProfile ? (MaterialProfile) this.materialComboBox.getSelectedItem() : null);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
      
    }
  }//GEN-LAST:event_materialMenuItemActionPerformed

  private void laserCutterComboBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_laserCutterComboBoxActionPerformed
  {//GEN-HEADEREND:event_laserCutterComboBoxActionPerformed
    if (!ignoreLaserCutterComboBoxUpdates)
    {
      LaserDevice newDev = laserCutterComboBox.getSelectedItem() instanceof LaserDevice ? (LaserDevice) laserCutterComboBox.getSelectedItem() : null;
      if (!Util.differ(newDev, visicutModel1.getSelectedLaserDevice()))
      {
        return;
      }
      this.visicutModel1.setSelectedLaserDevice(newDev);
      if (this.visicutModel1.getSelectedLaserDevice() == null || this.visicutModel1.getSelectedLaserDevice().getCameraURL() == null || "".equals(this.visicutModel1.getSelectedLaserDevice().getCameraURL()))
      {
        this.visicutModel1.setBackgroundImage(null);
      }

      this.refreshButtonStates(VisicutModel.PROP_SELECTEDLASERDEVICE);
      //if the image is too big, fit it and notify the user
      this.fitObjectsIntoBed();
    }
  }//GEN-LAST:event_laserCutterComboBoxActionPerformed

  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem2ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem2ActionPerformed
    ManageLasercuttersDialog d = new ManageLasercuttersDialog(this, true);
    d.setLaserCutters(LaserDeviceManager.getInstance().getAll());
    d.setVisible(true);
    List<LaserDevice> result = d.getLaserCutters();
    if (result != null)
    {
      try
      {
        LaserDevice old = VisicutModel.getInstance().getSelectedLaserDevice();
        LaserDeviceManager.getInstance().setAll(result);
        if (old != null)
        {
          boolean found = false;
          VisicutModel.getInstance().setSelectedLaserDevice(null);
          for (LaserDevice ld : result)
          {
            if (ld.getName().equals(old.getName()))
            {
              found = true;
              VisicutModel.getInstance().setSelectedLaserDevice(ld);
              break;
            }
          }
          if (!found && result.size() > 0)
          {//if the current selected lasercutter was deleted, select the first
            VisicutModel.getInstance().setSelectedLaserDevice(result.get(0));
          }
        }
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex, bundle.getString("ERROR SAVING PREFERENCES"));
      }
      this.fillComboBoxes();
      this.refreshButtonStates(VisicutModel.PROP_SELECTEDLASERDEVICE);
    }
  }//GEN-LAST:event_jMenuItem2ActionPerformed

  private void calculateTimeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_calculateTimeButtonActionPerformed
  {//GEN-HEADEREND:event_calculateTimeButtonActionPerformed
    new Thread()
    {

      @Override
      public void run()
      {
        try
        {
          MainView.this.calculateTimeButton.setEnabled(false);
          MainView.this.timeLabel.setText("...");
          MainView.this.timeLabel.setText(Helper.toHHMMSS(MainView.this.visicutModel1.estimateTime(MainView.this.getPropertyMapForCurrentJob())));
          MainView.this.calculateTimeButton.setEnabled(true);
        }
        catch (Exception ex)
        {
          dialog.showErrorMessage(ex);
          MainView.this.timeLabel.setText("error");
          MainView.this.calculateTimeButton.setEnabled(true);
        }
      }
    }.start();
  }//GEN-LAST:event_calculateTimeButtonActionPerformed

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
  {//GEN-HEADEREND:event_jButton1ActionPerformed
    previewPanel.setZoom(previewPanel.getZoom() + (previewPanel.getZoom() / 32));
  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
  {//GEN-HEADEREND:event_jButton2ActionPerformed
    previewPanel.setZoom(previewPanel.getZoom() - (previewPanel.getZoom() / 32));
  }//GEN-LAST:event_jButton2ActionPerformed

  private void jmExportSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmExportSettingsActionPerformed
  {//GEN-HEADEREND:event_jmExportSettingsActionPerformed
    File file = null;
    
    // Show warning if passwords are exported
    boolean fabqrPrivatePasswordSet = (PreferencesManager.getInstance() != null && PreferencesManager.getInstance().getPreferences() != null
                                      && PreferencesManager.getInstance().getPreferences().getFabqrPrivatePassword() != null && !PreferencesManager.getInstance().getPreferences().getFabqrPrivatePassword().isEmpty());
    
    boolean urlPasswordSet = (VisicutModel.getInstance() != null && VisicutModel.getInstance().getSelectedLaserDevice() != null
                                  && VisicutModel.getInstance().getSelectedLaserDevice().getURLPassword() != null && !VisicutModel.getInstance().getSelectedLaserDevice().getURLPassword().isEmpty());

    if (fabqrPrivatePasswordSet || urlPasswordSet)
    {
      if (!dialog.showYesNoQuestion(bundle.getString("DIALOG_QUESTION_EXPORT_PASSWORD")))
      {
        return;
      }
    }
    
    //On Mac os, awt.FileDialog looks more native
    if (Helper.isMacOS())
    {
      FileDialog fileDialog = new java.awt.FileDialog(this);
      fileDialog.setMode(FileDialog.SAVE);
      if (lastDirectory != null)
      {
        fileDialog.setDirectory(lastDirectory.getAbsolutePath());
      }
      fileDialog.setVisible(true);
      if (fileDialog.getFile() != null)
      {
        file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
      }
    }
    else
    {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogType(JFileChooser.SAVE_DIALOG);
      chooser.setCurrentDirectory(lastDirectory);
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
      {
        file = chooser.getSelectedFile();
      }
    }
    if (file != null)
    {
      if (!file.getName().toLowerCase().endsWith(".vcsettings"))
      {
        file = new File(file.getParentFile(), file.getName()+".vcsettings");
      }
      try
      {
        PreferencesManager.getInstance().exportSettings(file);
        dialog.showSuccessMessage(bundle.getString("SETTINGS SUCCESSFULLY EXPORTED"));
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
    }
  }//GEN-LAST:event_jmExportSettingsActionPerformed

  /**
   * load settings from file and update the GUI
   * @param file : File or null for loading the default example settings
   * @throws Exception 
   */
  private void importSettingsFromFile(File file) throws Exception {
    PreferencesManager.getInstance().importSettings(file);
    this.visicutModel1.setPreferences(PreferencesManager.getInstance().getPreferences());
    this.fillComboBoxes();
    this.refreshExampleMenu();
    dialog.showSuccessMessage(bundle.getString("SETTINGS SUCCESSFULLY IMPORTED"));
  }
  
  private void jmImportSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmImportSettingsActionPerformed
  {//GEN-HEADEREND:event_jmImportSettingsActionPerformed
    if (!askForOverwriteSettings()) {
      return;
    }
    this.importSettingsAskForFile();
  }//GEN-LAST:event_jmImportSettingsActionPerformed

  /**
   * display a file chooser for importing settings and then import them
   */
  private void importSettingsAskForFile() {
    try
    {
      final FileFilter zipFilter = new ExtensionFilter(new String[]{".zip",".vcsettings"}, bundle.getString("ZIPPED SETTINGS (*.ZIP)"));
      File file = null;
      //On Mac os, awt.FileDialog looks more native
      if (Helper.isMacOS())
      {
        FileDialog openFileChooser = new FileDialog(this, bundle.getString("PLEASE SELECT A FILE"));
        openFileChooser.setMode(FileDialog.LOAD);
        if (lastDirectory != null)
        {
          openFileChooser.setDirectory(lastDirectory.getAbsolutePath());
        }
        openFileChooser.setFilenameFilter(new FilenameFilter()
        {
          public boolean accept(File dir, String file)
          {
            return zipFilter.accept(new File(dir, file));
          }
        });
        openFileChooser.setVisible(true);
        if (openFileChooser.getFile() != null)
        {
          file = new File(new File(openFileChooser.getDirectory()), openFileChooser.getFile());
        }
      }
      else
      {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setAcceptAllFileFilterUsed(false);
        openFileChooser.addChoosableFileFilter(zipFilter);
        openFileChooser.setFileFilter(zipFilter);
        openFileChooser.setCurrentDirectory(lastDirectory);
        int returnVal = openFileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
          file = openFileChooser.getSelectedFile();
        }
      }
      if (file != null)
      {
        this.importSettingsFromFile(file);
      }
    }
    catch (Exception e)
    {
      dialog.showErrorMessage(e);
    }
  }
  
  private void jmManageLaserprofilesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmManageLaserprofilesActionPerformed
  {//GEN-HEADEREND:event_jmManageLaserprofilesActionPerformed
    EditProfilesDialog d = new EditProfilesDialog(this, true);
    List<LaserProfile> profiles = new LinkedList<LaserProfile>();
    profiles.addAll(ProfileManager.getInstance().getAll());
    d.setProfiles(profiles);
    d.setVisible(true);
    List<LaserProfile> result = d.getProfiles();
    if (result != null)
    {
      try
      {
        ProfileManager.getInstance().setAll(result);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
      this.fillComboBoxes();
    }
  }//GEN-LAST:event_jmManageLaserprofilesActionPerformed

  private void btAddMaterialActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btAddMaterialActionPerformed
  {//GEN-HEADEREND:event_btAddMaterialActionPerformed
    CreateNewMaterialDialog cd = new CreateNewMaterialDialog(this, true);
    cd.setVisible(true);
    if (!cd.isOkClicked())
    {//user pressed cancel
      return;
    }
    MaterialProfile m;
    MaterialProfile om = cd.getSelectedProfile();
    if (om != null)
    {//we're using an existing material profile
      m = om.clone();
      //create only the selected thicknesses
      m.setMaterialThicknesses(cd.getSelectedThicknesses());
      m.setName(m.getName()+" 2");
    }
    else
    {//create from scratch
      m = new MaterialProfile();
    }
    EditMaterialDialog d = new EditMaterialDialog(this, true);
    d.setMaterial(m);
    d.setVisible(true);
    m = d.getMaterial();
    if (m != null)
    {
      try
      {
        MaterialManager.getInstance().add(m);
        if (om != null)
        {//we were cloning, so copy all lasercutter settings for the selected thicknesses
          for (Float f : cd.getSelectedThicknesses())
          {
            if (m.getMaterialThicknesses().contains(f))
            {
              for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
              {
                for (LaserProfile lp : ProfileManager.getInstance().getAll())
                {
                  List<LaserProperty> props = LaserPropertyManager.getInstance().getLaserProperties(ld, om, lp, f);
                  if (props != null)
                  {
                    List<LaserProperty> clones = new LinkedList<LaserProperty>();
                    for (LaserProperty p : props)
                    {
                      clones.add(p.clone());
                    }
                    LaserPropertyManager.getInstance().saveLaserProperties(ld, m, lp, f, clones);
                  }
                }
              }
            }
          }
        }
        this.refreshMaterialComboBox();
        this.materialComboBox.setSelectedItem(m);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
    }
  }//GEN-LAST:event_btAddMaterialActionPerformed

  private void jmInstallInkscapeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmInstallInkscapeActionPerformed
  {//GEN-HEADEREND:event_jmInstallInkscapeActionPerformed
    try
    {
      Helper.installInkscapeExtension();
      dialog.showSuccessMessage(bundle.getString("INSTALLED EXTENSION SUCCESSFULLY"));
    }
    catch (Exception e)
    {
      dialog.showErrorMessage(e, bundle.getString("THERE WAS AN ERROR DURING THE INSTALLATION"));
    }
  }//GEN-LAST:event_jmInstallInkscapeActionPerformed

  private void jmInstallIllustratorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmInstallIllustratorActionPerformed
  {//GEN-HEADEREND:event_jmInstallIllustratorActionPerformed
    try
    {
      Helper.installIllustratorScript();
      dialog.showSuccessMessage(bundle.getString("INSTALLED EXTENSION SUCCESSFULLY"));
    }
    catch (Exception e)
    {
      dialog.showErrorMessage(e, bundle.getString("THERE WAS AN ERROR DURING THE INSTALLATION"));
    }
  }//GEN-LAST:event_jmInstallIllustratorActionPerformed
private void btAddMaterialThicknessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btAddMaterialThicknessActionPerformed
  MaterialProfile m = VisicutModel.getInstance().getMaterial();
  if (m != null)
  {
    CreateNewThicknessDialog d = new CreateNewThicknessDialog(this, true);
    d.setAvailableThicknesses(m.getMaterialThicknesses());
    d.setVisible(true);
    if (!d.isOkClicked())
    {
      return;
    }
    Float f = d.getSelectedNewThickness();
    List<Float> th = m.getMaterialThicknesses();
    if (th.contains(f))
    {
      return;
    }
    th.add(f);
    Collections.sort(th);
    try
    {
      MaterialManager.getInstance().save(m);
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex, "Could not save material thickness");
    }
    Float copyThickness = d.getSelectedCopyThickness();
    if (copyThickness != null)
    {
      for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
      {
        for (LaserProfile lp : ProfileManager.getInstance().getAll())
        {
          try
          {
            List<LaserProperty> props = LaserPropertyManager.getInstance().getLaserProperties(ld, m, lp, f);
            if (props != null)
            {
              List<LaserProperty> clones = new LinkedList<LaserProperty>();
              for (LaserProperty p : props)
              {
                clones.add(p.clone());
              }
              LaserPropertyManager.getInstance().saveLaserProperties(ld, m, lp, f, clones);
            }
          }
          catch (Exception ex)
          {
            dialog.showErrorMessage(ex, "Could not copy material thickness");
          }
        }
      }
    }
    this.refreshMaterialThicknessesComboBox();
    this.cbMaterialThickness.setSelectedItem(f);
  }
}//GEN-LAST:event_btAddMaterialThicknessActionPerformed

private void cbMaterialThicknessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbMaterialThicknessActionPerformed
  Float f = (Float) this.cbMaterialThickness.getSelectedItem();
  if (f != null)
  {
    this.visicutModel1.setMaterialThickness(f);
  }
}//GEN-LAST:event_cbMaterialThicknessActionPerformed

  private void btFitScreenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btFitScreenActionPerformed
  {//GEN-HEADEREND:event_btFitScreenActionPerformed
    this.previewPanel.setZoom(100d);
  }//GEN-LAST:event_btFitScreenActionPerformed

  private void bt1to1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_bt1to1ActionPerformed
  {//GEN-HEADEREND:event_bt1to1ActionPerformed
    this.previewPanel.setOneToOneZoom();
  }//GEN-LAST:event_bt1to1ActionPerformed

private void importMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importMenuItemActionPerformed
  this.openFileDialog(false);
}//GEN-LAST:event_importMenuItemActionPerformed

private void btRemoveObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btRemoveObjectActionPerformed
  VisicutModel.getInstance().removeSelectedPart();
}//GEN-LAST:event_btRemoveObjectActionPerformed

private void btAddObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btAddObjectActionPerformed
  importMenuItemActionPerformed(evt);
}//GEN-LAST:event_btAddObjectActionPerformed

private boolean ignoreObjectComboBoxEvents = false;
private void objectComboBoxChangeHandler(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_objectComboBoxChangeHandler
  if (ignoreObjectComboBoxEvents || !(this.objectComboBox.getSelectedItem() instanceof PlfPart)) {
    // the user selected the "please select something" item - ignore this
    return;
  }
  PlfPart selected = (PlfPart) this.objectComboBox.getSelectedItem();
  if (!VisicutModel.getInstance().getPlfFile().contains(selected)) {
    // not available - can this happen? maybe if a strange timing occurs while loading a file and changing the combobox
    return;
  }
  if (evt.getStateChange()!=java.awt.event.ItemEvent.SELECTED) {
    return;
  }
  VisicutModel.getInstance().setSelectedPart(selected);
}//GEN-LAST:event_objectComboBoxChangeHandler

private void jmPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmPreferencesActionPerformed
  PreferencesDialog pd = new PreferencesDialog(this, true);
  pd.setLocationRelativeTo(null);
  pd.setPreferences(this.visicutModel1.getPreferences().clone());
  pd.setVisible(true);
  if (pd.getPreferences() != null)
  {
    try
    {
      this.visicutModel1.setPreferences(pd.getPreferences());
      PreferencesManager.getInstance().savePreferences(pd.getPreferences());
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex);
    }
  }
}//GEN-LAST:event_jmPreferencesActionPerformed
  
  private void btFacebookActionPerformed(java.awt.event.ActionEvent evt)                                           
  {
    startFacebook();
  }                                          
  
  private String browserLoginDialog(String title, String loginUrl, String redirectUrlPrefix) throws Exception
  {
    // JavaFX available, load BrowserLoginDialog dynamically (depends on JavaFX)
    String browserCode = null;
    
    URL jarUrl = MainView.class.getResource("lib/BrowserLoginDialog.jar");
    URLClassLoader classLoader = new URLClassLoader(new URL[] { jarUrl }, MainView.class.getClassLoader());
    Class<?> ThingiverseLoginDialog = classLoader.loadClass("com.tur0kk.thingiverse.fxgui.BrowserLoginDialog");

    Class<?>[] constructorParameterTypes = new Class[]
    {
      java.awt.Frame.class,
      boolean.class,
      String.class,
      String.class,
      String.class
    };

    Constructor<?> constructor = ThingiverseLoginDialog.getConstructor(constructorParameterTypes);

    // Create instance
    Object loginDialog = constructor.newInstance(new Object[]
    {
      this, true, title, loginUrl, redirectUrlPrefix
    });

    // Parameter types for methods
    Class<?>[] setVisibleParameterTypes = new Class[]
    {
      boolean.class
    };

    Class<?>[] getBrowserCodeParameterTypes = new Class[]
    {

    };

    Method setVisibleMethod = ThingiverseLoginDialog.getMethod("setVisible", setVisibleParameterTypes);
    Method getBrowserCodeMethod = ThingiverseLoginDialog.getMethod("getBrowserCode", getBrowserCodeParameterTypes);

    Object[] setVisibleArgumentList = new Object[]
    {
      true
    };

    Object[] getBrowserCodeArgumentList = new Object[]
    {

    };

    // invoke JavaFX browser with thingiverse website
    setVisibleMethod.invoke(loginDialog, setVisibleArgumentList);
    browserCode = (String) getBrowserCodeMethod.invoke(loginDialog, getBrowserCodeArgumentList);
    
    return browserCode;
  }
  
  private String systemBrowserLogin(String name, String loginUrl) throws Exception
  {
    // if JavaFX is not available use system browser to show thingiverse website. Necessary to copy auth code by hand.
    String browserCode;
  
    Desktop.getDesktop().browse(URI.create(loginUrl));
    browserCode = javax.swing.JOptionPane.showInputDialog("Log in with your " + name + "-account, click allow, paste code here:");

    return browserCode;
  }
  
  private boolean isJavaFxAvailable()
  {
    try
    {
      ClassLoader classLoader = MainView.class.getClassLoader();
      classLoader.loadClass("javafx.embed.swing.JFXPanel");
      return true;
    }
    catch (ClassNotFoundException e)
    {
      return false;
    }
  }
private boolean askForOverwriteSettings() {
    if (LaserDeviceManager.getInstance().getAll().isEmpty() && 
      MaterialManager.getInstance().getAll().isEmpty() &&
      ProfileManager.getInstance().getAll().isEmpty() &&
      MappingManager.getInstance().getAll().isEmpty()) {
      // do not ask if settings are empty
      return true;
    }
    int answer=JOptionPane.showConfirmDialog(this, bundle.getString("IMPORT_SETTINGS_OVERWRITE?"), bundle.getString("WARNING"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    return answer==JOptionPane.OK_OPTION;
}

private void startFacebook()
{
  /*
   * just hide facebookDialog on close to keep state.
   * if logged out, create new instance of dialog
   */
  FacebookManager facebook = FacebookManager.getInstance();

  try
  {
    // Try login with persistent access token.
    boolean loginSuccess = facebook.logIn();

    if (!loginSuccess)
    {
      String loginUrl = facebook.initiateAuthentication();
      String browserCode;

      if (isJavaFxAvailable())
      {
        browserCode = browserLoginDialog("Facebook Login", loginUrl, facebook.getRedirectUrlPrefix());
      }
      else
      {
        // JavaFX not available...
        System.out.println("JavaFX is not available. Using fallback behavior.");
        browserCode = systemBrowserLogin("Facebook", loginUrl);
      }

      facebook.logIn(browserCode);
    }

    if (facebook.isLoggedIn())
    {
      FacebookDialog facebookDialog = new FacebookDialog(this, true);
      facebookDialog.setLocationRelativeTo(null);
      facebookDialog.setVisible(true);
    }
  }
  catch (Exception ex)
  {
    ex.printStackTrace();
    this.dialog.showErrorMessage("Unable to load FacebookDialog");
  }
}

private void startThingiverse()
{
  ThingiverseManager thingiverse = ThingiverseManager.getInstance();

  /*
   * Just hide thingiverseDialog on close to keep state.
   * if logged out, create new instance of ThingiverseDialog
   */
  if (!thingiverse.isLoggedIn() || thingiverseDialog == null)
  {
    try
    {
      // Try login with persistent access token from disk.
      boolean loginSuccess = thingiverse.logIn();

      if (!loginSuccess)
      {
        // Login with persitent token failed.
        // Start new authentication procedure.
        String loginUrl = thingiverse.initiateAuthentication();
        String browserCode = "";

        if (isJavaFxAvailable())
        {
          // If java fx is available we open a browser dialog and let the user
          // enter his credentials. This method blocks until the dialog
          // closes itself and returns a code from thingiverse.
          browserCode = browserLoginDialog("Thingiverse Login", loginUrl, thingiverse.getRedirectUrlPrefix());
        }
        else
        {
          // JavaFX not available...
          // Open system default browser and let the user copy/paste the
          // browser code.
          System.out.println("JavaFX is not available. Using fallback behavior.");
          browserCode = systemBrowserLogin("Thingiverse", loginUrl);
        }

        thingiverse.logIn(browserCode);
      }

      if (thingiverse.isLoggedIn())
      {
        thingiverseDialog = new ThingiverseDialog(this, true);
        thingiverseDialog.setLocationRelativeTo(null);
        thingiverseDialog.setVisible(true);
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      this.dialog.showErrorMessage("Unable to load ThingiverseDialog");
    }
  }
  else // instance available, show thingiverseDialog
  {
    thingiverseDialog.setVisible(true);
  }
}

private void jmDownloadSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmDownloadSettingsActionPerformed
  warningPanel.removeAllWarnings();
  Map<String,String> choices = new LinkedHashMap<String, String>();
  choices.put(bundle.getString("EXAMPLE_SETTINGS"), "__DEFAULT__");
  choices.put(bundle.getString("EMPTY_SETTINGS"), "__EMPTY__");
  choices.put(bundle.getString("IMPORT_SETTINGS_FROM_FILE"),"__FILE__");
  // TODO add FabLab Aachen etc.
  // Want your lab in this list? Look at https://github.com/t-oster/VisiCut/wiki/How-to-add-default-settings-for-your-lab !
  // choices.put("Country, City: Institution", "http://whatever.com/foo");
  choices.put("Germany, Aachen: FabLab RWTH Aachen", "https://github.com/renebohne/zing6030-visicut-settings/archive/master.zip");
  choices.put("Germany, Erlangen: FAU FabLab", "https://github.com/fau-fablab/visicut-settings/archive/master.zip");
  choices.put("Germany, Nuremberg: Fab lab Region Nürnberg e.V.", "https://github.com/fablabnbg/visicut-settings/archive/master.zip");
  choices.put("Germany, Berlin: Fab Lab Berlin", "https://github.com/FabLabBerlin/visicut-settings/archive/master.zip");
  choices.put("Germany, Bremen: FabLab Bremen e.V.", "http://www.fablab-bremen.org/FabLab_Bremen.vcsettings");
  choices.put("Germany, Paderborn: FabLab Paderborn e.V.", "https://github.com/fablab-paderborn/visicut-settings/archive/master.zip");
  choices.put("Netherlands, Amersfoort: FabLab", "https://github.com/Fablab-Amersfoort/visicut-settings/archive/master.zip");
  choices.put("United Kingdom, Manchester: Hackspace", "https://github.com/hacmanchester/visicut-settings/archive/master.zip");
  choices.put("United Kingdom, Leeds: Hackspace", "https://github.com/leedshackspace/visicut-settings/archive/master.zip");
  choices.put(bundle.getString("DOWNLOAD_NOT_IN_LIST"),"__HELP__");
  String s = (String)JOptionPane.showInputDialog(this, bundle.getString("DOWNLOAD_SETTINGS_INFO"), null, JOptionPane.PLAIN_MESSAGE, null, choices.keySet().toArray(), choices.keySet().toArray()[0]);
  if ((s==null) || (s.length() == 0)) {
    return;
  }
  if (!askForOverwriteSettings()) {
    return;
  }
  String url="";
  try {
    if (choices.containsKey(s)) {
      url=choices.get(s);
      if (url.equals("__DEFAULT__")) {
        // load default settings
        this.importSettingsFromFile(null);
        return;
      } else if (url.equals("__HELP__")) {
        dialog.showInfoMessage("Please look at https://github.com/t-oster/VisiCut/wiki/How-to-add-default-settings-for-your-lab . \n You can reopen this dialog in Edit -> Settings -> Download.");
        return;
      } else if (url.equals("__FILE__")) {
        this.importSettingsAskForFile();
        return;
      } else if (url.equals("__EMPTY__")) {
        this.importSettingsFromFile(new File("__EMPTY__"));
        return;
      }
    }
    if (!(url.startsWith("https://") || url.startsWith("http://"))) {
      dialog.showErrorMessage("Invalid URL or entry");
      return;
    }
    File tempfile=File.createTempFile("vcsettings-download",".zip");
    FileUtils.downloadUrlToFile(url,tempfile);
    this.importSettingsFromFile(tempfile);
    tempfile.delete();
  } catch (Exception e) {
    dialog.showErrorMessage("Could not download settings.\n"+e.getLocalizedMessage());
  }
}//GEN-LAST:event_jmDownloadSettingsActionPerformed

  private void btThingiverseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btThingiverseActionPerformed
  {//GEN-HEADEREND:event_btThingiverseActionPerformed
    startThingiverse();
  }//GEN-LAST:event_btThingiverseActionPerformed

private void zoomWindowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomWindowMenuItemActionPerformed
  this.previewPanel.setZoom(100d);
}//GEN-LAST:event_zoomWindowMenuItemActionPerformed

private void zoomRealMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomRealMenuItemActionPerformed
  this.previewPanel.setOneToOneZoom();
}//GEN-LAST:event_zoomRealMenuItemActionPerformed

private void thingiverseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thingiverseMenuItemActionPerformed
  startThingiverse();
}//GEN-LAST:event_thingiverseMenuItemActionPerformed

private void facebookMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_facebookMenuItemActionPerformed
  startFacebook();
}//GEN-LAST:event_facebookMenuItemActionPerformed

private void cameraActiveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cameraActiveMenuItemActionPerformed
  boolean cam = (!getVisiCam().isEmpty()) && cameraActiveMenuItem.isSelected();
  previewPanel.setShowBackgroundImage(cam);
  getDialog().removeMessageWithId("camera error");
  setCameraActive(cam);
}//GEN-LAST:event_cameraActiveMenuItemActionPerformed

private void projectorActiveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projectorActiveMenuItemActionPerformed
  setProjectorActive(!isProjectorActive());
}//GEN-LAST:event_projectorActiveMenuItemActionPerformed

  private void btQRWebcamScanActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btQRWebcamScanActionPerformed
  {//GEN-HEADEREND:event_btQRWebcamScanActionPerformed
    QRWebcamScanDialog qrWebcamScanDialog = new QRWebcamScanDialog(this, true);
    qrWebcamScanDialog.setLocationRelativeTo(null);
    qrWebcamScanDialog.setVisible(true);
  }//GEN-LAST:event_btQRWebcamScanActionPerformed

  private void webcamQRCodeMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_webcamQRCodeMenuItemActionPerformed
  {//GEN-HEADEREND:event_webcamQRCodeMenuItemActionPerformed
    QRWebcamScanDialog qrWebcamScanDialog = new QRWebcamScanDialog(this, true);
    qrWebcamScanDialog.setLocationRelativeTo(null);
    qrWebcamScanDialog.setVisible(true);
  }//GEN-LAST:event_webcamQRCodeMenuItemActionPerformed
  
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenu actionsMenu;
    private javax.swing.JButton bt1to1;
    private javax.swing.JButton btAddMaterial;
    private javax.swing.JButton btAddMaterialThickness;
    private javax.swing.JButton btAddObject;
    private javax.swing.JButton btFacebook;
    private javax.swing.JButton btFitScreen;
    private javax.swing.JButton btQRWebcamScan;
    private javax.swing.JButton btRemoveObject;
    private javax.swing.JButton btThingiverse;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton calculateTimeButton;
    private javax.swing.JMenuItem calibrateCameraMenuItem;
    private javax.swing.JCheckBoxMenuItem cameraActiveMenuItem;
    private javax.swing.JComboBox cbMaterialThickness;
    private javax.swing.JMenuItem editMappingMenuItem;
    private javax.swing.JButton executeJobButton;
    private javax.swing.JMenuItem executeJobMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem facebookMenuItem;
    private javax.swing.JMenu fileMenu;
    private com.t_oster.uicomponents.FilesDropSupport filesDropSupport1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem importMenuItem;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JMenu jmExamples;
    private javax.swing.JMenuItem jmExportSettings;
    private javax.swing.JMenu jmExtras;
    private javax.swing.JMenuItem jmImportSettings;
    private javax.swing.JMenuItem jmInstallIllustrator;
    private javax.swing.JMenuItem jmInstallInkscape;
    private javax.swing.JMenuItem jmManageLaserprofiles;
    private javax.swing.JMenuItem jmPreferences;
    private com.t_oster.uicomponents.ImageComboBox laserCutterComboBox;
    private com.t_oster.visicut.gui.mapping.MappingPanel mappingPanel;
    private javax.swing.JTabbedPane mappingTabbedPane;
    private com.t_oster.uicomponents.ImageComboBox materialComboBox;
    private javax.swing.JMenuItem materialMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JComboBox objectComboBox;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenu optionsMenu;
    private com.t_oster.uicomponents.PositionPanel positionPanel;
    private com.t_oster.visicut.gui.beans.PreviewPanel previewPanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JCheckBoxMenuItem projectorActiveMenuItem;
    private com.t_oster.visicut.gui.propertypanel.PropertiesPanel propertiesPanel;
    private javax.swing.JScrollPane propertyPanelContainer;
    private javax.swing.JMenu recentFilesMenu;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JFileChooser saveFileChooser;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem showGridMenuItem;
    private javax.swing.JMenuItem thingiverseMenuItem;
    private javax.swing.JLabel timeLabel;
    private javax.swing.JMenu viewMenu;
    private com.t_oster.visicut.VisicutModel visicutModel1;
    private com.t_oster.uicomponents.warnings.WarningPanel warningPanel;
    private javax.swing.JMenuItem webcamQRCodeMenuItem;
    private javax.swing.JMenuItem zoomInMenuItem;
    private javax.swing.JMenuItem zoomOutMenuItem;
    private javax.swing.JMenuItem zoomRealMenuItem;
    private javax.swing.JMenuItem zoomWindowMenuItem;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

  private void refreshMaterialThicknessesComboBox()
  {
    if (VisicutModel.getInstance().getMaterial() != null)
    {
      Float current = (Float) this.cbMaterialThickness.getSelectedItem();
      this.cbMaterialThickness.removeAllItems();
      for (float f : this.visicutModel1.getMaterial().getMaterialThicknesses())
      {
        this.cbMaterialThickness.addItem((Float) f);
        if (((Float) f).equals(current))
        {
          this.cbMaterialThickness.setSelectedItem((Float) f);
        }
      }
    }
  }

  /**
   * get a list of used LaserProfiles and their corresponding LaserProperty
   *
   * @param reallyExecuting true if the laserjob is about to be sent, false if we are only calculating the time
   * @param mayShowEditDialog true if this function may open a LaserProperty edit dialog for unknown profiles, false if not.
   * @return
   */
  private Map<LaserProfile, List<LaserProperty>> getPropertyMapForCurrentJob()
  {
    Map<LaserProfile, List<LaserProperty>> result = this.propertiesPanel.getPropertyMap();
    for (LaserProfile lp : result.keySet())
    {
      if (lp == null)//ignore-profile
      {
        continue;
      }
      if (!this.visicutModel1.getSelectedLaserDevice().getLaserCutter().getResolutions().contains((Double) lp.getDPI()))
      {
        double dist = -1;
        double res = 0;
        double soll = lp.getDPI();
        for(double r : this.visicutModel1.getSelectedLaserDevice().getLaserCutter().getResolutions())
        {
          if (dist == -1 || dist > Math.abs(soll-r))
          {
            dist = Math.abs(soll-r);
            res = r;
          }
        }
        if (!dialog.showYesNoQuestion(bundle.getString("THE LASERCUTTER YOU SELECTED, DOES NOT SUPPORT ")+soll+bundle.getString("DPI DO YOU WANT TO USE ")+res+bundle.getString("DPI INSTEAD?")))
        {
          return null;
        }
        //changing the DPI changes the hash-code, so we have to
        //remove and re-assign the profile to the map
        List<LaserProperty> val = result.get(lp);
        result.remove(lp);
        lp.setDPI(res);
        result.put(lp, val);
      }
    }
    return result;
  }

  public DialogHelper getDialog()
  {
    return this.dialog;
  }

  public boolean isPreviewPanelShowBackgroundImage()
  {
    return previewPanel.isShowBackgroundImage();
  }

  public void setPreviewPanelShowBackgroundImage(boolean showBackgroundImage)
  {
    previewPanel.setShowBackgroundImage(showBackgroundImage);
  }
  
  public boolean isCameraActive()
  {
    return cameraActive;
  }

  public void setCameraActive(boolean cameraActive)
  {
    this.cameraActive = cameraActive;

    // Set correct states on UI elements and manage threads
    // Visibility and enabled are already handled in other code places
    if (cameraActive)
    {
      if (cameraThread == null)
      {
        cameraThread = new RefreshCameraThread();
        cameraThread.start();
      }

      if (qrCodesTask == null)
      {
        qrCodesTask = new RefreshQRCodesTask();
        qrCodesTask.startOrContinueScan();
      }

      cameraActiveMenuItem.setSelected(true);
    }
    else
    {
      cameraActiveMenuItem.setSelected(false);
    }
  }
  
  /**
   * Get camera error message
   * @return  - last error message from camera capture
   *          - null if no image has yet been taken after resetCameraCapturingError()
   *          - "" if no error
   */
  public String getCameraCapturingError()
  {
    return cameraCapturingError;
  }
  
  public void resetCameraCapturingError()
  {
    cameraCapturingError = null;
  }
  
  public boolean isProjectorActive()
  {
    return projectorActive;
  }

  public void setProjectorActive(boolean projectorActive)
  {
    this.projectorActive = projectorActive;

    // Set correct states on UI elements and manage threads
    // Visibility and enabled are already handled in other code places
    if (projectorActive)
    {
      if (projectorThread == null)
      {
        projectorThread = new RefreshProjectorThread();
        projectorThread.start();
      }

      projectorActiveMenuItem.setSelected(true);
    }
    else
    {
      if (projectorThread != null)
      {
        projectorThread.startShutdown();
      }

      projectorActiveMenuItem.setSelected(false);
    }
  }
  
  public boolean isEditGuiForQRCodesDisabled()
  {
    return editGuiForQRCodesDisabled;
  }
  
  public synchronized void disableEditGuiForQRCodes(boolean disable)
  {
    // No state change at all, return
    if ((isEditGuiForQRCodesDisabled() && disable) || (!isEditGuiForQRCodesDisabled() && !disable))
    {
      return;
    }

    // Avoid adding the message multiple times on QR code detection fails or remove if not needed anymore
    warningPanel.removeMessageWithId("QR_CODE_DETECTION_GUI_DISABLE");

    if (disable)
    {
      objectComboBox.setVisible(false);
      
      executeJobButton.setEnabled(!disable);
      executeJobMenuItem.setEnabled(!disable);
      calculateTimeButton.setEnabled(!disable);

      // Message is automatically removed and closed, therefore no close button
      Message m = new Message("Info", bundle.getString("QR_CODE_DETECTION_GUI_DISABLE_TEXT"), Message.Type.INFO, new com.t_oster.uicomponents.warnings.Action[]
        {
          new com.t_oster.uicomponents.warnings.Action(bundle.getString("QR_CODE_DETECTION_GUI_DISABLE_BUTTON"))
          {
            @Override
            public boolean clicked()
            {
              if (qrCodesTask != null)
              {
                if (!qrCodesTask.isStorePositions())
                {
                  qrCodesTask.setStorePositions(true);
                }
              }
              return true;
            }
          }
        }
      );
      m.setCloseButtonVisible(false);
      m.setCloseListener(new ActionListener(){
        public void actionPerformed(ActionEvent ae)
        {
        }
      });
      warningPanel.addMessageOnce(m, "QR_CODE_DETECTION_GUI_DISABLE");
    }
    else
    {
      if (visicutModel1 != null && visicutModel1.getPlfFile() != null)
      {
        objectComboBox.setVisible(visicutModel1.getPlfFile().size() > 1);
        LinkedList<PlfPart> removePlfParts = new LinkedList<PlfPart>();
        
        // Iterate over elements in PLF file, remove preview loaded QR code objects, which were not stored yet
        for (PlfPart part : visicutModel1.getPlfFile())
        {
          QRCodeInfo qrCodePartInfo = part.getQRCodeInfo();
          
          if (qrCodePartInfo != null)
          {
            // Check if this part was loaded by preview QR code scanning and is not position stored
            if (qrCodePartInfo.isPreviewQRCodeSource() && !qrCodePartInfo.isPreviewPositionQRStored())
            {
                removePlfParts.add(part);
            }
          }
        }
        
        // Delete stored elements
        for (PlfPart part : removePlfParts)
        {
          visicutModel1.removePlfPart(part);
        }
      }
      
      refreshExecuteButtons(true);
    }
    
    // Handle GUI settings, disable most of the elements, which could cause some trouble
    // No need to deactivate those actions strictly, but they could cause some exceptions
    newMenuItem.setEnabled(!disable);
    openMenuItem.setEnabled(!disable);
    importMenuItem.setEnabled(!disable);
    recentFilesMenu.setEnabled(!disable);
    jmExamples.setEnabled(!disable);    
    btAddObject.setEnabled(!disable);
    btRemoveObject.setEnabled(!disable);
    objectComboBox.setEnabled(!disable);
    
    // State changed
    editGuiForQRCodesDisabled = disable;
  }

  public boolean isLaserJobInProgress()
  {
    return laserJobInProgress;
  }

  private void setLaserJobInProgress(boolean laserJobInProgress)
  {
    this.laserJobInProgress = laserJobInProgress;
  }
  
  private void laserJobStarted()
  {
    // Check correct state
    if (!isLaserJobInProgress())
    {
      return;
    }
    
    // Ask user for FabQR upload, if FabQR is enabled and available
    if (MainView.getInstance() != null && FabQRFunctions.isFabqrActive()
        && FabQRFunctions.getFabqrPrivateURL() != null && !FabQRFunctions.getFabqrPrivateURL().isEmpty() && !isFabqrUploadDialogOpened())
    {
      if (dialog.showYesNoQuestion(bundle.getString("DIALOG_QUESTION_FABQR_UPLOAD")))
      {
        setFabqrUploadDialogOpened(true);
        
        new Thread()
        {
          @Override
          public void run()
          {
            FabQRUploadDialog fabqrUploadDialg = new FabQRUploadDialog(MainView.getInstance(), true);
            fabqrUploadDialg.setLocationRelativeTo(null);
            fabqrUploadDialg.setVisible(true);
          }
        }.start();
      }
    }
  }
  
  private void laserJobStopped()
  {
    // Check correct state
    if (isLaserJobInProgress())
    {
      return;
    }
  }

  public boolean isFabqrUploadDialogOpened()
  {
    return isFabqrUploadDialogOpened;
  }

  public void setFabqrUploadDialogOpened(boolean isFabqrUploadDialogOpened)
  {
    this.isFabqrUploadDialogOpened = isFabqrUploadDialogOpened;
  }
}
