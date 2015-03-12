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
package com.tur0kk.thingiverse;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Adapted from https://github.com/frankkienl/FrankkieNL_ThingiverseLib
 * 
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
