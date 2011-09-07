/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

/**
 *
 * @author thommy
 */
public class ImportException extends Exception
{
  public ImportException(Throwable cause)
  {
    super(cause);
  }
  public ImportException(String message)
  {
    super(message);
  }
}
