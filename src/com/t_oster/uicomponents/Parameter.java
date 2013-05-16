package com.t_oster.uicomponents;

/**
 * A general class for representing Parameters
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Parameter<T>
{
  public T value;
  public T[] possibleValues;
  public T minValue;
  public T maxValue;
  public T steps;
  public String label;
  public T deflt;
}
