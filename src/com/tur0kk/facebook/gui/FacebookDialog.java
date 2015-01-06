/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FacebookDialog.java
 *
 * Created on 06.01.2015, 13:45:50
 */
package com.tur0kk.facebook.gui;

import com.github.sarxos.webcam.Webcam;
import com.tur0kk.facebook.FacebookManager;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 *
 * @author Sven
 */
public class FacebookDialog extends javax.swing.JDialog
{

  /** Creates new form FacebookDialog */
  public FacebookDialog(java.awt.Frame parent, boolean modal)
  {
    super(parent, modal);
    initComponents();
    
    // enable picture taking
    if(isCameraDetected()){
      btnPhoto.setEnabled(true);
      lblAttachMessage.setVisible(false);
    }
    else{
      btnPhoto.setEnabled(false);
      lblAttachMessage.setVisible(true);
    }
    
    final FacebookManager facebook = FacebookManager.getInstance();
    
    // display username
    new Thread(new Runnable() {
      String username = null;
      public void run()
      {
        username = facebook.getUserName();
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
      
      public void run()
      {
        // profile picture, resized to label
        try
        {
          String path = facebook.getUserImage();
          URL url = new URL(path);

          // load profile picture and scale to label
          ImageIcon imageIcon = new ImageIcon(url);
          Image rawImage = imageIcon.getImage();
          Image scaledImage = rawImage.getScaledInstance(
            lProfilePicture.getWidth(),
            lProfilePicture.getHeight(),
            Image.SCALE_SMOOTH);
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

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnLogout = new javax.swing.JButton();
        lUserName = new javax.swing.JLabel();
        lProfilePicture = new javax.swing.JLabel();
        btnPhoto = new javax.swing.JButton();
        lblPhoto = new javax.swing.JLabel();
        lblAttachMessage = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(FacebookDialog.class);
        btnLogout.setText(resourceMap.getString("btnLogout.text")); // NOI18N
        btnLogout.setName("btnLogout"); // NOI18N
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        lUserName.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lUserName.setName("lUserName"); // NOI18N

        lProfilePicture.setAlignmentX(5.0F);
        lProfilePicture.setAlignmentY(5.0F);
        lProfilePicture.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(-16777216,true)));
        lProfilePicture.setName("lProfilePicture"); // NOI18N

        btnPhoto.setText(resourceMap.getString("btnPhoto.text")); // NOI18N
        btnPhoto.setName("btnPhoto"); // NOI18N
        btnPhoto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPhotoActionPerformed(evt);
            }
        });

        lblPhoto.setText(resourceMap.getString("lblPhoto.text")); // NOI18N
        lblPhoto.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(-16777216,true)));
        lblPhoto.setName("lblPhoto"); // NOI18N

        lblAttachMessage.setText(resourceMap.getString("lblAttachMessage.text")); // NOI18N
        lblAttachMessage.setName("lblAttachMessage"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 346, Short.MAX_VALUE)
                        .addComponent(btnLogout))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnPhoto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblAttachMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblAttachMessage, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(btnPhoto))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
    FacebookManager.getInstance().logOut();
    this.dispose();
}//GEN-LAST:event_btnLogoutActionPerformed

private void btnPhotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPhotoActionPerformed
  if(isCameraDetected()){
    
    // start thread to take photo and display
    new Thread(new Runnable() 
    {
        public void run()
        {
          // get webcam
          Webcam webcam = Webcam.getDefault();
          webcam.open();

          // take picture
          BufferedImage image = webcam.getImage();
          ImageIcon imageIcon = new ImageIcon(image);

          // scale to label
          Image rawImage = imageIcon.getImage();
          Image scaledImage = rawImage.getScaledInstance(
            lblPhoto.getWidth(),
            lblPhoto.getHeight(),
            Image.SCALE_SMOOTH);
          final ImageIcon picture = new ImageIcon(scaledImage);
          SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
              lblPhoto.setIcon(picture);
            }
          });
        }
      }).start();
    
  }
}//GEN-LAST:event_btnPhotoActionPerformed

  
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnPhoto;
    private javax.swing.JLabel lProfilePicture;
    private javax.swing.JLabel lUserName;
    private javax.swing.JLabel lblAttachMessage;
    private javax.swing.JLabel lblPhoto;
    // End of variables declaration//GEN-END:variables


private boolean isCameraDetected(){
  Webcam webcam = Webcam.getDefault();
  if (webcam != null) {
    return true;
  } else {
    return false;
  }
}

}
