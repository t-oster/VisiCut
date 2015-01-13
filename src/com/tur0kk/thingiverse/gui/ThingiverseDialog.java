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
import com.tur0kk.thingiverse.uicomponents.LoadingIcon;
import java.awt.Component;
import java.io.IOException;
import java.net.MalformedURLException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
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
public class ThingiverseDialog extends javax.swing.JFrame
{
  private AtomicInteger numberLoadingLiked = new AtomicInteger();
  private AtomicInteger numberLoadingMyThings = new AtomicInteger();
  private AtomicInteger numberLoadingSearch = new AtomicInteger();
  private List<JCheckBox> filterCheckBoxes = new LinkedList<JCheckBox>(); // holds the filter checkboxes for passing to non gui class
  final private MainView mainview;
  

  /** Creates new form ThingiverseDialog */
  public ThingiverseDialog(java.awt.Frame parent, boolean modal) throws AccessException, MalformedURLException, IOException
  {    
    // just hide to keep state
    this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    
    // save parent for loading files to main view
    this.mainview = (MainView) parent;
    
    // init componentes
    initComponents(); // auto generated code
    initTabbedPaneHeader();
    initComponentsByHand();
    
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
   
    
    // display things
    actionLoadMyThings();
    
    // display liked things
    actionLoadLiked();
    
  }
  
  private void actionLoadLiked(){
    
    new Thread(new Runnable() {
     
      public void run()
      {        
        final ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        
        // get things
        LinkedList<Thing> things = thingiverse.getLikedThings();
        
        // init my things model with loading images
        DefaultListModel myThingsModel = new DefaultListModel(); // model for JList
        Iterator<Thing> i1 = things.iterator(); // iterate over each thingiverse thing and add to model
        int index = 0;
        while (i1.hasNext()) 
        {
          // get loading icon
          ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);
          
          // set changing observer for loading images to update gif 
          loadingIcon.setImageObserver(new AnimationImageObserverList(lstLiked, index));
          
          // add thing to model
          Thing aThing = i1.next();
          aThing.setImage(loadingIcon);
          myThingsModel.addElement(aThing);
          
          index +=1;
        }
       
        // display myThingsModel in my things list
        final DefaultListModel model = myThingsModel;
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            lstLiked.setModel(model);            
          }
        });
        
        // set an atomic counter to keep track of number of loading icons. If 0 again, disable loading header.
        numberLoadingLiked.set(myThingsModel.size());
        
