/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author thommy
 */
public interface Importer
{
  
  FileFilter getFileFilter();
  GraphicSet importFile(File inputFile) throws ImportException;
  
}
