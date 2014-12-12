/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.thingiverse;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * This annotation is useful when using reflection, to see the parameter-names
 * @author frankkie
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ThingMethod {

  String[] params();
}
class ThingMethodImpl implements ThingMethod {

  @Override
  public String[] params() {
    return new String[]{""};
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return ThingMethod.class;
  }
}
