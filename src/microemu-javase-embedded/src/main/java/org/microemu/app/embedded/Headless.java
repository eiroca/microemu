/**
 * MicroEmulator Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
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
package org.microemu.app.embedded;

import java.io.InputStream;
import java.util.ArrayList;
import org.microemu.DisplayComponent;
import org.microemu.MIDletBridge;
import org.microemu.app.Common;
import org.microemu.app.ui.Message;
import org.microemu.app.ui.noui.NoUiDisplayComponent;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.DeviceDisplay;
import org.microemu.device.EmulatorContext;
import org.microemu.device.FontManager;
import org.microemu.device.InputMethod;
import org.microemu.device.impl.DeviceImpl;
import org.microemu.device.j2se.J2SEDevice;
import org.microemu.device.j2se.J2SEDeviceDisplay;
import org.microemu.device.j2se.J2SEFontManager;
import org.microemu.device.j2se.J2SEInputMethod;
import org.microemu.log.Logger;

public class Headless {

  private final Common emulator;

  private final EmulatorContext context = new EmulatorContext() {

    private final DisplayComponent displayComponent = new NoUiDisplayComponent();

    private final InputMethod inputMethod = new J2SEInputMethod();

    private final DeviceDisplay deviceDisplay = new J2SEDeviceDisplay(this);

    private final FontManager fontManager = new J2SEFontManager();

    @Override
    public DisplayComponent getDisplayComponent() {
      return displayComponent;
    }

    @Override
    public InputMethod getDeviceInputMethod() {
      return inputMethod;
    }

    @Override
    public DeviceDisplay getDeviceDisplay() {
      return deviceDisplay;
    }

    @Override
    public FontManager getDeviceFontManager() {
      return fontManager;
    }

    @Override
    public InputStream getResourceAsStream(final Class origClass, final String name) {
      return MIDletBridge.getCurrentMIDlet().getClass().getResourceAsStream(name);
    }

    @Override
    public boolean platformRequest(final String URL) {
      new Thread(() -> Message.info("MIDlet requests that the device handle the following URL: " + URL)).start();

      return false;
    }
  };

  public Headless() {
    emulator = new Common(context);
  }

  public static void main(final String[] args) {
    final StringBuffer debugArgs = new StringBuffer();
    final ArrayList params = new ArrayList();

    // Allow to override in command line
    // Non-persistent RMS
    params.add("--rms");
    params.add("memory");

    for (final String arg : args) {
      params.add(arg);
      if (debugArgs.length() != 0) {
        debugArgs.append(", ");
      }
      debugArgs.append("[").append(arg).append("]");
    }

    if (args.length > 0) {
      Logger.debug("headless arguments", debugArgs.toString());
    }

    final Headless app = new Headless();

    final DeviceEntry defaultDevice = new DeviceEntry("Default device", null, DeviceImpl.DEFAULT_LOCATION, true, false);

    app.emulator.initParams(params, defaultDevice, J2SEDevice.class);
    app.emulator.initMIDlet(true);
  }

}
