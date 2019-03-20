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
 *
 * Contributor(s): Andres Navarro
 */

package org.microemu.device.j2se;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;
import org.microemu.DisplayAccess;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.app.ui.swing.SwingDisplayComponent;
import org.microemu.app.util.IOUtils;
import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.EmulatorContext;
import org.microemu.device.InputMethod;
import org.microemu.device.impl.Button;
import org.microemu.device.impl.Color;
import org.microemu.device.impl.DeviceDisplayImpl;
import org.microemu.device.impl.PositionedImage;
import org.microemu.device.impl.Rectangle;
import org.microemu.device.impl.Shape;
import org.microemu.device.impl.SoftButton;

public class J2SEDeviceDisplay implements DeviceDisplayImpl {

  EmulatorContext context;

  Rectangle displayRectangle;

  Rectangle displayPaintable;

  boolean isColor;

  int numColors;

  int numAlphaLevels;

  java.awt.Color backgroundColor;

  java.awt.Color foregroundColor;

  PositionedImage mode123Image;

  PositionedImage modeAbcUpperImage;

  PositionedImage modeAbcLowerImage;

  Image gameCanvasImage = null;

  javax.microedition.lcdui.Graphics gameCanvasGraphics;

  boolean resizable;

  public J2SEDeviceDisplay(final EmulatorContext context) {
    this.context = context;
  }

  @Override
  public int getHeight() {
    return displayPaintable.height;
  }

  @Override
  public int getWidth() {
    return displayPaintable.width;
  }

  @Override
  public int getFullHeight() {
    return displayRectangle.height;
  }

  @Override
  public int getFullWidth() {
    return displayRectangle.width;
  }

  @Override
  public boolean isColor() {
    return isColor;
  }

  @Override
  public boolean isFullScreenMode() {
    final MIDletAccess ma = MIDletBridge.getMIDletAccess();
    if (ma == null) {
      return false;
    }
    else {
      final DisplayAccess da = ma.getDisplayAccess();
      if (da == null) {
        return false;
      }
      else {
        return da.isFullScreenMode();
      }
    }
  }

  @Override
  public int numAlphaLevels() {
    return numAlphaLevels;
  }

  @Override
  public int numColors() {
    return numColors;
  }

  public void paintControls(final Graphics g) {
    final Device device = DeviceFactory.getDevice();

    g.setColor(backgroundColor);
    g.fillRect(0, 0, displayRectangle.width, displayPaintable.y);
    g.fillRect(0, displayPaintable.y, displayPaintable.x, displayPaintable.height);
    g.fillRect(displayPaintable.x + displayPaintable.width, displayPaintable.y, displayRectangle.width
        - displayPaintable.x - displayPaintable.width, displayPaintable.height);
    g.fillRect(0, displayPaintable.y + displayPaintable.height, displayRectangle.width, displayRectangle.height
        - displayPaintable.y - displayPaintable.height);

    g.setColor(foregroundColor);
    for (final Enumeration s = device.getSoftButtons().elements(); s.hasMoreElements();) {
      ((J2SESoftButton)s.nextElement()).paint(g);
    }

    final int inputMode = device.getInputMethod().getInputMode();
    if (inputMode == InputMethod.INPUT_123) {
      g.drawImage(((J2SEImmutableImage)mode123Image.getImage()).getImage(), mode123Image.getRectangle().x,
          mode123Image.getRectangle().y, null);
    }
    else if (inputMode == InputMethod.INPUT_ABC_UPPER) {
      g.drawImage(((J2SEImmutableImage)modeAbcUpperImage.getImage()).getImage(), modeAbcUpperImage
          .getRectangle().x, modeAbcUpperImage.getRectangle().y, null);
    }
    else if (inputMode == InputMethod.INPUT_ABC_LOWER) {
      g.drawImage(((J2SEImmutableImage)modeAbcLowerImage.getImage()).getImage(), modeAbcLowerImage
          .getRectangle().x, modeAbcLowerImage.getRectangle().y, null);
    }
  }

  public void paintDisplayable(final J2SEGraphicsSurface graphicsSurface, final int x, final int y, final int width, final int height) {
    final MIDletAccess ma = MIDletBridge.getMIDletAccess();
    if (ma == null) { return; }
    final DisplayAccess da = ma.getDisplayAccess();
    if (da == null) { return; }
    final Displayable current = da.getCurrent();
    if (current == null) { return; }

    final Graphics g = graphicsSurface.getGraphics();
    g.setColor(foregroundColor);
    final java.awt.Shape oldclip = g.getClip();
    if (!(current instanceof Canvas) || (((Canvas)current).getWidth() != displayRectangle.width)
        || (((Canvas)current).getHeight() != displayRectangle.height)) {
      g.translate(displayPaintable.x, displayPaintable.y);
    }
    g.setClip(x, y, width, height);
    final Font oldf = g.getFont();
    ma.getDisplayAccess().paint(new J2SEDisplayGraphics(graphicsSurface));
    g.setFont(oldf);
    if (!(current instanceof Canvas) || (((Canvas)current).getWidth() != displayRectangle.width)
        || (((Canvas)current).getHeight() != displayRectangle.height)) {
      g.translate(-displayPaintable.x, -displayPaintable.y);
    }
    g.setClip(oldclip);
  }

