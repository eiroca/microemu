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
 */
package org.microemu.app.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import org.microemu.app.util.IOUtils;
import org.microemu.log.Logger;

/**
 * Main features of this class loader Security aware - enables load and run app in Webstart. Proper
 * class loading order. MIDlet classes loaded first then system and MicroEmulator classes Proper
 * resource loading order. MIDlet resources only can be loaded. MIDlet Bytecode
 * preprocessing/instrumentation
 *
 * @author vlads
 *
 */
public class MIDletClassLoader extends URLClassLoader {

  // TODO make this configurable

  public static boolean instrumentMIDletClasses = true;

  public static boolean traceClassLoading = false;

  public static boolean traceSystemClassLoading = false;

  public static boolean enhanceCatchBlock = false;

  private final static boolean debug = false;

  private boolean delegatingToParent = false;

  private boolean findPathInParent = false;

  private final InstrumentationConfig config;

  private final Set noPreporcessingNames;

  /* The context to be used when loading classes and resources */
  private final AccessControlContext acc;

  private static class LoadClassByParentException extends ClassNotFoundException {

    public LoadClassByParentException(final String name) {
      super(name);
    }

    private static final long serialVersionUID = 1L;

  }

  public MIDletClassLoader(final ClassLoader parent) {
    super(new URL[] {}, parent);
    noPreporcessingNames = new HashSet();
    acc = AccessController.getContext();
    config = new InstrumentationConfig();
    config.setEnhanceCatchBlock(MIDletClassLoader.enhanceCatchBlock);
    config.setEnhanceThreadCreation(true);
  }

  // public MIDletClassLoader(URL[] urls, ClassLoader parent) {
  // super(urls, parent);
  // noPreporcessingNames = new HashSet();
  // }

  public void configure(final MIDletClassLoaderConfig clConfig, final boolean forJad) throws MalformedURLException {
    for (final Iterator iter = clConfig.appclasspath.iterator(); iter.hasNext();) {
      final String path = (String)iter.next();
      final StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
      while (st.hasMoreTokens()) {
        addURL(new URL(IOUtils.getCanonicalFileClassLoaderURL(new File(st.nextToken()))));
      }
    }
    for (final Iterator iter = clConfig.appclasses.iterator(); iter.hasNext();) {
      addClassURL((String)iter.next());
    }
    final int delegationType = clConfig.getDelegationType(forJad);
    delegatingToParent = (delegationType == MIDletClassLoaderConfig.DELEGATION_DELEGATING);
    findPathInParent = (delegationType == MIDletClassLoaderConfig.DELEGATION_RELAXED);
  }

  /**
   * Appends the Class Location URL to the list of URLs to search for classes and resources.
   *
   * @param Class Name
   */
  public void addClassURL(final String className) throws MalformedURLException {
    final String resource = MIDletClassLoader.getClassResourceName(className);
    URL url = getParent().getResource(resource);
    if (url == null) {
      url = getResource(resource);
    }
    if (url == null) { throw new MalformedURLException("Unable to find class " + className + " URL"); }
    final String path = url.toExternalForm();
    if (MIDletClassLoader.debug) {
      Logger.debug("addClassURL ", path);
    }
    addURL(new URL(path.substring(0, path.length() - resource.length())));
  }

  static URL getClassURL(final ClassLoader parent, final String className) throws MalformedURLException {
    final String resource = MIDletClassLoader.getClassResourceName(className);
    final URL url = parent.getResource(resource);
    if (url == null) { throw new MalformedURLException("Unable to find class " + className + " URL"); }
    final String path = url.toExternalForm();
    return new URL(path.substring(0, path.length() - resource.length()));
  }

  @Override
  public void addURL(final URL url) {
    if (MIDletClassLoader.debug) {
      Logger.debug("addURL ", url.toString());
    }
    super.addURL(url);
  }

