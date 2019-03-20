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

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import org.microemu.MIDletBridge;
import org.microemu.MIDletContext;
import org.microemu.log.Logger;
import org.microemu.util.ThreadUtils;

/**
 * MIDletContext is used to hold keys to running Threads created by MIDlet
 *
 * @author vlads
 */
public class MIDletThread extends Thread {

  public static int graceTerminationPeriod = 5000;

  private static final String THREAD_NAME_PREFIX = "";

  static boolean debug = false;

  private static boolean terminator = false;

  private static Map midlets = new WeakHashMap();

  private static int threadInitNumber;

  private String callLocation;

  private static synchronized int nextThreadNum() {
    return MIDletThread.threadInitNumber++;
  }

  public MIDletThread() {
    super(MIDletThread.THREAD_NAME_PREFIX + MIDletThread.nextThreadNum());
    MIDletThread.register(this);
  }

  public MIDletThread(final Runnable target) {
    super(target, MIDletThread.THREAD_NAME_PREFIX + MIDletThread.nextThreadNum());
    MIDletThread.register(this);
  }

  public MIDletThread(final Runnable target, final String name) {
    super(target, MIDletThread.THREAD_NAME_PREFIX + name);
    MIDletThread.register(this);
  }

  public MIDletThread(final String name) {
    super(MIDletThread.THREAD_NAME_PREFIX + name);
    MIDletThread.register(this);
  }

  private static void register(final MIDletThread thread) {
    final MIDletContext midletContext = MIDletBridge.getMIDletContext();
    if ((midletContext == null) && MIDletThread.debug) {
      Logger.error("Creating thread with no MIDlet context", new Throwable());
      return;
    }
    thread.callLocation = ThreadUtils.getCallLocation(MIDletThread.class.getName());
    Map threads = (Map)MIDletThread.midlets.get(midletContext);
    if (threads == null) {
      threads = new WeakHashMap();
      MIDletThread.midlets.put(midletContext, threads);
    }
    threads.put(thread, midletContext);
  }

  //TODO overrite run() in user Threads using ASM
  @Override
  public void run() {
    try {
      super.run();
    }
    catch (final Throwable e) {
      if (MIDletThread.debug) {
        Logger.debug("MIDletThread throws", e);
      }
    }
    //Logger.debug("thread ends, created from " + callLocation);	
  }

  /**
   * Terminate all Threads created by MIDlet
   * @param previousMidletAccess
   */
  public static void contextDestroyed(final MIDletContext midletContext) {
    if (midletContext == null) { return; }
    final Map threads = (Map)MIDletThread.midlets.remove(midletContext);
    if ((threads != null) && (threads.size() != 0)) {
      MIDletThread.terminator = true;
      final Thread terminator = new Thread("MIDletThreadsTerminator") {

        @Override
        public void run() {
          MIDletThread.terminateThreads(threads);
        }
      };
      terminator.start();
    }
    MIDletTimer.contextDestroyed(midletContext);
  }

  public static boolean hasRunningThreads(final MIDletContext midletContext) {
    //return (midlets.get(midletContext) != null);
    return MIDletThread.terminator;
  }

  private static void terminateThreads(final Map threads) {
    final long endTime = System.currentTimeMillis() + MIDletThread.graceTerminationPeriod;
    for (final Iterator iter = threads.keySet().iterator(); iter.hasNext();) {
      final Object o = iter.next();
      if (o == null) {
        continue;
      }
      if (o instanceof MIDletThread) {
        final MIDletThread t = (MIDletThread)o;
        if (t.isAlive()) {
          Logger.info("wait thread [" + t.getName() + "] end");
          while ((endTime > System.currentTimeMillis()) && (t.isAlive())) {
            try {
              t.join(700);
            }
            catch (final InterruptedException e) {
              break;
            }
          }
          if (t.isAlive()) {
            Logger.warn("MIDlet thread [" + t.getName() + "] still running" + ThreadUtils.getTreadStackTrace(t));
            if (t.callLocation != null) {
              Logger.info("this thread [" + t.getName() + "] was created from " + t.callLocation);
            }
            t.interrupt();
          }
        }
      }
      else {
        Logger.debug("unrecognized Object [" + o.getClass().getName() + "]");
      }
    }
    Logger.debug("all " + threads.size() + " thread(s) finished");
    MIDletThread.terminator = false;
  }

}
