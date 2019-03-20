/**
 * MicroEmulator Copyright (C) 2002 Bartek Teodorczyk <barteo@barteo.net>
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
 */
package org.microemu.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import org.microemu.app.util.DeviceEntry;
import org.microemu.app.util.IOUtils;
import org.microemu.app.util.MIDletSystemProperties;
import org.microemu.app.util.MRUList;
import org.microemu.app.util.MidletURLReference;
import org.microemu.device.EmulatorContext;
import org.microemu.device.impl.DeviceImpl;
import org.microemu.device.impl.Rectangle;
import org.microemu.log.Logger;
import org.microemu.microedition.ImplementationInitialization;
import nanoxml.XMLElement;
import nanoxml.XMLParseException;

public class Config {

  private static File meHome;
  /**
   * emulatorID used for multiple instance of MicroEmulator, now redefine home
   */
  private static String emulatorID;
  private static XMLElement configXml = new XMLElement();
  private static DeviceEntry defaultDevice;
  private static DeviceEntry resizableDevice;
  private static EmulatorContext emulatorContext;
  private static MRUList urlsMRU = new MRUList(MidletURLReference.class, "midlet");

  private static File initMEHomePath() {
    try {
      final File meHome = new File(System.getProperty("user.home") + "/.microemulator/");
      if (Config.emulatorID != null) {
        return new File(meHome, Config.emulatorID);
      }
      else {
        return meHome;
      }
    }
    catch (final SecurityException e) {
      Logger.error("Cannot access user.home", e);
      return null;
    }
  }

  public static void loadConfig(final DeviceEntry defaultDevice, final EmulatorContext emulatorContext) {
    Config.defaultDevice = defaultDevice;
    Config.emulatorContext = emulatorContext;
    File configFile = new File(Config.getConfigPath(), "config2.xml");
    try {
      if (configFile.exists()) {
        Config.loadConfigFile("config2.xml");
      }
      else {
        configFile = new File(Config.getConfigPath(), "config.xml");
        if (configFile.exists()) {
          // migrate from config.xml
          Config.loadConfigFile("config.xml");
          for (final Enumeration e = Config.getDeviceEntries().elements(); e.hasMoreElements();) {
            final DeviceEntry entry = (DeviceEntry)e.nextElement();
            if (!entry.canRemove()) {
              continue;
            }
            Config.removeDeviceEntry(entry);
            final File src = new File(Config.getConfigPath(), entry.getFileName());
            final File dst = File.createTempFile("dev", ".jar", Config.getConfigPath());
            IOUtils.copyFile(src, dst);
            entry.setFileName(dst.getName());
            Config.addDeviceEntry(entry);
          }
        }
        else {
          Config.createDefaultConfigXml();
        }
        Config.saveConfig();
      }
    }
    catch (final IOException ex) {
      Logger.error(ex);
      Config.createDefaultConfigXml();
    }
    finally {
      // Happens in webstart untrusted environment
      if (Config.configXml == null) {
        Config.createDefaultConfigXml();
      }
    }
    Config.urlsMRU.read(Config.configXml.getChildOrNew("files").getChildOrNew("recent"));
    Config.initSystemProperties();
  }

