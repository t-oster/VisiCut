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

package com.t_oster.visicut.model.graphicelements;

import com.t_oster.visicut.model.PlfPart;
import java.io.File;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public abstract class AbstractImporter implements Importer
{

  public PlfPart importFile(File inputFile, List<String> warnings) throws ImportException
  {
    PlfPart p = new PlfPart();
    p.setSourceFile(inputFile);
    p.setGraphicObjects(this.importSetFromFile(inputFile, warnings));
    return p;
  }
  
  public abstract GraphicSet importSetFromFile(File inputFile, List<String> warnings) throws ImportException;

}
