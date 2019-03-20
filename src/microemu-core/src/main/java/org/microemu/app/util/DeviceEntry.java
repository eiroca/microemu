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

package org.microemu.app.util;

import org.microemu.device.EmulatorContext;

public class DeviceEntry {

  private final String name;

  private String fileName;

  private final String descriptorLocation;

  private boolean defaultDevice;

  private final boolean canRemove;

  /**
   * @deprecated
   */
  @Deprecated
  private String className;

  /**
   * @deprecated
   */
  @Deprecated
  private EmulatorContext emulatorContext;

  public DeviceEntry(final String name, final String fileName, final String descriptorLocation, final boolean defaultDevice) {
    this(name, fileName, descriptorLocation, defaultDevice, true);
  }

  public DeviceEntry(final String name, final String fileName, final String descriptorLocation, final boolean defaultDevice, final boolean canRemove) {
    this.name = name;
    this.fileName = fileName;
    this.descriptorLocation = descriptorLocation;
    this.defaultDevice = defaultDevice;
    this.canRemove = canRemove;
  }

  /**
   * @deprecated use new DeviceEntry(String name, String fileName, String descriptorLocation,
   *             boolean defaultDevice);
   */
  @Deprecated
  public DeviceEntry(final String name, final String fileName, final boolean defaultDevice, final String className, final EmulatorContext emulatorContext) {
    this(name, fileName, null, defaultDevice, true);

    this.className = className;
    this.emulatorContext = emulatorContext;
  }

  public boolean canRemove() {
    return canRemove;
  }

  public String getDescriptorLocation() {
    return descriptorLocation;
  }

  public String getFileName() {
    return fileName;
  }

  /**
   * @deprecated
   */
  @Deprecated
  public void setFileName(final String fileName) {
    this.fileName = fileName;
  }

  public String getName() {
    return name;
  }

  public boolean isDefaultDevice() {
    return defaultDevice;
  }

  public void setDefaultDevice(final boolean b) {
    defaultDevice = b;
  }

  public boolean equals(final DeviceEntry test) {
    if (test == null) { return false; }
    if (test.getDescriptorLocation().equals(getDescriptorLocation())) { return true; }

    return false;
  }

  @Override
  public String toString() {
    if (defaultDevice) {
      return name + " (default)";
    }
    else {
      return name;
    }
  }

}
