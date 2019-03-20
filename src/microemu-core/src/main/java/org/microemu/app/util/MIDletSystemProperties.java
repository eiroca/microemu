/**
 * MicroEmulator Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net> Copyright (C)
 * 2006-2007 Vlad Skarzhevskyy
 *
 * It is licensed under the following two licenses as alternatives: 1. GNU Lesser General Public
 * License (the "LGPL") version 2.1 or any newer version 2. Apache License (the "AL") Version 2.0
 *
 * You may not use this file except in compliance with at least one of the above two licenses.
 *
 * You may obtain a copy of the LGPL at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 * You may obtain a copy of the AL at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 *
 * @version $Id$
 */
package org.microemu.app.util;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.microemu.device.Device;
import org.microemu.log.Logger;

/**
 * @author vlads
 *
 *         This class is called by MIDlet to access System Property. Call injection is made by
 *         MIDlet ClassLoaded
 *
 */
public class MIDletSystemProperties {

  /**
   * This may be a configuration option. But not for applet and Web Start.
   */
  public static boolean applyToJavaSystemProperties = true;

  /**
   * Permits null values.
   */
  private static final Map props = new HashMap();

  private static final Map permissions = new HashMap();

  private static Map systemPropertiesPreserve;

  private static List systemPropertiesDevice;

  private static boolean wanrOnce = true;

  private static boolean initialized = false;

  /* The context to be used when starting MicroEmulator */
  private static AccessControlContext acc;

  private static void initOnce() {
    // Can't use static initializer because of applyToJavaSystemProperties
    // in applet
    if (MIDletSystemProperties.initialized) { return; }
    MIDletSystemProperties.initialized = true;
    MIDletSystemProperties.setProperty("microedition.configuration", "CLDC-1.1");
    MIDletSystemProperties.setProperty("microedition.profiles", "MIDP-2.0");
    MIDletSystemProperties.setProperty("microedition.platform", "MicroEmulator");
    MIDletSystemProperties.setProperty("microedition.encoding", MIDletSystemProperties.getSystemProperty("file.encoding"));
  }

  /**
   * Allow Access to system properties from MIDlet
   */
  public static void initContext() {
    MIDletSystemProperties.acc = AccessController.getContext();
  }

  /**
   * Gets the system property indicated by the specified key. The only function called by MIDlet
   * 
   * @param key the name of the system property
   * @return
   */
  public static String getProperty(final String key) {
    MIDletSystemProperties.initOnce();
    if (MIDletSystemProperties.props.containsKey(key)) { return (String)MIDletSystemProperties.props.get(key); }
    final String v = MIDletSystemProperties.getDynamicProperty(key);
    if (v != null) { return v; }
    try {
      return MIDletSystemProperties.getSystemProperty(key);
    }
    catch (final SecurityException e) {
      return null;
    }
  }

  public static String getSystemProperty(final String key) {
    try {
      if (MIDletSystemProperties.acc != null) {
        return MIDletSystemProperties.getSystemPropertySecure(key);
      }
      else {
        return System.getProperty(key);
      }
    }
    catch (final SecurityException e) {
      return null;
    }
  }

  private static String getSystemPropertySecure(final String key) {
    try {
      return (String)AccessController.doPrivileged((PrivilegedExceptionAction)() -> System.getProperty(key), MIDletSystemProperties.acc);
    }
    catch (final Throwable e) {
      return null;
    }
  }

  private static String getDynamicProperty(final String key) {
    if (key.equals("microedition.locale")) { return Locale.getDefault().getLanguage(); }
    return null;
  }

  public static Set getPropertiesSet() {
    MIDletSystemProperties.initOnce();
    return MIDletSystemProperties.props.entrySet();
  }

  public static String setProperty(final String key, final String value) {
    MIDletSystemProperties.initOnce();
    if (MIDletSystemProperties.applyToJavaSystemProperties) {
      try {
        if (value == null) {
          System.getProperties().remove(key);
        }
        else {
          System.setProperty(key, value);
        }
      }
      catch (final SecurityException e) {
        if (MIDletSystemProperties.wanrOnce) {
          MIDletSystemProperties.wanrOnce = false;
          Logger.error("Cannot update Java System.Properties", e);
          Logger.debug("Continue ME2 operations with no updates to system Properties");
        }
      }
    }
    return (String)MIDletSystemProperties.props.put(key, value);
  }

  public static String clearProperty(final String key) {
    if (MIDletSystemProperties.applyToJavaSystemProperties) {
      try {
        System.getProperties().remove(key);
      }
      catch (final SecurityException e) {
        if (MIDletSystemProperties.wanrOnce) {
          MIDletSystemProperties.wanrOnce = false;
          Logger.error("Cannot update Java System.Properties", e);
        }
      }
    }
    return (String)MIDletSystemProperties.props.remove(key);
  }

  public static void setProperties(final Map properties) {
    MIDletSystemProperties.initOnce();
    for (final Iterator i = properties.entrySet().iterator(); i.hasNext();) {
      final Map.Entry e = (Map.Entry)i.next();
      MIDletSystemProperties.setProperty((String)e.getKey(), (String)e.getValue());
    }
  }

  public static int getPermission(final String permission) {
    final Integer value = (Integer)MIDletSystemProperties.permissions.get(permission);
    if (value == null) {
      return -1;
    }
    else {
      return value.intValue();
    }
  }

  public static void setPermission(final String permission, final int value) {
    MIDletSystemProperties.permissions.put(permission, new Integer(value));
  }

  public static void setDevice(final Device newDevice) {
    MIDletSystemProperties.initOnce();
    // Restore System Properties from previous device activation.
    if (MIDletSystemProperties.systemPropertiesDevice != null) {
      for (final Iterator iter = MIDletSystemProperties.systemPropertiesDevice.iterator(); iter.hasNext();) {
        MIDletSystemProperties.clearProperty((String)iter.next());
      }
    }
    if (MIDletSystemProperties.systemPropertiesPreserve != null) {
      for (final Iterator i = MIDletSystemProperties.systemPropertiesPreserve.entrySet().iterator(); i.hasNext();) {
        final Map.Entry e = (Map.Entry)i.next();
        MIDletSystemProperties.setProperty((String)e.getKey(), (String)e.getValue());
      }
    }
    MIDletSystemProperties.systemPropertiesDevice = new Vector();
    MIDletSystemProperties.systemPropertiesPreserve = new HashMap();
    for (final Iterator i = newDevice.getSystemProperties().entrySet().iterator(); i.hasNext();) {
      final Map.Entry e = (Map.Entry)i.next();
      final String key = (String)e.getKey();
      if (MIDletSystemProperties.props.containsKey(key)) {
        MIDletSystemProperties.systemPropertiesPreserve.put(key, MIDletSystemProperties.props.get(key));
      }
      else {
        MIDletSystemProperties.systemPropertiesDevice.add(key);
      }
      MIDletSystemProperties.setProperty(key, (String)e.getValue());
    }
  }
}
