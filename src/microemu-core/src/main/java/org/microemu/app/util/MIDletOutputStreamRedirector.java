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

import java.io.OutputStream;
import java.io.PrintStream;
import org.microemu.log.Logger;

/**
 * @author vlads
 *
 *         This class allow redirection of stdout and stderr from MIDlet to MicroEmulator logger
 *         console
 *
 */
public class MIDletOutputStreamRedirector extends PrintStream {

  private final static boolean keepMultiLinePrint = true;

  public final static PrintStream out = MIDletOutputStreamRedirector.outPrintStream();

  public final static PrintStream err = MIDletOutputStreamRedirector.errPrintStream();

  private final boolean isErrorStream;

  static {
    Logger.addLogOrigin(MIDletOutputStreamRedirector.class);
    Logger.addLogOrigin(OutputStream2Log.class);
  }

  private static class OutputStream2Log extends OutputStream {

    boolean isErrorStream;

    StringBuffer buffer = new StringBuffer();

    OutputStream2Log(final boolean error) {
      isErrorStream = error;
    }

    @Override
    public void flush() {
      if (buffer.length() > 0) {
        write('\n');
      }
    }

    @Override
    public void write(final int b) {
      if ((b == '\n') || (b == '\r')) {
        if (buffer.length() > 0) {
          if (isErrorStream) {
            Logger.error(buffer.toString());
          }
          else {
            Logger.info(buffer.toString());
          }
          buffer = new StringBuffer();
        }
      }
      else {
        buffer.append((char)b);
      }
    }

  }

  private MIDletOutputStreamRedirector(final boolean error) {
    super(new OutputStream2Log(error));

    isErrorStream = error;
  }

  private static PrintStream outPrintStream() {
    return new MIDletOutputStreamRedirector(false);
  }

  private static PrintStream errPrintStream() {
    return new MIDletOutputStreamRedirector(true);
  }

  // Override methods to be able to get proper stack trace

  @Override
  public void print(final boolean b) {
    super.print(b);
  }

  @Override
  public void print(final char c) {
    super.print(c);
  }

  @Override
  public void print(final char[] s) {
    super.print(s);
  }

  @Override
  public void print(final double d) {
    super.print(d);
  }

  @Override
  public void print(final float f) {
    super.print(f);
  }

  @Override
  public void print(final int i) {
    super.print(i);
  }

  @Override
  public void print(final long l) {
    super.print(l);
  }

  @Override
  public void print(final Object obj) {
    super.print(obj);
  }

  @Override
  public void print(final String s) {
    super.print(s);
  }

  @Override
  public void println() {
    super.println();
  }

  @Override
  public void println(final boolean x) {
    super.println(x);
  }

  @Override
  public void println(final char x) {
    super.println(x);
  }

  @Override
  public void println(final char[] x) {
    super.println(x);
  }

  @Override
  public void println(final double x) {
    super.println(x);
  }

  @Override
  public void println(final float x) {
    super.println(x);
  }

  @Override
  public void println(final int x) {
    super.println(x);
  }

  @Override
  public void println(final long x) {
    super.println(x);
  }

  @Override
  public void println(final Object x) {
    if (MIDletOutputStreamRedirector.keepMultiLinePrint) {
      super.flush();
      if (isErrorStream) {
        Logger.error(x);
      }
      else {
        Logger.info(x);
      }
    }
    else {
      super.println(x);
    }
  }

  @Override
  public void println(final String x) {
    if (MIDletOutputStreamRedirector.keepMultiLinePrint) {
      super.flush();
      if (isErrorStream) {
        Logger.error(x);
      }
      else {
        Logger.info(x);
      }
    }
    else {
      super.println(x);
    }
  }

  @Override
  public void write(final byte[] buf, final int off, final int len) {
    super.write(buf, off, len);
  }

  @Override
  public void write(final int b) {
    super.write(b);
  }

}
