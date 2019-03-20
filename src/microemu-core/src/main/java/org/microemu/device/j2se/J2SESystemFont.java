/**
 * MicroEmulator Copyright (C) 2006 Bartek Teodorczyk <barteo@barteo.net>
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

package org.microemu.device.j2se;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class J2SESystemFont implements J2SEFont {

  private final static Graphics2D graphics = (Graphics2D)new BufferedImage(1, 1,
      BufferedImage.TYPE_INT_ARGB).getGraphics();

  private final String name;

  private final String style;

  private final int size;

  private boolean antialiasing;

  private boolean initialized;

  private FontMetrics fontMetrics;

  public J2SESystemFont(final String name, final String style, final int size, final boolean antialiasing) {
    this.name = name;
    this.style = style.toLowerCase();
    this.size = size;
    this.antialiasing = antialiasing;

    initialized = false;
  }

  @Override
  public void setAntialiasing(final boolean antialiasing) {
    if (this.antialiasing != antialiasing) {
      this.antialiasing = antialiasing;
      initialized = false;
    }
  }

  @Override
  public int charWidth(final char ch) {
    checkInitialized();

    return fontMetrics.charWidth(ch);
  }

  @Override
  public int charsWidth(final char[] ch, final int offset, final int length) {
    checkInitialized();

    return fontMetrics.charsWidth(ch, offset, length);
  }

  @Override
  public int getBaselinePosition() {
    checkInitialized();

    return fontMetrics.getAscent();
  }

  @Override
  public int getHeight() {
    checkInitialized();

    return fontMetrics.getHeight();
  }

  @Override
  public int stringWidth(final String str) {
    checkInitialized();

    return fontMetrics.stringWidth(str);
  }

  @Override
  public Font getFont() {
    checkInitialized();

    return fontMetrics.getFont();
  }

  private synchronized void checkInitialized() {
    if (!initialized) {
      int awtStyle = 0;
      if (style.indexOf("plain") != -1) {
        awtStyle |= Font.PLAIN;
      }
      if (style.indexOf("bold") != -1) {
        awtStyle |= Font.BOLD;
      }
      if (style.indexOf("italic") != -1) {
        awtStyle |= Font.ITALIC;
      }
      if (style.indexOf("underlined") != -1) {
        // TODO underlined style not implemented
      }
      if (antialiasing) {
        J2SESystemFont.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      }
      else {
        J2SESystemFont.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
      }
      fontMetrics = J2SESystemFont.graphics.getFontMetrics(new Font(name, awtStyle, size));
      initialized = true;
    }
  }

}
