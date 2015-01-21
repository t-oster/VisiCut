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
import com.tur0kk.thingiverse.model.ThingCollection;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.rmi.AccessException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

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
  private List<JCheckBox> filterCheckBoxes = new LinkedList<JCheckBox>(); // holds the filter checkboxes for passing to non gui class
  final private MainView mainview;

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

    // save parent for loading files to main view
    this.mainview = (MainView) parent;

    // init componentes
    initComponents(); // auto generated code
    initComponentsByHand();
    
    // display things
    initMyThingsTab();

    initLikedTab();
    
    initSearchTab();
    
    initCollectionTab();

    // init filter checkboxes list to be passed to non gui class 
    initFilters();

    // list cell renderer for images + name of things
    initListCellRenderers();

    /* click listener for items to display in thing panels */
    initListClickListeners();

    // display username
    initUserName();

    // set profile picture
    initProfilePicture();

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
                    feedbackLabel.setVisible(false);
                  }
                }
              });
            }
          }).start();
        }

      }
    }).start();
  }

  // called once when dialog is set up
  private void initLikedTab()
  {
    // load liked things asynchronously
    new Thread(new Runnable() {

      public void run()
      {
        // enable userfeedback 
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            // header for Liked
            pnlLiked = new JPanel();
            lblLiked = new JLabel();
            lblLoadingLiked = new JLabel();
            pnlLiked.setAlignmentX(0.0F);
            pnlLiked.setAlignmentY(0.0F);
            pnlLiked.setName("pnlLiked");
            pnlLiked.setOpaque(false);

            lblLiked.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lblLiked.setText("Liked");
            lblLiked.setAlignmentY(0.0F);
            lblLiked.setName("lblLiked");
            lblLiked.setFont(new Font(lblLiked.getFont().getName(), lblLiked.getFont().getStyle(), 14));

            lblLoadingLiked.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lblLoadingLiked.setText("");
            lblLoadingLiked.setAlignmentY(0.0F);
            lblLoadingLiked.setName("lblLoadingLiked");

            javax.swing.GroupLayout pnlLikedLayout = new javax.swing.GroupLayout(pnlLiked);
            pnlLiked.setLayout(pnlLikedLayout);
            pnlLikedLayout.setHorizontalGroup(
              pnlLikedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(pnlLikedLayout.createSequentialGroup()
                .addComponent(lblLiked)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblLoadingLiked, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
            );
            pnlLikedLayout.setVerticalGroup(
              pnlLikedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(pnlLikedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlLikedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addComponent(lblLoadingLiked, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addComponent(lblLiked))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
            
            // set loading headers
            ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL);
            lblLoadingLiked.setIcon((Icon) loadingIcon);
            tpLists.setTabComponentAt(1, pnlLiked);
            
            lblLiked.setVisible(true);
          }
        });
        
        // get things
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        List<Thing> things = thingiverse.getLikedThings();
        loadTab(things, lstLiked, numberLoadingLiked, lblLoadingLiked);
      }
    }).start();
  }

  // called once when dialog is set up
  private void initMyThingsTab()
  {
    // load mythings things asynchronously
    new Thread(new Runnable() {

      public void run()
      {
        // enable userfeedback
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            // header for MyThings
            pnlMyThings = new JPanel();
            lblMyThings = new JLabel();
            lblLoadingMyThings = new JLabel();
            pnlMyThings.setAlignmentX(0.0F);
            pnlMyThings.setAlignmentY(0.0F);
            pnlMyThings.setName("pnlMyThings");
            pnlMyThings.setOpaque(false);

            lblMyThings.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lblMyThings.setText("MyThings");
            lblMyThings.setAlignmentY(0.0F);
            lblMyThings.setName("lblMyThings");
            lblMyThings.setFont(new Font(lblMyThings.getFont().getName(), lblMyThings.getFont().getStyle(), 14));

            lblLoadingMyThings.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lblLoadingMyThings.setText("");
            lblLoadingMyThings.setAlignmentY(0.0F);
            lblLoadingMyThings.setName("lblLoadingMyThings");

            javax.swing.GroupLayout pnlMyThingsLayout = new javax.swing.GroupLayout(pnlMyThings);
            pnlMyThings.setLayout(pnlMyThingsLayout);
            pnlMyThingsLayout.setHorizontalGroup(
              pnlMyThingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(pnlMyThingsLayout.createSequentialGroup()
                .addComponent(lblMyThings)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblLoadingMyThings, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
            );
            pnlMyThingsLayout.setVerticalGroup(
              pnlMyThingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(pnlMyThingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlMyThingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addComponent(lblLoadingMyThings, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addComponent(lblMyThings))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
            
            // set loading headers
            ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL);
            lblLoadingMyThings.setIcon((Icon) loadingIcon);
            tpLists.setTabComponentAt(0, pnlMyThings);
            
            lblLoadingMyThings.setVisible(true);
          }
        });
        
        // get things
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        List<Thing> things = thingiverse.getMyThings();
        loadTab(things, lstMyThings, numberLoadingMyThings, lblLoadingMyThings);
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
        pnlSearch = new JPanel();
        lblSearch = new JLabel();
        lblLoadingSearch = new JLabel();
        pnlSearch.setAlignmentX(0.0F);
        pnlSearch.setAlignmentY(0.0F);
        pnlSearch.setName("pnlSearch");
        pnlSearch.setOpaque(false);

        lblSearch.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSearch.setText("Search");
        lblSearch.setAlignmentY(0.0F);
        lblSearch.setName("lblSearch");
        lblSearch.setFont(new Font(lblSearch.getFont().getName(), lblSearch.getFont().getStyle(), 14));

        lblLoadingSearch.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblLoadingSearch.setText("");
        lblLoadingSearch.setAlignmentY(0.0F);
        lblLoadingSearch.setName("lblLoadingSearch");

        javax.swing.GroupLayout pnlSearchLayout = new javax.swing.GroupLayout(pnlSearch);
        pnlSearch.setLayout(pnlSearchLayout);
        pnlSearchLayout.setHorizontalGroup(
          pnlSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(pnlSearchLayout.createSequentialGroup()
            .addComponent(lblSearch)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(lblLoadingSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlSearchLayout.setVerticalGroup(
          pnlSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(pnlSearchLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(pnlSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lblLoadingSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lblSearch))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        // set loading headers
        ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL);
        lblLoadingSearch.setIcon((Icon) loadingIcon);
        tpLists.setTabComponentAt(3, pnlSearch);


        lblLoadingSearch.setVisible(false);
      }
    });
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
            pnlCollection = new JPanel();
            lblCollection = new JLabel();
            lblLoadingCollection = new JLabel();
            cbCollection = new JComboBox();
            pnlCollection.setAlignmentX(0.0F);
            pnlCollection.setAlignmentY(0.0F);
            pnlCollection.setName("pnlMyThings");
            pnlCollection.setOpaque(false);

            lblCollection.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lblCollection.setText("Collection");
            lblCollection.setAlignmentY(0.0F);
            lblCollection.setName("lblMCollection");
            lblCollection.setFont(new Font(lblCollection.getFont().getName(), lblCollection.getFont().getStyle(), 14));

            lblLoadingCollection.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lblLoadingCollection.setText("");
            lblLoadingCollection.setAlignmentY(0.0F);
            lblLoadingCollection.setName("lblLoadingCollection");
            
            cbCollection.setAlignmentX(0.0F);
            cbCollection.setAlignmentY(0.0F);
            cbCollection.setName("cbCollection");
            cbCollection.setFocusable(false);
            

            javax.swing.GroupLayout pnlCollectionLayout = new javax.swing.GroupLayout(pnlCollection);
            pnlCollection.setLayout(pnlCollectionLayout);
            pnlCollectionLayout.setHorizontalGroup(
              pnlCollectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(pnlCollectionLayout.createSequentialGroup()
                .addComponent(lblCollection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblLoadingCollection, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbCollection, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
              )
            );
            pnlCollectionLayout.setVerticalGroup(
              pnlCollectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(pnlCollectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCollectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addComponent(lblLoadingCollection, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addComponent(lblCollection)
                  .addComponent(cbCollection)
                )
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
            
            // set loading headers
            ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL);
            lblLoadingCollection.setIcon((Icon) loadingIcon);
            tpLists.setTabComponentAt(2, pnlCollection);
            
            lblLoadingCollection.setVisible(true);
          }
        });
        
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
        
        cbCollection.addMouseListener(new MouseListener() {

          public void mouseClicked(MouseEvent e)
          {
            tpLists.setSelectedIndex(2);
          }

          public void mousePressed(MouseEvent e)
          {
          }

          public void mouseReleased(MouseEvent e)
          {
          }

          public void mouseEntered(MouseEvent e)
          {
          }

          public void mouseExited(MouseEvent e)
          {
          }
        });
        
       
        
        // finished initialization, just init with currently selected collection
        actionCollection();
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
            lblLoadingCollection.setVisible(true);
          }
        });
        
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        List<Thing> things = thingiverse.getThingsByCollection(collection);
        loadTab(things, lstCollection, numberLoadingCollection, lblLoadingCollection);
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
        // enable userfeedback
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {   
            lblLoadingSearch.setVisible(true);
          }
        });
        
    
        ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        String queryString = txtSearch.getText();
        List<String> selectedFileTypes = new LinkedList<String>();
        for (JCheckBox filterCheckBox : filterCheckBoxes)
        {
          if (filterCheckBox.isSelected())
          {
            selectedFileTypes.add(filterCheckBox.getText());
          }
        }
        List<Thing> things = thingiverse.search(queryString);
        loadTab(things, lstSearch, numberLoadingSearch, lblLoadingSearch);
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
        sclpCollections = new javax.swing.JScrollPane();
        lstCollection = new javax.swing.JList();
        sclpCollectionThing = new javax.swing.JScrollPane();
        lstCollectionThing = new javax.swing.JList();
        spltpSearchContainer = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        spltpSearch = new javax.swing.JSplitPane();
        sclpSearch = new javax.swing.JScrollPane();
        lstSearch = new javax.swing.JList();
        sclpSearchThing = new javax.swing.JScrollPane();
        lstSearchThing = new javax.swing.JList();
        btnLogout = new javax.swing.JButton();
        pnlFilter = new javax.swing.JPanel();
        cbSVG = new javax.swing.JCheckBox();
        cbDXF = new javax.swing.JCheckBox();
        cbPLF = new javax.swing.JCheckBox();
        cbGCODE = new javax.swing.JCheckBox();
        cbEPS = new javax.swing.JCheckBox();
        lblOpeningFile = new javax.swing.JLabel();
        lblFilter = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
        setName("thingiverseDialog"); // NOI18N

        lProfilePicture.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lProfilePicture.setAlignmentY(0.0F);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(ThingiverseDialog.class);
        lProfilePicture.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lProfilePicture.border.lineColor"))); // NOI18N
        lProfilePicture.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lProfilePicture.setName("lProfilePicture"); // NOI18N

        lUserName.setVerticalAlignment(javax.swing.SwingConstants.TOP);
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

        tpLists.addTab("MyThings", spltpMyThings);

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

        spltpCollections.setName("spltpCollections"); // NOI18N

        sclpCollections.setBorder(null);
        sclpCollections.setMinimumSize(new Dimension(200,300));
        sclpCollections.setName("sclpCollections"); // NOI18N
        sclpCollections.setPreferredSize(new Dimension(400,300));

        lstCollection.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstCollection.setAlignmentX(0.0F);
        lstCollection.setAlignmentY(0.0F);
        lstCollection.setName("lstCollection"); // NOI18N
        sclpCollections.setViewportView(lstCollection);

        spltpCollections.setLeftComponent(sclpCollections);

        sclpCollectionThing.setMinimumSize(new Dimension(200,300));
        sclpCollectionThing.setName("sclpCollectionThing"); // NOI18N

        lstCollectionThing.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstCollectionThing.setName("lstCollectionThing"); // NOI18N
        sclpCollectionThing.setViewportView(lstCollectionThing);

        spltpCollections.setRightComponent(sclpCollectionThing);

        tpLists.addTab(resourceMap.getString("spltpCollections.TabConstraints.tabTitle"), spltpCollections); // NOI18N

        spltpSearchContainer.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spltpSearchContainer.setName("spltpSearchContainer"); // NOI18N

        jPanel1.setMinimumSize(new Dimension(300,300));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        txtSearch.setFont(resourceMap.getFont("txtSearch.font")); // NOI18N
        txtSearch.setName("txtSearch"); // NOI18N
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });
        jPanel1.add(txtSearch, java.awt.BorderLayout.CENTER);

        btnSearch.setText(resourceMap.getString("btnSearch.text")); // NOI18N
        btnSearch.setMaximumSize(new Dimension(50,30));
        btnSearch.setMinimumSize(new Dimension(50,30));
        btnSearch.setName("btnSearch"); // NOI18N
        btnSearch.setPreferredSize(new Dimension(50,30));
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        jPanel1.add(btnSearch, java.awt.BorderLayout.EAST);

        spltpSearchContainer.setTopComponent(jPanel1);

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

        pnlFilter.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("pnlFilter.border.lineColor"))); // NOI18N
        pnlFilter.setAlignmentX(0.0F);
        pnlFilter.setAlignmentY(0.0F);
        pnlFilter.setName("pnlFilter"); // NOI18N
        pnlFilter.setLayout(new java.awt.GridLayout(3, 3, 1, 0));

        cbSVG.setSelected(true);
        cbSVG.setText(resourceMap.getString("cbSVG.text")); // NOI18N
        cbSVG.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cbSVG.setName("cbSVG"); // NOI18N
        pnlFilter.add(cbSVG);

        cbDXF.setSelected(true);
        cbDXF.setText(resourceMap.getString("cbDXF.text")); // NOI18N
        cbDXF.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cbDXF.setName("cbDXF"); // NOI18N
        pnlFilter.add(cbDXF);

        cbPLF.setSelected(true);
        cbPLF.setText(resourceMap.getString("cbPLF.text")); // NOI18N
        cbPLF.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cbPLF.setName("cbPLF"); // NOI18N
        pnlFilter.add(cbPLF);

        cbGCODE.setSelected(true);
        cbGCODE.setText(resourceMap.getString("cbGCODE.text")); // NOI18N
        cbGCODE.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cbGCODE.setName("cbGCODE"); // NOI18N
        pnlFilter.add(cbGCODE);

        cbEPS.setSelected(true);
        cbEPS.setText(resourceMap.getString("cbEPS.text")); // NOI18N
        cbEPS.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cbEPS.setName("cbEPS"); // NOI18N
        pnlFilter.add(cbEPS);

        lblOpeningFile.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblOpeningFile.setIcon(LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL));
        lblOpeningFile.setText(resourceMap.getString("lblOpeningFile.text")); // NOI18N
        lblOpeningFile.setName("lblOpeningFile"); // NOI18N

        lblFilter.setText(resourceMap.getString("lblFilter.text")); // NOI18N
        lblFilter.setName("lblFilter"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnLogout)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(399, 399, 399)
                                .addComponent(lblOpeningFile, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 261, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFilter)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addComponent(pnlFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
            .addComponent(tpLists, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1044, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblOpeningFile, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblFilter)
                        .addComponent(pnlFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(lUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnLogout))
                        .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(tpLists, javax.swing.GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void initComponentsByHand()
  {
    // set general content padding
    JPanel content = (JPanel) this.getContentPane();
    content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
    lstMyThings.addListSelectionListener(new ThingSelectionListener(lstMyThingsThing, filterCheckBoxes));
    lstSearch.addListSelectionListener(new ThingSelectionListener(lstSearchThing, filterCheckBoxes));
    lstLiked.addListSelectionListener(new ThingSelectionListener(lstLikedThing, filterCheckBoxes));
    lstCollection.addListSelectionListener(new ThingSelectionListener(lstCollectionThing, filterCheckBoxes));
    
    // set adapter for ThingFile-lists to listen for double clicks -> load selected file    
    lstSearchThing.addMouseListener(new ThingFileClickListener(mainview, lblOpeningFile));
    lstMyThingsThing.addMouseListener(new ThingFileClickListener(mainview, lblOpeningFile));
    lstLikedThing.addMouseListener(new ThingFileClickListener(mainview, lblOpeningFile));
    lstCollectionThing.addMouseListener(new ThingFileClickListener(mainview, lblOpeningFile));
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

  private void initFilters()
  {
    // initializes the filter list with all filter checkboxes
    Component[] components = pnlFilter.getComponents();
    for (Component comp : components)
    {
      if (comp instanceof JCheckBox)
      {
        this.filterCheckBoxes.add((JCheckBox) comp);
      }
    }
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
    private javax.swing.JButton btnSearch;
    private javax.swing.JCheckBox cbDXF;
    private javax.swing.JCheckBox cbEPS;
    private javax.swing.JCheckBox cbGCODE;
    private javax.swing.JCheckBox cbPLF;
    private javax.swing.JCheckBox cbSVG;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lProfilePicture;
    private javax.swing.JLabel lUserName;
    private javax.swing.JLabel lblFilter;
    private javax.swing.JLabel lblOpeningFile;
    private javax.swing.JList lstCollection;
    private javax.swing.JList lstCollectionThing;
    private javax.swing.JList lstLiked;
    private javax.swing.JList lstLikedThing;
    private javax.swing.JList lstMyThings;
    private javax.swing.JList lstMyThingsThing;
    private javax.swing.JList lstSearch;
    private javax.swing.JList lstSearchThing;
    private javax.swing.JPanel pnlFilter;
    private javax.swing.JScrollPane sclpCollectionThing;
    private javax.swing.JScrollPane sclpCollections;
    private javax.swing.JScrollPane sclpLiked;
    private javax.swing.JScrollPane sclpLikedThing;
    private javax.swing.JScrollPane sclpMyThings;
    private javax.swing.JScrollPane sclpMyThingsThing;
    private javax.swing.JScrollPane sclpSearch;
    private javax.swing.JScrollPane sclpSearchThing;
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
  private javax.swing.JComboBox cbCollection;
}
