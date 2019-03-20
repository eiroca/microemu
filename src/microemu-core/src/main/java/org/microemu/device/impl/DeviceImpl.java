/**
 * MicroEmulator Copyright (C) 2002-2005 Bartek Teodorczyk <barteo@barteo.net>
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

package org.microemu.device.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;
import org.microemu.app.util.IOUtils;
import org.microemu.device.Device;
import org.microemu.device.DeviceDisplay;
import org.microemu.device.EmulatorContext;
import org.microemu.device.FontManager;
import org.microemu.device.InputMethod;
import nanoxml.XMLElement;
import nanoxml.XMLParseException;

public abstract class DeviceImpl implements Device {

  private String name;

  private static EmulatorContext context;

  private Image normalImage;

  private Image overImage;

  private Image pressedImage;

  private final Vector buttons;

  private final Vector softButtons;

  private boolean hasPointerEvents;

  private boolean hasPointerMotionEvents;

  // TODO not implemented yet
  private boolean hasRepeatEvents;

  private final Map systemProperties;

  private int skinVersion;

  public static final String DEFAULT_LOCATION = "org/microemu/device/default/device.xml";

  public static final String RESIZABLE_LOCATION = "org/microemu/device/resizable/device.xml";

  /**
   * @deprecated
   */
  @Deprecated
  private String descriptorLocation;

  private static Map specialInheritanceAttributeSet;

  public DeviceImpl() {
    // Permits null values.
    systemProperties = new HashMap();
    buttons = new Vector();
    softButtons = new Vector();
  }

  public static DeviceImpl create(final EmulatorContext context, final ClassLoader classLoader, final String descriptorLocation, final Class defaultDeviceClass) throws IOException {
    final XMLElement doc = DeviceImpl.loadDeviceDescriptor(classLoader, descriptorLocation);
    // saveDevice(doc);
    DeviceImpl device = null;
    for (final Enumeration e = doc.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement tmp = (XMLElement)e.nextElement();
      if (tmp.getName().equals("class-name")) {
        try {
          final Class deviceClass = Class.forName(tmp.getContent(), true, classLoader);
          device = (DeviceImpl)deviceClass.newInstance();
        }
        catch (final ClassNotFoundException ex) {
          throw new IOException(ex.getMessage());
        }
        catch (final InstantiationException ex) {
          throw new IOException(ex.getMessage());
        }
        catch (final IllegalAccessException ex) {
          throw new IOException(ex.getMessage());
        }
        break;
      }
    }

    if (device == null) {
      try {
        device = (DeviceImpl)defaultDeviceClass.newInstance();
      }
      catch (final InstantiationException ex) {
        throw new IOException(ex.getMessage());
      }
      catch (final IllegalAccessException ex) {
        throw new IOException(ex.getMessage());
      }
    }
    DeviceImpl.context = context;
    device.descriptorLocation = descriptorLocation;
    device.loadConfig(classLoader, DeviceImpl.besourceBase(descriptorLocation), doc);

    return device;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#init()
   */
  @Override
  public void init() {

  }

  /**
   * @deprecated use Device.create(EmulatorContext context, ClassLoader classLoader, String
   *             descriptorLocation);
   */
  @Deprecated
  public void init(final EmulatorContext context) {
    init(context, DeviceImpl.DEFAULT_LOCATION);
  }

  /**
   * @deprecated use Device.create(EmulatorContext context, ClassLoader classLoader, String
   *             descriptorLocation);
   */
  @Deprecated
  public void init(final EmulatorContext context, final String descriptorLocation) {
    DeviceImpl.context = context;
    if (descriptorLocation.startsWith("/")) {
      this.descriptorLocation = descriptorLocation.substring(1);
    }
    else {
      this.descriptorLocation = descriptorLocation;
    }

    try {
      final String base = descriptorLocation.substring(0, descriptorLocation.lastIndexOf("/"));
      final XMLElement doc = DeviceImpl.loadDeviceDescriptor(getClass().getClassLoader(), descriptorLocation);
      loadConfig(getClass().getClassLoader(), base, doc);
    }
    catch (final IOException ex) {
      System.out.println("Cannot load config: " + ex);
    }
  }

  /**
   * @deprecated
   */
  @Deprecated
  public String getDescriptorLocation() {
    return descriptorLocation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#destroy()
   */
  @Override
  public void destroy() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  public static EmulatorContext getEmulatorContext() {
    return DeviceImpl.context;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getInputMethod()
   */
  @Override
  public InputMethod getInputMethod() {
    return DeviceImpl.context.getDeviceInputMethod();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getFontManager()
   */
  @Override
  public FontManager getFontManager() {
    return DeviceImpl.context.getDeviceFontManager();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getDeviceDisplay()
   */
  @Override
  public DeviceDisplay getDeviceDisplay() {
    return DeviceImpl.context.getDeviceDisplay();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getNormalImage()
   */
  @Override
  public Image getNormalImage() {
    return normalImage;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getOverImage()
   */
  @Override
  public Image getOverImage() {
    return overImage;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getPressedImage()
   */
  @Override
  public Image getPressedImage() {
    return pressedImage;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getSoftButtons()
   */
  @Override
  public Vector getSoftButtons() {
    return softButtons;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getButtons()
   */
  @Override
  public Vector getButtons() {
    return buttons;
  }

  protected void loadConfig(final ClassLoader classLoader, final String base, final XMLElement doc) throws IOException {
    final String deviceName = doc.getStringAttribute("name");
    if (deviceName != null) {
      name = deviceName;
    }
    else {
      name = base;
    }

    loadSkinVersion(doc);

    hasPointerEvents = false;
    hasPointerMotionEvents = false;
    hasRepeatEvents = false;

    ((FontManagerImpl)getFontManager()).setAntialiasing(false);

    /*
     * parseDisplay have to be performed first to check if device display
     * has resizable flag set, parseInput skips rectangle or polygon element
     * then, also normalImage, overImage and pressedImage aren't needed
     */
    parseDisplay(classLoader, base, doc.getChild("display"));

    for (final Enumeration e = doc.enumerateChildren(); e.hasMoreElements();) {
      final XMLElement tmp = (XMLElement)e.nextElement();
      if (tmp.getName().equals("system-properties")) {
        parseSystemProperties(tmp);
      }
      else if (tmp.getName().equals("img") && !((DeviceDisplayImpl)getDeviceDisplay()).isResizable()) {
        try {
          if (tmp.getStringAttribute("name").equals("normal")) {
            normalImage = loadImage(classLoader, base, tmp.getStringAttribute("src"));
          }
          else if (tmp.getStringAttribute("name").equals("over")) {
            overImage = loadImage(classLoader, base, tmp.getStringAttribute("src"));
          }
          else if (tmp.getStringAttribute("name").equals("pressed")) {
            pressedImage = loadImage(classLoader, base, tmp.getStringAttribute("src"));
          }
        }
        catch (final IOException ex) {
          System.out.println("Cannot load " + tmp.getStringAttribute("src"));
          return;
        }
      }
      else if (tmp.getName().equals("fonts")) {
        parseFonts(classLoader, base, tmp);
      }
      else if (tmp.getName().equals("input") || tmp.getName().equals("keyboard")) {
        // "keyboard" is for backward compatibility
        parseInput(tmp);
      }
    }
  }

  private void loadSkinVersion(final XMLElement doc) {
    final String xmlns = doc.getStringAttribute("xmlns");
    if (xmlns == null) {
      skinVersion = 20000;
    }
    else {
      if (xmlns.endsWith("/2.0.2/")) {
        skinVersion = 20002;
      }
      else {
        skinVersion = 20000;
      }
    }
  }

  private void parseDisplay(final ClassLoader classLoader, final String base, final XMLElement tmp) throws IOException {
    final DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl)getDeviceDisplay();

    final String resizable = tmp.getStringAttribute("resizable", "false");
    if (resizable.equalsIgnoreCase("true")) {
      deviceDisplay.setResizable(true);
    }
    else {
      deviceDisplay.setResizable(false);
    }

    for (final Enumeration e_display = tmp.enumerateChildren(); e_display.hasMoreElements();) {
      final XMLElement tmp_display = (XMLElement)e_display.nextElement();
      if (tmp_display.getName().equals("numcolors")) {
        deviceDisplay.setNumColors(Integer.parseInt(tmp_display.getContent()));
      }
      else if (tmp_display.getName().equals("iscolor")) {
        deviceDisplay.setIsColor(parseBoolean(tmp_display.getContent()));
      }
      else if (tmp_display.getName().equals("numalphalevels")) {
        deviceDisplay.setNumAlphaLevels(Integer.parseInt(tmp_display.getContent()));
      }
      else if (tmp_display.getName().equals("background")) {
        deviceDisplay.setBackgroundColor(new Color(Integer.parseInt(tmp_display.getContent(), 16)));
      }
      else if (tmp_display.getName().equals("foreground")) {
        deviceDisplay.setForegroundColor(new Color(Integer.parseInt(tmp_display.getContent(), 16)));
      }
      else if (tmp_display.getName().equals("rectangle")) {
        final Rectangle rect = getRectangle(tmp_display);
        if (deviceDisplay.isResizable()) {
          rect.x = 0;
          rect.y = 0;
        }
        deviceDisplay.setDisplayRectangle(rect);
      }
      else if (tmp_display.getName().equals("paintable")) {
        deviceDisplay.setDisplayPaintable(getRectangle(tmp_display));
      }
    }
    for (final Enumeration e_display = tmp.enumerateChildren(); e_display.hasMoreElements();) {
      final XMLElement tmp_display = (XMLElement)e_display.nextElement();
      if (tmp_display.getName().equals("img")) {
        if (tmp_display.getStringAttribute("name").equals("up")
            || tmp_display.getStringAttribute("name").equals("down")) {
          // deprecated, moved to icon
          final SoftButton icon = deviceDisplay.createSoftButton(skinVersion, tmp_display
              .getStringAttribute("name"), getRectangle(tmp_display.getChild("paintable")),
              loadImage(
                  classLoader, base, tmp_display.getStringAttribute("src")),
              loadImage(classLoader, base,
                  tmp_display.getStringAttribute("src")));
          getSoftButtons().addElement(icon);
        }
        else if (tmp_display.getStringAttribute("name").equals("mode")) {
          // deprecated, moved to status
          if (tmp_display.getStringAttribute("type").equals("123")) {
            deviceDisplay.setMode123Image(new PositionedImage(loadImage(classLoader, base, tmp_display
                .getStringAttribute("src")), getRectangle(tmp_display.getChild("paintable"))));
          }
          else if (tmp_display.getStringAttribute("type").equals("abc")) {
            deviceDisplay.setModeAbcLowerImage(new PositionedImage(loadImage(classLoader, base, tmp_display
                .getStringAttribute("src")), getRectangle(tmp_display.getChild("paintable"))));
          }
          else if (tmp_display.getStringAttribute("type").equals("ABC")) {
            deviceDisplay.setModeAbcUpperImage(new PositionedImage(loadImage(classLoader, base, tmp_display
                .getStringAttribute("src")), getRectangle(tmp_display.getChild("paintable"))));
          }
        }
      }
      else if (tmp_display.getName().equals("icon")) {
        Image iconNormalImage = null;
        Image iconPressedImage = null;
        for (final Enumeration e_icon = tmp_display.enumerateChildren(); e_icon.hasMoreElements();) {
          final XMLElement tmp_icon = (XMLElement)e_icon.nextElement();
          if (tmp_icon.getName().equals("img")) {
            if (tmp_icon.getStringAttribute("name").equals("normal")) {
              iconNormalImage = loadImage(classLoader, base, tmp_icon.getStringAttribute("src"));
            }
            else if (tmp_icon.getStringAttribute("name").equals("pressed")) {
              iconPressedImage = loadImage(classLoader, base, tmp_icon.getStringAttribute("src"));
            }
          }
        }
        final SoftButton icon = deviceDisplay.createSoftButton(skinVersion, tmp_display.getStringAttribute("name"),
            getRectangle(tmp_display.getChild("paintable")), iconNormalImage, iconPressedImage);
        getSoftButtons().addElement(icon);
      }
      else if (tmp_display.getName().equals("status")) {
        if (tmp_display.getStringAttribute("name").equals("input")) {
          final Rectangle paintable = getRectangle(tmp_display.getChild("paintable"));
          for (final Enumeration e_status = tmp_display.enumerateChildren(); e_status.hasMoreElements();) {
            final XMLElement tmp_status = (XMLElement)e_status.nextElement();
            if (tmp_status.getName().equals("img")) {
              if (tmp_status.getStringAttribute("name").equals("123")) {
                deviceDisplay.setMode123Image(new PositionedImage(loadImage(classLoader, base,
                    tmp_status.getStringAttribute("src")), paintable));
              }
              else if (tmp_status.getStringAttribute("name").equals("abc")) {
                deviceDisplay.setModeAbcLowerImage(new PositionedImage(loadImage(classLoader, base,
                    tmp_status.getStringAttribute("src")), paintable));
              }
              else if (tmp_status.getStringAttribute("name").equals("ABC")) {
                deviceDisplay.setModeAbcUpperImage(new PositionedImage(loadImage(classLoader, base,
                    tmp_status.getStringAttribute("src")), paintable));
              }
            }
          }
        }
      }
    }
  }

  private void parseFonts(final ClassLoader classLoader, final String base, final XMLElement tmp) throws IOException {
    final FontManagerImpl fontManager = (FontManagerImpl)getFontManager();

    final String hint = tmp.getStringAttribute("hint");
    boolean antialiasing = false;
    if ((hint != null) && hint.equals("antialiasing")) {
      antialiasing = true;
    }
    fontManager.setAntialiasing(antialiasing);

    for (final Enumeration e_fonts = tmp.enumerateChildren(); e_fonts.hasMoreElements();) {
      final XMLElement tmp_font = (XMLElement)e_fonts.nextElement();
      if (tmp_font.getName().equals("font")) {
        String face = tmp_font.getStringAttribute("face").toLowerCase();
        String style = tmp_font.getStringAttribute("style").toLowerCase();
        String size = tmp_font.getStringAttribute("size").toLowerCase();

        if (face.startsWith("face_")) {
          face = face.substring("face_".length());
        }
        if (style.startsWith("style_")) {
          style = style.substring("style_".length());
        }
        if (size.startsWith("size_")) {
          size = size.substring("size_".length());
        }

        for (final Enumeration e_defs = tmp_font.enumerateChildren(); e_defs.hasMoreElements();) {
          final XMLElement tmp_def = (XMLElement)e_defs.nextElement();
          if (tmp_def.getName().equals("system")) {
            final String defName = tmp_def.getStringAttribute("name");
            final String defStyle = tmp_def.getStringAttribute("style");
            final int defSize = Integer.parseInt(tmp_def.getStringAttribute("size"));

            fontManager.setFont(face, style, size, fontManager.createSystemFont(defName, defStyle, defSize,
                antialiasing));
          }
          else if (tmp_def.getName().equals("ttf")) {
            final String defSrc = tmp_def.getStringAttribute("src");
            final String defStyle = tmp_def.getStringAttribute("style");
            final int defSize = Integer.parseInt(tmp_def.getStringAttribute("size"));

            fontManager.setFont(face, style, size, fontManager.createTrueTypeFont(getResourceUrl(
                classLoader, base, defSrc), defStyle, defSize, antialiasing));
          }
        }
      }
    }
  }

  private void parseInput(final XMLElement tmp) {
    final DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl)getDeviceDisplay();
    final boolean resizable = deviceDisplay.isResizable();

    for (final Enumeration e_keyboard = tmp.enumerateChildren(); e_keyboard.hasMoreElements();) {
      final XMLElement tmp_keyboard = (XMLElement)e_keyboard.nextElement();
      if (tmp_keyboard.getName().equals("haspointerevents")) {
        hasPointerEvents = parseBoolean(tmp_keyboard.getContent());
      }
      else if (tmp_keyboard.getName().equals("haspointermotionevents")) {
        hasPointerMotionEvents = parseBoolean(tmp_keyboard.getContent());
      }
      else if (tmp_keyboard.getName().equals("hasrepeatevents")) {
        hasRepeatEvents = parseBoolean(tmp_keyboard.getContent());
      }
      else if (tmp_keyboard.getName().equals("button")) {
        Shape shape = null;
        final Hashtable inputToChars = new Hashtable();
        for (final Enumeration e_button = tmp_keyboard.enumerateChildren(); e_button.hasMoreElements();) {
          final XMLElement tmp_button = (XMLElement)e_button.nextElement();
          if (tmp_button.getName().equals("chars")) {
            final String input = tmp_button.getStringAttribute("input", "common");
            final Vector stringArray = new Vector();
            for (final Enumeration e_chars = tmp_button.enumerateChildren(); e_chars.hasMoreElements();) {
              final XMLElement tmp_chars = (XMLElement)e_chars.nextElement();
              if (tmp_chars.getName().equals("char")) {
                stringArray.addElement(tmp_chars.getContent());
              }
            }
            final char[] charArray = new char[stringArray.size()];
            for (int i = 0; i < stringArray.size(); i++) {
              final String str = (String)stringArray.elementAt(i);
              if (str.length() > 0) {
                charArray[i] = str.charAt(0);
              }
              else {
                charArray[i] = ' ';
              }
            }
            inputToChars.put(input, charArray);
          }
          else if (tmp_button.getName().equals("rectangle") && !resizable) {
            shape = getRectangle(tmp_button);
          }
          else if (tmp_button.getName().equals("polygon") && !resizable) {
            shape = getPolygon(tmp_button);
          }
        }
        final int keyCode = tmp_keyboard.getIntAttribute("keyCode", Integer.MIN_VALUE);
        getButtons().addElement(
            deviceDisplay.createButton(skinVersion, tmp_keyboard.getStringAttribute("name"), shape,
                keyCode, tmp_keyboard.getStringAttribute("key"), tmp_keyboard
                    .getStringAttribute("keyboardChars"),
                inputToChars, tmp_keyboard
                    .getBooleanAttribute("modeChange", false)));
      }
      else if (tmp_keyboard.getName().equals("softbutton")) {
        final Vector commands = new Vector();
        Shape shape = null;
        Rectangle paintable = null;
        Font font = null;
        for (final Enumeration e_button = tmp_keyboard.enumerateChildren(); e_button.hasMoreElements();) {
          final XMLElement tmp_button = (XMLElement)e_button.nextElement();
          if (tmp_button.getName().equals("rectangle") && !resizable) {
            shape = getRectangle(tmp_button);
          }
          else if (tmp_button.getName().equals("polygon") && !resizable) {
            shape = getPolygon(tmp_button);
          }
          else if (tmp_button.getName().equals("paintable")) {
            paintable = getRectangle(tmp_button);
          }
          else if (tmp_button.getName().equals("command")) {
            commands.addElement(tmp_button.getContent());
          }
          else if (tmp_button.getName().equals("font")) {
            font = DeviceImpl.getFont(tmp_button.getStringAttribute("face"), tmp_button.getStringAttribute("style"),
                tmp_button.getStringAttribute("size"));
          }
        }
        final int keyCode = tmp_keyboard.getIntAttribute("keyCode", Integer.MIN_VALUE);
        final SoftButton button = deviceDisplay.createSoftButton(skinVersion,
            tmp_keyboard.getStringAttribute("name"), shape, keyCode,
            tmp_keyboard.getStringAttribute("key"), paintable,
            tmp_keyboard.getStringAttribute("alignment"), commands, font);
        getButtons().addElement(button);
        getSoftButtons().addElement(button);
      }
    }
  }

  private void parseSystemProperties(final XMLElement tmp) {
    for (final Enumeration e_prop = tmp.enumerateChildren(); e_prop.hasMoreElements();) {
      final XMLElement tmp_prop = (XMLElement)e_prop.nextElement();
      if (tmp_prop.getName().equals("system-property")) {
        systemProperties.put(tmp_prop.getStringAttribute("name"), tmp_prop.getStringAttribute("value"));
      }
    }
  }

  private static Font getFont(final String face, final String style, final String size) {
    int meFace = 0;
    if (face.equalsIgnoreCase("system")) {
      meFace |= Font.FACE_SYSTEM;
    }
    else if (face.equalsIgnoreCase("monospace")) {
      meFace |= Font.FACE_MONOSPACE;
    }
    else if (face.equalsIgnoreCase("proportional")) {
      meFace |= Font.FACE_PROPORTIONAL;
    }

    int meStyle = 0;
    final String testStyle = style.toLowerCase();
    if (testStyle.indexOf("plain") != -1) {
      meStyle |= Font.STYLE_PLAIN;
    }
    if (testStyle.indexOf("bold") != -1) {
      meStyle |= Font.STYLE_BOLD;
    }
    if (testStyle.indexOf("italic") != -1) {
      meStyle |= Font.STYLE_ITALIC;
    }
    if (testStyle.indexOf("underlined") != -1) {
      meStyle |= Font.STYLE_UNDERLINED;
    }

    int meSize = 0;
    if (size.equalsIgnoreCase("small")) {
      meSize |= Font.SIZE_SMALL;
    }
    else if (size.equalsIgnoreCase("medium")) {
      meSize |= Font.SIZE_MEDIUM;
    }
    else if (size.equalsIgnoreCase("large")) {
      meSize |= Font.SIZE_LARGE;
    }

    return Font.getFont(meFace, meStyle, meSize);
  }

  private Rectangle getRectangle(final XMLElement source) {
    final Rectangle rect = new Rectangle();

    for (final Enumeration e_rectangle = source.enumerateChildren(); e_rectangle.hasMoreElements();) {
      final XMLElement tmp_rectangle = (XMLElement)e_rectangle.nextElement();
      if (tmp_rectangle.getName().equals("x")) {
        rect.x = Integer.parseInt(tmp_rectangle.getContent());
      }
      else if (tmp_rectangle.getName().equals("y")) {
        rect.y = Integer.parseInt(tmp_rectangle.getContent());
      }
      else if (tmp_rectangle.getName().equals("width")) {
        rect.width = Integer.parseInt(tmp_rectangle.getContent());
      }
      else if (tmp_rectangle.getName().equals("height")) {
        rect.height = Integer.parseInt(tmp_rectangle.getContent());
      }
    }

    return rect;
  }

  private Polygon getPolygon(final XMLElement source) {
    final Polygon poly = new Polygon();

    for (final Enumeration e_poly = source.enumerateChildren(); e_poly.hasMoreElements();) {
      final XMLElement tmp_point = (XMLElement)e_poly.nextElement();
      if (tmp_point.getName().equals("point")) {
        poly.addPoint(Integer.parseInt(tmp_point.getStringAttribute("x")), Integer.parseInt(tmp_point
            .getStringAttribute("y")));
      }
    }

    return poly;
  }

  private boolean parseBoolean(final String value) {
    if (value.toLowerCase().equals(new String("true").toLowerCase())) {
      return true;
    }
    else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#hasPointerEvents()
   */
  @Override
  public boolean hasPointerEvents() {
    return hasPointerEvents;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#hasPointerMotionEvents()
   */
  @Override
  public boolean hasPointerMotionEvents() {
    return hasPointerMotionEvents;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#hasRepeatEvents()
   */
  @Override
  public boolean hasRepeatEvents() {
    return hasRepeatEvents;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#hasRepeatEvents()
   */
  @Override
  public boolean vibrate(final int duration) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.microemu.device.DeviceA#getSystemProperties()
   */
  @Override
  public Map getSystemProperties() {
    return systemProperties;
  }

  private static void saveDevice(final XMLElement doc) {
    final File configFile = new File(".", "device-tmp.xml");
    FileWriter fw = null;
    try {
      fw = new FileWriter(configFile);
      doc.write(fw);
      fw.close();
    }
    catch (final IOException ex) {
      System.out.println(ex);
    }
    finally {
      IOUtils.closeQuietly(fw);
    }
  }

  private static XMLElement loadDeviceDescriptor(final ClassLoader classLoader, final String descriptorLocation)
      throws IOException {
    final InputStream descriptor = classLoader.getResourceAsStream(descriptorLocation);
    if (descriptor == null) { throw new IOException("Cannot find descriptor at: " + descriptorLocation); }
    XMLElement doc;
    try {
      doc = DeviceImpl.loadXmlDocument(descriptor);
    }
    finally {
      IOUtils.closeQuietly(descriptor);
    }

    final String parent = doc.getStringAttribute("extends");
    if (parent != null) { return DeviceImpl.inheritXML(DeviceImpl.loadDeviceDescriptor(classLoader, DeviceImpl.expandResourcePath(DeviceImpl.besourceBase(descriptorLocation),
        parent)), doc, "/"); }
    return doc;
  }

  private static void inheritanceConstInit() {
    if (DeviceImpl.specialInheritanceAttributeSet == null) {
      DeviceImpl.specialInheritanceAttributeSet = new Hashtable();
      DeviceImpl.specialInheritanceAttributeSet.put("//FONTS/FONT", new String[] {
          "face", "style", "size"
      });
    }
  }

  /**
   * Very simple xml inheritance for devices.
   */
  static XMLElement inheritXML(final XMLElement parent, final XMLElement child, final String parentName) {
    DeviceImpl.inheritanceConstInit();
    if (parent == null) { return child; }
    parent.setContent(child.getContent());
    for (final Enumeration ena = child.enumerateAttributeNames(); ena.hasMoreElements();) {
      final String key = (String)ena.nextElement();
      parent.setAttribute(key, child.getAttribute(key));
    }
    for (final Enumeration enc = child.enumerateChildren(); enc.hasMoreElements();) {
      final XMLElement c = (XMLElement)enc.nextElement();
      final String fullName = (parentName + "/" + c.getName()).toUpperCase(Locale.ENGLISH);
      // System.out.println("processing [" + fullName + "]");
      boolean inheritWithName = false;
      if (c.getStringAttribute("name") != null) {
        inheritWithName = true;
      }
      else {
        // Find if element has siblings
        inheritWithName = ((child.getChildCount(c.getName()) > 1) || (parent.getChildCount(c.getName()) > 1));
      }
      XMLElement p;
      if (inheritWithName) {
        final String[] equalAttributes = (String[])DeviceImpl.specialInheritanceAttributeSet.get(fullName);
        if (equalAttributes != null) {
          p = parent.getChild(c.getName(), c.getStringAttributes(equalAttributes));
        }
        else {
          p = parent.getChild(c.getName(), c.getStringAttribute("name"));
        }
      }
      else {
        p = parent.getChild(c.getName());
      }
      final boolean inheritOverride = c.getBooleanAttribute("override", false);
      if (inheritOverride) {
        // System.out.println("inheritXML override parent:" +
        // parent.toString());
        // System.out.println("inheritXML override c :" + c.toString());
        // System.out.println("inheritXML override p :" + p.toString());
        c.removeAttribute("override");
        if (p != null) {
          parent.removeChild(p);
          p = null;
        }
      }
      // System.out.println("inheritXML " + c.getName());
      if (p == null) {
        parent.addChild(c);
      }
      else {
        DeviceImpl.inheritXML(p, c, fullName);
      }
    }
    return parent;
  }

  private static XMLElement loadXmlDocument(final InputStream descriptor) throws IOException {
    final XMLElement doc = new XMLElement();
    try {
      doc.parse(descriptor, 1);
    }
    catch (final XMLParseException ex) {
      throw new IOException(ex.toString());
    }
    return doc;
  }

  private static String besourceBase(final String descriptorLocation) {
    return descriptorLocation.substring(0, descriptorLocation.lastIndexOf("/"));
  }

  private static String expandResourcePath(final String base, final String src) throws IOException {
    String expandedSource;
    if (src.startsWith("/")) {
      expandedSource = src;
    }
    else {
      expandedSource = base + "/" + src;
    }
    if (expandedSource.startsWith("/")) {
      expandedSource = expandedSource.substring(1);
    }
    return expandedSource;
  }

  private URL getResourceUrl(final ClassLoader classLoader, final String base, final String src) throws IOException {
    final String expandedSource = DeviceImpl.expandResourcePath(base, src);

    final URL result = classLoader.getResource(expandedSource);

    if (result == null) { throw new IOException("Cannot find resource: " + expandedSource); }

    return result;
  }

  private Image loadImage(final ClassLoader classLoader, final String base, final String src) throws IOException {
    final URL url = getResourceUrl(classLoader, base, src);

    return ((DeviceDisplayImpl)getDeviceDisplay()).createSystemImage(url);
  }

}
