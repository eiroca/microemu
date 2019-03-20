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
package org.microemu.device.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.microedition.lcdui.Canvas;

/**
 *
 * This class defines default device key codes and game actions for buttons.
 *
 * Key code is reported to MIDP application by Canvas.keyPressed()
 *
 * Game action is reported to MIDP application by Canvas.getGameAction()
 *
 * Use 'device.xml' to redefine codes for your device if required.
 *
 * @author vlads
 *
 */
public abstract class ButtonDetaultDeviceKeyCodes {

  private static Map codes = new HashMap();

  private static Map gameActions = new HashMap();

  public static int getKeyCode(final ButtonName name) {
    final Integer code = (Integer)ButtonDetaultDeviceKeyCodes.codes.get(name);
    if (code != null) { return code.intValue(); }
    return 0;
  }

  public static int getGameAction(final ButtonName name) {
    final Integer code = (Integer)ButtonDetaultDeviceKeyCodes.gameActions.get(name);
    if (code != null) { return code.intValue(); }
    return 0;
  }

  public static ButtonName getButtonNameByGameAction(final int gameAction) {
    final Integer value = new Integer(gameAction);
    if (ButtonDetaultDeviceKeyCodes.gameActions.containsValue(value)) {
      for (final Iterator iterator = ButtonDetaultDeviceKeyCodes.gameActions.entrySet().iterator(); iterator.hasNext();) {
        final Map.Entry v = (Map.Entry)iterator.next();
        if (v.getValue().equals(value)) { return (ButtonName)v.getKey(); }
      }
    }
    throw new IllegalArgumentException("Illegal action " + gameAction);
  }

  static {
    ButtonDetaultDeviceKeyCodes.code(ButtonName.SOFT1, -6);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.SOFT2, -7);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.SELECT, -5, Canvas.FIRE);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.UP, -1, Canvas.UP);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.DOWN, -2, Canvas.DOWN);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.LEFT, -3, Canvas.LEFT);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.RIGHT, -4, Canvas.RIGHT);

    ButtonDetaultDeviceKeyCodes.code(ButtonName.BACK_SPACE, -8);

    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM0, Canvas.KEY_NUM0);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM1, Canvas.KEY_NUM1, Canvas.GAME_A);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM2, Canvas.KEY_NUM2);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM3, Canvas.KEY_NUM3, Canvas.GAME_B);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM4, Canvas.KEY_NUM4);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM5, Canvas.KEY_NUM5);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM6, Canvas.KEY_NUM6);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM7, Canvas.KEY_NUM7, Canvas.GAME_C);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM8, Canvas.KEY_NUM8);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_NUM9, Canvas.KEY_NUM9, Canvas.GAME_D);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_STAR, Canvas.KEY_STAR);
    ButtonDetaultDeviceKeyCodes.code(ButtonName.KEY_POUND, Canvas.KEY_POUND);
  }

  private static void code(final ButtonName name, final int code) {
    ButtonDetaultDeviceKeyCodes.codes.put(name, new Integer(code));
  }

  private static void code(final ButtonName name, final int code, final int gameAction) {
    ButtonDetaultDeviceKeyCodes.code(name, code);
    ButtonDetaultDeviceKeyCodes.gameActions.put(name, new Integer(gameAction));
  }
}
