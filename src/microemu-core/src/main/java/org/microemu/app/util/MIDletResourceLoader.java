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

import java.io.InputStream;
import org.microemu.Injected;
import org.microemu.log.Logger;
import org.microemu.util.ThreadUtils;

/**
 * @author vlads
 *
 *         Use MIDletResourceLoader to load resources. To solve resource resource loading paterns
 *         commonly used in MIDlet and not aceptable in Java SE application when System class is
 *         called to load resource
 *
 *         j2me example:
 *
 *         String.class.getResourceAsStream(resourceName)
 *
 */
public class MIDletResourceLoader {

  // TODO make this configurable

  public static boolean traceResourceLoading = false;

  /**
   * @deprecated find better solution to share variable
   */
  @Deprecated
  public static ClassLoader classLoader;

  private static final String FQCN = Injected.class.getName();

  public static InputStream getResourceAsStream(final Class origClass, String resourceName) {
    if (MIDletResourceLoader.traceResourceLoading) {
      Logger.debug("Loading MIDlet resource", resourceName);
    }
    if (MIDletResourceLoader.classLoader != origClass.getClassLoader()) {
      // showWarning
      final String callLocation = ThreadUtils.getCallLocation(MIDletResourceLoader.FQCN);
      if (MIDletResourceLoader.traceResourceLoading && (callLocation != null)) {
        Logger.warn("attempt to load resource [" + resourceName + "] using System ClasslLoader from "
            + callLocation);
      }
    }
    resourceName = MIDletResourceLoader.resolveName(origClass, resourceName);

    final InputStream is = MIDletResourceLoader.classLoader.getResourceAsStream(resourceName);
    if (is == null) {
      Logger.debug("Resource not found ", resourceName);
      return null;
    }
    else {
      return new MIDletResourceInputStream(is);
    }
  }

  private static String resolveName(Class origClass, String name) {
    if (name == null) { return name; }
    if (!name.startsWith("/")) {
      while (origClass.isArray()) {
        origClass = origClass.getComponentType();
      }
      final String baseName = origClass.getName();
      final int index = baseName.lastIndexOf('.');
      if (index != -1) {
        name = baseName.substring(0, index).replace('.', '/') + "/" + name;
      }
    }
    else {
      name = name.substring(1);
    }
    return name;
  }
}
