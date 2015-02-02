/*
 * ThingiverseDialog.java
 *
 * Created on 13.12.2014, 17:42:20
 */
package com.tur0kk.thingiverse.gui;

import com.t_oster.visicut.gui.MainView;
import com.tur0kk.thingiverse.gui.mapping.AnimationImageObserverList;
import com.tur0kk.thingiverse.gui.mapping.ThingListRenderer;
import com.tur0kk.thingiverse.model.Thing;
import com.tur0kk.thingiverse.ThingiverseManager;
import com.tur0kk.thingiverse.gui.mapping.ThingFileClickListener;
import com.tur0kk.thingiverse.gui.mapping.ThingFileListRenderer;
import com.tur0kk.thingiverse.gui.mapping.ThingSelectionListener;
import com.tur0kk.LoadingIcon;
import com.tur0kk.thingiverse.gui.mapping.ThingCollectionComboboxModel;
import com.tur0kk.thingiverse.gui.mapping.ThingCollectionComboboxRenderer;
import com.tur0kk.thingiverse.gui.mapping.ThingFileSelectionListener;
import com.tur0kk.thingiverse.model.ThingCollection;
import com.tur0kk.thingiverse.model.ThingFile;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.rmi.AccessException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Sven
 */
public class ThingiverseDialog extends javax.swing.JDialog
{

  private AtomicInteger numberLoadingLiked = new AtomicInteger();
  private AtomicInteger numberLoadingMyThings = new AtomicInteger();
  private AtomicInteger numberLoadingSearch = new AtomicInteger();
  private AtomicInteger numberLoadingCollection = new AtomicInteger();

  /**
   * Creates new form ThingiverseDialog
   */
  public ThingiverseDialog(java.awt.Frame parent, boolean modal) throws AccessException, MalformedURLException, IOException
  {
    super(parent, modal);
    
    // just hide to keep state
    this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    // control window events
    initWindowListener();

    // init componentes
    initComponents(); // auto generated code
    initComponentsByHand();
    
    // display things
    initMyThingsTab();

    initLikedTab();
    
    initSearchTab();
    
    initCollectionTab();
    
    // list cell renderer for images + name of things
    initListCellRenderers();

    /* click listener for items to display in thing panels */
    initListClickListeners();
    
    initChangeListener();

    // display username
    initUserName();

    // set profile picture
    initProfilePicture();
    
    // resize everything to content
    pack();
  }
  
  private void refreshAll(){
    lstMyThingsThing.setModel(new DefaultListModel());
    lstLikedThing.setModel(new DefaultListModel());
    lstCollectionThing.setModel(new DefaultListModel());
    lstSearchThing.setModel(new DefaultListModel());
    initCollectionDropdown();
    actionMyThings();
    actionLiked();
    actionCollection();
    actionSearch();
  }
  
  private void loadTab(final List<Thing> thingsToLoad, final JList thingList, final AtomicInteger feedbackCounter, final JLabel feedbackLabel){
    // load all images for a tab asynchronously
    new Thread(new Runnable()
    {

      public void run()
      {
        final ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        
        // init my things model with loading images
        DefaultListModel myThingsModel = new DefaultListModel(); // model for JList
        Iterator<Thing> i1 = thingsToLoad.iterator(); // iterate over each thing and add to model
        int index = 0;
        while (i1.hasNext())
        {
          // get loading icon
          ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);

          // set changing observer for loading images to update gif 
          loadingIcon.setImageObserver(new AnimationImageObserverList(thingList, index));

          // add thing to model
          Thing aThing = i1.next();
          aThing.setImage(loadingIcon);
          myThingsModel.addElement(aThing);

          index += 1;
        }

        // display myThingsModel in my things list
        final DefaultListModel model = myThingsModel;
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            thingList.setModel(model);
          }
        });

        // set an atomic counter to keep track of number of loading icons. If 0 again, disable loading header.
        feedbackCounter.set(myThingsModel.size());

