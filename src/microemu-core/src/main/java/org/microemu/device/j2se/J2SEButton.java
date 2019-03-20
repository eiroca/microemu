/**
 * MicroEmulator Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
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

package org.microemu.device.j2se;

import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.microemu.device.InputMethod;
import org.microemu.device.impl.Button;
import org.microemu.device.impl.ButtonDetaultDeviceKeyCodes;
import org.microemu.device.impl.ButtonName;
import org.microemu.device.impl.Shape;

public class J2SEButton implements Button {

  private final String name;

  private ButtonName functionalName;

  private final Shape shape;

  private int[] keyboardKeys;

  private String keyboardCharCodes;

  private int keyCode;

  private final Hashtable inputToChars;

  private boolean modeChange;

  /**
   * Create special functional buttons. e.g. ButtonName.DELETE and ButtonName.BACK_SPACE if not
   * defined in 'device.xml'
   * 
   * @param name
   */
  J2SEButton(final ButtonName functionalName) {
    this(20002, functionalName.getName(), null, Integer.MIN_VALUE, null, null, null, false);
  }

  /**
   * @param name
   * @param shape
   * @param keyCode - Integer.MIN_VALUE when unspecified
   * @param keyName
   * @param chars
   */
  public J2SEButton(final int skinVersion, final String name, final Shape shape, final int keyCode, final String keyboardKeys, final String keyboardChars, final Hashtable inputToChars, final boolean modeChange) {
    this.name = name;
    this.shape = shape;
    if (skinVersion >= Button.NAME_RIMARY_SINCE_SKIN_VERSION) {
      functionalName = ButtonName.getButtonName(name);
    }
    else {
      functionalName = J2SEButtonDefaultKeyCodes.getBackwardCompatibleName(J2SEButton.parseKeyboardKey(keyboardKeys));
      if (functionalName == null) {
        functionalName = ButtonName.getButtonName(name);
      }
    }

    if (skinVersion >= Button.NAME_RIMARY_SINCE_SKIN_VERSION) {
      this.modeChange = modeChange;
    }
    else {
      this.modeChange = (functionalName == ButtonName.KEY_POUND);
    }

    if (keyCode == Integer.MIN_VALUE) {
      this.keyCode = ButtonDetaultDeviceKeyCodes.getKeyCode(functionalName);
    }
    else {
      this.keyCode = keyCode;
    }

    if (keyboardKeys != null) {
      final StringTokenizer st = new StringTokenizer(keyboardKeys, " ");
      while (st.hasMoreTokens()) {
        final int key = J2SEButton.parseKeyboardKey(st.nextToken());
        if (key == -1) {
          continue;
        }
        if (this.keyboardKeys == null) {
          this.keyboardKeys = new int[1];
        }
        else {
          final int[] newKeyboardKeys = new int[this.keyboardKeys.length + 1];
          System.arraycopy(this.keyboardKeys, 0, newKeyboardKeys, 0, this.keyboardKeys.length);
          this.keyboardKeys = newKeyboardKeys;
        }
        this.keyboardKeys[this.keyboardKeys.length - 1] = key;
      }
    }
    if ((this.keyboardKeys == null) || (this.keyboardKeys.length == 0)) {
      this.keyboardKeys = J2SEButtonDefaultKeyCodes.getKeyCodes(functionalName);
    }
    if (keyboardChars != null) {
      keyboardCharCodes = keyboardChars;
    }
    else {
      keyboardCharCodes = J2SEButtonDefaultKeyCodes.getCharCodes(functionalName);
    }

    this.inputToChars = inputToChars;
  }

  /**
   * @deprecated
   */
  @Deprecated
  public int getKeyboardKey() {
    if (keyboardKeys.length == 0) { return 0; }
    return keyboardKeys[0];
  }

  public int getKeyCode() {
    return keyCode;
  }

  public ButtonName getFunctionalName() {
    return functionalName;
  }

  public int[] getKeyboardKeyCodes() {
    return keyboardKeys;
  }

  /**
   * CharCodes do not depends on InputMode. This is computer keyboard codes when it is impossible to
   * map to VK keys.
   */
  public char[] getKeyboardCharCodes() {
    if (keyboardCharCodes == null) { return new char[0]; }
    return keyboardCharCodes.toCharArray();
  }

  public boolean isModeChange() {
    return modeChange;
  }

  void setModeChange() {
    modeChange = true;
  }

  public char[] getChars(final int inputMode) {
    char[] result = null;
    switch (inputMode) {
      case InputMethod.INPUT_123:
        result = (char[])inputToChars.get("123");
        break;
      case InputMethod.INPUT_ABC_LOWER:
        result = (char[])inputToChars.get("abc");
        break;
      case InputMethod.INPUT_ABC_UPPER:
        result = (char[])inputToChars.get("ABC");
        break;
    }
    if (result == null) {
      result = (char[])inputToChars.get("common");
    }
    if (result == null) {
      result = new char[0];
    }

    return result;
  }

  public boolean isChar(char c, final int inputMode) {
    if (inputToChars == null) { return false; }
    c = Character.toLowerCase(c);
    final char[] chars = getChars(inputMode);
    if (chars != null) {
      for (final char d : chars) {
        if (c == Character.toLowerCase(d)) { return true; }
      }
    }

    return false;
  }

  public String getName() {
    return name;
  }

  public Shape getShape() {
    return shape;
  }

  private static int parseKeyboardKey(final String keyName) {
    int key;
    try {
      key = KeyEvent.class.getField(keyName).getInt(null);
    }
    catch (final Exception e) {
      try {
        key = Integer.parseInt(keyName);
      }
      catch (final NumberFormatException e1) {
        key = -1;
      }
    }
    return key;
  }

}
