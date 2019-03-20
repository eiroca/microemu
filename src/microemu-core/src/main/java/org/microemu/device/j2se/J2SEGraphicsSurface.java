/**
 * MicroEmulator Copyright (C) 2001-2009 Bartek Teodorczyk <barteo@barteo.net>
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
 * @version $Id: J2SEGraphicsSurface.java 1907 2009-01-12 13:19:36Z barteo $
 */

package org.microemu.device.j2se;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

public class J2SEGraphicsSurface {

  private static final DirectColorModel ALPHA_COLOR_MODEL = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);

  private static final DirectColorModel NO_ALPHA_COLOR_MODEL = new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);

  private final int[] imageData;

  private BufferedImage image;

  private final Graphics2D graphics;

  public J2SEGraphicsSurface(final int width, final int height, final boolean withAlpha, final int fillColor) {
    imageData = new int[width * height];
    final DataBuffer dataBuffer = new DataBufferInt(imageData, width * height);
    if (withAlpha) {
      final SampleModel sampleModel = new SinglePixelPackedSampleModel(
          DataBuffer.TYPE_INT, width, height, new int[] {
              0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000
          });
      final WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
      image = new BufferedImage(J2SEGraphicsSurface.ALPHA_COLOR_MODEL, raster, true, new Hashtable());
    }
    else {
      final SampleModel sampleModel = new SinglePixelPackedSampleModel(
          DataBuffer.TYPE_INT, width, height, new int[] {
              0xff0000, 0x00ff00, 0x0000ff
          });
      final WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
      image = new BufferedImage(J2SEGraphicsSurface.NO_ALPHA_COLOR_MODEL, raster, false, new Hashtable());
    }
    graphics = image.createGraphics();
    graphics.setColor(new Color(fillColor));
    graphics.fillRect(0, 0, width, height);
  }

  public Graphics2D getGraphics() {
    return graphics;
  }

  public BufferedImage getImage() {
    return image;
  }

  public int[] getImageData() {
    return imageData;
  }

}
