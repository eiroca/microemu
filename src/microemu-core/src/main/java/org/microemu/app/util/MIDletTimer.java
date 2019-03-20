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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import org.microemu.MIDletBridge;
import org.microemu.MIDletContext;
import org.microemu.log.Logger;

/**
 * Terminate all timers on MIDlet exit. TODO Name all the timer Threads created by MIDlet in Java 5
 *
 * @author vlads
 */
public class MIDletTimer extends Timer implements Runnable {

  private static Map midlets = new WeakHashMap();

  private final String name;

  private MIDletContext midletContext;

  // TODO use better data structure
  List tasks;

  private boolean cancelled;

  private final MIDletThread thread;

  public MIDletTimer() {
    super();
    final StackTraceElement[] ste = new Throwable().getStackTrace();
    name = ste[1].getClassName() + "." + ste[1].getMethodName();
    tasks = new ArrayList();
    cancelled = false;
    thread = new MIDletThread(this);
    thread.start();
  }

  // TODO exceptions
  @Override
  public void schedule(final TimerTask task, final Date time) {
    MIDletTimer.register(this);
    schedule(task, time.getTime(), -1, false);
  }

  // TODO exceptions
  @Override
  public void schedule(final TimerTask task, final Date firstTime, final long period) {
    MIDletTimer.register(this);
    schedule(task, firstTime.getTime(), period, false);
  }

  // TODO exceptions
  @Override
  public void schedule(final TimerTask task, final long delay) {
    MIDletTimer.register(this);
    schedule(task, System.currentTimeMillis() + delay, -1, false);
  }

  // TODO exceptions
  @Override
  public void schedule(final TimerTask task, final long delay, final long period) {
    MIDletTimer.register(this);
    schedule(task, System.currentTimeMillis() + delay, period, false);
  }

  // TODO exceptions
  @Override
  public void scheduleAtFixedRate(final TimerTask task, final Date firstTime, final long period) {
    MIDletTimer.register(this);
    schedule(task, firstTime.getTime(), period, true);
  }

  // TODO exceptions
  @Override
  public void scheduleAtFixedRate(final TimerTask task, final long delay, final long period) {
    MIDletTimer.register(this);
    schedule(task, System.currentTimeMillis() + delay, period, true);
  }

  @Override
  public void cancel() {
    MIDletTimer.unregister(this);

    terminate();
  }

  @Override
  public void run() {
    while (!cancelled) {
      MIDletTimerTask task = null;
      long nextTimeTask = Long.MAX_VALUE;
      synchronized (tasks) {
        final Iterator it = tasks.iterator();
        while (it.hasNext()) {
          final MIDletTimerTask candidate = (MIDletTimerTask)it.next();
          if (candidate.time > System.currentTimeMillis()) {
            if (candidate.time < nextTimeTask) {
              nextTimeTask = candidate.time;
            }
            continue;
          }
          if (task == null) {
            task = candidate;
          }
          else if (candidate.time < task.time) {
            if (task.time < nextTimeTask) {
              nextTimeTask = task.time;
            }
            task = candidate;
          }
          else if (candidate.time < nextTimeTask) {
            nextTimeTask = candidate.time;
          }
        }
        if (task != null) {
          if (task.period > 0) {
            task.oneTimeTaskExcecuted = true;
          }
          tasks.remove(task);
        }
      }

      if (task != null) {
        try {
          task.run();

          synchronized (tasks) {
            // TODO implement scheduling for fixed rate tasks	
            if (task.period > 0) {
              task.time = System.currentTimeMillis() + task.period;
              tasks.add(task);
              if (task.time < nextTimeTask) {
                nextTimeTask = task.time;
              }
            }
          }
        }
        catch (final Throwable t) {
          if (MIDletThread.debug) {
            Logger.debug("MIDletTimerTask throws", t);
          }
        }
      }

      synchronized (tasks) {
        try {
          if (nextTimeTask == Long.MAX_VALUE) {
            tasks.wait();
          }
          else {
            final long timeout = nextTimeTask - System.currentTimeMillis();
            if (timeout > 0) {
              tasks.wait(timeout);
            }
          }
        }
        catch (final InterruptedException e) {
        }
      }
    }
  }

  private void terminate() {
    cancelled = true;
  }

  private void schedule(final TimerTask task, final long time, final long period, final boolean fixedRate) {
    synchronized (tasks) {
      ((MIDletTimerTask)task).timer = this;
      ((MIDletTimerTask)task).time = time;
      ((MIDletTimerTask)task).period = period;
      tasks.add(task);
      tasks.notify();
    }
  }

  private static void register(final MIDletTimer timer) {
    if (timer.midletContext == null) {
      timer.midletContext = MIDletBridge.getMIDletContext();
    }
    if (timer.midletContext == null) {
      Logger.error("Creating Timer with no MIDlet context", new Throwable());
      return;
    }
    Map timers = (Map)MIDletTimer.midlets.get(timer.midletContext);
    if (timers == null) {
      // Can't use WeakHashMap Timers are disposed by JVM
      timers = new HashMap();
      MIDletTimer.midlets.put(timer.midletContext, timers);
    }
    // Logger.debug("Register timer created from [" + timer.name + "]");
    timers.put(timer, timer.midletContext);
  }

  private static void unregister(final MIDletTimer timer) {
    if (timer.midletContext == null) {
      // Logger.error("Timer with no MIDlet context", new Throwable());
      return;
    }
    final Map timers = (Map)MIDletTimer.midlets.get(timer.midletContext);
    if (timers == null) { return; }
    // Logger.debug("Unregister timer created from [" + timer.name + "]");
    timers.remove(timer);
  }

  /**
   * Terminate all Threads created by MIDlet
   */
  public static void contextDestroyed(final MIDletContext midletContext) {
    if (midletContext == null) { return; }
    final Map timers = (Map)MIDletTimer.midlets.get(midletContext);
    if (timers != null) {
      MIDletTimer.terminateTimers(timers);
      MIDletTimer.midlets.remove(midletContext);
    }
  }

  private static void terminateTimers(final Map timers) {
    for (final Iterator iter = timers.keySet().iterator(); iter.hasNext();) {
      final Object o = iter.next();
      if (o == null) {
        continue;
      }
      if (o instanceof MIDletTimer) {
        final MIDletTimer tm = (MIDletTimer)o;
        Logger.warn("MIDlet timer created from [" + tm.name + "] still running");
        tm.terminate();
      }
      else {
        Logger.debug("unrecognized Object [" + o.getClass().getName() + "]");
      }
    }
  }

}
