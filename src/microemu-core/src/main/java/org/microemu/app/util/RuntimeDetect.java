/**
 * MicroEmulator Copyright (C) 2006-2008 Bartek Teodorczyk <barteo@barteo.net> Copyright (C)
 * 2006-2008 Vlad Skarzhevskyy
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

/**
 * Detect Java runtime environment.
 *
 * @author vlads
 */
public class RuntimeDetect {

  private static boolean java13 = false;

  private static boolean java14 = false;

  private static boolean java15 = false;

  private static boolean inEclipseUnitTests = false;

  private static boolean runtimeDetected = false;

  private static synchronized void detect() {
    if (RuntimeDetect.runtimeDetected) { return; }
    if (RuntimeDetect.detectJava14()) {
      RuntimeDetect.detectJava15();
      RuntimeDetect.detectEclipse();
    }
    RuntimeDetect.runtimeDetected = true;
  }

  private static synchronized boolean detectJava14() {
    try {
      Class.forName("java.lang.StackTraceElement");
      RuntimeDetect.java14 = true;
    }
    catch (final Throwable ignore) {
    }
    return RuntimeDetect.java14;
  }

  private static boolean detectJava15() {
    try {
      RuntimeDetect.java5Function();
      RuntimeDetect.java15 = true;
    }
    catch (final Throwable ignore) {
    }
    return RuntimeDetect.java15;
  }

  private static boolean java5Function() {
    return (Thread.currentThread().getStackTrace() != null);
  }

  private static void detectEclipse() {
    final StackTraceElement[] ste = new Throwable().getStackTrace();
    for (final StackTraceElement s : ste) {
      if (s.getClassName().startsWith("org.eclipse.jdt")) {
        RuntimeDetect.inEclipseUnitTests = true;
        break;
      }
    }
  }

  public static boolean isJava13() {
    RuntimeDetect.detect();
    return RuntimeDetect.java13;
  }

  public static boolean isJava14() {
    RuntimeDetect.detect();
    return RuntimeDetect.java14;
  }

  public static boolean isJava15() {
    RuntimeDetect.detect();
    return RuntimeDetect.java15;
  }

  public static boolean isInEclipseUnitTests() {
    RuntimeDetect.detect();
    return RuntimeDetect.inEclipseUnitTests;
  }
}
