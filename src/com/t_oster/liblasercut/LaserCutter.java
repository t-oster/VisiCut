/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import java.util.List;
import java.util.LinkedList;

/**
 *
 * @author thommy
 */
public abstract class LaserCutter
{

  /**
   * Performs sanity checks on the LaserJob and sends it to the Cutter
   * @param job
   * @throws IllegalJobException if the Job didn't pass the SanityCheck
   * @throws Exception  if there is a Problem with the Communication or Queue
   */
  public abstract void sendJob(LaserJob job) throws IllegalJobException, Exception;

  /**
   * Returns the available Resolutions in DPI
   * @return 
   */
  public abstract List<Integer> getResolutions();

  /**
   * Returns the Maximum width of a LaserJob in mm
   * @return 
   */
  public abstract double getBedWidth();

  /**
   * Returns the Maximum height of a LaserJob in mm
   * @return 
   */
  public abstract double getBedHeight();

  /*
   * Finnpappe 3mm ?? ?? 100 50 Auto  
  Birkensperrholz 4mm (toom) ?? ?? 100 65 Auto  
  Alu Profil (eloxiert) 50 50 NA NA NA  
  Dicke Wellpappe ?? ?? 100 50 ??  
  Expo Giveaway Pappe 100 12 100 12 500  
  schwarze Klebefolie (auf 3D-Drucker) ?? ?? 100 30 300  
  Flexfolie (T-Shirt druck) ?? ?? 30 5 5000  
  Flockfolie (T-Shirt druck) ?? ?? 50 5 5000  
  Holzklötze Friedenstisch 100 20 25-30 80 500  
  Kunstleder (Sarahs Kalender) 100 30 ?? ?? ??  
  MDF 6,3mm ?? ?? 21 100 2058  
  Papier 100 4-5 100 10 500-1000  
  Laseracryl (Nametags) 100 26 100 50 5000  
  Laseracryl (>3mm) 100 2*50 50 100 5000  
  Latex (0.35mm)  -  -  13  2  30  
  Rubber  ?? ?? 15  10  100  
  Auge (bis -1.5 Dioptrin) 100 3-4 15 10 230  
  Moosgummi PU (Polyuretan) 2mm  ?? ?? 30 5 5000  
  PS Polystyrol (Mon Cherie, Rocher, etc.) 1mm ?? ?? 100 45 500  
  
   */
  public List<MaterialProperty> getMaterialPropertys()
  {
    //TODO: get Propertys for actual Cutter from a subdirectory or similar
    int AUTO = 5000;
    List<MaterialProperty> result = new LinkedList<MaterialProperty>();
    result.add(new MaterialProperty("Finnpappe", 3, -1, -1, 50, 100, AUTO));
    result.add(new MaterialProperty("Birkensperrholz", 4, -1, -1, 65, 100, AUTO));
    result.add(new MaterialProperty("Dicke Wellpappe", 5, -1, -1, 50, 100, AUTO));
    result.add(new MaterialProperty("Expo Giveaway Pappe", 3, -1, -1, 12, 100, 500));

    result.add(new MaterialProperty("Papier", 0.2, -1, -1, 10, 100, 1000));

    result.add(new MaterialProperty("Bastelpappe", 3, -1, -1, 90, 100, 5000));
    result.add(new MaterialProperty("Plexiglass XT (blau)", 6, -1, -1, 85, 20, 5000));
    //TODO rest of list
    return result;
  }
}