  @Override
  public void repaint(final int x, final int y, final int width, final int height) {
    context.getDisplayComponent().repaintRequest(x, y, width, height);
  }

  @Override
  public void setScrollDown(final boolean state) {
    final Enumeration en = DeviceFactory.getDevice().getSoftButtons().elements();
    while (en.hasMoreElements()) {
      final SoftButton button = (SoftButton)en.nextElement();
      if ((button.getType() == SoftButton.TYPE_ICON) && button.getName().equals("down")) {
        button.setVisible(state);
      }
    }
  }

  @Override
  public void setScrollUp(final boolean state) {
    final Enumeration en = DeviceFactory.getDevice().getSoftButtons().elements();
    while (en.hasMoreElements()) {
      final SoftButton button = (SoftButton)en.nextElement();
      if ((button.getType() == SoftButton.TYPE_ICON) && button.getName().equals("up")) {
        button.setVisible(state);
      }
    }
  }

  @Override
  public boolean isResizable() {
    return resizable;
  }

  @Override
  public void setResizable(final boolean state) {
    resizable = state;
  }

  public Rectangle getDisplayRectangle() {
    return displayRectangle;
  }

  public Rectangle getDisplayPaintable() {
    return displayPaintable;
  }

  public Color getBackgroundColor() {
    return new Color(backgroundColor.getRGB());
  }

  public Color getForegroundColor() {
    return new Color(foregroundColor.getRGB());
  }

  @Override
  public Image createImage(final int width, final int height, final boolean withAlpha, final int fillColor) {
    if ((width <= 0) || (height <= 0)) { throw new IllegalArgumentException(); }

    return new J2SEMutableImage(width, height, withAlpha, fillColor);
  }

  @Override
  public Image createImage(final String name) throws IOException {
    return getImage(name);
  }

  @Override
  public Image createImage(final javax.microedition.lcdui.Image source) {
    if (source.isMutable()) {
      return new J2SEImmutableImage((J2SEMutableImage)source);
    }
    else {
      return source;
    }
  }

  // Andres Navarro
  @Override
  public Image createImage(final InputStream is) throws IOException {
    if (is == null) { throw new IOException(); }
    return getImage(is);
  }

  @Override
  public Image createRGBImage(int[] rgb, final int width, final int height, final boolean processAlpha) {
    if (rgb == null) { throw new NullPointerException(); }
    if ((width <= 0) || (height <= 0)) { throw new IllegalArgumentException(); }

    final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    if (!processAlpha) {
      // we should eliminate the transparency info
      // but can't touch the original array
      // so we just create another
      final int l = rgb.length;

      final int[] rgbAux = new int[l];
      for (int i = 0; i < l; i++) {
        rgbAux[i] = rgb[i] | 0xff000000;
      }
      rgb = rgbAux;
    }

    img.setRGB(0, 0, width, height, rgb, 0, width);

    // now apply the corresponding filter
    ImageFilter filter = null;
    if (isColor()) {
      if ((backgroundColor.getRed() != 255) || (backgroundColor.getGreen() != 255)
          || (backgroundColor.getBlue() != 255) || (foregroundColor.getRed() != 0)
          || (foregroundColor.getGreen() != 0) || (foregroundColor.getBlue() != 0)) {
        filter = new RGBImageFilter();
      }
    }
    else {
      if (numColors() == 2) {
        filter = new BWImageFilter();
      }
      else {
        filter = new GrayImageFilter();
      }
    }
    if (filter != null) {
      final FilteredImageSource imageSource = new FilteredImageSource(img.getSource(), filter);
      return new J2SEImmutableImage(Toolkit.getDefaultToolkit().createImage(imageSource));
    }
    else {
      return new J2SEImmutableImage(img);
    }
  }

