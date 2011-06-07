/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.util;

/**
 *
 * @author thommy
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
}
