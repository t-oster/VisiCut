/*
 * ThingiverseDialog.java
 *
 * Created on 13.12.2014, 17:42:20
 */
package com.t_oster.visicut.gui;

import com.t_oster.uicomponents.LoadingIcon;
import com.t_oster.visicut.gui.mapping.AnimationImageObserverList;
import com.t_oster.visicut.gui.mapping.ThingListRenderer;
import com.tur0kk.thingiverse.Thing;
import com.tur0kk.thingiverse.ThingiverseManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.rmi.AccessException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;


/**
 *
 * @author Sven
 */
public class ThingiverseDialog extends javax.swing.JDialog
{
  private AtomicInteger numberLoadingMyThings = new AtomicInteger();
  private AtomicInteger numberLoadingFeatured = new AtomicInteger();

  /** Creates new form ThingiverseDialog */
  public ThingiverseDialog(java.awt.Frame parent, boolean modal) throws AccessException, MalformedURLException, IOException
  {
    super(parent, modal);
    initComponents();
    
    
    // login necessary for this thingiverse integration
    final ThingiverseManager thingiverse = ThingiverseManager.getInstance();
    if(!thingiverse.isLoggedIn())
    {
      thingiverse.logIn();
    }
    if(!thingiverse.isLoggedIn())
    {
      throw new AccessException("No correct access token");
    }
    
    //else
    // display username
    new Thread(new Runnable() {
      String username = null;
      public void run()
      {
        username = thingiverse.getUserName();
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            lUserName.setText("Hello " + username);
          }
        });
      }
    }).start();
    
    // set profile picture
    new Thread(new Runnable() {
      ImageIcon profilePicture = null;
      
      public void run()
      {
        // profile picture, resized to label
        try
        {
          String path = thingiverse.getUserImage();
          URL url = new URL(path);
          // Hack: Avoid loading the default image from web (which fails)          
          if (url.toString().startsWith("https://www.thingiverse.com/img/default/avatar/avatar"))
          {
            url = LoadingIcon.class.getResource("resources/avatar_default.jpg");
          }

          ImageIcon imageIcon = new ImageIcon(url);
          Image rawImage = imageIcon.getImage();
          Image scaledImage = rawImage.getScaledInstance(
            lProfilePicture.getWidth(),
            lProfilePicture.getHeight(),
            Image.SCALE_SMOOTH);
          profilePicture = new ImageIcon(scaledImage);
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
   
    // fill thing lists
    fillLists();
    
  }
  
  private void fillLists() {
    initTabbedPaneHeader();
    
    final ThingiverseManager thingiverse = ThingiverseManager.getInstance();
    
    // display MyThings
    new Thread(new Runnable() {
     
      public void run()
      {
        String tagList = txtFilter.getText();
        
        // get things
        LinkedList<Thing> things = thingiverse.getMyThings(tagList);
        
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
            lstMyThings.setCellRenderer(new ThingListRenderer());
            
          }
        });
        
        // set an atomic counter to keep track of number of loading icons. If 0 again, disable loading header.
        numberLoadingMyThings.set(myThingsModel.size());
        
        // start a thread for each image to load image asynchronous
        for (final Thing entry : things)
        {
          final String url = entry.getImageLocation();
          new Thread(new Runnable() 
          {
              public void run()
              {
                
                // load image
                ImageIcon icon;
                try
                {
                  icon = new ImageIcon(new URL(url));
                }
                catch (MalformedURLException ex)
                {
                  System.err.println("Image not found: " + url);
                  icon = new ImageIcon(LoadingIcon.class.getResource("resources/image_not_found.png"));
                }
                     
                // overwrite image
                final ImageIcon objectImage = icon;
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
    
    // display Featured
    new Thread(new Runnable() {
     
      public void run()
      {
        String tagList = txtFilter.getText();
        
        // get url map
        LinkedList<Thing> things = thingiverse.getFeatured(tagList);
        
        // init my things model with loading images
        DefaultListModel featuredModel = new DefaultListModel(); // model for JList
        Iterator<Thing> i1 = things.iterator(); // iterate over each thingiverse thing and add to model
        int index = 0;
        while (i1.hasNext()) 
        {
          // get loading icon
          ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);
          
          // set changing observer for loading images to update gif 
          loadingIcon.setImageObserver(new AnimationImageObserverList(lstFeatured, index));
          
          // add thing to model
          Thing aThing = i1.next();
          aThing.setImage(loadingIcon);
          featuredModel.addElement(aThing);
          
          index += 1;
        }
        
        // display myThingsModel in my things list
        final DefaultListModel model = featuredModel;
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            lstFeatured.setModel(model);
            lstFeatured.setCellRenderer(new ThingListRenderer());
          }
        });
        
        numberLoadingFeatured.set(featuredModel.size());
        
        // start a thread for each image to load asynchronous
        for (final Thing entry : things)
        {
          final String url = entry.getImageLocation();
          new Thread(new Runnable() 
          {
              public void run()
              {

                // load image
                ImageIcon icon;
                try
                {
                  icon = new ImageIcon(new URL(url));
                }
                catch (MalformedURLException ex)
                {
                  System.err.println("Image not found: " + url);
                  icon = new ImageIcon(LoadingIcon.class.getResource("resources/image_not_found.png"));
                }
                
                // overwrite image
                final ImageIcon objectImage = icon;
                SwingUtilities.invokeLater(new Runnable() {
                  public void run()
                  {
                    entry.setImage(objectImage);
                    lstFeatured.updateUI();
                    
                    // image loaded, decrement loading images
                    if(numberLoadingFeatured.decrementAndGet()==0){
                      lblLoadingFeatured.setVisible(false);
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
        spltpFeatured = new javax.swing.JSplitPane();
        sclpFeatured = new javax.swing.JScrollPane();
        lstFeatured = new javax.swing.JList();
        sclpFeaturedThing = new javax.swing.JScrollPane();
        lstFeaturedThing = new javax.swing.JList();
        txtFilter = new javax.swing.JTextField();
        btnFilter = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(ThingiverseDialog.class);
        setTitle(resourceMap.getString("thingiverseDialog.title")); // NOI18N
        setLocationByPlatform(true);
        setName("thingiverseDialog"); // NOI18N

        lProfilePicture.setBackground(resourceMap.getColor("lProfilePicture.background")); // NOI18N
        lProfilePicture.setText(resourceMap.getString("lProfilePicture.text")); // NOI18N
        lProfilePicture.setAlignmentX(5.0F);
        lProfilePicture.setAlignmentY(5.0F);
        lProfilePicture.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(-16777216,true)));
        lProfilePicture.setName("lProfilePicture"); // NOI18N

        lUserName.setText(resourceMap.getString("lUserName.text")); // NOI18N
        lUserName.setName("lUserName"); // NOI18N

        tpLists.setAlignmentX(5.0F);
        tpLists.setAlignmentY(5.0F);
        tpLists.setMaximumSize(new Dimension(50000, 300));
        tpLists.setMinimumSize(new Dimension(250, 300));
        tpLists.setName("tpLists"); // NOI18N
        tpLists.setPreferredSize(new Dimension(250, 300));

        spltpMyThings.setName("spltpMyThings"); // NOI18N

        sclpMyThings.setBorder(null);
        sclpMyThings.setMinimumSize(new Dimension(220,300));
        sclpMyThings.setName("sclpMyThings"); // NOI18N

        lstMyThings.setAlignmentX(0.0F);
        lstMyThings.setAlignmentY(0.0F);
        lstMyThings.setName("lstMyThings"); // NOI18N
        sclpMyThings.setViewportView(lstMyThings);

        spltpMyThings.setLeftComponent(sclpMyThings);

        sclpMyThingsThing.setName("sclpMyThingsThing"); // NOI18N

        lstMyThingsThing.setName("lstMyThingsThing"); // NOI18N
        sclpMyThingsThing.setViewportView(lstMyThingsThing);

        spltpMyThings.setRightComponent(sclpMyThingsThing);

        tpLists.addTab(resourceMap.getString("spltpMyThings.TabConstraints.tabTitle"), spltpMyThings); // NOI18N

        spltpFeatured.setName("spltpFeatured"); // NOI18N

        sclpFeatured.setBorder(null);
        sclpFeatured.setMinimumSize(new Dimension(220,300));
        sclpFeatured.setName("sclpFeatured"); // NOI18N

        lstFeatured.setName("lstFeatured"); // NOI18N
        sclpFeatured.setViewportView(lstFeatured);

        spltpFeatured.setLeftComponent(sclpFeatured);

        sclpFeaturedThing.setName("sclpFeaturedThing"); // NOI18N

        lstFeaturedThing.setName("lstFeaturedThing"); // NOI18N
        sclpFeaturedThing.setViewportView(lstFeaturedThing);

        spltpFeatured.setRightComponent(sclpFeaturedThing);

        tpLists.addTab(resourceMap.getString("spltpFeatured.TabConstraints.tabTitle"), spltpFeatured); // NOI18N

        txtFilter.setText(resourceMap.getString("txtFilter.text")); // NOI18N
        txtFilter.setName("txtFilter"); // NOI18N

        btnFilter.setText(resourceMap.getString("btnFilter.text")); // NOI18N
        btnFilter.setName("btnFilter"); // NOI18N
        btnFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(lUserName))
                    .addComponent(txtFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(tpLists, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tpLists, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lUserName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 113, Short.MAX_VALUE)
                        .addComponent(btnFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        lProfilePicture.getAccessibleContext().setAccessibleDescription(resourceMap.getString("lProfilePic.AccessibleContext.accessibleDescription")); // NOI18N
        tpLists.getAccessibleContext().setAccessibleName(resourceMap.getString("tpLists.AccessibleContext.accessibleName")); // NOI18N

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void btnFilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnFilterActionPerformed
  {//GEN-HEADEREND:event_btnFilterActionPerformed
    // fill lists again with tag list from textfield, read out by this method
    fillLists();

  }//GEN-LAST:event_btnFilterActionPerformed

  
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
  
  
  // header for Featured
  pnlFeatured = new JPanel();
  lblFeatured = new JLabel();
  lblLoadingFeatured = new JLabel();
  pnlFeatured.setAlignmentX(0.0F);
  pnlFeatured.setAlignmentY(0.0F);
  pnlFeatured.setName("pnlFeatured");
  pnlFeatured.setOpaque(false);

  lblFeatured.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
  lblFeatured.setText("Featured");
  lblFeatured.setAlignmentY(0.0F);
  lblFeatured.setName("lblFeatured"); 

  lblLoadingFeatured.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
  lblLoadingFeatured.setText("");
  lblLoadingFeatured.setAlignmentY(0.0F);
  lblLoadingFeatured.setName("lblLoadingFeatured"); 

  javax.swing.GroupLayout pnlFeaturedLayout = new javax.swing.GroupLayout(pnlFeatured);
  pnlFeatured.setLayout(pnlFeaturedLayout);
  pnlFeaturedLayout.setHorizontalGroup(
      pnlFeaturedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(pnlFeaturedLayout.createSequentialGroup()
          .addComponent(lblFeatured)
          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(lblLoadingFeatured, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
  );
  pnlFeaturedLayout.setVerticalGroup(
      pnlFeaturedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(pnlFeaturedLayout.createSequentialGroup()
          .addContainerGap()
          .addGroup(pnlFeaturedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lblLoadingFeatured, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lblFeatured))
          .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
  );
  
  
  // set loading headers
  ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL);
  lblLoadingMyThings.setIcon((Icon)loadingIcon);
  lblLoadingFeatured.setIcon((Icon)loadingIcon);
  tpLists.setTabComponentAt(0, pnlMyThings);
  tpLists.setTabComponentAt(1, pnlFeatured);
}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFilter;
    private javax.swing.JLabel lProfilePicture;
    private javax.swing.JLabel lUserName;
    private javax.swing.JList lstFeatured;
    private javax.swing.JList lstFeaturedThing;
    private javax.swing.JList lstMyThings;
    private javax.swing.JList lstMyThingsThing;
    private javax.swing.JScrollPane sclpFeatured;
    private javax.swing.JScrollPane sclpFeaturedThing;
    private javax.swing.JScrollPane sclpMyThings;
    private javax.swing.JScrollPane sclpMyThingsThing;
    private javax.swing.JSplitPane spltpFeatured;
    private javax.swing.JSplitPane spltpMyThings;
    private javax.swing.JTabbedPane tpLists;
    private javax.swing.JTextField txtFilter;
    // End of variables declaration//GEN-END:variables

    // hand written variable declaration
    private javax.swing.JPanel pnlMyThings;
    private javax.swing.JLabel lblMyThings;
    private javax.swing.JLabel lblLoadingMyThings;
    private javax.swing.JPanel pnlFeatured;
    private javax.swing.JLabel lblFeatured;
    private javax.swing.JLabel lblLoadingFeatured;
}
