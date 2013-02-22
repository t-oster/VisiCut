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
package com.t_oster.visicut.model.graphicelements.lssupport;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ScriptInterpreter
{
  public void execute(String script, ScriptInterface si) throws ScriptException
  {
     this.execute(new StringReader(script), si);
  }
  
  public void execute(Reader script, ScriptInterface si) throws ScriptException
  {
    // create a script engine manager
    ScriptEngineManager factory = new ScriptEngineManager();
    // create JavaScript engine
    ScriptEngine engine = factory.getEngineByName("JavaScript");
    // evaluate JavaScript code from given file - specified by first argument
    engine.put("_instance", si);
    engine.eval(new InputStreamReader(this.getClass().getResourceAsStream("LaserScriptBootstrap.js")));
    engine.eval(script);
  }
}
