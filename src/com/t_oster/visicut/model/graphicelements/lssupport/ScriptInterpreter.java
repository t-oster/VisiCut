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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import javax.script.ScriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

/**
 * This class provides a JavaScript interpreter which is pretty sandboxed.
 * The only accessible stuff are the methods "move/line/get/set from the provided
 * ScriptInterface object).
 * 
 * The sandboxing code is taken from:
 * http://codeutopia.net/blog/2009/01/02/sandboxing-rhino-in-java/
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ScriptInterpreter
{

  public static class SandboxNativeJavaObject extends NativeJavaObject {
    public SandboxNativeJavaObject(Scriptable scope, Object javaObject, Class staticType) {
      super(scope, javaObject, staticType);
    }

    @Override
    public Object get(String name, Scriptable start) {
      //don't allow the getClass method
      if (name.equals("getClass")) {
        System.err.println("ScriptingSecurity: LaserScript tried to access 'getClass'");
        return NOT_FOUND;
      }
      return super.get(name, start);
    }
  }
  
  public static class SandboxWrapFactory extends WrapFactory {
    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class staticType) {
      return new SandboxNativeJavaObject(scope, javaObject, staticType);
    }
  }
  
  public class SandboxContextFactory extends ContextFactory {
    @Override
    protected Context makeContext() {
      Context cx = super.makeContext();
      cx.setWrapFactory(new SandboxWrapFactory());
      return cx;
    }
  }
  
  public void execute(String script, ScriptInterface si) throws ScriptException, IOException
  {
    this.execute(new StringReader(script), si);
  }

  public void execute(final Reader script, final ScriptInterface si) throws ScriptException, IOException
  {
    if (!ContextFactory.hasExplicitGlobal())
    {
      ContextFactory.initGlobal(new SandboxContextFactory());
    }
    Context cx = ContextFactory.getGlobal().enter();
    try
    {
      cx.setClassShutter(ScriptingSecurity.getInstance());
    }
    catch (SecurityException e)
    {
      //already registered for the current thread....
    }
    // Scriptable represents the script environment
    Scriptable scope = cx.initStandardObjects(null);
    scope.put("_instance", scope, Context.toObject(si, scope));
    ScriptingSecurity.getInstance().setLocked(false);
    cx.evaluateReader(scope, new InputStreamReader(this.getClass().getResourceAsStream("LaserScriptBootstrap.js")), "LaserScriptBootstrap.js", -1, null);
    ScriptingSecurity.getInstance().setLocked(true);
    try
    {
      cx.evaluateReader(scope, script, "laserscript", -1, null);
    }
    catch (Exception e)
    {
      if (e instanceof ScriptException)
      {
        throw (ScriptException) e;
      }
      else
      {
        throw new ScriptException(e);
      }
    }
  }
}
