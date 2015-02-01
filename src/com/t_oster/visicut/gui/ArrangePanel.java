/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Marcel
 */
public class ArrangePanel extends JPanel
{
  static int arrangementCount;
  static JLabel arrangementsFound;
  static JLabel countLabel;
  
  ArrangePanel(){
    this.setLocation(0,0);
    this.setSize(100,100);
    this.setLayout(new BorderLayout());
    
    arrangementCount = 0;
    
    arrangementsFound  = new JLabel("0 arrangements found");
    this.add(arrangementsFound, BorderLayout.PAGE_START);
    
    PreviousButton prevButton = new PreviousButton();
    NextButton nextButton = new NextButton();
    
    this.add(prevButton, BorderLayout.LINE_START);
    this.add(nextButton, BorderLayout.LINE_END);
    
    countLabel = new JLabel("0/0");
  }
  
  public static void setArrangementCount(int count){
    arrangementCount = count;
    arrangementsFound.setText("" + arrangementCount + " arrangements found");
    countLabel.setText("1/" + arrangementCount );
  }
  
  static class PreviousButton extends JButton{
    
    PreviousButton(){
      super("prev");
    }
    
  }
  
  static class NextButton extends JButton{
    
    NextButton(){
      super("next");
    }
    
  }
  
  static class DiscardButton extends JButton{
    
    DiscardButton(){
      super("discard");
    }
    
  }
  
  static class ConfirmButton extends JButton{
    
    ConfirmButton(){
      super("confirm");
    }
    
  }
}

