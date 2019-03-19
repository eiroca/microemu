/**
 * Copyright (C) 2006-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - GPL >= 3.0
 * 
 * Portion Copyright (C) Juan Antonio Agudo
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/
 */
package net.eiroca.j2me.bubblet;

/**
 * The Class CellBlock.
 */
public class CellBlock {

  /** The xval. */
  int xval;

  /** The yval. */
  int yval;

  /** The color. */
  int color;

  /**
   * Instantiates a new cell block.
   *
   * @param x the x
   * @param y the y
   */
  public CellBlock(final int x, final int y) {
    xval = x;
    yval = y;
  }

  /**
   * Instantiates a new cell block.
   *
   * @param x the x
   * @param y the y
   * @param color the color
   */
  public CellBlock(final int x, final int y, final int color) {
    xval = x;
    yval = y;
    this.color = color;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(final Object o) {
    if (o == this) { return true; }
    if ((o instanceof CellBlock)) {
      final CellBlock c = (CellBlock)o;
      if ((c.xval == xval) && (c.yval == yval)) { return true; }
    }
    return false;
  }

}
