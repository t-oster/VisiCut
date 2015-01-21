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
import com.t_oster.visicut.gui.MainView;
import com.tur0kk.facebook.FacebookManager;
import com.tur0kk.LoadingIcon;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Sven
 */
public class FacebookDialog extends javax.swing.JDialog
{
  private Thread livecamThread;
  final private MainView mainview;
  
  /** Creates new form FacebookDialog */
  public FacebookDialog(java.awt.Frame parent, boolean modal){
    super(parent, modal);
    
    initComponents();
    
    // just hide to keep state
    this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    
    // close camera on exit
    initWindowListener();
    
    // save parent for modality faking
    this.mainview = (MainView) parent;
    
    // change cam 
    slCam.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e)
      {
        if(!slCam.getValueIsAdjusting()){
          setupCamera();
        }
        
      }
    });
    
    // enable picture taking
    setupCamera();
    
    // user name
    initUsername();
    
    // profile picture
    initProfilePicture();
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
        pnlFoto = new javax.swing.JPanel();
        btnPhoto = new javax.swing.JButton();
        btnPhotoRedo = new javax.swing.JButton();
        lblAttachMessage = new javax.swing.JLabel();
        lblPhoto = new javax.swing.JLabel();
        pnlPublish = new javax.swing.JPanel();
        lblPublish = new javax.swing.JLabel();
        lblPublishSuccessStatus = new javax.swing.JLabel();
        btnPublish = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtaPublish = new javax.swing.JTextArea();
        lblLoading = new javax.swing.JLabel();
        slCam = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setResizable(false);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(FacebookDialog.class);
        btnLogout.setText(resourceMap.getString("btnLogout.text")); // NOI18N
        btnLogout.setAlignmentX(5.0F);
        btnLogout.setAlignmentY(5.0F);
        btnLogout.setName("btnLogout"); // NOI18N
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        lUserName.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lUserName.setName("lUserName"); // NOI18N

        lProfilePicture.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lProfilePicture.setAlignmentX(5.0F);
        lProfilePicture.setAlignmentY(5.0F);
        lProfilePicture.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lProfilePicture.border.lineColor"))); // NOI18N
        lProfilePicture.setName("lProfilePicture"); // NOI18N

        pnlFoto.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("pnlFoto.border.lineColor"))); // NOI18N
        pnlFoto.setName("pnlFoto"); // NOI18N

        btnPhoto.setText(resourceMap.getString("btnPhoto.text")); // NOI18N
        btnPhoto.setName("btnPhoto"); // NOI18N
        btnPhoto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPhotoActionPerformed(evt);
            }
        });

        btnPhotoRedo.setText(resourceMap.getString("btnPhotoRedo.text")); // NOI18N
        btnPhotoRedo.setName("btnPhotoRedo"); // NOI18N
        btnPhotoRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPhotoRedoActionPerformed(evt);
            }
        });

        lblAttachMessage.setText(resourceMap.getString("lblAttachMessage.text")); // NOI18N
        lblAttachMessage.setName("lblAttachMessage"); // NOI18N

        lblPhoto.setText(resourceMap.getString("lblPhoto.text")); // NOI18N
        lblPhoto.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lProfilePicture.border.lineColor"))); // NOI18N
        lblPhoto.setName("lblPhoto"); // NOI18N

        javax.swing.GroupLayout pnlFotoLayout = new javax.swing.GroupLayout(pnlFoto);
        pnlFoto.setLayout(pnlFotoLayout);
        pnlFotoLayout.setHorizontalGroup(
            pnlFotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFotoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnPhoto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnPhotoRedo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAttachMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(54, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlFotoLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        pnlFotoLayout.setVerticalGroup(
            pnlFotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFotoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlFotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPhoto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnPhotoRedo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblAttachMessage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pnlPublish.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("pnlPublish.border.lineColor"))); // NOI18N
        pnlPublish.setName("pnlPublish"); // NOI18N

        lblPublish.setText(resourceMap.getString("lblPublish.text")); // NOI18N
        lblPublish.setAlignmentY(0.0F);
        lblPublish.setName("lblPublish"); // NOI18N

        lblPublishSuccessStatus.setText(resourceMap.getString("lblPublishSuccessStatus.text")); // NOI18N
        lblPublishSuccessStatus.setName("lblPublishSuccessStatus"); // NOI18N

        btnPublish.setText(resourceMap.getString("btnPublish.text")); // NOI18N
        btnPublish.setName("btnPublish"); // NOI18N
        btnPublish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPublishActionPerformed(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        txtaPublish.setColumns(20);
        txtaPublish.setRows(5);
        txtaPublish.setAlignmentX(0.0F);
        txtaPublish.setAlignmentY(0.0F);
        txtaPublish.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lProfilePicture.border.lineColor"))); // NOI18N
        txtaPublish.setName("txtaPublish"); // NOI18N
        jScrollPane1.setViewportView(txtaPublish);

        lblLoading.setIcon(LoadingIcon.get(LoadingIcon.CIRCLEBALL_SMALL));
        lblLoading.setText(resourceMap.getString("lblLoading.text")); // NOI18N
        lblLoading.setName("lblLoading"); // NOI18N

        javax.swing.GroupLayout pnlPublishLayout = new javax.swing.GroupLayout(pnlPublish);
        pnlPublish.setLayout(pnlPublishLayout);
        pnlPublishLayout.setHorizontalGroup(
            pnlPublishLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPublishLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPublishLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnlPublishLayout.createSequentialGroup()
                        .addComponent(lblPublish)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblPublishSuccessStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblLoading, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnPublish))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 338, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        pnlPublishLayout.setVerticalGroup(
            pnlPublishLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPublishLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPublishLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPublish, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnPublish)
                    .addComponent(lblLoading, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPublishSuccessStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                .addContainerGap())
        );

        slCam.setMaximum(1);
        slCam.setFocusable(false);
        slCam.setName("slCam"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlFoto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pnlPublish, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lProfilePicture, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 228, Short.MAX_VALUE)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(slCam, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel2))
                            .addComponent(btnLogout))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lProfilePicture, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(lUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                        .addComponent(btnLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(slCam, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlPublish, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlFoto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
    FacebookManager.getInstance().logOut();
    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)); // closing event closes the camera
}//GEN-LAST:event_btnLogoutActionPerformed

