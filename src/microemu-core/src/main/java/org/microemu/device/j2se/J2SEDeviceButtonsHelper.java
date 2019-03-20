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
package org.microemu.device.j2se;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.impl.ButtonName;
import org.microemu.device.impl.SoftButton;
import org.microemu.log.Logger;

/**
 * Maps keyboard and mouse events to Buttons
 *
 * @author vlads
 *
 */
public class J2SEDeviceButtonsHelper {

  private static Map devices = new WeakHashMap();

  private static class DeviceInformation {

    Map keyboardKeyCodes = new HashMap();

    Map keyboardCharCodes = new HashMap();

    Map functions = new HashMap();
  }

  public static SoftButton getSoftButton(final MouseEvent ev) {
    final Iterator it = DeviceFactory.getDevice().getSoftButtons().iterator();
    while (it.hasNext()) {
      final SoftButton button = (SoftButton)it.next();
      if (button.isVisible()) {
        final org.microemu.device.impl.Rectangle pb = button.getPaintable();
        if ((pb != null) && pb.contains(ev.getX(), ev.getY())) { return button; }
      }
    }
    return null;
  }

  public static J2SEButton getSkinButton(final MouseEvent ev) {
    for (final Enumeration en = DeviceFactory.getDevice().getButtons().elements(); en.hasMoreElements();) {
      final J2SEButton button = (J2SEButton)en.nextElement();
      if (button.getShape() != null) {
        if (button.getShape().contains(ev.getX(), ev.getY())) { return button; }
      }
    }
    return null;
  }

  public static J2SEButton getButton(final KeyEvent ev) {
    final DeviceInformation inf = J2SEDeviceButtonsHelper.getDeviceInformation();
    final J2SEButton button = (J2SEButton)inf.keyboardCharCodes.get(new Integer(ev.getKeyChar()));
    if (button != null) { return button; }
    return (J2SEButton)inf.keyboardKeyCodes.get(new Integer(ev.getKeyCode()));
  }

  public static J2SEButton getButton(final ButtonName functionalName) {
    final DeviceInformation inf = J2SEDeviceButtonsHelper.getDeviceInformation();
    return (J2SEButton)inf.functions.get(functionalName);
  }

  private static DeviceInformation getDeviceInformation() {
    final Device dev = DeviceFactory.getDevice();
    DeviceInformation inf;
    synchronized (J2SEDeviceButtonsHelper.class) {
      inf = (DeviceInformation)J2SEDeviceButtonsHelper.devices.get(dev);
      if (inf == null) {
        inf = J2SEDeviceButtonsHelper.createDeviceInformation(dev);
      }
    }
    return inf;
  }

  private static DeviceInformation createDeviceInformation(final Device dev) {
    final DeviceInformation inf = new DeviceInformation();
    boolean hasModeChange = false;
    for (final Enumeration en = dev.getButtons().elements(); en.hasMoreElements();) {
      final J2SEButton button = (J2SEButton)en.nextElement();
      final int keyCodes[] = button.getKeyboardKeyCodes();
      for (final int keyCode : keyCodes) {
        inf.keyboardKeyCodes.put(new Integer(keyCode), button);
      }
      final char charCodes[] = button.getKeyboardCharCodes();
      for (final char charCode : charCodes) {
        inf.keyboardCharCodes.put(new Integer(charCode), button);
      }
      inf.functions.put(button.getFunctionalName(), button);
      if (button.isModeChange()) {
        hasModeChange = true;
      }
    }

    // Correction to missing code in xml
    if (!hasModeChange) {
      final J2SEButton button = (J2SEButton)inf.functions.get(ButtonName.KEY_POUND);
      if (button != null) {
        button.setModeChange();
      }
      else {
        Logger.warn("Device has no ModeChange and POUND buttons");
      }
    }

    if (inf.functions.get(ButtonName.DELETE) == null) {
      dev.getButtons().add(new J2SEButton(ButtonName.DELETE));
    }
    if (inf.functions.get(ButtonName.BACK_SPACE) == null) {
      dev.getButtons().add(new J2SEButton(ButtonName.BACK_SPACE));
    }
    return inf;
  }
}
