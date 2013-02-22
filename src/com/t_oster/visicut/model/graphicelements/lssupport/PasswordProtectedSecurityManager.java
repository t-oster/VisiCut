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

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.AccessControlException;
import java.security.Permission;

/**
 *
 * @author thommy
 */
public class PasswordProtectedSecurityManager extends SecurityManager
{

  private Object secret;

  public PasswordProtectedSecurityManager(Object pass)
  {
    secret = pass;
  }

  public void disable(Object pass)
  {
    if (pass == secret)
    {
      secret = null;
    }
    else
    {
      throw new AccessControlException("Wrong Password");
    }
  }

  @Override
  public void checkMemberAccess(Class<?> type, int i)
  {
    if (secret == null)
    {
      return;
    }
    super.checkMemberAccess(type, i);
  }

  @Override
  public void checkAccess(Thread thread)
  {
    if (secret == null)
    {
      return;
    }
    super.checkAccess(thread);
  }

  @Override
  public void checkAccess(ThreadGroup tg)
  {
    if (secret == null)
    {
      return;
    }
    super.checkAccess(tg);
  }

  @Override
  public void checkExec(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkExec(string);
  }

  @Override
  public void checkAccept(String string, int i)
  {
    if (secret == null)
    {
      return;
    }
    super.checkAccept(string, i);
  }

  @Override
  public void checkPermission(Permission prmsn)
  {
    if (secret == null)
    {
      return;
    }
    super.checkPermission(prmsn);
  }

  @Override
  public void checkPermission(Permission prmsn, Object o)
  {
    if (secret == null)
    {
      return;
    }
    super.checkPermission(prmsn, o);
  }

  @Override
  public void checkCreateClassLoader()
  {
    if (secret == null)
    {
      return;
    }
    super.checkCreateClassLoader();
  }

  @Override
  public void checkExit(int i)
  {
    if (secret == null)
    {
      return;
    }
    super.checkExit(i);
  }

  @Override
  public void checkLink(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkLink(string);
  }

  @Override
  public void checkRead(FileDescriptor fd)
  {
    if (secret == null)
    {
      return;
    }
    super.checkRead(fd);
  }

  @Override
  public void checkRead(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkRead(string);
  }

  @Override
  public void checkRead(String string, Object o)
  {
    if (secret == null)
    {
      return;
    }
    super.checkRead(string, o);
  }

  @Override
  public void checkWrite(FileDescriptor fd)
  {
    super.checkWrite(fd);
  }

  @Override
  public void checkWrite(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkWrite(string);
  }

  @Override
  public void checkDelete(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkDelete(string);
  }

  @Override
  public void checkConnect(String string, int i)
  {
    if (secret == null)
    {
      return;
    }
    super.checkConnect(string, i);
  }

  @Override
  public void checkConnect(String string, int i, Object o)
  {
    if (secret == null)
    {
      return;
    }
    super.checkConnect(string, i, o);
  }

  @Override
  public void checkListen(int i)
  {
    if (secret == null)
    {
      return;
    }
    super.checkListen(i);
  }

  @Override
  public void checkMulticast(InetAddress ia)
  {
    if (secret == null)
    {
      return;
    }
    super.checkMulticast(ia);
  }

  @Override
  public void checkPropertiesAccess()
  {
    if (secret == null)
    {
      return;
    }
    super.checkPropertiesAccess();
  }

  @Override
  public void checkPropertyAccess(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkPropertyAccess(string);
  }

  @Override
  public boolean checkTopLevelWindow(Object o)
  {
    if (secret == null)
    {
      return true;
    }
    return super.checkTopLevelWindow(o);
  }

  @Override
  public void checkPrintJobAccess()
  {
    if (secret == null)
    {
      return;
    }
    super.checkPrintJobAccess();
  }

  @Override
  public void checkSystemClipboardAccess()
  {
    if (secret == null)
    {
      return;
    }
    super.checkSystemClipboardAccess();
  }

  @Override
  public void checkAwtEventQueueAccess()
  {
    if (secret == null)
    {
      return;
    }
    super.checkAwtEventQueueAccess();
  }

  @Override
  public void checkPackageAccess(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkPackageAccess(string);
  }

  @Override
  public void checkPackageDefinition(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkPackageDefinition(string);
  }

  @Override
  public void checkSetFactory()
  {
    if (secret == null)
    {
      return;
    }
    super.checkSetFactory();
  }

  @Override
  public void checkSecurityAccess(String string)
  {
    if (secret == null)
    {
      return;
    }
    super.checkSecurityAccess(string);
  }
  
  
  
 
  
}
