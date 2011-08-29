/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import java.io.File;

/**
 *
 * @author thommy
 */
public interface Importer
{

  GraphicSet importFile(File inputFile) throws ImportException;
  
}
