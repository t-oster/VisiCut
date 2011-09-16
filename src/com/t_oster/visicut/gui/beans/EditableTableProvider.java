/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.gui.beans;

/**
 * A class needs to implement this interface
 * to provide Data for an EditableTablePanel
 * 
 * @author thommy
 */
public interface EditableTableProvider
{
  public Object getNewInstance();
  /**
   * Edits the Object and returns the new Object
   * if editing shall be saved, null otherwise
   * @param o
   * @return 
   */
  public Object editObject(Object o);
}
