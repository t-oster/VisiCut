/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.tur0kk;

import javax.swing.ImageIcon;

/**
 * This class hides the loading of resources. In this special case, the loading of ImageIcons with loading symbols. 
 * Find all available resources in com.tur0kk.resources and in the static variables of this class.
 * @author Sven
 */
public class LoadingIcon{
  
  // available loading gif resources which can be requested
  public static final String CIRCLEBALL_SMALL = "circleball_small";
  public static final String CIRCLEBALL_MEDIUM = "circleball_medium";
  
  
  // global function to load an Imageicon with an loading symbol. Use available global resource variables of this class as parameter.
  public static ImageIcon get(String type){
    try
    {
      return new ImageIcon(LoadingIcon.class.getResource("resources/"+type+".gif"));
    }
    catch (Exception ex)
    {
      System.err.println("Error loading "+"resources/"+type+".gif");
      ex.printStackTrace();
    }
    return null;
  }
  
}

