/**
 * MicroEmulator Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net>
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
package org.microemu.app.ui.swing;

import java.awt.Rectangle;

public class XYConstraints implements Cloneable {

  int x;
  int y;
  int width; // <= 0 means use the components's preferred size
  int height; // <= 0 means use the components's preferred size

  public XYConstraints() {
    this(0, 0, 0, 0);
  }

  public XYConstraints(final int x, final int y, final int width, final int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public XYConstraints(final Rectangle rect) {
    x = rect.x;
    y = rect.y;
    width = rect.width;
    height = rect.height;
  }

  public int getX() {
    return x;
  }

  public void setX(final int x) {
    this.x = x;
  }

  public int getY() {
    return y;
  }

  public void setY(final int y) {
    this.y = y;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(final int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(final int height) {
    this.height = height;
  }

  /**
   * Returns the hashcode for this XYConstraints.
   */
  @Override
  public int hashCode() {
    return x ^ (y * 37) ^ (width * 43) ^ (height * 47);
  }

  /**
   * Checks whether two XYConstraints are equal.
   */
  @Override
  public boolean equals(final Object that) {
    if (that instanceof XYConstraints) {
      final XYConstraints other = (XYConstraints)that;
      return (other.x == x) && (other.y == y) && (other.width == width) && (other.height == height);
    }
    return false;
  }

  @Override
  public Object clone() {
    return new XYConstraints(x, y, width, height);
  }

  @Override
  public String toString() {
    return "XYConstraints[" + x + "," + y + "," + width + "," + height + "]";
  }

}
