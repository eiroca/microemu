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
 * Contributor(s): Andres Navarro
 */

package org.microemu.device.j2se;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.PixelGrabber;
import org.microemu.device.MutableImage;
import org.microemu.log.Logger;

public class J2SEMutableImage extends MutableImage {

  private final J2SEGraphicsSurface graphicsSurface;
  private PixelGrabber grabber = null;
  private int[] pixels;

  public J2SEMutableImage(final int width, final int height, final boolean withAlpha, final int fillColor) {
    graphicsSurface = new J2SEGraphicsSurface(width, height, withAlpha, fillColor);
  }

  @Override
  public javax.microedition.lcdui.Graphics getGraphics() {
    final Graphics2D g = graphicsSurface.getGraphics();
    g.setTransform(new AffineTransform());
    g.setClip(0, 0, getWidth(), getHeight());
    final J2SEDisplayGraphics displayGraphics = new J2SEDisplayGraphics(graphicsSurface);
    displayGraphics.setColor(0x00000000);
    displayGraphics.translate(-displayGraphics.getTranslateX(), -displayGraphics.getTranslateY());

    return displayGraphics;
  }

  @Override
  public boolean isMutable() {
    return true;
  }

  @Override
  public int getHeight() {
    return graphicsSurface.getImage().getHeight();
  }

  public java.awt.Image getImage() {
    return graphicsSurface.getImage();
  }

  @Override
  public int getWidth() {
    return graphicsSurface.getImage().getWidth();
  }

  @Override
  public int[] getData() {
    if (grabber == null) {
      pixels = new int[getWidth() * getHeight()];
      grabber = new PixelGrabber(graphicsSurface.getImage(), 0, 0, getWidth(), getHeight(), pixels, 0, getWidth());
    }

    try {
      grabber.grabPixels();
    }
    catch (final InterruptedException e) {
      Logger.error(e);
    }

    return pixels;
  }

  @Override
  public void getRGB(final int[] argb, final int offset, final int scanlength, final int x, final int y, final int width, final int height) {

    if ((width <= 0) || (height <= 0)) { return; }
    if ((x < 0) || (y < 0) || ((x + width) > getWidth()) || ((y + height) > getHeight())) { throw new IllegalArgumentException("Specified area exceeds bounds of image"); }
    if ((scanlength < 0 ? -scanlength : scanlength) < width) { throw new IllegalArgumentException("abs value of scanlength is less than width"); }
    if (argb == null) { throw new NullPointerException("null rgbData"); }
    if ((offset < 0) || ((offset + width) > argb.length)) { throw new ArrayIndexOutOfBoundsException(); }
    if (scanlength < 0) {
      if ((offset + (scanlength * (height - 1))) < 0) { throw new ArrayIndexOutOfBoundsException(); }
    }
    else {
      if ((offset + (scanlength * (height - 1)) + width) > argb.length) { throw new ArrayIndexOutOfBoundsException(); }
    }

    try {
      (new PixelGrabber(graphicsSurface.getImage(), x, y, width, height, argb, offset, scanlength)).grabPixels();
    }
    catch (final InterruptedException e) {
      Logger.error(e);
    }
  }

}
