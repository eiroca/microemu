/**
 * MicroEmulator Copyright (C) 2001,2002 Bartek Teodorczyk <barteo@barteo.net>
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
package org.microemu.app.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Command;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.microemu.DisplayAccess;
import org.microemu.DisplayComponent;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.app.Common;
import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.impl.DeviceDisplayImpl;
import org.microemu.device.impl.Rectangle;
import org.microemu.device.impl.SoftButton;
import org.microemu.device.impl.ui.CommandManager;
import org.microemu.device.j2se.J2SEButton;
import org.microemu.device.j2se.J2SEDeviceButtonsHelper;
import org.microemu.device.j2se.J2SEDeviceDisplay;
import org.microemu.device.j2se.J2SEImmutableImage;
import org.microemu.device.j2se.J2SEInputMethod;
import org.microemu.device.j2se.J2SEMutableImage;
import org.microemu.log.Logger;

public class SwingDeviceComponent extends JPanel implements KeyListener, InputMethodListener, InputMethodRequests {

  private static final long serialVersionUID = 1L;

  SwingDisplayComponent dc;
  J2SEButton prevOverButton;
  J2SEButton overButton;
  J2SEButton pressedButton;
  Image offi;
  Graphics offg;

  private boolean mouseButtonDown = false;
  private final boolean showMouseCoordinates = false;
  private int pressedX;
  private int pressedY;

  private static class MouseRepeatedTimerTask extends TimerTask {

    private static final int DELAY = 100;

    Timer timer;
    Component source;
    J2SEButton button;
    J2SEInputMethod inputMethod;

    static MouseRepeatedTimerTask task;

    static void schedule(final Component source, final J2SEButton button, final J2SEInputMethod inputMethod) {
      if (MouseRepeatedTimerTask.task != null) {
        MouseRepeatedTimerTask.task.cancel();
      }
      MouseRepeatedTimerTask.task = new MouseRepeatedTimerTask();
      MouseRepeatedTimerTask.task.source = source;
      MouseRepeatedTimerTask.task.button = button;
      MouseRepeatedTimerTask.task.inputMethod = inputMethod;
      MouseRepeatedTimerTask.task.timer = new Timer();
      MouseRepeatedTimerTask.task.timer.scheduleAtFixedRate(MouseRepeatedTimerTask.task, 5 * MouseRepeatedTimerTask.DELAY, MouseRepeatedTimerTask.DELAY);
    }

    static void stop() {
      if (MouseRepeatedTimerTask.task != null) {
        MouseRepeatedTimerTask.task.inputMethod = null;
        if (MouseRepeatedTimerTask.task.timer != null) {
          MouseRepeatedTimerTask.task.timer.cancel();
        }
        MouseRepeatedTimerTask.task.cancel();
        MouseRepeatedTimerTask.task = null;
      }
    }

    public static void mouseReleased() {
      if ((MouseRepeatedTimerTask.task != null) && (MouseRepeatedTimerTask.task.inputMethod != null)) {
        MouseRepeatedTimerTask.task.inputMethod.buttonReleased(MouseRepeatedTimerTask.task.button, '\0');
        MouseRepeatedTimerTask.stop();
      }

    }

    @Override
    public void run() {
      if (inputMethod != null) {
        inputMethod.buttonPressed(button, '\0');
      }
    }

  }

  private final MouseAdapter mouseListener = new MouseAdapter() {

    @Override
    public void mousePressed(final MouseEvent e) {
      requestFocus();
      mouseButtonDown = true;
      pressedX = e.getX();
      pressedY = e.getY();

      MouseRepeatedTimerTask.stop();
      if (MIDletBridge.getCurrentMIDlet() == null) { return; }

      final Device device = DeviceFactory.getDevice();
      final J2SEInputMethod inputMethod = (J2SEInputMethod)device.getInputMethod();
      // if the displayable is in full screen mode, we should not
      // invoke any associated commands, but send the raw key codes
      // instead
      final boolean fullScreenMode = device.getDeviceDisplay().isFullScreenMode();

      pressedButton = J2SEDeviceButtonsHelper.getSkinButton(e);
      if (pressedButton != null) {
        if ((pressedButton instanceof SoftButton) && !fullScreenMode) {
          final Command cmd = ((SoftButton)pressedButton).getCommand();
          if (cmd != null) {
            final MIDletAccess ma = MIDletBridge.getMIDletAccess();
            if (ma == null) { return; }
            final DisplayAccess da = ma.getDisplayAccess();
            if (da == null) { return; }
            if (cmd.equals(CommandManager.CMD_MENU)) {
              CommandManager.getInstance().commandAction(cmd);
            }
            else {
              da.commandAction(cmd, da.getCurrent());
            }
          }
        }
        else {
          inputMethod.buttonPressed(pressedButton, '\0');
          MouseRepeatedTimerTask.schedule(SwingDeviceComponent.this, pressedButton, inputMethod);
        }
        // optimize for some video cards.
        repaint(pressedButton.getShape().getBounds());
      }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
      mouseButtonDown = false;
      MouseRepeatedTimerTask.stop();

      if (pressedButton == null) { return; }

      if (MIDletBridge.getCurrentMIDlet() == null) { return; }

      final Device device = DeviceFactory.getDevice();
      final J2SEInputMethod inputMethod = (J2SEInputMethod)device.getInputMethod();
      final J2SEButton prevOverButton = J2SEDeviceButtonsHelper.getSkinButton(e);
      if (prevOverButton != null) {
        inputMethod.buttonReleased(prevOverButton, '\0');
      }
      pressedButton = null;
      // optimize for some video cards.
      if (prevOverButton != null) {
        repaint(prevOverButton.getShape().getBounds());
      }
      else {
        repaint();
      }
    }

  };

  private final MouseMotionListener mouseMotionListener = new MouseMotionListener() {

    @Override
    public void mouseDragged(final MouseEvent e) {
      mouseMoved(e);
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
      if (showMouseCoordinates) {
        final StringBuffer buf = new StringBuffer();
        if (mouseButtonDown) {
          final int width = e.getX() - pressedX;
          final int height = e.getY() - pressedY;
          buf.append(pressedX).append(",").append(pressedY).append(" ").append(width).append("x").append(
              height);
        }
        else {
          buf.append(e.getX()).append(",").append(e.getY());
        }
        Common.setStatusBar(buf.toString());
      }

      if (mouseButtonDown && (pressedButton == null)) { return; }

      prevOverButton = overButton;
      overButton = J2SEDeviceButtonsHelper.getSkinButton(e);
      if (overButton != prevOverButton) {
        // optimize for some video cards.
        if (prevOverButton != null) {
          MouseRepeatedTimerTask.mouseReleased();
          pressedButton = null;
          repaint(prevOverButton.getShape().getBounds());
        }
        if (overButton != null) {
          repaint(overButton.getShape().getBounds());
        }
      }
      else if (overButton == null) {
        MouseRepeatedTimerTask.mouseReleased();
        pressedButton = null;
        if (prevOverButton != null) {
          repaint(prevOverButton.getShape().getBounds());
        }
      }
    }

  };

  public SwingDeviceComponent() {
    dc = new SwingDisplayComponent(this);
    setLayout(new XYLayout());

    addMouseListener(mouseListener);
    addMouseMotionListener(mouseMotionListener);

    //Input methods support begin
    enableInputMethods(true);
    addInputMethodListener(this);
    //End
  }

  public DisplayComponent getDisplayComponent() {
    return dc;
  }

  public void init() {
    dc.init();

    remove(dc);

    final Rectangle r = ((J2SEDeviceDisplay)DeviceFactory.getDevice().getDeviceDisplay()).getDisplayRectangle();
    add(dc, new XYConstraints(r.x, r.y, -1, -1));

    revalidate();
  }

  private void repaint(final Rectangle r) {
    repaint(r.x, r.y, r.width, r.height);
  }

  public void switchShowMouseCoordinates() {
    // TODO skin editing mode.
    // showMouseCoordinates = !showMouseCoordinates;
    dc.switchShowMouseCoordinates();
  }

  //Input method support begin

  private static final AttributedCharacterIterator EMPTY_TEXT = new AttributedString("").getIterator();

  @Override
  public void caretPositionChanged(final InputMethodEvent event) {
    repaint();
  }

  @Override
  public void inputMethodTextChanged(final InputMethodEvent event) {
    final StringBuffer committedText = new StringBuffer();
    final AttributedCharacterIterator text = event.getText();
    final Device device = DeviceFactory.getDevice();
    final J2SEInputMethod inputMethod = (J2SEInputMethod)device.getInputMethod();
    if (text != null) {
      int toCopy = event.getCommittedCharacterCount();
      char c = text.first();
      while (toCopy-- > 0) {
        committedText.append(c);
        c = text.next();
      }
      if (committedText.length() > 0) {
        inputMethod.clipboardPaste(committedText.toString());
      }
    }
    repaint();
  }

  @Override
  public InputMethodRequests getInputMethodRequests() {
    return this;
  }

  @Override
  public int getCommittedTextLength() {
    return 0;
  }

  @Override
  public int getInsertPositionOffset() {
    return getCommittedTextLength();
  }

  @Override
  public AttributedCharacterIterator getCommittedText(final int beginIndex, final int endIndex, final AttributedCharacterIterator.Attribute[] attributes) {
    return null;
  }

  @Override
  public java.awt.Rectangle getTextLocation(final TextHitInfo offset) {
    return null;
  }

  @Override
  public TextHitInfo getLocationOffset(final int x, final int y) {
    return null;
  }

  @Override
  public AttributedCharacterIterator getSelectedText(final AttributedCharacterIterator.Attribute[] attributes) {
    return SwingDeviceComponent.EMPTY_TEXT;
  }

  @Override
  public AttributedCharacterIterator cancelLatestCommittedText(final AttributedCharacterIterator.Attribute[] attributes) {
    return null;
  }

  //Input method support end

  @Override
  public void keyTyped(final KeyEvent ev) {
    if (MIDletBridge.getCurrentMIDlet() == null) { return; }

    final J2SEInputMethod inputMethod = ((J2SEInputMethod)DeviceFactory.getDevice().getInputMethod());
    final J2SEButton button = inputMethod.getButton(ev);
    if (button != null) {
      inputMethod.buttonTyped(button);
    }
  }

  @Override
  public void keyPressed(final KeyEvent ev) {
    if (MIDletBridge.getCurrentMIDlet() == null) { return; }

    final Device device = DeviceFactory.getDevice();
    final J2SEInputMethod inputMethod = (J2SEInputMethod)device.getInputMethod();

    if ((ev.getKeyCode() == KeyEvent.VK_V) && ((ev.getModifiers() & InputEvent.CTRL_MASK) != 0)) {
      final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      final Transferable transferable = clipboard.getContents(null);
      if (transferable != null) {
        try {
          final Object data = transferable.getTransferData(DataFlavor.stringFlavor);
          if (data instanceof String) {
            inputMethod.clipboardPaste((String)data);
          }
        }
        catch (final UnsupportedFlavorException ex) {
          Logger.error(ex);
        }
        catch (final IOException ex) {
          Logger.error(ex);
        }
      }
      return;
    }

    switch (ev.getKeyCode()) {
      case KeyEvent.VK_ALT:
      case KeyEvent.VK_CONTROL:
      case KeyEvent.VK_SHIFT:
        return;
      case 0:
        // Don't know what is the case was intended for but this may be
        // national keyboard letter, so let it work
        if (ev.getKeyChar() == '\0') { return; }
    }

    char keyChar = '\0';
    if ((ev.getKeyChar() >= 32) && (ev.getKeyChar() != 65535)) {
      keyChar = ev.getKeyChar();
    }
    final J2SEButton button = inputMethod.getButton(ev);
    if (button != null) {
      pressedButton = button;
      // numeric keypad functions as hot keys for buttons only
      if ((ev.getKeyCode() >= KeyEvent.VK_NUMPAD0) && (ev.getKeyCode() <= KeyEvent.VK_NUMPAD9)) {
        keyChar = '\0';
      }
      // soft buttons
      if ((ev.getKeyCode() >= KeyEvent.VK_F1) && (ev.getKeyCode() <= KeyEvent.VK_F12)) {
        keyChar = '\0';
      }
      final org.microemu.device.impl.Shape shape = button.getShape();
      if (shape != null) {
        repaint(shape.getBounds());
      }
    }
    else {
      // Logger.debug0x("no button for KeyCode", ev.getKeyCode());
    }
    inputMethod.buttonPressed(button, keyChar);
  }

  @Override
  public void keyReleased(final KeyEvent ev) {
    if (MIDletBridge.getCurrentMIDlet() == null) { return; }

    switch (ev.getKeyCode()) {
      case KeyEvent.VK_ALT:
      case KeyEvent.VK_CONTROL:
      case KeyEvent.VK_SHIFT:
        return;
      case 0:
        // Don't know what is the case was intended for but this may be
        // national keyboard letter, so let it work
        if (ev.getKeyChar() == '\0') { return; }
    }

    final Device device = DeviceFactory.getDevice();
    final J2SEInputMethod inputMethod = (J2SEInputMethod)device.getInputMethod();

    char keyChar = '\0';
    if ((ev.getKeyChar() >= 32) && (ev.getKeyChar() != 65535)) {
      keyChar = ev.getKeyChar();
    }
    // numeric keypad functions as hot keys for buttons only
    if ((ev.getKeyCode() >= KeyEvent.VK_NUMPAD0) && (ev.getKeyCode() <= KeyEvent.VK_NUMPAD9)) {
      keyChar = '\0';
    }
    // soft buttons
    if ((ev.getKeyCode() >= KeyEvent.VK_F1) && (ev.getKeyCode() <= KeyEvent.VK_F12)) {
      keyChar = '\0';
    }
    // Logger.debug0x("keyReleased [" + keyChar + "]", keyChar);
    inputMethod.buttonReleased(inputMethod.getButton(ev), keyChar);

    prevOverButton = pressedButton;
    pressedButton = null;
    if (prevOverButton != null) {
      final org.microemu.device.impl.Shape shape = prevOverButton.getShape();
      if (shape != null) {
        repaint(shape.getBounds());
      }
    }
  }

  public MouseListener getDefaultMouseListener() {
    return mouseListener;
  }

  public MouseMotionListener getDefaultMouseMotionListener() {
    return mouseMotionListener;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    if ((offg == null) || (offi.getWidth(null) != getSize().width) || (offi.getHeight(null) != getSize().height)) {
      offi = new J2SEMutableImage(getSize().width, getSize().height, false, 0x000000).getImage();
      offg = offi.getGraphics();
    }

    final Dimension size = getSize();
    offg.setColor(UIManager.getColor("text"));
    try {
      offg.fillRect(0, 0, size.width, size.height);
    }
    catch (final NullPointerException ex) {
      // Fix for NPE in sun.java2d.pipe.SpanShapeRenderer.renderRect(..) on Mac platform
    }
    final Device device = DeviceFactory.getDevice();
    if (device == null) {
      g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
      return;
    }
    if (((DeviceDisplayImpl)device.getDeviceDisplay()).isResizable()) { return; }

    offg.drawImage(((J2SEImmutableImage)device.getNormalImage()).getImage(), 0, 0, this);

    if (prevOverButton != null) {
      final org.microemu.device.impl.Shape shape = prevOverButton.getShape();
      if (shape != null) {
        drawImageInShape(offg, ((J2SEImmutableImage)device.getNormalImage()).getImage(), shape);
      }
      prevOverButton = null;
    }
    if (overButton != null) {
      final org.microemu.device.impl.Shape shape = overButton.getShape();
      if (shape != null) {
        drawImageInShape(offg, ((J2SEImmutableImage)device.getOverImage()).getImage(), shape);
      }
    }
    if (pressedButton != null) {
      final org.microemu.device.impl.Shape shape = pressedButton.getShape();
      if (shape != null) {
        drawImageInShape(offg, ((J2SEImmutableImage)device.getPressedImage()).getImage(), shape);
      }
    }

    g.drawImage(offi, 0, 0, null);
  }

  private void drawImageInShape(final Graphics g, final Image image, final org.microemu.device.impl.Shape shape) {
    final Shape clipSave = g.getClip();
    if (shape instanceof org.microemu.device.impl.Polygon) {
      final Polygon poly = new Polygon(((org.microemu.device.impl.Polygon)shape).xpoints, ((org.microemu.device.impl.Polygon)shape).ypoints, ((org.microemu.device.impl.Polygon)shape).npoints);
      g.setClip(poly);
    }
    final org.microemu.device.impl.Rectangle r = shape.getBounds();
    g.drawImage(image, r.x, r.y, r.x + r.width, r.y + r.height, r.x, r.y, r.x + r.width, r.y + r.height, null);
    g.setClip(clipSave);
  }

  @Override
  public Dimension getPreferredSize() {
    final Device device = DeviceFactory.getDevice();
    if (device == null) { return new Dimension(0, 0); }
    final DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl)DeviceFactory.getDevice().getDeviceDisplay();
    if (deviceDisplay.isResizable()) {
      return new Dimension(deviceDisplay.getFullWidth(), deviceDisplay.getFullHeight());
    }
    else {
      final javax.microedition.lcdui.Image img = device.getNormalImage();
      return new Dimension(img.getWidth(), img.getHeight());
    }
  }

}
