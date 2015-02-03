/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * AutoArrangePanel.java
 *
 * Created on Feb 1, 2015, 9:08:43 PM
 */
package com.t_oster.visicut.gui;

import com.mcp14.Autoarrange.AutoArrange;
import com.t_oster.visicut.gui.beans.PreviewPanel;
import java.awt.geom.NoninvertibleTransformException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.jdesktop.application.Action;

/**
 *
 * @author sughoshkumar
 */
public class AutoArrangePanel extends javax.swing.JPanel
{
  private int count;
  private static int totalNumberOfArrangements;
  public JFrame parentFrame;
  private int sliderValue;
  

  /** Creates new form AutoArrangePanel */
  public AutoArrangePanel(JFrame parent)
  {
    parentFrame = parent;
    count = 1;
    initComponents();
    totalNumberOfArrangements = AutoArrange.allValues.size();
    if(totalNumberOfArrangements > 1){
      
    }
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        distanceSlider = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        confirmButton = new javax.swing.JButton();
        discardButton = new javax.swing.JButton();

        setName("Form"); // NOI18N

        distanceSlider.setMajorTickSpacing(1);
        distanceSlider.setMaximum(10);
        distanceSlider.setPaintLabels(true);
        distanceSlider.setPaintTicks(true);
        distanceSlider.setSnapToTicks(true);
        distanceSlider.setValue(0);
        distanceSlider.setName("distanceSlider"); // NOI18N
        distanceSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                distanceSliderMouseReleased(evt);
            }
        });
        distanceSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                distanceSliderStateChanged(evt);
            }
        });

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(AutoArrangePanel.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getActionMap(AutoArrangePanel.class, this);
        confirmButton.setAction(actionMap.get("confirm")); // NOI18N
        confirmButton.setText(resourceMap.getString("confirmButton.text")); // NOI18N
        confirmButton.setName("confirmButton"); // NOI18N

        discardButton.setAction(actionMap.get("discard")); // NOI18N
        discardButton.setText(resourceMap.getString("discardButton.text")); // NOI18N
        discardButton.setName("discardButton"); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                            .add(discardButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(confirmButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(org.jdesktop.layout.GroupLayout.LEADING, distanceSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 356, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(9, 9, 9)
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(distanceSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(discardButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 34, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(confirmButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 34, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

  private void distanceSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_distanceSliderStateChanged
    // TODO add your handling code here:
    sliderValue = distanceSlider.getValue();
  }//GEN-LAST:event_distanceSliderStateChanged

  private void distanceSliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_distanceSliderMouseReleased
    // TODO add your handling code here:
    try
    {
      // TODO add your handling code here:
      MainView.previewStaticPanel.autoArrange(distanceSlider.getValue());
      totalNumberOfArrangements = AutoArrange.allValues.size();
      count = 1;
    }
    catch (FileNotFoundException ex)
    {
      Logger.getLogger(AutoArrangePanel.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (UnsupportedEncodingException ex)
    {
      Logger.getLogger(AutoArrangePanel.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (NoninvertibleTransformException ex)
    {
      Logger.getLogger(AutoArrangePanel.class.getName()).log(Level.SEVERE, null, ex);
    }
    
  }//GEN-LAST:event_distanceSliderMouseReleased

  
  // to set the step count value

  @Action
  public void discard()
  {
    try
    {
      MainView.previewStaticPanel.autoArrange(0);
      parentFrame.dispose();
      MainView.arrangeFrame = null;
      MainView.undoArrange();
    }
    catch (FileNotFoundException ex)
    {
      Logger.getLogger(AutoArrangePanel.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (UnsupportedEncodingException ex)
    {
      Logger.getLogger(AutoArrangePanel.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (NoninvertibleTransformException ex)
    {
      Logger.getLogger(AutoArrangePanel.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Action
  public void confirm()
  {
    parentFrame.dispose();
    MainView.arrangeFrame = null;
  }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton confirmButton;
    private javax.swing.JButton discardButton;
    private javax.swing.JSlider distanceSlider;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