        // start a thread for each image to load image asynchronous
        for (final Thing entry : things)
        {
          final String url = entry.getImageUrl();
          new Thread(new Runnable() 
          {
              public void run()
              {
                
                String file = thingiverse.downloadImage(url); // download  image
                ImageIcon imageIcon = null;
                if("".equals(file)){ // load default image if image not avaliable
                  imageIcon = new ImageIcon(LoadingIcon.class.getResource("resources/image_not_found.png"));
                }
                else{
                  imageIcon = new ImageIcon(file);
                }
                
                     
                // overwrite image
                final ImageIcon objectImage = imageIcon;
                SwingUtilities.invokeLater(new Runnable() {
                  public void run()
                  {
                    // overwrite image
                    entry.setImage(objectImage);
                    lstLiked.updateUI();
                    
                    // image loaded, decrement loading images
                    if(numberLoadingLiked.decrementAndGet()==0){
                      lblLoadingLiked.setVisible(false);
                    }
                  }
                });
              }
            }).start();
          }
        
      }
    }).start();
  }
  
  private void actionLoadMyThings(){
    
    new Thread(new Runnable() {
     
      public void run()
      {        
        final ThingiverseManager thingiverse = ThingiverseManager.getInstance();
        
        // get things
        LinkedList<Thing> things = thingiverse.getMyThings();
        
        // init my things model with loading images
        DefaultListModel myThingsModel = new DefaultListModel(); // model for JList
        Iterator<Thing> i1 = things.iterator(); // iterate over each thingiverse thing and add to model
        int index = 0;
        while (i1.hasNext()) 
        {
          // get loading icon
          ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);
          
          // set changing observer for loading images to update gif 
          loadingIcon.setImageObserver(new AnimationImageObserverList(lstMyThings, index));
          
          // add thing to model
          Thing aThing = i1.next();
          aThing.setImage(loadingIcon);
          myThingsModel.addElement(aThing);
          
          index +=1;
        }
       
        // display myThingsModel in my things list
        final DefaultListModel model = myThingsModel;
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            lstMyThings.setModel(model);            
          }
        });
        
        // set an atomic counter to keep track of number of loading icons. If 0 again, disable loading header.
        numberLoadingMyThings.set(myThingsModel.size());
        
        // start a thread for each image to load image asynchronous
        for (final Thing entry : things)
        {
          final String url = entry.getImageUrl();
          new Thread(new Runnable() 
          {
              public void run()
              {
                
                String file = thingiverse.downloadImage(url); // download  image
                ImageIcon imageIcon = null;
                if("".equals(file)){ // load default image if image not avaliable
                  imageIcon = new ImageIcon(LoadingIcon.class.getResource("resources/image_not_found.png"));
                }
                else{
                  imageIcon = new ImageIcon(file);
                }
                     
                // overwrite image
                final ImageIcon objectImage = imageIcon;
                SwingUtilities.invokeLater(new Runnable() {
                  public void run()
                  {
                    // overwrite image
                    entry.setImage(objectImage);
                    lstMyThings.updateUI();
                    
                    // image loaded, decrement loading images
                    if(numberLoadingMyThings.decrementAndGet()==0){
                      lblLoadingMyThings.setVisible(false);
                    }
                  }
                });
              }
            }).start();
          }
        
      }
    }).start();
  }
  
  private void actionSearch(){
    lblLoadingSearch.setVisible(true);

    final ThingiverseManager thingiverse = ThingiverseManager.getInstance();

    // display Featured
    new Thread(new Runnable()
    {

      public void run()
      {
        String tagList = txtSearch.getText();

        // get url map
        LinkedList<Thing> things = thingiverse.search(tagList);

        // init my things model with loading images
        DefaultListModel searchModel = new DefaultListModel(); // model for JList
        Iterator<Thing> i1 = things.iterator(); // iterate over each thingiverse thing and add to model
        int index = 0;
        while (i1.hasNext())
        {
          // get loading icon
          ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);

          // set changing observer for loading images to update gif 
          loadingIcon.setImageObserver(new AnimationImageObserverList(lstSearch, index));

          // add thing to model
          Thing aThing = i1.next();
          aThing.setImage(loadingIcon);
          searchModel.addElement(aThing);

          index += 1;
        }

        // display myThingsModel in my things list
        final DefaultListModel model = searchModel;
        SwingUtilities.invokeLater(new Runnable()
        {

          public void run()
          {
            lstSearch.setModel(model);
          }
        });

        numberLoadingSearch.set(searchModel.size());

        // start a thread for each image to load asynchronous
        for (final Thing entry : things)
        {
          final String url = entry.getImageUrl();
          new Thread(new Runnable()
          {

            public void run()
            {

              String file = thingiverse.downloadImage(url); // download  image
              ImageIcon imageIcon = null;
              if("".equals(file)){ // load default image if image not avaliable
                imageIcon = new ImageIcon(LoadingIcon.class.getResource("resources/image_not_found.png"));
              }
              else{
                imageIcon = new ImageIcon(file);
              }

              // overwrite image
              final ImageIcon objectImage = imageIcon;
              SwingUtilities.invokeLater(new Runnable()
              {

                public void run()
                {
                  entry.setImage(objectImage);
                  lstSearch.updateUI();

                  // image loaded, decrement loading images
                  if (numberLoadingSearch.decrementAndGet() == 0)
                  {
                    lblLoadingSearch.setVisible(false);
                  }
                }
              });
            }
          }).start();
        }

      }
    }).start();
    
    
  }


  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
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
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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

private void initComponentsByHand(){
  // set general content padding
  JPanel content = (JPanel)this.getContentPane();
  content.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
  
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
    if(key == '\n'){
      this.actionSearch();
    }
  }//GEN-LAST:event_txtSearchKeyPressed

  
