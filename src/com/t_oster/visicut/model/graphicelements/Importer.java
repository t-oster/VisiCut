/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import java.io.File;
import java.util.List;

/**
 * The Importer class Takes an InputFile and returns
 * a List of GraphicObjects
 * 
 * @author thommy
 */
public interface Importer
{
  public List<GraphicObject> importFile(File inputFile) throws ImportException;
}