  /**
   * Loads the class with the specified <a href="#name">binary name</a>.
   *
   * <p>
   * Search order is reverse to standard implemenation
   * </p>
   *
   * This implementation of this method searches for classes in the following order:
   *
   * <p>
   * <ol>
   *
   * <li>
   * <p>
   * Invoke {@link #findLoadedClass(String)} to check if the class has already been loaded.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * Invoke the {@link #findClass(String)} method to find the class in this class loader URLs.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * Invoke the {@link #loadClass(String) <tt>loadClass</tt>} method on the parent class loader. If
   * the parent is <tt>null</tt> the class loader built-in to the virtual machine is used, instead.
   * </p>
   * </li>
   *
   * </ol>
   *
   */
  @Override
  protected synchronized Class loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
    if (MIDletClassLoader.debug) {
      Logger.debug("loadClass", name);
    }
    // First, check if the class has already been loaded
    Class result = findLoadedClass(name);
    if (result == null) {
      try {
        result = findClass(name);
        if (MIDletClassLoader.debug && (result == null)) {
          Logger.debug("loadClass not found", name);
        }
      }
      catch (final ClassNotFoundException e) {

        if ((e instanceof LoadClassByParentException) || delegatingToParent) {
          if (MIDletClassLoader.traceSystemClassLoading) {
            Logger.info("Load system class", name);
          }
          // This will call our findClass again if Class is not found
          // in parent
          result = super.loadClass(name, false);
          if (result == null) { throw new ClassNotFoundException(name); }
        }
      }
    }
    if (resolve) {
      resolveClass(result);
    }
    return result;
  }

  /**
   * Finds the resource with the given name. A resource is some data (images, audio, text, etc) that
   * can be accessed by class code in a way that is independent of the location of the code.
   *
   * <p>
   * The name of a resource is a '<tt>/</tt>'-separated path name that identifies the resource.
   *
   * <p>
   * Search order is reverse to standard implementation
   * </p>
   *
   * <p>
   * This method will first use {@link #findResource(String)} to find the resource. That failing,
   * this method will NOT invoke the parent class loader if delegatingToParent=false.
   * </p>
   *
   * @param name The resource name
   *
   * @return A <tt>URL</tt> object for reading the resource, or <tt>null</tt> if the resource could
   *         not be found or the invoker doesn't have adequate privileges to get the resource.
   *
   */

  @Override
  public URL getResource(final String name) {
    try {
      return (URL)AccessController.doPrivileged((PrivilegedExceptionAction)() -> {
        URL url = findResource(name);
        if ((url == null) && delegatingToParent && (getParent() != null)) {
          url = getParent().getResource(name);
        }
        return url;
      }, acc);
    }
    catch (final PrivilegedActionException e) {
      if (MIDletClassLoader.debug) {
        Logger.error("Unable to find resource " + name + " ", e);
      }
      return null;
    }
  }

  /**
   * Allow access to resources
   */
  @Override
  public InputStream getResourceAsStream(final String name) {
    final URL url = getResource(name);
    if (url == null) { return null; }

    try {
      return (InputStream)AccessController.doPrivileged((PrivilegedExceptionAction)() -> url.openStream(), acc);
    }
    catch (final PrivilegedActionException e) {
      if (MIDletClassLoader.debug) {
        Logger.debug("Unable to find resource for class " + name + " ", e);
      }
      return null;
    }

  }

  public boolean classLoadByParent(final String className) {
    /* This java standard */
    if (className.startsWith("java.")) { return true; }
    /*
     * This is required when Class.forName().newInstance() used to create instances with inheritance
     */
    if (className.startsWith("sun.reflect.")) { return true; }
    /* No real device allow overloading this package */
    if (className.startsWith("javax.microedition.")) { return true; }
    if (className.startsWith("com.nokia.mid.")) { return true; }
    if (className.startsWith("javax.")) { return true; }
    if (noPreporcessingNames.contains(className)) { return true; }
    return false;
  }

  /**
   * Special case for classes injected to MIDlet
   *
   * @param klass
   */
  public void disableClassPreporcessing(final Class klass) {
    disableClassPreporcessing(klass.getName());
  }

  public void disableClassPreporcessing(final String className) {
    noPreporcessingNames.add(className);
  }

  public static String getClassResourceName(final String className) {
    return className.replace('.', '/').concat(".class");
  }

  @Override
  protected Class findClass(final String name) throws ClassNotFoundException {
    if (MIDletClassLoader.debug) {
      Logger.debug("findClass", name);
    }
    if (classLoadByParent(name)) { throw new LoadClassByParentException(name); }
    InputStream is;
    try {
      is = (InputStream)AccessController.doPrivileged((PrivilegedExceptionAction)() -> getResourceAsStream(MIDletClassLoader.getClassResourceName(name)), acc);

      // Relax ClassLoader behavior
      if ((is == null) && (findPathInParent)) {
        boolean classFound;
        try {
          addClassURL(name);
          classFound = true;
        }
        catch (final MalformedURLException e) {
          classFound = false;
        }
        if (classFound) {
          is = (InputStream)AccessController.doPrivileged((PrivilegedExceptionAction)() -> getResourceAsStream(MIDletClassLoader.getClassResourceName(name)), acc);
        }
      }
    }
    catch (final PrivilegedActionException e) {
      if (MIDletClassLoader.debug) {
        Logger.debug("Unable to find resource for class " + name + " ", e);
      }
      throw new ClassNotFoundException(name, e.getCause());
    }

    if (is == null) {
      if (MIDletClassLoader.debug) {
        Logger.debug("Unable to find resource for class", name);
      }
      throw new ClassNotFoundException(name);
    }
    byte[] byteCode;
    int byteCodeLength;
    try {
      if (MIDletClassLoader.traceClassLoading) {
        Logger.info("Load MIDlet class", name);
      }
      if (MIDletClassLoader.instrumentMIDletClasses) {
        byteCode = ClassPreprocessor.instrument(is, config);
        byteCodeLength = byteCode.length;
      }
      else {
        final int chunkSize = 1024 * 2;
        // No class or data object must be bigger than 16 Kilobyte
        final int maxClassSizeSize = 1024 * 16;
        byteCode = new byte[chunkSize];
        byteCodeLength = 0;
        do {
          int retrived;
          try {
            retrived = is.read(byteCode, byteCodeLength, byteCode.length - byteCodeLength);
          }
          catch (final IOException e) {
            throw new ClassNotFoundException(name, e);
          }
          if (retrived == -1) {
            break;
          }
          if ((byteCode.length + chunkSize) > maxClassSizeSize) { throw new ClassNotFoundException(name, new ClassFormatError(
              "Class object is bigger than 16 Kilobyte")); }
          byteCodeLength += retrived;
          if (byteCode.length == byteCodeLength) {
            final byte[] newData = new byte[byteCode.length + chunkSize];
            System.arraycopy(byteCode, 0, newData, 0, byteCode.length);
            byteCode = newData;
          }
          else if (byteCode.length < byteCodeLength) { throw new ClassNotFoundException(name, new ClassFormatError("Internal read error")); }
        }
        while (true);
      }
    }
    finally {
      try {
        is.close();
      }
      catch (final IOException ignore) {
      }
    }
    if ((MIDletClassLoader.debug) && (MIDletClassLoader.instrumentMIDletClasses)) {
      Logger.debug("instrumented ", name);
    }
    return defineClass(name, byteCode, 0, byteCodeLength);
  }

}