private void btnPhotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPhotoActionPerformed
  if(isCameraDetected()){
    closeCamera();
    
    // enable publishing
    btnPhoto.setEnabled(false);
    btnPhotoRedo.setEnabled(true);
    btnPublish.setEnabled(true);
    txtaPublish.setEditable(true);
    txtaPublish.setBackground(Color.white);
  }
  else{
    setupCamera(); // disabled
  }
}//GEN-LAST:event_btnPhotoActionPerformed

  private void btnPhotoRedoActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnPhotoRedoActionPerformed
  {//GEN-HEADEREND:event_btnPhotoRedoActionPerformed
    setupCamera();
  }//GEN-LAST:event_btnPhotoRedoActionPerformed

  private void btnPublishActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnPublishActionPerformed
  {//GEN-HEADEREND:event_btnPublishActionPerformed
    
    
    // disable controls for publishing
    lblLoading.setVisible(true);
    btnPhotoRedo.setEnabled(false);
    btnPublish.setEnabled(false);
    txtaPublish.setEditable(false);
    txtaPublish.setBackground(Color.lightGray);
    
    // things to publish
    ImageIcon icon = (ImageIcon)lblPhoto.getIcon();
    final Image image = icon.getImage();    
    final String message = txtaPublish.getText();
    
    
    new Thread(new Runnable() {

      public void run()
      {
        FacebookManager facebook = FacebookManager.getInstance();
        boolean success = facebook.publishProject(message, image);
        String msg = "";
        if(success){
          msg = "Successful upload";
        }else{
          msg = "Error uploading photo";
        }
        
        final String message = msg;
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            lblPublishSuccessStatus.setText(message);
            lblPublishSuccessStatus.setVisible(true);
            btnPhotoRedo.setEnabled(true);
            lblLoading.setVisible(false);
          }
        });
        
      }
    }).start();
    
    
  }//GEN-LAST:event_btnPublishActionPerformed