  private static void loadConfigFile(final String configFileName) throws IOException {
    final File configFile = new File(Config.getConfigPath(), configFileName);
    InputStream is = null;
    String xml = "";
    try {
      final InputStream dis = new BufferedInputStream(is = new FileInputStream(configFile));
      while (dis.available() > 0) {
        final byte[] b = new byte[dis.available()];
        dis.read(b);
        xml += new String(b);
      }
      Config.configXml = new XMLElement();
      Config.configXml.parseString(xml);
    }
    catch (final XMLParseException e) {
      Logger.error(e);
      Config.createDefaultConfigXml();
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  private static void createDefaultConfigXml() {
    Config.configXml = new XMLElement();
    Config.configXml.setName("config");
  }

  public static void saveConfig() {

    Config.urlsMRU.save(Config.configXml.getChildOrNew("files").getChildOrNew("recent"));

    final File configFile = new File(Config.getConfigPath(), "config2.xml");

    Config.getConfigPath().mkdirs();
    FileWriter fw = null;
    try {
      fw = new FileWriter(configFile);
      Config.configXml.write(fw);
      fw.close();
    }
    catch (final IOException ex) {
      Logger.error(ex);
    }
    finally {
      IOUtils.closeQuietly(fw);
    }
  }

  static Map getExtensions() {
    final Map extensions = new HashMap();
    final XMLElement extensionsXml = Config.configXml.getChild("extensions");
    if (extensionsXml == null) { return extensions; }
    for (final Enumeration en = extensionsXml.enumerateChildren(); en.hasMoreElements();) {
      final XMLElement extension = (XMLElement)en.nextElement();
      if (!extension.getName().equals("extension")) {
        continue;
      }
      final String className = extension.getChildString("className", null);
      if (className == null) {
        continue;
      }

      final Map parameters = new HashMap();
      parameters.put(ImplementationInitialization.PARAM_EMULATOR_ID, Config.getEmulatorID());

      for (final Enumeration een = extension.enumerateChildren(); een.hasMoreElements();) {
        final XMLElement propXml = (XMLElement)een.nextElement();
        if (propXml.getName().equals("properties")) {
          for (final Enumeration e_prop = propXml.enumerateChildren(); e_prop.hasMoreElements();) {
            final XMLElement tmp_prop = (XMLElement)e_prop.nextElement();
            if (tmp_prop.getName().equals("property")) {
              parameters.put(tmp_prop.getStringAttribute("name"), tmp_prop.getStringAttribute("value"));
            }
          }
        }
      }

      extensions.put(className, parameters);
    }
    return extensions;
  }

  private static void initSystemProperties() {
    Map systemProperties = null;

    for (final Enumeration e = Config.configXml.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement tmp = (XMLElement)e.nextElement();
      if (tmp.getName().equals("system-properties")) {
        // Permits null values.
        systemProperties = new HashMap();
        for (final Enumeration e_prop = tmp.enumerateChildren(); e_prop.hasMoreElements();) {
          final XMLElement tmp_prop = (XMLElement)e_prop.nextElement();
          if (tmp_prop.getName().equals("system-property")) {
            systemProperties.put(tmp_prop.getStringAttribute("name"), tmp_prop.getStringAttribute("value"));
          }
        }
      }
    }

    // No <system-properties> in config2.xml
    if (systemProperties == null) {
      systemProperties = new Properties();
      // Ask avetana to ignore MIDP profiles and load JSR-82
      // implementation dll or so
      systemProperties.put("avetana.forceNativeLibrary", Boolean.TRUE.toString());

      final XMLElement propertiesXml = Config.configXml.getChildOrNew("system-properties");

      for (final Iterator i = systemProperties.entrySet().iterator(); i.hasNext();) {
        final Map.Entry e = (Map.Entry)i.next();
        final XMLElement xmlProperty = propertiesXml.addChild("system-property");
        xmlProperty.setAttribute("value", e.getValue());
        xmlProperty.setAttribute("name", e.getKey());
      }

      Config.saveConfig();
    }

    MIDletSystemProperties.setProperties(systemProperties);
  }

  public static File getConfigPath() {
    if (Config.meHome == null) {
      Config.meHome = Config.initMEHomePath();
    }
    return Config.meHome;
  }

  public static Vector getDeviceEntries() {
    final Vector result = new Vector();

    if (Config.defaultDevice == null) {
      Config.defaultDevice = new DeviceEntry("Default device", null, DeviceImpl.DEFAULT_LOCATION, true, false);
    }
    Config.defaultDevice.setDefaultDevice(true);
    result.add(Config.defaultDevice);

    if (Config.resizableDevice == null) {
      Config.resizableDevice = new DeviceEntry("Resizable device", null, DeviceImpl.RESIZABLE_LOCATION, false, false);
      Config.addDeviceEntry(Config.resizableDevice);
    }

    final XMLElement devicesXml = Config.configXml.getChild("devices");
    if (devicesXml == null) { return result; }

    for (final Enumeration e_device = devicesXml.enumerateChildren(); e_device.hasMoreElements();) {
      final XMLElement tmp_device = (XMLElement)e_device.nextElement();
      if (tmp_device.getName().equals("device")) {
        boolean devDefault = false;
        if ((tmp_device.getStringAttribute("default") != null)
            && tmp_device.getStringAttribute("default").equals("true")) {
          devDefault = true;
          Config.defaultDevice.setDefaultDevice(false);
        }
        final String devName = tmp_device.getChildString("name", null);
        final String devFile = tmp_device.getChildString("filename", null);
        final String devClass = tmp_device.getChildString("class", null);
        final String devDescriptor = tmp_device.getChildString("descriptor", null);

        if (devDescriptor == null) {
          result.add(new DeviceEntry(devName, devFile, devDefault, devClass, Config.emulatorContext));
        }
        else {
          result.add(new DeviceEntry(devName, devFile, devDescriptor, devDefault));
        }
      }
    }

    return result;
  }

  public static void addDeviceEntry(final DeviceEntry entry) {
    for (final Enumeration en = Config.getDeviceEntries().elements(); en.hasMoreElements();) {
      final DeviceEntry test = (DeviceEntry)en.nextElement();
      if (test.getDescriptorLocation().equals(entry.getDescriptorLocation())) { return; }
    }

    final XMLElement devicesXml = Config.configXml.getChildOrNew("devices");

    final XMLElement deviceXml = devicesXml.addChild("device");
    if (entry.isDefaultDevice()) {
      deviceXml.setAttribute("default", "true");
    }
    deviceXml.addChild("name", entry.getName());
    deviceXml.addChild("filename", entry.getFileName());
    deviceXml.addChild("descriptor", entry.getDescriptorLocation());

    Config.saveConfig();
  }

  public static void removeDeviceEntry(final DeviceEntry entry) {
    final XMLElement devicesXml = Config.configXml.getChild("devices");
    if (devicesXml == null) { return; }

    for (final Enumeration e_device = devicesXml.enumerateChildren(); e_device.hasMoreElements();) {
      final XMLElement tmp_device = (XMLElement)e_device.nextElement();
      if (tmp_device.getName().equals("device")) {
        final String testDescriptor = tmp_device.getChildString("descriptor", null);
        // this is needed by migration config.xml -> config2.xml
        if (testDescriptor == null) {
          devicesXml.removeChild(tmp_device);

          Config.saveConfig();
          continue;
        }
        if (testDescriptor.equals(entry.getDescriptorLocation())) {
          devicesXml.removeChild(tmp_device);

          Config.saveConfig();
          break;
        }
      }
    }
  }

  public static void changeDeviceEntry(final DeviceEntry entry) {
    final XMLElement devicesXml = Config.configXml.getChild("devices");
    if (devicesXml == null) { return; }

    for (final Enumeration e_device = devicesXml.enumerateChildren(); e_device.hasMoreElements();) {
      final XMLElement tmp_device = (XMLElement)e_device.nextElement();
      if (tmp_device.getName().equals("device")) {
        final String testDescriptor = tmp_device.getChildString("descriptor", null);
        if (testDescriptor.equals(entry.getDescriptorLocation())) {
          if (entry.isDefaultDevice()) {
            tmp_device.setAttribute("default", "true");
          }
          else {
            tmp_device.removeAttribute("default");
          }
          Config.saveConfig();
          break;
        }
      }
    }
  }

  public static Rectangle getDeviceEntryDisplaySize(final DeviceEntry entry) {
    final XMLElement devicesXml = Config.configXml.getChild("devices");
    if (devicesXml != null) {
      for (final Enumeration e_device = devicesXml.enumerateChildren(); e_device.hasMoreElements();) {
        final XMLElement tmp_device = (XMLElement)e_device.nextElement();
        if (tmp_device.getName().equals("device")) {
          final String testDescriptor = tmp_device.getChildString("descriptor", null);
          if (testDescriptor.equals(entry.getDescriptorLocation())) {
            final XMLElement rectangleXml = tmp_device.getChild("rectangle");
            if (rectangleXml != null) {
              final Rectangle result = new Rectangle();
              result.x = rectangleXml.getChildInteger("x", -1);
              result.y = rectangleXml.getChildInteger("y", -1);
              result.width = rectangleXml.getChildInteger("width", -1);
              result.height = rectangleXml.getChildInteger("height", -1);
              return result;
            }
          }
        }
      }
    }
    return null;
  }

  public static void setDeviceEntryDisplaySize(final DeviceEntry entry, final Rectangle rect) {
    if (entry == null) { return; }
    final XMLElement devicesXml = Config.configXml.getChild("devices");
    if (devicesXml == null) { return; }
    for (final Enumeration e_device = devicesXml.enumerateChildren(); e_device.hasMoreElements();) {
      final XMLElement tmp_device = (XMLElement)e_device.nextElement();
      if (tmp_device.getName().equals("device")) {
        final String testDescriptor = tmp_device.getChildString("descriptor", null);
        if (testDescriptor.equals(entry.getDescriptorLocation())) {
          final XMLElement mainXml = tmp_device.getChildOrNew("rectangle");
          XMLElement xml = mainXml.getChildOrNew("x");
          xml.setContent(String.valueOf(rect.x));
          xml = mainXml.getChildOrNew("y");
          xml.setContent(String.valueOf(rect.y));
          xml = mainXml.getChildOrNew("width");
          xml.setContent(String.valueOf(rect.width));
          xml = mainXml.getChildOrNew("height");
          xml.setContent(String.valueOf(rect.height));
          Config.saveConfig();
          break;
        }
      }
    }
  }

  public static String getRecordStoreManagerClassName() {
    final XMLElement recordStoreManagerXml = Config.configXml.getChild("recordStoreManager");
    if (recordStoreManagerXml == null) { return null; }
    return recordStoreManagerXml.getStringAttribute("class");
  }

  public static void setRecordStoreManagerClassName(final String className) {
    final XMLElement recordStoreManagerXml = Config.configXml.getChildOrNew("recordStoreManager");
    recordStoreManagerXml.setAttribute("class", className);
    Config.saveConfig();
  }

  public static boolean isLogConsoleLocationEnabled() {
    final XMLElement logConsoleXml = Config.configXml.getChild("logConsole");
    if (logConsoleXml == null) { return true; }

    return logConsoleXml.getBooleanAttribute("locationEnabled", true);
  }

  public static void setLogConsoleLocationEnabled(final boolean state) {
    final XMLElement logConsoleXml = Config.configXml.getChildOrNew("logConsole");
    if (state) {
      logConsoleXml.setAttribute("locationEnabled", "true");
    }
    else {
      logConsoleXml.setAttribute("locationEnabled", "false");
    }
    Config.saveConfig();
  }

  public static boolean isWindowOnStart(final String name) {
    final XMLElement windowsXml = Config.configXml.getChild("windows");
    if (windowsXml == null) { return false; }
    final XMLElement mainXml = windowsXml.getChild(name);
    if (mainXml == null) { return false; }
    final String attr = mainXml.getStringAttribute("onstart", "false");
    if (attr.trim().toLowerCase().equals("true")) {
      return true;
    }
    else {
      return false;
    }
  }

  public static Rectangle getWindow(final String name, final Rectangle defaultWindow) {
    final XMLElement windowsXml = Config.configXml.getChild("windows");
    if (windowsXml == null) { return defaultWindow; }
    final XMLElement mainXml = windowsXml.getChild(name);
    if (mainXml == null) { return defaultWindow; }
    final Rectangle window = new Rectangle();
    window.x = mainXml.getChildInteger("x", defaultWindow.x);
    window.y = mainXml.getChildInteger("y", defaultWindow.y);
    window.width = mainXml.getChildInteger("width", defaultWindow.width);
    window.height = mainXml.getChildInteger("height", defaultWindow.height);
    return window;
  }

  public static void setWindow(final String name, final Rectangle window, final boolean onStart) {
    final XMLElement windowsXml = Config.configXml.getChildOrNew("windows");
    final XMLElement mainXml = windowsXml.getChildOrNew(name);
    if (onStart) {
      mainXml.setAttribute("onstart", "true");
    }
    else {
      mainXml.removeAttribute("onstart");
    }
    XMLElement xml = mainXml.getChildOrNew("x");
    xml.setContent(String.valueOf(window.x));
    xml = mainXml.getChildOrNew("y");
    xml.setContent(String.valueOf(window.y));
    xml = mainXml.getChildOrNew("width");
    xml.setContent(String.valueOf(window.width));
    xml = mainXml.getChildOrNew("height");
    xml.setContent(String.valueOf(window.height));
    Config.saveConfig();
  }

  public static String getRecentDirectory(final String key) {
    final String defaultResult = ".";
    final XMLElement filesXml = Config.configXml.getChild("files");
    if (filesXml == null) { return defaultResult; }
    return filesXml.getChildString(key, defaultResult);
  }

  public static void setRecentDirectory(final String key, final String recentJadDirectory) {
    final XMLElement filesXml = Config.configXml.getChildOrNew("files");
    final XMLElement recentJadDirectoryXml = filesXml.getChildOrNew(key);
    recentJadDirectoryXml.setContent(recentJadDirectory);
    Config.saveConfig();
  }

  public static MRUList getUrlsMRU() {
    return Config.urlsMRU;
  }

  public static String getEmulatorID() {
    return Config.emulatorID;
  }

  public static void setEmulatorID(final String emulatorID) {
    Config.emulatorID = emulatorID;
  }

}
