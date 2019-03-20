/**
 * MicroEmulator Copyright (C) 2001-2003 Bartek Teodorczyk <barteo@barteo.net>
 *
 * It is licensed under the following two licenses as alternatives:
 *
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *
 * 2. Apache License (the "AL") Version 2.0
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
package org.microemu.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipException;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.microemu.Injected;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.MIDletContext;
import org.microemu.MIDletEntry;
import org.microemu.MicroEmulator;
import org.microemu.RecordStoreManager;
import org.microemu.app.classloader.ExtensionsClassLoader;
import org.microemu.app.classloader.MIDletClassLoader;
import org.microemu.app.classloader.MIDletClassLoaderConfig;
import org.microemu.app.launcher.Launcher;
import org.microemu.app.ui.Message;
import org.microemu.app.ui.ResponseInterfaceListener;
import org.microemu.app.ui.StatusBarListener;
import org.microemu.app.util.DeviceEntry;
import org.microemu.app.util.FileRecordStoreManager;
import org.microemu.app.util.IOUtils;
import org.microemu.app.util.MIDletResourceLoader;
import org.microemu.app.util.MIDletSystemProperties;
import org.microemu.app.util.MIDletThread;
import org.microemu.app.util.MIDletTimer;
import org.microemu.app.util.MIDletTimerTask;
import org.microemu.app.util.MidletURLReference;
import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.EmulatorContext;
import org.microemu.device.impl.DeviceDisplayImpl;
import org.microemu.device.impl.DeviceImpl;
import org.microemu.device.impl.Rectangle;
import org.microemu.log.Logger;
import org.microemu.log.StdOutAppender;
import org.microemu.microedition.ImplFactory;
import org.microemu.microedition.ImplementationInitialization;
import org.microemu.microedition.io.ConnectorImpl;
import org.microemu.util.Base64Coder;
import org.microemu.util.JadMidletEntry;
import org.microemu.util.JadProperties;
import org.microemu.util.MemoryRecordStoreManager;

public class Common implements MicroEmulator, CommonInterface {

  public JadProperties jad = new JadProperties();

  protected EmulatorContext emulatorContext;

  private static Common instance;

  private static Launcher launcher;

  private static StatusBarListener statusBarListener = null;

  private final JadProperties manifest = new JadProperties();

  private RecordStoreManager recordStoreManager;

  private ResponseInterfaceListener responseInterfaceListener = null;

  private ExtensionsClassLoader extensionsClassLoader;

  private final Vector extensions = new Vector();

  private MIDletClassLoaderConfig mIDletClassLoaderConfig;

  private boolean useSystemClassLoader = false;

  private boolean autoTests = false;

  private String propertiesJad = null;

  private String midletClassOrUrl = null;

  private String jadURL = null;

  private final Object destroyNotify = new Object();

  private boolean exitOnMIDletDestroy = false;

  public Common(final EmulatorContext context) {
    Common.instance = this;
    emulatorContext = context;

    /*
     * Initialize secutity context for implemenations, May be there are better place
     * for this call
     */
    ImplFactory.instance();
    MIDletSystemProperties.initContext();
    // TODO integrate with ImplementationInitialization
    ImplFactory.registerGCF(ImplFactory.DEFAULT, new ConnectorImpl());

    MIDletBridge.setMicroEmulator(this);
  }

  @Override
  public RecordStoreManager getRecordStoreManager() {
    return recordStoreManager;
  }

  public void setRecordStoreManager(final RecordStoreManager manager) {
    recordStoreManager = manager;
  }

  @Override
  public String getAppProperty(final String key) {
    if (key.equals("microedition.platform")) {
      return "MicroEmulator";
    }
    else if (key.equals("microedition.profiles")) {
      return "MIDP-2.0";
    }
    else if (key.equals("microedition.configuration")) {
      return "CLDC-1.0";
    }
    else if (key.equals("microedition.locale")) {
      return Locale.getDefault().getLanguage();
    }
    else if (key.equals("microedition.encoding")) { return System.getProperty("file.encoding"); }

    String result = jad.getProperty(key);
    if (result == null) {
      result = manifest.getProperty(key);
    }

    return result;
  }

  @Override
  public InputStream getResourceAsStream(final Class origClass, final String name) {
    return emulatorContext.getResourceAsStream(origClass, name);
  }

  @Override
  public void notifyDestroyed(final MIDletContext midletContext) {
    Logger.debug("notifyDestroyed");
    notifyImplementationMIDletDestroyed();
    startLauncher(midletContext);
  }

  @Override
  public void destroyMIDletContext(final MIDletContext midletContext) {
    if ((midletContext != null) && (MIDletBridge.getMIDletContext() == midletContext) && !midletContext.isLauncher()) {
      Logger.debug("destroyMIDletContext");
    }
    MIDletThread.contextDestroyed(midletContext);
    synchronized (destroyNotify) {
      destroyNotify.notifyAll();
    }
  }

  @Override
  public Launcher getLauncher() {
    return Common.launcher;
  }

  public static void dispose() {
    try {
      final MIDletAccess midletAccess = MIDletBridge.getMIDletAccess();
      if (midletAccess != null) {
        midletAccess.destroyApp(true);
      }
    }
    catch (final MIDletStateChangeException ex) {
      Logger.error(ex);
    }
    // TODO to be removed when event dispatcher will run input method task
    DeviceFactory.getDevice().getInputMethod().dispose();
  }

  public static boolean isMIDletUrlExtension(String nameString) {
    if (nameString == null) { return false; }
    // Remove query
    if (nameString.startsWith("http://") || nameString.startsWith("https://")) {
      final int s = nameString.lastIndexOf('?');
      if (s != -1) {
        nameString = nameString.substring(0, s);
      }
    }
    final int end = nameString.lastIndexOf('.');
    if (end == -1) { return false; }

    return (nameString.substring(end + 1, nameString.length()).toLowerCase(Locale.ENGLISH).equals("jad") || nameString.substring(end + 1,
        nameString.length()).toLowerCase(Locale.ENGLISH).equals("jar"));
  }

  /**
   * TODO add proper Error handling and display in this function.
   */
  public static void openMIDletUrlSafe(final String urlString) {
    try {
      Common.getInstance().openMIDletUrl(urlString);
    }
    catch (final IOException e) {
      Message.error("Unable to open jad " + urlString, e);
    }
  }

  protected void openMIDletUrl(final String urlString) throws IOException {
    midletClassOrUrl = urlString;
    if (!autoTests) {
      openMIDletUrl(urlString, createMIDletClassLoader(true));
    }
    else {
      runAutoTests(urlString, false);
    }
  }

  private void runAutoTests(final String urlString, final boolean exitAtTheEnd) {
    final Common common = Common.getInstance();
    final Thread t = new Thread("AutoTestsThread") {

      @Override
      public void run() {
        boolean firstJad = true;
        do {
          common.jad.clear();
          Logger.debug("AutoTests open jad", urlString);
          try {
            common.jad = Common.loadJadProperties(urlString);
          }
          catch (final IOException e) {
            if (firstJad) {
              Logger.debug(e);
            }
            else {
              Logger.debug("AutoTests no more tests");
            }
            break;
          }
          firstJad = false;

          JadMidletEntry jadMidletEntry;
          final Iterator it = common.jad.getMidletEntries().iterator();
          if (!it.hasNext()) {
            Message.error("MIDlet Suite has no entries");
            break;
          }
          jadMidletEntry = (JadMidletEntry)it.next();
          final String midletClassName = jadMidletEntry.getClassName();

          boolean firstJar = true;
          do {
            final MIDletClassLoader midletClassLoader = createMIDletClassLoader(true);
            final String tmpURL = saveJar2TmpFile(urlString, firstJar);
            if (tmpURL == null) {
              Logger.debug("AutoTests no new jar");
              break;
            }
            firstJar = false;
            Class midletClass;
            try {
              loadJar(urlString, tmpURL, midletClassLoader);
              midletClass = midletClassLoader.loadClass(midletClassName);
            }
            catch (final ClassNotFoundException e) {
              Logger.debug(e);
              break;
            }
            Logger.debug("AutoTests start class", midletClassName);
            final MIDlet m = loadMidlet(midletClass, MIDletBridge.getMIDletAccess());
            try {
              MIDletBridge.getMIDletAccess(m).startApp();
            }
            catch (final MIDletStateChangeException e) {
              Logger.error(e);
            }
            // TODO Proper test If this is still active conetex.
            if (MIDletBridge.getMIDletContext() == MIDletBridge.getMIDletContext(m)) {
              synchronized (destroyNotify) {
                try {
                  destroyNotify.wait();
                }
                catch (final InterruptedException e) {
                  return;
                }
              }
            }
            while (MIDletThread.hasRunningThreads(MIDletBridge.getMIDletContext(m))) {
              try {
                Thread.sleep(100);
              }
              catch (final InterruptedException e) {
                break;
              }
            }
            Logger.debug("AutoTests ends");
          }
          while (true);

        }
        while (true);

        if (exitAtTheEnd) {
          System.exit(0);
        }
      }
    };

    t.start();
  }

  protected String saveJar2TmpFile(final String jarUrl, final boolean reportError) {
    InputStream is = null;
    try {
      final URL url = new URL(jad.getJarURL());
      final URLConnection conn = url.openConnection();
      if (url.getUserInfo() != null) {
        final String userInfo = new String(Base64Coder.encode(url.getUserInfo().getBytes("UTF-8")));
        conn.setRequestProperty("Authorization", "Basic " + userInfo);
      }
      is = conn.getInputStream();
      File tmpDir = null;
      final String systemTmpDir = MIDletSystemProperties.getSystemProperty("java.io.tmpdir");
      if (systemTmpDir != null) {
        tmpDir = new File(systemTmpDir, "microemulator-apps-" + MIDletSystemProperties.getSystemProperty("user.name"));
        if ((!tmpDir.exists()) && (!tmpDir.mkdirs())) {
          tmpDir = null;
        }
      }
      final File tmp = File.createTempFile("me2-app-", ".jar", tmpDir);
      tmp.deleteOnExit();
      IOUtils.copyToFile(is, tmp);
      return IOUtils.getCanonicalFileClassLoaderURL(tmp);
    }
    catch (final IOException e) {
      if (reportError) {
        Message.error("Unable to open jar " + jarUrl, e);
      }
      return null;
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  private void openMIDletUrl(final String urlString, final MIDletClassLoader midletClassLoader) throws IOException {
    try {
      Common.setStatusBar("Loading...");
      jad.clear();

      if (urlString.toLowerCase().endsWith(".jad")) {
        Logger.debug("openJad", urlString);
        jad = Common.loadJadProperties(urlString);
        loadJar(urlString, jad.getJarURL(), midletClassLoader);
      }
      else {
        jad.setCorrectedJarURL(urlString);
        loadJar(null, urlString, midletClassLoader);
      }

      Config.getUrlsMRU().push(new MidletURLReference(jad.getSuiteName(), urlString));
    }
    catch (final MalformedURLException ex) {
      throw ex;
    }
    catch (final ClassNotFoundException ex) {
      Logger.error(ex);
      throw new IOException(ex.getMessage());
    }
    catch (final FileNotFoundException ex) {
      Message.error("File Not found", urlString, ex);
    }
    catch (final NullPointerException ex) {
      Logger.error("Cannot open jad", urlString, ex);
    }
    catch (final IllegalArgumentException ex) {
      Logger.error("Cannot open jad", urlString, ex);
    }
  }

  private MIDlet loadMidlet(final Class midletClass, final MIDletAccess previousMidletAccess) {
    try {
      if (previousMidletAccess != null) {
        previousMidletAccess.destroyApp(true);
      }
    }
    catch (final Throwable e) {
      Message.error("Unable to destroy MIDlet, " + Message.getCauseMessage(e), e);
    }

    final MIDletContext context = new MIDletContext();
    MIDletBridge.setThreadMIDletContext(context);
    MIDletBridge.getRecordStoreManager().init(MIDletBridge.getMicroEmulator());
    try {
      MIDlet m;

      final String errorTitle = "Error starting MIDlet";

      try {
        final Object object = midletClass.newInstance();
        if (!(object instanceof MIDlet)) {
          Message.error(errorTitle, "Class " + midletClass.getName() + " should extend MIDlet");
          return null;
        }
        m = (MIDlet)object;
      }
      catch (final Throwable e) {
        Message.error(errorTitle, "Unable to create MIDlet, " + Message.getCauseMessage(e), e);
        MIDletBridge.destroyMIDletContext(context);
        return null;
      }

      try {
        if (context.getMIDlet() != m) { throw new Error("MIDlet Context corrupted"); }

        Common.launcher.setCurrentMIDlet(m);
        notifyImplementationMIDletStart();
        return m;
      }
      catch (final Throwable e) {
        Message.error(errorTitle, "Unable to start MIDlet, " + Message.getCauseMessage(e), e);
        MIDletBridge.destroyMIDletContext(context);
        return null;
      }

    }
    finally {
      MIDletBridge.setThreadMIDletContext(null);
    }

  }

  protected void startLauncher(final MIDletContext midletContext) {
    if ((midletContext != null) && (midletContext.isLauncher())) { return; }
    if (midletContext != null) {
      try {
        final MIDletAccess previousMidletAccess = midletContext.getMIDletAccess();
        if (previousMidletAccess != null) {
          previousMidletAccess.destroyApp(true);
        }
      }
      catch (final Throwable e) {
        Logger.error("destroyApp error", e);
      }

      if (exitOnMIDletDestroy) {
        System.exit(0);
      }
    }

    try {
      Common.launcher = new Launcher(this);
      MIDletBridge.getMIDletAccess(Common.launcher).startApp();
      Common.launcher.setCurrentMIDlet(Common.launcher);
    }
    catch (final Throwable e) {
      Message.error("Unable to start launcher MIDlet, " + Message.getCauseMessage(e), e);
      handleStartMidletException(e);
    }
    finally {
      MIDletBridge.setThreadMIDletContext(null);
    }
  }

  public void setStatusBarListener(final StatusBarListener listener) {
    Common.statusBarListener = listener;
  }

  @Override
  public int checkPermission(final String permission) {
    return MIDletSystemProperties.getPermission(permission);
  }

  @Override
  public boolean platformRequest(final String URL) throws ConnectionNotFoundException {
    return emulatorContext.platformRequest(URL);
  }

  public void setResponseInterfaceListener(final ResponseInterfaceListener listener) {
    responseInterfaceListener = listener;
  }

  protected void handleStartMidletException(final Throwable e) {

  }

  /**
   * Show message describing problem with jar if any
   */
  protected boolean describeJarProblem(final URL jarUrl, final MIDletClassLoader midletClassLoader) {
    InputStream is = null;
    JarInputStream jis = null;
    try {
      final String message = "Unable to open jar " + jarUrl;
      URLConnection conn;
      try {
        conn = jarUrl.openConnection();
      }
      catch (final IOException e) {
        Message.error(message, e);
        return true;
      }
      try {
        is = conn.getInputStream();
      }
      catch (final FileNotFoundException e) {
        Message.error("The system cannot find the jar file " + jarUrl, e);
        return true;
      }
      catch (final IOException e) {
        Message.error(message, e);
        return true;
      }
      try {
        jis = new JarInputStream(is);
      }
      catch (final IOException e) {
        Message.error(message, e);
        return true;
      }
      try {
        final JarEntry entry = jis.getNextJarEntry();
        if (entry == null) {
          Message.error("Empty jar " + jarUrl);
          return true;
        }
        // Read till the end
        while (jis.getNextJarEntry() != null) {
          ;
        }
      }
      catch (final ZipException e) {
        Message.error("Problem reading jar " + jarUrl, e);
        return true;
      }
      catch (final IOException e) {
        Message.error("Problem reading jar " + jarUrl, e);
        return true;
      }
      // There seems to be no poblem with jar
      return false;
    }
    finally {
      IOUtils.closeQuietly(jis);
      IOUtils.closeQuietly(is);
    }
  }

  protected void loadJar(final String jadUrl, final String jarUrl, final MIDletClassLoader midletClassLoader) throws ClassNotFoundException {
    if (jarUrl == null) { throw new ClassNotFoundException("Cannot find MIDlet-Jar-URL property in jad"); }
    Logger.debug("openJar", jarUrl);

    // Close Current MIDlet before oppening new one.
    Common.dispose();
    // MIDletBridge.destroyMIDletContext(MIDletBridge.getMIDletContext());
    MIDletBridge.clear();

    setResponseInterface(false);
    try {
      URL url = null;
      try {
        url = new URL(jarUrl);
      }
      catch (final MalformedURLException ex) {
        if (jadUrl != null) {
          try {
            url = new URL(jadUrl.substring(0, jadUrl.lastIndexOf('/') + 1) + jarUrl);
            // TODO check if IOUtils.getCanonicalFileURL is needed
            jad.setCorrectedJarURL(url.toExternalForm());
            Logger.debug("openJar url", url);
          }
          catch (final MalformedURLException ex1) {
            Logger.error("Unable to find jar url", ex1);
            setResponseInterface(true);
            return;
          }
        }
        else {
          Logger.error("Unable to find jar url", ex);
          setResponseInterface(true);
          return;
        }
      }
      // Support Basic Authentication; Copy jar file to tmp directory
      if (url.getUserInfo() != null) {
        final String tmpURL = saveJar2TmpFile(jarUrl, true);
        if (tmpURL == null) { return; }
        try {
          url = new URL(tmpURL);
        }
        catch (final MalformedURLException e) {
          Logger.error("Unable to open tmporary jar url", e);
        }
      }
      midletClassLoader.addURL(url);

      Launcher.removeMIDletEntries();

      // if we get properties via commandline --propertiesjad, then
      // dont bother with any of this
      if (propertiesJad == null) {
        manifest.clear();
        InputStream is = null;
        try {
          is = midletClassLoader.getResourceAsStream("META-INF/MANIFEST.MF");
          if (is == null) {
            if (!describeJarProblem(url, midletClassLoader)) {
              Message.error("Unable to find MANIFEST in MIDlet jar");
            }
            return;
          }
          manifest.read(is);

          final Attributes attributes = manifest.getMainAttributes();
          for (final Object element : attributes.keySet()) {
            final Attributes.Name key = (Attributes.Name)element;
            final String value = (String)attributes.get(key);
            // jad takes precedence over manifest, so only merge
            // in attributes that are not already in the jad
            if (jad.getProperty(key.toString()) == null) {
              jad.getMainAttributes().put(key, value);
            }
          }
        }
        catch (final IOException e) {
          Message.error("Unable to read MANIFEST", e);
        }
        finally {
          IOUtils.closeQuietly(is);
        }
      }
      Launcher.setSuiteName(jad.getSuiteName());

      for (final Enumeration e = jad.getMidletEntries().elements(); e.hasMoreElements();) {
        final JadMidletEntry jadEntry = (JadMidletEntry)e.nextElement();
        final Class midletClass = midletClassLoader.loadClass(jadEntry.getClassName());
        Launcher.addMIDletEntry(new MIDletEntry(jadEntry.getName(), midletClass));
      }
      startLauncher(MIDletBridge.getMIDletContext());
      Common.setStatusBar("");
    }
    finally {
      setResponseInterface(true);
    }
  }

  public Device getDevice() {
    return DeviceFactory.getDevice();
  }

  public void setDevice(final Device device) {
    MIDletSystemProperties.setDevice(device);
    DeviceFactory.setDevice(device);
  }

  private static Common getInstance() {
    return Common.instance;
  }

  public static void setStatusBar(final String text) {
    if (Common.statusBarListener != null) {
      Common.statusBarListener.statusBarChanged(text);
    }
  }

  private void setResponseInterface(final boolean state) {
    if (responseInterfaceListener != null) {
      responseInterfaceListener.stateChanged(state);
    }
  }

  public void registerImplementation(final String implClassName, final Map properties, final boolean notFoundError) {
    final String errorText = "Implementation initialization";
    try {
      final Class implClass = Common.getExtensionsClassLoader().loadClass(implClassName);
      if (ImplementationInitialization.class.isAssignableFrom(implClass)) {
        final Object inst = implClass.newInstance();
        final Map parameters = new HashMap();
        parameters.put(ImplementationInitialization.PARAM_EMULATOR_ID, Config.getEmulatorID());
        if (properties != null) {
          parameters.putAll(properties);
        }
        else {
          final Map extensions = Config.getExtensions();
          final Map prop = (Map)extensions.get(implClassName);
          if (prop != null) {
            parameters.putAll(prop);
          }
        }
        ((ImplementationInitialization)inst).registerImplementation(parameters);
        Logger.debug("implementation registered", implClassName);
        extensions.add(inst);
      }
      else {
        Logger.debug("initialize implementation", implClassName);
        boolean isStatic = true;
        try {
          // Create and object or call static initializer instance();
          final Constructor c = implClass.getConstructor(null);
          if (Modifier.isPublic(c.getModifiers())) {
            isStatic = false;
            implClass.newInstance();
          }
        }
        catch (final NoSuchMethodException e) {
        }

        if (isStatic) {
          try {
            final Method getinst = implClass.getMethod("instance", null);
            if (Modifier.isStatic(getinst.getModifiers())) {
              getinst.invoke(implClass, null);
            }
            else {
              Logger.debug("No known way to initialize implementation class");
            }
          }
          catch (final NoSuchMethodException e) {
            Logger.debug("No known way to initialize implementation class");
          }
          catch (final InvocationTargetException e) {
            Logger.debug("Unable to initialize Implementation", e.getCause());
          }
        }
      }
    }
    catch (final ClassNotFoundException e) {
      if (notFoundError) {
        Logger.error(errorText, e);
      }
      else {
        Logger.warn(errorText + " " + e);
      }
    }
    catch (final InstantiationException e) {
      Logger.error(errorText, e);
    }
    catch (final IllegalAccessException e) {
      Logger.error(errorText, e);
    }
  }

  public void loadImplementationsFromConfig() {
    final Map extensions = Config.getExtensions();
    for (final Iterator iterator = extensions.entrySet().iterator(); iterator.hasNext();) {
      final Map.Entry entry = (Map.Entry)iterator.next();
      registerImplementation((String)entry.getKey(), (Map)entry.getValue(), false);
    }
  }

  public void notifyImplementationMIDletStart() {
    for (final Iterator iterator = extensions.iterator(); iterator.hasNext();) {
      final ImplementationInitialization impl = (ImplementationInitialization)iterator.next();
      impl.notifyMIDletStart();
    }
  }

  public void notifyImplementationMIDletDestroyed() {
    for (final Iterator iterator = extensions.iterator(); iterator.hasNext();) {
      final ImplementationInitialization impl = (ImplementationInitialization)iterator.next();
      impl.notifyMIDletDestroyed();
    }
  }

  public boolean initParams(final List params, final DeviceEntry defaultDevice, final Class defaultDeviceClass) {
    boolean defaultDeviceSelected = false;

    final MIDletClassLoaderConfig clConfig = new MIDletClassLoaderConfig();
    Class deviceClass = null;
    String deviceDescriptorLocation = null;
    int overrideDeviceWidth = -1;
    int overrideDeviceHeight = -1;
    RecordStoreManager paramRecordStoreManager = null;

    final Iterator argsIterator = params.iterator();

    try {
      while (argsIterator.hasNext()) {
        final String arg = (String)argsIterator.next();
        argsIterator.remove();

        if ((arg.equals("--help")) || (arg.equals("-help"))) {
          System.out.println(Common.usage());
          System.exit(0);
        }
        else if (arg.equals("--id")) {
          Config.setEmulatorID((String)argsIterator.next());
          argsIterator.remove();
        }
        else if ((arg.equals("--appclasspath")) || (arg.equals("-appclasspath")) || (arg.equals("-appcp"))) {
          if (clConfig == null) { throw new ConfigurationException("Wrong command line argument order"); }
          clConfig.addAppClassPath((String)argsIterator.next());
          argsIterator.remove();
        }
        else if (arg.equals("--appclass")) {
          if (clConfig == null) { throw new ConfigurationException("Wrong command line argument order"); }
          clConfig.addAppClass((String)argsIterator.next());
          argsIterator.remove();
        }
        else if (arg.startsWith("-Xautotest:")) {
          autoTests = true;
          jadURL = arg.substring("-Xautotest:".length());
        }
        else if (arg.equals("-Xautotest")) {
          autoTests = true;
        }
        else if (arg.equals("--propertiesjad")) {
          final File file = new File((String)argsIterator.next());
          argsIterator.remove();
          propertiesJad = file.exists() ? IOUtils.getCanonicalFileURL(file) : arg;
        }
        else if (arg.equals("--appclassloader")) {
          if (clConfig == null) {
            Message.error("Error", "Wrong command line argument order");
            break;
          }
          clConfig.setDelegationType((String)argsIterator.next());
          argsIterator.remove();
        }
        else if (arg.equals("--usesystemclassloader")) {
          useSystemClassLoader = true;
          clConfig.setDelegationType("system");
        }
        else if (arg.equals("-d") || arg.equals("--device")) {
          if (argsIterator.hasNext()) {
            final String tmpDevice = (String)argsIterator.next();
            argsIterator.remove();
            if (!tmpDevice.toLowerCase().endsWith(".xml")) {
              try {
                deviceClass = Class.forName(tmpDevice);
              }
              catch (final ClassNotFoundException ex) {
              }
            }
            if (deviceClass == null) {
              deviceDescriptorLocation = tmpDevice;
            }
          }
        }
        else if (arg.equals("--resizableDevice")) {
          overrideDeviceWidth = Integer.parseInt((String)argsIterator.next());
          argsIterator.remove();
          overrideDeviceHeight = Integer.parseInt((String)argsIterator.next());
          argsIterator.remove();
          deviceDescriptorLocation = DeviceImpl.RESIZABLE_LOCATION;
        }
        else if (arg.equals("--rms")) {
          if (argsIterator.hasNext()) {
            final String tmpRms = (String)argsIterator.next();
            argsIterator.remove();
            if (tmpRms.equals("file")) {
              paramRecordStoreManager = new FileRecordStoreManager();
            }
            else if (tmpRms.equals("memory")) {
              paramRecordStoreManager = new MemoryRecordStoreManager();
            }
          }
        }
        else if ((arg.equals("--classpath")) || (arg.equals("-classpath")) || (arg.equals("-cp"))) {
          Common.getExtensionsClassLoader().addClasspath((String)argsIterator.next());
          argsIterator.remove();
        }
        else if (arg.equals("--impl")) {
          registerImplementation((String)argsIterator.next(), null, true);
          argsIterator.remove();
        }
        else if (arg.equals("--quit")) {
          exitOnMIDletDestroy = true;
        }
        else if (arg.equals("--logCallLocation")) {
          Logger.setLocationEnabled(Boolean.valueOf((String)argsIterator.next()).booleanValue());
        }
        else if (arg.equals("--traceClassLoading")) {
          MIDletClassLoader.traceClassLoading = true;
        }
        else if (arg.equals("--traceSystemClassLoading")) {
          MIDletClassLoader.traceSystemClassLoading = true;
        }
        else if (arg.equals("--enhanceCatchBlock")) {
          MIDletClassLoader.enhanceCatchBlock = true;
        }
        else if (arg.equals("--quiet")) {
          StdOutAppender.enabled = false;
        }
        else if (arg.equals("--headless")) {
          // Ignore this here.
        }
        else if (arg.startsWith("--")) {
          // Allow to add new arguments in future that are not supported by older version
          Logger.warn("Unknown argument " + arg);
        }
        else {
          midletClassOrUrl = arg;
        }
      }
    }
    catch (final ConfigurationException e) {
      Message.error("Error", e.getMessage(), e);
      return defaultDeviceSelected;
    }

    mIDletClassLoaderConfig = clConfig;

    // TODO registerImplementations by reading jar files in classpath.

    ClassLoader classLoader = Common.getExtensionsClassLoader();
    if (deviceDescriptorLocation != null) {
      try {
        setDevice(DeviceImpl.create(emulatorContext, classLoader, deviceDescriptorLocation, defaultDeviceClass));
        final DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl)DeviceFactory.getDevice().getDeviceDisplay();
        if ((overrideDeviceWidth != -1) && (overrideDeviceHeight != -1)) {
          deviceDisplay.setDisplayRectangle(new Rectangle(0, 0, overrideDeviceWidth, overrideDeviceHeight));
        }
      }
      catch (final IOException ex) {
        Logger.error(ex);
      }
    }
    if (DeviceFactory.getDevice() == null) {
      try {
        if (deviceClass == null) {
          if (defaultDevice.getFileName() != null) {
            final URL[] urls = new URL[1];
            urls[0] = new File(Config.getConfigPath(), defaultDevice.getFileName()).toURI().toURL();
            classLoader = Common.createExtensionsClassLoader(urls);
          }
          setDevice(DeviceImpl.create(emulatorContext, classLoader, defaultDevice.getDescriptorLocation(), defaultDeviceClass));
          defaultDeviceSelected = true;
        }
        else {
          final DeviceImpl device = (DeviceImpl)deviceClass.newInstance();
          device.init(emulatorContext);
          setDevice(device);
        }
      }
      catch (final InstantiationException ex) {
        Logger.error(ex);
      }
      catch (final IllegalAccessException ex) {
        Logger.error(ex);
      }
      catch (final IOException ex) {
        Logger.error(ex);
      }
    }

    try {
      Common.launcher = new Launcher(this);
      Common.launcher.setCurrentMIDlet(Common.launcher);
    }
    finally {
      MIDletBridge.setThreadMIDletContext(null);
    }

    if (getRecordStoreManager() == null) {
      if (paramRecordStoreManager == null) {
        final String className = Config.getRecordStoreManagerClassName();
        if (className != null) {
          try {
            final Class clazz = Class.forName(className);
            setRecordStoreManager((RecordStoreManager)clazz.newInstance());
          }
          catch (final ClassNotFoundException ex) {
            Logger.error(ex);
          }
          catch (final InstantiationException ex) {
            Logger.error(ex);
          }
          catch (final IllegalAccessException ex) {
            Logger.error(ex);
          }
        }
        if (getRecordStoreManager() == null) {
          setRecordStoreManager(new FileRecordStoreManager());
        }
      }
      else {
        setRecordStoreManager(paramRecordStoreManager);
      }
    }

    return defaultDeviceSelected;
  }

  private static ExtensionsClassLoader getExtensionsClassLoader() {
    if (Common.instance.extensionsClassLoader == null) {
      Common.instance.extensionsClassLoader = new ExtensionsClassLoader(new URL[] {}, Common.instance.getClass().getClassLoader());
    }
    return Common.instance.extensionsClassLoader;
  }

  private MIDletClassLoader createMIDletClassLoader(final boolean forJad) {
    final MIDletClassLoader mcl = new MIDletClassLoader(Common.getExtensionsClassLoader());
    if (!Serializable.class.isAssignableFrom(Injected.class)) {
      Logger.error("classpath configuration error, Wrong Injected class detected. microemu-injected module should be after microemu-javase in eclipse");
    }
    if (mIDletClassLoaderConfig != null) {
      try {
        mcl.configure(mIDletClassLoaderConfig, forJad);
      }
      catch (final MalformedURLException e) {
        Message.error("Error", "Unable to find MIDlet classes, " + Message.getCauseMessage(e), e);
      }
    }
    mcl.disableClassPreporcessing(Injected.class);
    mcl.disableClassPreporcessing(MIDletThread.class);
    mcl.disableClassPreporcessing(MIDletTimer.class);
    mcl.disableClassPreporcessing(MIDletTimerTask.class);
    MIDletResourceLoader.classLoader = mcl;
    return mcl;
  }

  public static ClassLoader createExtensionsClassLoader(final URL[] urls) {
    return new ExtensionsClassLoader(urls, Common.getExtensionsClassLoader());
  }

  private static JadProperties loadJadProperties(final String urlString) throws IOException {
    final JadProperties properties = new JadProperties();

    final URL url = new URL(urlString);
    if (url.getUserInfo() == null) {
      properties.read(url.openStream());
    }
    else {
      final URLConnection cn = url.openConnection();
      final String userInfo = new String(Base64Coder.encode(url.getUserInfo().getBytes("UTF-8")));
      cn.setRequestProperty("Authorization", "Basic " + userInfo);
      properties.read(cn.getInputStream());
    }

    return properties;
  }

  @Override
  public MIDlet initMIDlet(final boolean startMidlet) {
    Class midletClass = null;

    if ((midletClassOrUrl != null) && Common.isMIDletUrlExtension(midletClassOrUrl)) {
      try {
        final File file = new File(midletClassOrUrl);
        final String url = file.exists() ? IOUtils.getCanonicalFileURL(file) : midletClassOrUrl;
        openMIDletUrl(url);
      }
      catch (final IOException exception) {
        Logger.error("Cannot load " + midletClassOrUrl + " URL", exception);
      }
    }
    else if (midletClassOrUrl != null) {
      useSystemClassLoader = mIDletClassLoaderConfig.isClassLoaderDisabled();
      if (!useSystemClassLoader) {
        final MIDletClassLoader classLoader = createMIDletClassLoader(false);
        try {
          classLoader.addClassURL(midletClassOrUrl);
          midletClass = classLoader.loadClass(midletClassOrUrl);
        }
        catch (final MalformedURLException e) {
          Message.error("Error", "Unable to find MIDlet class, " + Message.getCauseMessage(e), e);
          return null;
        }
        catch (final NoClassDefFoundError e) {
          Message.error("Error", "Unable to find MIDlet class, " + Message.getCauseMessage(e), e);
          return null;
        }
        catch (final ClassNotFoundException e) {
          Message.error("Error", "Unable to find MIDlet class, " + Message.getCauseMessage(e), e);
          return null;
        }
      }
      else {
        try {
          midletClass = Common.instance.getClass().getClassLoader().loadClass(midletClassOrUrl);
        }
        catch (final ClassNotFoundException e) {
          Message.error("Error", "Unable to find MIDlet class, " + Message.getCauseMessage(e), e);
          return null;
        }
      }
    }

    MIDlet midlet = null;

    if (autoTests) {
      if (jadURL != null) {
        runAutoTests(jadURL, true);
      }
    }
    else {

      if ((midletClass != null) && (propertiesJad != null)) {
        try {
          jad = Common.loadJadProperties(propertiesJad);
        }
        catch (final IOException e) {
          Logger.error("Cannot load " + propertiesJad + " URL", e);
        }
      }

      if (midletClass == null) {
        final MIDletEntry entry = Common.launcher.getSelectedMidletEntry();
        if (entry != null) {
          midlet = loadMidlet(entry.getMIDletClass(), MIDletBridge.getMIDletAccess());
          if (startMidlet) {
            try {
              MIDletBridge.getMIDletAccess(midlet).startApp();
            }
            catch (final MIDletStateChangeException e) {
              Logger.error(e);
            }
          }
        }
      }
      else {
        midlet = loadMidlet(midletClass, MIDletBridge.getMIDletAccess());
        if (startMidlet) {
          try {
            MIDletBridge.getMIDletAccess(midlet).startApp();
          }
          catch (final MIDletStateChangeException e) {
            Logger.error(e);
          }
        }
      }
      if (midlet == null) {
        startLauncher(MIDletBridge.getMIDletContext());
      }
    }

    return midlet;
  }

  public static String usage() {
    return "[(-d | --device) ({device descriptor} | {device class name}) ] \n" + "[--rms (file | memory)] \n" + "[--id EmulatorID ] \n"
        + "[--impl {JSR implementation class name}]\n" + "[(--classpath|-cp) <JSR CLASSPATH>]\n" + "[(--appclasspath|--appcp) <MIDlet CLASSPATH>]\n"
        + "[--appclass <library class name>]\n" + "[--appclassloader strict|relaxed|delegating|system] \n" + "[-Xautotest:<JAD file url>\n"
        + "[--quit]\n" + "[--logCallLocation true|false]\n" + "[--traceClassLoading\n[--traceSystemClassLoading]\n[--enhanceCatchBlock]\n]"
        + "[--resizableDevice {width} {height}]\n"
        + "(({MIDlet class name} [--propertiesjad {jad file location}]) | {jad file location} | {jar file location})";
  }

}
