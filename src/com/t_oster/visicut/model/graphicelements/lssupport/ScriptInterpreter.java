/**
 * This file is part of VisiCut. Copyright (C) 2011 - 2013 Thomas Oster
 * <thomas.oster@rwth-aachen.de> RWTH Aachen University - 52062 Aachen, Germany
 *
 * VisiCut is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * VisiCut is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VisiCut. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.t_oster.visicut.model.graphicelements.lssupport;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  private AccessControlContext secureContext()
  {
    Permissions perms = new Permissions(); //permissions for scriptengine
    perms.add(new RuntimePermission("accessDeclaredMembers"));
    perms.add(new RuntimePermission("createClassLoader"));
    perms.add(new RuntimePermission("accessClassInPackage.sun.org.mozilla.javascript.internal"));
    perms.add(new RuntimePermission("accessClassInPackage.sun.org.mozilla.javascript"));
    perms.add(new RuntimePermission("accessClassInPackage.org.mozilla.javascript"));

    ProtectionDomain domain = new ProtectionDomain(
      new CodeSource(null, (Certificate[]) null), perms);
    AccessControlContext _accessControlContext = new AccessControlContext(
      new ProtectionDomain[]
      {
        domain
      });
    return _accessControlContext;
  }

  public void execute(final Reader script, final ScriptInterface si) throws ScriptException, AccessControlException
  {
    AccessControlContext acc = secureContext();
    final List<Exception> exceptions = new LinkedList<Exception>();
    AccessController.doPrivileged(new PrivilegedAction()
    {
      @Override
      public Boolean run()
      {
        try
        {
          // create a script engine manager
          ScriptEngineManager factory = new ScriptEngineManager();
          // create JavaScript engine
          ScriptEngine engine = factory.getEngineByName("JavaScript");
          // evaluate JavaScript code from given file - specified by first argument
          engine.put("_instance", si);
          engine.eval(new InputStreamReader(this.getClass().getResourceAsStream("LaserScriptBootstrap.js")));
          SecurityManager bak = System.getSecurityManager();
          Double password = Math.random();
          PasswordProtectedSecurityManager secman = new PasswordProtectedSecurityManager(password);
          System.setSecurityManager(secman);
          try
          {
            engine.eval(script);
          }
          catch (Exception se)
          {
            exceptions.add(se);
          }
          finally
          {
            secman.disable(password);
            System.setSecurityManager(bak);
          }
          return true;
        }
        catch (ScriptException ex)
        {
          exceptions.add(ex);
          return false;
        }
      }
    }, acc);
    for (Exception e: exceptions)
    {
      if (e instanceof ScriptException)
      {
        if (e.getCause() instanceof sun.org.mozilla.javascript.WrappedException && e.getCause().getCause() instanceof AccessControlException)
        {
          throw (AccessControlException) e.getCause().getCause();
        }
        throw (ScriptException) e;
      }
      else if (e instanceof AccessControlException)
      {
        throw (AccessControlException) e;
      }
      else
      {
        throw new RuntimeException(e);
      }
    }
  }
}