  @Override
  public Image createImage(Image image, final int x, final int y, int width, int height, final int transform) {
    if (image == null) { throw new NullPointerException(); }
    if (((x + width) > image.getWidth()) || ((y + height) > image.getHeight()) || (width <= 0) || (height <= 0) || (x < 0)
        || (y < 0)) { throw new IllegalArgumentException("Area out of Image"); }

    int[] rgbData = new int[height * width];
    final int[] rgbTransformedData = new int[height * width];
    if (image instanceof J2SEImmutableImage) {
      ((J2SEImmutableImage)image).getRGB(rgbData, 0, width, x, y, width, height);
    }
    else {
      ((J2SEMutableImage)image).getRGB(rgbData, 0, width, x, y, width, height);
    }

    int colIncr, rowIncr, offset;

    switch (transform) {
      case Sprite.TRANS_NONE: {
        offset = 0;
        colIncr = 1;
        rowIncr = 0;
        break;
      }
      case Sprite.TRANS_ROT90: {
        offset = (height - 1) * width;
        colIncr = -width;
        rowIncr = (height * width) + 1;
        final int temp = width;
        width = height;
        height = temp;
        break;
      }
      case Sprite.TRANS_ROT180: {
        offset = (height * width) - 1;
        colIncr = -1;
        rowIncr = 0;
        break;
      }
      case Sprite.TRANS_ROT270: {
        offset = width - 1;
        colIncr = width;
        rowIncr = -(height * width) - 1;
        final int temp = width;
        width = height;
        height = temp;
        break;
      }
      case Sprite.TRANS_MIRROR: {
        offset = width - 1;
        colIncr = -1;
        rowIncr = width << 1;
        break;
      }
      case Sprite.TRANS_MIRROR_ROT90: {
        offset = (height * width) - 1;
        colIncr = -width;
        rowIncr = (height * width) - 1;
        final int temp = width;
        width = height;
        height = temp;
        break;
      }
      case Sprite.TRANS_MIRROR_ROT180: {
        offset = (height - 1) * width;
        colIncr = 1;
        rowIncr = -(width << 1);
        break;
      }
      case Sprite.TRANS_MIRROR_ROT270: {
        offset = 0;
        colIncr = width;
        rowIncr = -(height * width) + 1;
        final int temp = width;
        width = height;
        height = temp;
        break;
      }
      default:
        throw new IllegalArgumentException("Bad transform");
    }

    // now the loops!
    for (int row = 0, i = 0; row < height; row++, offset += rowIncr) {
      for (int col = 0; col < width; col++, offset += colIncr, i++) {
        rgbTransformedData[i] = rgbData[offset];
      }
    }
    // to aid gc
    rgbData = null;
    image = null;

    return createRGBImage(rgbTransformedData, width, height, true);
  }

  // Andres Navarro

  @Override
  public Image createImage(final byte[] imageData, final int imageOffset, final int imageLength) {
    final ByteArrayInputStream is = new ByteArrayInputStream(imageData, imageOffset, imageLength);
    try {
      return getImage(is);
    }
    catch (final IOException ex) {
      throw new IllegalArgumentException(ex.toString());
    }
  }

  @Override
  public javax.microedition.lcdui.Graphics getGraphics(final GameCanvas gameCanvas) {
    if (gameCanvasImage == null) {
      gameCanvasImage = createImage(gameCanvas.getWidth(), gameCanvas.getHeight(), true, 0x00000000);
      gameCanvasGraphics = gameCanvasImage.getGraphics();
    }

    return gameCanvasGraphics;
  }

  @Override
  public void flushGraphics(final GameCanvas gameCanvas, final int x, final int y, final int width, final int height) {
    if (gameCanvasImage != null) {
      final J2SEGraphicsSurface surface = ((SwingDisplayComponent)context.getDisplayComponent()).getGraphicsSurface();
      if (surface != null) {
        surface.getGraphics().drawImage(
            ((J2SEMutableImage)gameCanvasImage).getImage(),
            x, y, x + width, y + height,
            x, y, x + width, y + height,
            null);
        ((SwingDisplayComponent)context.getDisplayComponent()).fireDisplayRepaint(
            surface, 0, 0, surface.getImage().getWidth(), surface.getImage().getHeight());
      }
    }
  }