        // start a thread for each image to load image asynchronous
        for (final Thing entry : thingsToLoad)
        {
          final String url = entry.getImageUrl();
          new Thread(new Runnable()
          {
            public void run()
            {

              String file = thingiverse.downloadImage(url); // download  image
              ImageIcon imageIcon = null;
              if ("".equals(file))
              { // load default image if image not avaliable
                imageIcon = new ImageIcon(LoadingIcon.class.getResource("resources/image_not_found.png"));
              }
              else
              {
                imageIcon = new ImageIcon(file);
              }

              // overwrite image
              final ImageIcon objectImage = imageIcon;
              SwingUtilities.invokeLater(new Runnable()
              {
                public void run()
                {
                  // overwrite image
                  entry.setImage(objectImage);
                  thingList.updateUI();

                  // image loaded, decrement loading images
                  if (feedbackCounter.decrementAndGet() == 0)
                  {
                    feedbackLabel.setIcon(null);
                  }
                }
              });
            }
          }).start();
        }
        
        
        // if list empty disable feedback
        if(thingsToLoad.isEmpty()){
          feedbackLabel.setIcon(null);
        }
        // do for other tabs

      }
    }).start();
  }

  // called once when dialog is set up
  private void initLikedTab()
  {
    
        // enable userfeedback 
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            // header for Liked
            lblLiked = new JLabel();
            lblLiked.setText("Liked");
            lblLiked.setHorizontalTextPosition(JLabel.LEADING);
            tpLists.setTabComponentAt(1, lblLiked);
            actionLiked();
          }
        });
        

  }
  
  private void actionLiked(){

    // load liked things asynchronously
    new Thread(new Runnable() {

      public void run()
      {
        SwingUtilities.invokeLater(new Runnable() {

          public void run()
          {
            lblLiked.setIcon(LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL));
          }
        });
        
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        List<Thing> things = thingiverse.getLikedThings(cbExtensions.isSelected(), cbTags.isSelected());
        loadTab(things, lstLiked, numberLoadingLiked, lblLiked);
      }
    }).start();
  }

  // called once when dialog is set up
  private void initMyThingsTab()
  {
      // enable userfeedback
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          // header for MyThings
          lblMyThings = new JLabel();
          lblMyThings.setText("MyThings");
          lblMyThings.setHorizontalTextPosition(JLabel.LEADING);
          tpLists.setTabComponentAt(0, lblMyThings);
          actionMyThings();
        }
      });

  }
  
  private void actionMyThings(){
    // load mythings things asynchronously
    new Thread(new Runnable() {

      public void run()
      {
        SwingUtilities.invokeLater(new Runnable() {

          public void run()
          {
            lblMyThings.setIcon(LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL));
          }
        });
        // get things
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        List<Thing> things = thingiverse.getMyThings(cbExtensions.isSelected(), cbTags.isSelected());
        loadTab(things, lstMyThings, numberLoadingMyThings, lblMyThings);
      }
    }).start();
      
  }
  
  // init search tab, the other tabs are initialized by their action, but because the search tab is called everytime the user wants to search for something, the initialization has to be done somewhere else
  private void initSearchTab(){
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        // header for Search
        lblSearch = new JLabel();
        lblSearch.setText("Search");
        lblSearch.setHorizontalTextPosition(JLabel.LEADING);
        tpLists.setTabComponentAt(3, lblSearch);
      }
    });
  }
  
  private void initCollectionDropdown(){
    // get collections
    ThingiverseManager thingiverse = ThingiverseManager.getInstance();
    List<ThingCollection> collections = thingiverse.getMyCollections();

    // init combobox by hand, because it also is added to the header by hand
    // fill combobox with names of collections
    ThingCollectionComboboxModel comboboxModel = new ThingCollectionComboboxModel(collections);
    cbCollection.setModel(comboboxModel);
    cbCollection.setRenderer(new ThingCollectionComboboxRenderer()); // display just the name of a collection
    cbCollection.addItemListener(new ItemListener() { // listen for item changes and display collection
      public void itemStateChanged(ItemEvent e)
      {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          actionCollection();
        }
      }
    }); // end listener
  }
  
  private void initCollectionTab(){
    // load things asynchronously
    new Thread(new Runnable() {

      public void run()
      {
        // enable userfeedback
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {            
            // header for MyThings
            lblCollection = new JLabel();
            lblCollection.setText("Collections");
            lblCollection.setHorizontalTextPosition(JLabel.LEADING);
            tpLists.setTabComponentAt(2, lblCollection);            
          }
        });
        
        initCollectionDropdown();
        
        actionCollection(); // starts own non gui thread, needed here to ensure that gui set up is ready       
      }
    }).start();
  }
  
  // loads the currently selected collection asynchronously
  private void actionCollection(){
    new Thread(new Runnable() {

      public void run()
      {
        ThingCollection collection = (ThingCollection) cbCollection.getSelectedItem();
        
        if(collection == null){
          return; // break execution, because no valid collection was loaded
        }
        // else, continue with loading the selected collection
        
        
        // enable userfeedback
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {   
            lblCollection.setIcon(LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL));
          }
        });
        
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        List<Thing> things = thingiverse.getThingsByCollection(collection, cbExtensions.isSelected(), cbTags.isSelected());
        loadTab(things, lstCollection, numberLoadingCollection, lblCollection);
      }
    }).start();
  }

  // called everytime the user wants to search for something, so check if stuff already initialized
  private void actionSearch()
  {
    // load searched things asynchronously
    new Thread(new Runnable() {

      public void run()
      {
        
        String queryString = txtSearch.getText();
        if("".equals(queryString)){ // just do a search for valid search strings
          return;
        }
        
        // enable userfeedback
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {   
            lstSearchThing.setModel(new DefaultListModel()); // clear thing file list so that list is cleared if no things were found
            lblSearch.setIcon(LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL));
          }
        });
        
    
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        List<Thing> things = thingiverse.search(queryString, cbExtensions.isSelected(), cbTags.isSelected());
        loadTab(things, lstSearch, numberLoadingSearch, lblSearch);
      }
    }).start();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lProfilePicture = new javax.swing.JLabel();
        lUserName = new javax.swing.JLabel();
        tpLists = new javax.swing.JTabbedPane();
        spltpMyThings = new javax.swing.JSplitPane();
        sclpMyThings = new javax.swing.JScrollPane();
        lstMyThings = new javax.swing.JList();
        sclpMyThingsThing = new javax.swing.JScrollPane();
        lstMyThingsThing = new javax.swing.JList();
        spltpLiked = new javax.swing.JSplitPane();
        sclpLiked = new javax.swing.JScrollPane();
        lstLiked = new javax.swing.JList();
        sclpLikedThing = new javax.swing.JScrollPane();
        lstLikedThing = new javax.swing.JList();
        spltpCollections = new javax.swing.JSplitPane();
        pnlCollectionChooser = new javax.swing.JPanel();
        cbCollection = new javax.swing.JComboBox();
        spltpCollection = new javax.swing.JSplitPane();
        sclpCollections = new javax.swing.JScrollPane();
        lstCollection = new javax.swing.JList();
        sclpCollectionThing = new javax.swing.JScrollPane();
        lstCollectionThing = new javax.swing.JList();
        spltpSearchContainer = new javax.swing.JSplitPane();
        pnlSearchField = new javax.swing.JPanel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        spltpSearch = new javax.swing.JSplitPane();
        sclpSearch = new javax.swing.JScrollPane();
        lstSearch = new javax.swing.JList();
        sclpSearchThing = new javax.swing.JScrollPane();
        lstSearchThing = new javax.swing.JList();
        btnLogout = new javax.swing.JButton();
        pnlFilter = new javax.swing.JPanel();
        cbExtensions = new javax.swing.JCheckBox();
        cbTags = new javax.swing.JCheckBox();
        lblOpeningFile = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        btnMadeOne = new javax.swing.JButton();
        btnOpenFile = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(ThingiverseDialog.class);
        setTitle(resourceMap.getString("thingiverseDialog.title")); // NOI18N
        setLocationByPlatform(true);
        setMinimumSize(new Dimension(800,600));
        setName("thingiverseDialog"); // NOI18N

        lProfilePicture.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lProfilePicture.setAlignmentY(0.0F);
        lProfilePicture.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lProfilePicture.border.lineColor"))); // NOI18N
        lProfilePicture.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lProfilePicture.setName("lProfilePicture"); // NOI18N

        lUserName.setFont(resourceMap.getFont("lUserName.font")); // NOI18N
        lUserName.setToolTipText(resourceMap.getString("lUserName.toolTipText")); // NOI18N
        lUserName.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lUserName.setName("lUserName"); // NOI18N

        tpLists.setAlignmentX(5.0F);
        tpLists.setAlignmentY(5.0F);
        tpLists.setMaximumSize(new Dimension(50000, 300));
        tpLists.setMinimumSize(new Dimension(250, 300));
        tpLists.setName("tpLists"); // NOI18N
        tpLists.setPreferredSize(new Dimension(250, 300));

        spltpMyThings.setName("spltpMyThings"); // NOI18N

        sclpMyThings.setBorder(null);
        sclpMyThings.setMinimumSize(new Dimension(200,300));
        sclpMyThings.setName("sclpMyThings"); // NOI18N
        sclpMyThings.setPreferredSize(new Dimension(400,300));

        lstMyThings.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstMyThings.setAlignmentX(0.0F);
        lstMyThings.setAlignmentY(0.0F);
        lstMyThings.setName("lstMyThings"); // NOI18N
        sclpMyThings.setViewportView(lstMyThings);

        spltpMyThings.setLeftComponent(sclpMyThings);

        sclpMyThingsThing.setMinimumSize(new Dimension(200,300));
        sclpMyThingsThing.setName("sclpMyThingsThing"); // NOI18N

        lstMyThingsThing.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstMyThingsThing.setName("lstMyThingsThing"); // NOI18N
        sclpMyThingsThing.setViewportView(lstMyThingsThing);

        spltpMyThings.setRightComponent(sclpMyThingsThing);

        tpLists.addTab(resourceMap.getString("spltpMyThings.TabConstraints.tabTitle"), spltpMyThings); // NOI18N

        spltpLiked.setName("spltpLiked"); // NOI18N

        sclpLiked.setBorder(null);
        sclpLiked.setMinimumSize(new Dimension(200,300));
        sclpLiked.setName("sclpLiked"); // NOI18N
        sclpLiked.setPreferredSize(new Dimension(400,300));

        lstLiked.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstLiked.setAlignmentX(0.0F);
        lstLiked.setAlignmentY(0.0F);
        lstLiked.setName("lstLiked"); // NOI18N
        sclpLiked.setViewportView(lstLiked);

        spltpLiked.setLeftComponent(sclpLiked);

        sclpLikedThing.setMinimumSize(new Dimension(200,300));
        sclpLikedThing.setName("sclpLikedThing"); // NOI18N

        lstLikedThing.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstLikedThing.setName("lstLikedThing"); // NOI18N
        sclpLikedThing.setViewportView(lstLikedThing);

        spltpLiked.setRightComponent(sclpLikedThing);

        tpLists.addTab(resourceMap.getString("spltpLiked.TabConstraints.tabTitle"), spltpLiked); // NOI18N

        spltpCollections.setDividerLocation(35);
        spltpCollections.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spltpCollections.setEnabled(false);
        spltpCollections.setName("spltpCollections"); // NOI18N

        pnlCollectionChooser.setName("pnlCollectionChooser"); // NOI18N
        pnlCollectionChooser.setLayout(new java.awt.BorderLayout());

        cbCollection.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbCollection.setName("cbCollection"); // NOI18N
        pnlCollectionChooser.add(cbCollection, java.awt.BorderLayout.CENTER);

        spltpCollections.setTopComponent(pnlCollectionChooser);

        spltpCollection.setName("spltpCollection"); // NOI18N

        sclpCollections.setBorder(null);
        sclpCollections.setMinimumSize(new Dimension(200,300));
        sclpCollections.setName("sclpCollections"); // NOI18N
        sclpCollections.setPreferredSize(new Dimension(400,300));

        lstCollection.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstCollection.setName("lstCollection"); // NOI18N
        sclpCollections.setViewportView(lstCollection);

        spltpCollection.setLeftComponent(sclpCollections);

        sclpCollectionThing.setMinimumSize(new Dimension(200,300));
        sclpCollectionThing.setName("sclpCollectionThing"); // NOI18N

        lstCollectionThing.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstCollectionThing.setName("lstCollectionThing"); // NOI18N
        sclpCollectionThing.setViewportView(lstCollectionThing);

        spltpCollection.setRightComponent(sclpCollectionThing);

        spltpCollections.setRightComponent(spltpCollection);

        tpLists.addTab(resourceMap.getString("spltpCollections.TabConstraints.tabTitle"), spltpCollections); // NOI18N

        spltpSearchContainer.setDividerLocation(35);
        spltpSearchContainer.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spltpSearchContainer.setEnabled(false);
        spltpSearchContainer.setName("spltpSearchContainer"); // NOI18N

        pnlSearchField.setName("pnlSearchField"); // NOI18N
        pnlSearchField.setLayout(new java.awt.BorderLayout());

        txtSearch.setFont(resourceMap.getFont("txtSearch.font")); // NOI18N
        txtSearch.setName("txtSearch"); // NOI18N
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });
        pnlSearchField.add(txtSearch, java.awt.BorderLayout.CENTER);

        btnSearch.setText(resourceMap.getString("btnSearch.text")); // NOI18N
        btnSearch.setMinimumSize(new Dimension(50,30));
        btnSearch.setName("btnSearch"); // NOI18N
        btnSearch.setPreferredSize(new Dimension(50,30));
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        pnlSearchField.add(btnSearch, java.awt.BorderLayout.EAST);

        spltpSearchContainer.setTopComponent(pnlSearchField);

        spltpSearch.setName("spltpSearch"); // NOI18N

        sclpSearch.setBorder(null);
        sclpSearch.setMinimumSize(new Dimension(200,300));
        sclpSearch.setName("sclpSearch"); // NOI18N
        sclpSearch.setPreferredSize(new Dimension(400,300));

        lstSearch.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstSearch.setName("lstSearch"); // NOI18N
        sclpSearch.setViewportView(lstSearch);

        spltpSearch.setLeftComponent(sclpSearch);

        sclpSearchThing.setMinimumSize(new Dimension(200,300));
        sclpSearchThing.setName("sclpSearchThing"); // NOI18N

        lstSearchThing.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstSearchThing.setName("lstSearchThing"); // NOI18N
        sclpSearchThing.setViewportView(lstSearchThing);

        spltpSearch.setRightComponent(sclpSearchThing);

        spltpSearchContainer.setRightComponent(spltpSearch);

        tpLists.addTab("Search", spltpSearchContainer);

        btnLogout.setText(resourceMap.getString("btnLogout.text")); // NOI18N
        btnLogout.setAlignmentX(5.0F);
        btnLogout.setAlignmentY(5.0F);
        btnLogout.setName("btnLogout"); // NOI18N
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        pnlFilter.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlFilter.border.title"))); // NOI18N
        pnlFilter.setToolTipText(resourceMap.getString("pnlFilter.toolTipText")); // NOI18N
        pnlFilter.setAlignmentX(0.0F);
        pnlFilter.setAlignmentY(0.0F);
        pnlFilter.setName("pnlFilter"); // NOI18N
        pnlFilter.setLayout(new java.awt.GridLayout(2, 1, 1, 0));

        cbExtensions.setText(resourceMap.getString("cbExtensions.text")); // NOI18N
        cbExtensions.setToolTipText(resourceMap.getString("cbExtensions.toolTipText")); // NOI18N
        cbExtensions.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cbExtensions.setName("cbExtensions"); // NOI18N
        pnlFilter.add(cbExtensions);

        cbTags.setText(resourceMap.getString("cbTags.text")); // NOI18N
        cbTags.setToolTipText(resourceMap.getString("cbTags.toolTipText")); // NOI18N
        cbTags.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cbTags.setName("cbTags"); // NOI18N
        pnlFilter.add(cbTags);

        lblOpeningFile.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblOpeningFile.setIcon(LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL));
        lblOpeningFile.setText(resourceMap.getString("lblOpeningFile.text")); // NOI18N
        lblOpeningFile.setName("lblOpeningFile"); // NOI18N

        btnRefresh.setText(resourceMap.getString("btnRefresh.text")); // NOI18N
        btnRefresh.setToolTipText(resourceMap.getString("btnRefresh.toolTipText")); // NOI18N
        btnRefresh.setName("btnRefresh"); // NOI18N
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        btnMadeOne.setText(resourceMap.getString("btnMadeOne.text")); // NOI18N
        btnMadeOne.setToolTipText(resourceMap.getString("btnMadeOne.toolTipText")); // NOI18N
        btnMadeOne.setEnabled(false);
        btnMadeOne.setName("btnMadeOne"); // NOI18N
        btnMadeOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMadeOneActionPerformed(evt);
            }
        });

        btnOpenFile.setText(resourceMap.getString("btnOpenFile.text")); // NOI18N
        btnOpenFile.setToolTipText(resourceMap.getString("btnOpenFile.toolTipText")); // NOI18N
        btnOpenFile.setEnabled(false);
        btnOpenFile.setName("btnOpenFile"); // NOI18N
        btnOpenFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(tpLists, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 919, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lUserName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnLogout, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 480, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(btnRefresh, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(pnlFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblOpeningFile, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnMadeOne, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnlFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(lProfilePicture, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(tpLists, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnMadeOne, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblOpeningFile, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pnlFilter.getAccessibleContext().setAccessibleDescription(resourceMap.getString("pnlFilter.AccessibleContext.accessibleDescription")); // NOI18N

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void initComponentsByHand()
  {
    // set general content padding
    //JPanel content = (JPanel) this.getContentPane();
   // content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    // disable controls
    lblOpeningFile.setVisible(false);
  }
  private void btnSearchActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSearchActionPerformed
  {//GEN-HEADEREND:event_btnSearchActionPerformed
    this.actionSearch();
  }//GEN-LAST:event_btnSearchActionPerformed

  private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnLogoutActionPerformed
  {//GEN-HEADEREND:event_btnLogoutActionPerformed
    /*
     * just hide thingiverseDialog on close to keep state.
     * dispose only on logout
     */
    ThingiverseManager.getInstance().logOut();
    dispose();
  }//GEN-LAST:event_btnLogoutActionPerformed

  private void txtSearchKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_txtSearchKeyPressed
  {//GEN-HEADEREND:event_txtSearchKeyPressed
    char key = evt.getKeyChar();

    // on enter perform search
    if (key == '\n')
    {
      this.actionSearch();
    }
  }//GEN-LAST:event_txtSearchKeyPressed

  private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnRefreshActionPerformed
  {//GEN-HEADEREND:event_btnRefreshActionPerformed
    refreshAll();
  }//GEN-LAST:event_btnRefreshActionPerformed

  private void btnMadeOneActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnMadeOneActionPerformed
  {//GEN-HEADEREND:event_btnMadeOneActionPerformed
    // assume in current list a thing is selected, otherwise button is not enabled
    ThingiverseUploadDialog uploadDialog = new ThingiverseUploadDialog(MainView.getInstance(), true, (Thing)getCurrentTabThingList().getSelectedValue());
    uploadDialog.setLocationRelativeTo(null);
    uploadDialog.setVisible(true);
  }//GEN-LAST:event_btnMadeOneActionPerformed

  private void btnOpenFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnOpenFileActionPerformed
  {//GEN-HEADEREND:event_btnOpenFileActionPerformed
      final ThingFile aFile = (ThingFile) getCurrentTabThingFileList().getSelectedValue();
      if(aFile == null){ // nothing visible
        return;
      }
      
      // user feedback
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lblOpeningFile.setVisible(true);
        }
      });
      
      // load file
      new Thread(new Runnable() {

            public void run()
            {   
              ThingiverseManager thingiverse = ThingiverseManager.getInstance();
              File svgfile = thingiverse.downloadThingFile(aFile);
              MainView.getInstance().loadFile(svgfile, false);
              
              // disable user feedback
              SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                  lblOpeningFile.setVisible(false);
                }
              });
            }
      }).start();
  }//GEN-LAST:event_btnOpenFileActionPerformed


  private void initListCellRenderers()
  {
    // thing list renderer
    lstMyThings.setCellRenderer(new ThingListRenderer());
    lstSearch.setCellRenderer(new ThingListRenderer());
    lstLiked.setCellRenderer(new ThingListRenderer());
    lstCollection.setCellRenderer(new ThingListRenderer());
    
    // thing file list renderer
    lstSearchThing.setCellRenderer(new ThingFileListRenderer());
    lstMyThingsThing.setCellRenderer(new ThingFileListRenderer());
    lstLikedThing.setCellRenderer(new ThingFileListRenderer());
    lstCollectionThing.setCellRenderer(new ThingFileListRenderer());
  }

  private void initListClickListeners()
  {
    // click listener loads files of selected thing  
    lstMyThings.addListSelectionListener(new ThingSelectionListener(lstMyThingsThing, cbExtensions, btnMadeOne));
    lstSearch.addListSelectionListener(new ThingSelectionListener(lstSearchThing, cbExtensions, btnMadeOne));
    lstLiked.addListSelectionListener(new ThingSelectionListener(lstLikedThing, cbExtensions, btnMadeOne));
    lstCollection.addListSelectionListener(new ThingSelectionListener(lstCollectionThing, cbExtensions, btnMadeOne));
    
    // set adapter for ThingFile-lists to listen for double clicks -> load selected file    
    lstSearchThing.addMouseListener(new ThingFileClickListener(lblOpeningFile));
    lstMyThingsThing.addMouseListener(new ThingFileClickListener(lblOpeningFile));
    lstLikedThing.addMouseListener(new ThingFileClickListener(lblOpeningFile));
    lstCollectionThing.addMouseListener(new ThingFileClickListener(lblOpeningFile));
    
    // set listeners to enable open file button
    lstSearchThing.addListSelectionListener(new ThingFileSelectionListener(btnOpenFile));
    lstMyThingsThing.addListSelectionListener(new ThingFileSelectionListener(btnOpenFile));
    lstLikedThing.addListSelectionListener(new ThingFileSelectionListener(btnOpenFile));
    lstCollectionThing.addListSelectionListener(new ThingFileSelectionListener(btnOpenFile));
  }
  
  private void initChangeListener(){
    
    // add change listener for switching tabs, to check if a thing is selected, to enable or disable the "I made one" button
    tpLists.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          // allow I made one only if thing is selected
          JList tabThingList = getCurrentTabThingList();
          if(tabThingList != null){
            if(tabThingList.getSelectedIndex() != -1){
              // Thing is selected, allow "I made one"
              btnMadeOne.setEnabled(true);
            }
            else{
              // no thing is selected, disallow "I made one", because for that a thing is needed
              btnMadeOne.setEnabled(false);
            }
          }
          
          // allow open file only if ThingFile is selected
          JList tabThingFileList = getCurrentTabThingFileList();
          if(tabThingFileList != null){
            if(tabThingFileList.getSelectedIndex() != -1){
              // Thing is selected, allow "I made one"
              btnOpenFile.setEnabled(true);
            }
            else{
              // no thing is selected, disallow "I made one", because for that a thing is needed
              btnOpenFile.setEnabled(false);
            }
          }
        }
    });
  }
  
  private JList getCurrentTabThingList(){
    JList tabThingList = null;
    switch(tpLists.getSelectedIndex()){
      case 0:
        tabThingList = lstMyThings;
        break;
      case 1:
        tabThingList = lstLiked;
        break;
      case 2:
        tabThingList = lstCollection;
        break;
      case 3:
        tabThingList = lstSearch;
        break;
      default: 
        break; // invalid tab
    }
    return tabThingList;
  }
  
  private JList getCurrentTabThingFileList(){
    JList tabThingFileList = null;
    switch(tpLists.getSelectedIndex()){
      case 0:
        tabThingFileList = lstMyThingsThing;
        break;
      case 1:
        tabThingFileList = lstLikedThing;
        break;
      case 2:
        tabThingFileList = lstCollectionThing;
        break;
      case 3:
        tabThingFileList = lstSearchThing;
        break;
      default: 
        break; // invalid tab
    }
    return tabThingFileList;
  }

  private void initUserName()
  {
    new Thread(new Runnable()
    {

      String username = null;

      public void run()
      {
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();

        username = thingiverse.getUserName();
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            lUserName.setText("Hello " + username);
          }
        });
      }
    }).start();
  }

  private void initProfilePicture()
  {

    new Thread(new Runnable()
    {

      public void run()
      {
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();

        // profile picture, resized to label
        try
        {
          // get loading icon
          final ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);
          // display loading icon in label
          SwingUtilities.invokeLater(new Runnable()
          {
            public void run()
            {
              lProfilePicture.setIcon(loadingIcon);
            }
          });

          // get actual user profile picture
          String path = thingiverse.getUserImage();
          String file = thingiverse.downloadImage(path); // download user image
          ImageIcon imageIcon = null;
          if (file == "")
          { // load default image if image not avaliable
            imageIcon = new ImageIcon(LoadingIcon.class.getResource("resources/avatar_default.jpg"));
          }
          else
          {
            imageIcon = new ImageIcon(file);
          }

          // scale image to label size
          Image rawImage = imageIcon.getImage();
          Image scaledImage = rawImage.getScaledInstance(
            lProfilePicture.getWidth(),
            lProfilePicture.getHeight(),
            Image.SCALE_SMOOTH);

          // display image in label
          final ImageIcon profilePicture = new ImageIcon(scaledImage);
          SwingUtilities.invokeLater(new Runnable()
          {
            public void run()
            {
              lProfilePicture.setIcon(profilePicture);
            }
          });
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
    }).start();
  }

  private void initWindowListener()
  {
    this.addWindowListener(new WindowListener()
    {

      public void windowOpened(WindowEvent e)
      {
      }

      public void windowClosing(WindowEvent e)
      {
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
  }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnMadeOne;
    private javax.swing.JButton btnOpenFile;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSearch;
    private javax.swing.JComboBox cbCollection;
    private javax.swing.JCheckBox cbExtensions;
    private javax.swing.JCheckBox cbTags;
    private javax.swing.JLabel lProfilePicture;
    private javax.swing.JLabel lUserName;
    private javax.swing.JLabel lblOpeningFile;
    private javax.swing.JList lstCollection;
    private javax.swing.JList lstCollectionThing;
    private javax.swing.JList lstLiked;
    private javax.swing.JList lstLikedThing;
    private javax.swing.JList lstMyThings;
    private javax.swing.JList lstMyThingsThing;
    private javax.swing.JList lstSearch;
    private javax.swing.JList lstSearchThing;
    private javax.swing.JPanel pnlCollectionChooser;
    private javax.swing.JPanel pnlFilter;
    private javax.swing.JPanel pnlSearchField;
    private javax.swing.JScrollPane sclpCollectionThing;
    private javax.swing.JScrollPane sclpCollections;
    private javax.swing.JScrollPane sclpLiked;
    private javax.swing.JScrollPane sclpLikedThing;
    private javax.swing.JScrollPane sclpMyThings;
    private javax.swing.JScrollPane sclpMyThingsThing;
    private javax.swing.JScrollPane sclpSearch;
    private javax.swing.JScrollPane sclpSearchThing;
    private javax.swing.JSplitPane spltpCollection;
    private javax.swing.JSplitPane spltpCollections;
    private javax.swing.JSplitPane spltpLiked;
    private javax.swing.JSplitPane spltpMyThings;
    private javax.swing.JSplitPane spltpSearch;
    private javax.swing.JSplitPane spltpSearchContainer;
    private javax.swing.JTabbedPane tpLists;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

  // hand written variable declaration
  private javax.swing.JPanel pnlMyThings;
  private javax.swing.JLabel lblMyThings;
  private javax.swing.JLabel lblLoadingMyThings;
  private javax.swing.JPanel pnlLiked;
  private javax.swing.JLabel lblLiked;
  private javax.swing.JLabel lblLoadingLiked;
  private javax.swing.JPanel pnlSearch;
  private javax.swing.JLabel lblSearch;
  private javax.swing.JLabel lblLoadingSearch;
  private javax.swing.JPanel pnlCollection;
  private javax.swing.JLabel lblCollection;
  private javax.swing.JLabel lblLoadingCollection;
}
