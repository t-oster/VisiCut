/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.platform;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Tuple<A, B>
{

  private A a = null;
  private B b = null;

  public Tuple()
  {
  }

  public Tuple(A a, B b)
  {
    this.a = a;
    this.b = b;
  }

  public A getA()
  {
    return this.a;
  }

  public B getB()
  {
    return this.b;
  }

  public void setA(A a)
  {
    this.a = a;
  }

  public void setB(B b)
  {
    this.b = b;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof Tuple)
    {
      return !Util.differ(this.a, ((Tuple) o).a) && !Util.differ(this.b, ((Tuple) o).b);
    }
    else if (o == null)
    {
      return false;
    }
    else
    {
      return super.equals(this);
    }
  }

  @Override
  public int hashCode()
  {
    int hash = 5;
    hash = 89 * hash + (this.a != null ? this.a.hashCode() : 0);
    hash = 89 * hash + (this.b != null ? this.b.hashCode() : 0);
    return hash;
  }
  
  @Override
  public String toString()
  {
    return a!= null ? a.toString() : b.toString();
  }
}