private void initTabbedPaneHeader(){
  
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
  lblLoadingMyThings.setIcon((Icon)loadingIcon);
  lblLoadingSearch.setIcon((Icon)loadingIcon);
  lblLoadingLiked.setIcon((Icon)loadingIcon);
  tpLists.setTabComponentAt(0, pnlMyThings);
  tpLists.setTabComponentAt(1, pnlLiked);
  tpLists.setTabComponentAt(2, pnlSearch);
  
  lblLoadingSearch.setVisible(false); // only visible if searching for things
}


private void initListCellRenderers(){
  // thing list renderer
  lstMyThings.setCellRenderer(new ThingListRenderer());
  lstSearch.setCellRenderer(new ThingListRenderer());
  lstLiked.setCellRenderer(new ThingListRenderer());
  
  // thing file list renderer
  lstSearchThing.setCellRenderer(new ThingFileListRenderer());
  lstMyThingsThing.setCellRenderer(new ThingFileListRenderer());
  lstLikedThing.setCellRenderer(new ThingFileListRenderer());
}

private void initListClickListeners(){
  // click listener loads files of selected thing  
  lstMyThings.addListSelectionListener(new ThingSelectionListener(lstMyThingsThing, filterCheckBoxes));  
  lstSearch.addListSelectionListener(new ThingSelectionListener(lstSearchThing, filterCheckBoxes));
  lstLiked.addListSelectionListener(new ThingSelectionListener(lstLikedThing, filterCheckBoxes));
  
  // set adapter for ThingFile-lists to listen for double clicks -> load selected file    
  lstSearchThing.addMouseListener(new ThingFileClickListener(mainview, lblOpeningFile));
  lstMyThingsThing.addMouseListener(new ThingFileClickListener(mainview, lblOpeningFile));
  lstLikedThing.addMouseListener(new ThingFileClickListener(mainview, lblOpeningFile));
}

private void initUserName(){
  new Thread(new Runnable() {
    
    String username = null;
    public void run()
    {
      ThingiverseManager thingiverse = ThingiverseManager.getInstance();
      
      username = thingiverse.getUserName();
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lUserName.setText("Hello " + username);
        }
      });
    }
  }).start();
}

private void initProfilePicture(){
  
    new Thread(new Runnable() {

    public void run()
    {
      ThingiverseManager thingiverse = ThingiverseManager.getInstance();
      
      // profile picture, resized to label
      try
      {
        // get loading icon
        final ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);
        // display loading icon in label
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            lProfilePicture.setIcon(loadingIcon);
          }
        });
        
        // get actual user profile picture
        String path = thingiverse.getUserImage();
        String file = thingiverse.downloadImage(path); // download user image
        ImageIcon imageIcon = null;
        if(file == ""){ // load default image if image not avaliable
          imageIcon = new ImageIcon(LoadingIcon.class.getResource("resources/avatar_default.jpg"));
        }
        else{
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
        SwingUtilities.invokeLater(new Runnable() {
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

private void initFilters(){
  // initializes the filter list with all filter checkboxes
  Component[] components = pnlFilter.getComponents();
  for(Component comp: components){
    if(comp instanceof JCheckBox){
      this.filterCheckBoxes.add((JCheckBox)comp);
    }
  }
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
    private javax.swing.JList lstLiked;
    private javax.swing.JList lstLikedThing;
    private javax.swing.JList lstMyThings;
    private javax.swing.JList lstMyThingsThing;
    private javax.swing.JList lstSearch;
    private javax.swing.JList lstSearchThing;
    private javax.swing.JPanel pnlFilter;
    private javax.swing.JScrollPane sclpLiked;
    private javax.swing.JScrollPane sclpLikedThing;
    private javax.swing.JScrollPane sclpMyThings;
    private javax.swing.JScrollPane sclpMyThingsThing;
    private javax.swing.JScrollPane sclpSearch;
    private javax.swing.JScrollPane sclpSearchThing;
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
}