private void initWindowListener(){
  this.addWindowListener(new WindowListener() {

      public void windowOpened(WindowEvent e)
      {

      }

      public void windowClosing(WindowEvent e)
      {
         closeCamera();
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

private void initUsername(){
    
  // display username
  new Thread(new Runnable() {
    String username = null;
    public void run()
    {
      // display loading icon
      final ImageIcon loadingIcon = LoadingIcon.get(LoadingIcon.CIRCLEBALL_MEDIUM);
      // display loading icon in label
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lProfilePicture.setIcon(loadingIcon);
        }
      });
        
      // get profile picture
      FacebookManager facebook = FacebookManager.getInstance();
      
      username = facebook.getUserName();
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
    
  // set profile picture
  new Thread(new Runnable() {

    public void run()
    {
      // profile picture, resized to label
      try
      {
        FacebookManager facebook = FacebookManager.getInstance();
        
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
  
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnPhoto;
    private javax.swing.JButton btnPhotoRedo;
    private javax.swing.JButton btnPublish;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lProfilePicture;
    private javax.swing.JLabel lUserName;
    private javax.swing.JLabel lblAttachMessage;
    private javax.swing.JLabel lblLoading;
    private javax.swing.JLabel lblPhoto;
    private javax.swing.JLabel lblPublish;
    private javax.swing.JLabel lblPublishSuccessStatus;
    private javax.swing.JPanel pnlFoto;
    private javax.swing.JPanel pnlPublish;
    private javax.swing.JSlider slCam;
    private javax.swing.JTextArea txtaPublish;
    // End of variables declaration//GEN-END:variables

    
/*
 * Camera functions
 */
    
private void setupCamera(){
  // disable publish functions
  lblLoading.setVisible(false);
  lblPublishSuccessStatus.setVisible(false);
  btnPhotoRedo.setEnabled(false);
  btnPublish.setEnabled(false);
  txtaPublish.setText("");
  txtaPublish.setEditable(false);
  txtaPublish.setBackground(Color.lightGray);
  
  if(isCameraDetected()){
    lblAttachMessage.setVisible(false); // webcam error message
    
    // start picture taking thread to display live preview
    livecamThread = new Thread(new Runnable() 
    {
        public void run()
        {
          try{
            while(true){
              if(Thread.interrupted()){
                return;
              }
              else{

                ImageIcon picture = takePicture();
                if(picture == null){
                  return;
                }
                displayPicture(picture);

                Thread.sleep(100);

              }
            }
          }catch(Exception ex){
            return;
          }
          
        }
      });
    livecamThread.start();
    
    btnPhoto.setEnabled(true);
  }
  else{
    // disable taking photos
    btnPhoto.setEnabled(false);
    lblAttachMessage.setVisible(true); // webcam error message
    lblPhoto.setIcon(null);
  }
}

private void closeCamera(){
  if(livecamThread != null){
    livecamThread.interrupt(); // stop live stream thread
    livecamThread = null;
  }
    
  if(isCameraDetected()){ // camera is in use
    if(slCam.getValue() == 1){ // webcam
      Webcam webcam = Webcam.getDefault();
      if(webcam.isOpen()){
        webcam.close();
      }
    }
    else{
      //visicam
      // nothing to close, just request no further images
    }
  }
}

/*
 * displays an image in the photo label
 */
private void displayPicture(ImageIcon image){
  
  final ImageIcon picture = image;
  
  SwingUtilities.invokeLater(new Runnable() {
    public void run()
    {
      lblPhoto.setIcon(picture);
    }
  });
  
}

/*
 * uses the attached webcam to take a photo
 */
private ImageIcon takePicture(){
  if(isCameraDetected()){
    
    ImageIcon imageIcon = null;
    if(slCam.getValue() == 1){ // webcam
        // get webcam
        Webcam webcam = Webcam.getDefault();
        webcam.open();

        // take picture
        BufferedImage image = webcam.getImage();
        imageIcon = new ImageIcon(image);

    }
    else{ // visicam
      try{
        URL src = new URL(mainview.getVisiCam());
        imageIcon = new ImageIcon(src);
      }
      catch(Exception e){
        return null;
      } 
    }
    // scale to label
      Image rawImage = imageIcon.getImage();
      Image scaledImage = rawImage.getScaledInstance(
        lblPhoto.getWidth(),
        lblPhoto.getHeight(),
        Image.SCALE_SMOOTH);
      ImageIcon picture = new ImageIcon(scaledImage);
      return picture;
  }
  else{
    return null;
  }
}

/*
 * returns wether a camera is plugged in
 */
private boolean isCameraDetected(){
  if(slCam.getValue() == 1){ // webcam
    Webcam webcam = Webcam.getDefault();
    if (webcam != null) { 
      return true;
    } else {
      return false;
    }
  }
  else{
    // visicam
    return mainview.isVisiCamDetected();
  }
  
}

}