  @Override
  public void setNumAlphaLevels(final int i) {
    numAlphaLevels = i;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setNumColors(int)
   */
  @Override
  public void setNumColors(final int i) {
    numColors = i;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setIsColor(boolean)
   */
  @Override
  public void setIsColor(final boolean b) {
    isColor = b;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setBackgroundColor(java.awt.Color)
   */
  @Override
  public void setBackgroundColor(final Color color) {
    backgroundColor = new java.awt.Color(color.getRGB());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setForegroundColor(java.awt.Color)
   */
  @Override
  public void setForegroundColor(final Color color) {
    foregroundColor = new java.awt.Color(color.getRGB());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setDisplayRectangle(java.awt.Rectangle)
   */
  @Override
  public void setDisplayRectangle(final Rectangle rectangle) {
    displayRectangle = rectangle;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setDisplayPaintable(java.awt.Rectangle)
   */
  @Override
  public void setDisplayPaintable(final Rectangle rectangle) {
    displayPaintable = rectangle;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setMode123Image(com.barteo.emulator.device.impl.PositionedImage)
   */
  @Override
  public void setMode123Image(final PositionedImage object) {
    mode123Image = object;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setModeAbcLowerImage(com.barteo.emulator.device.impl.PositionedImage)
   */
  @Override
  public void setModeAbcLowerImage(final PositionedImage object) {
    modeAbcLowerImage = object;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.barteo.emulator.device.impl.DeviceDisplayImpl#setModeAbcUpperImage(com.barteo.emulator.device.impl.PositionedImage)
   */
  @Override
  public void setModeAbcUpperImage(final PositionedImage object) {
    modeAbcUpperImage = object;
  }

  @Override
  public Image createSystemImage(final URL url) throws IOException {
    final java.awt.Image resultImage = Toolkit.getDefaultToolkit().createImage(url);

    // TODO not elegant solution, maybe use ImageObserver in
    // image.getWitdth(..) instead
    final MediaTracker mediaTracker = new MediaTracker(new java.awt.Canvas());
    mediaTracker.addImage(resultImage, 0);
    try {
      mediaTracker.waitForID(0);
    }
    catch (final InterruptedException ex) {
    }
    if (mediaTracker.isErrorID(0)) { throw new IOException(); }

    return new J2SEImmutableImage(resultImage);
  }

  private Image getImage(final String str) throws IOException {
    // TODO not always true, there could be some loading images before
    // invoke startApp, right now getCurrentMIDlet returns prevoius MIDlet
    Object midlet = MIDletBridge.getCurrentMIDlet();
    if (midlet == null) {
      midlet = getClass();
    }
    final InputStream is = midlet.getClass().getResourceAsStream(str);

    if (is == null) { throw new IOException(str + " could not be found."); }
    try {
      return getImage(is);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  private Image getImage(final InputStream is) throws IOException {
    final byte[] imageBytes = new byte[1024];
    int num;
    final ByteArrayOutputStream ba = new ByteArrayOutputStream();
    while ((num = is.read(imageBytes)) != -1) {
      ba.write(imageBytes, 0, num);
    }

    final java.awt.Image image = Toolkit.getDefaultToolkit().createImage(ba.toByteArray());

    ImageFilter filter = null;
    if (isColor()) {
      if ((backgroundColor.getRed() != 255) || (backgroundColor.getGreen() != 255)
          || (backgroundColor.getBlue() != 255) || (foregroundColor.getRed() != 0)
          || (foregroundColor.getGreen() != 0) || (foregroundColor.getBlue() != 0)) {
        filter = new RGBImageFilter();
      }
    }
    else {
      if (numColors() == 2) {
        filter = new BWImageFilter();
      }
      else {
        filter = new GrayImageFilter();
      }
    }
    java.awt.Image resultImage;
    if (filter != null) {
      final FilteredImageSource imageSource = new FilteredImageSource(image.getSource(), filter);
      resultImage = Toolkit.getDefaultToolkit().createImage(imageSource);
    }
    else {
      resultImage = image;
    }

    // TODO not elegant solution, maybe use ImageObserver in
    // image.getWitdth(..) instead
    final MediaTracker mediaTracker = new MediaTracker(new java.awt.Canvas());
    mediaTracker.addImage(resultImage, 0);
    try {
      mediaTracker.waitForID(0);
    }
    catch (final InterruptedException ex) {
    }
    if (mediaTracker.isErrorID(0)) { throw new IOException(); }

    return new J2SEImmutableImage(resultImage);
  }

  @Override
  public Button createButton(final int skinVersion, final String name, final org.microemu.device.impl.Shape shape, final int keyCode, final String keyboardKeys, final String keyboardChars, final Hashtable inputToChars, final boolean modeChange) {
    return new J2SEButton(skinVersion, name, shape, keyCode, keyboardKeys, keyboardChars, inputToChars, modeChange);
  }

  @Override
  public SoftButton createSoftButton(final int skinVersion, final String name, final Shape shape, final int keyCode, final String keyboardKeys, final Rectangle paintable, final String alignmentName, final Vector commands, final javax.microedition.lcdui.Font font) {
    return new J2SESoftButton(skinVersion, name, shape, keyCode, keyboardKeys, paintable, alignmentName, commands,
        font);
  }

  @Override
  public SoftButton createSoftButton(final int skinVersion, final String name, final Rectangle paintable, final Image normalImage, final Image pressedImage) {
    return new J2SESoftButton(skinVersion, name, paintable, normalImage, pressedImage);
  }

}
