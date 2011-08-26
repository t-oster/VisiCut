/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.ImportException;
import java.io.File;
import java.util.List;

/**
 *
 * @author thommy
 */
public interface Importer
{

  List<GraphicObject> importFile(File inputFile) throws ImportException;
  
}
