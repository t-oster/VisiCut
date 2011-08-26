package com.t_oster.visicut.gui.mappingdialog;

import java.io.File;
import javax.swing.JRadioButton;

/**
 * This class provides a RadioButton which contains
 * an icon in the label through html support
 * 
 * @author thommy
 */
public class JiconRadioButton extends JRadioButton
{

  private File icon;
  private String text;
  
  public void setLabelIcon(File icon)
  {
    this.icon = icon;
    this.setHtmlText();
  }
  
  public File getLabelIcon()
  {
    return this.icon;
  }
  
  @Override
  public void setText(String text)
  {
    this.text = text;
    setHtmlText();
  }
  
  private void setHtmlText()
  {
    String label = "<html><table cellpadding=0><tr><td>";
    if (icon != null)
    {
      label +="<img src=file://"+icon.getAbsolutePath()+"/> ";
    }
    label+="</td><td width=3><td>";
    if (text != null)
    {
      label+=text;
    }
    label += "</td></tr></table></html>";
    super.setText(label);
  }
}
