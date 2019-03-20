/**
 * Copyright (C) 2006-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - GPL >= 3.0
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
package net.eiroca.j2me.host;

import java.util.ArrayList;
import java.util.List;
import org.microemu.app.embedded.Main;

public class J2meHost implements Runnable {

  private final List<String> params = new ArrayList<>();

  public J2meHost(final Class<?> midlet, final String[] args) {
    params.add("--quit");
    final String className = midlet.getCanonicalName();
    params.add(className);
    for (final String arg : args) {
      params.add(arg);
    }
  }

  @Override
  public void run() {
    Main.run(params);
  }

}
