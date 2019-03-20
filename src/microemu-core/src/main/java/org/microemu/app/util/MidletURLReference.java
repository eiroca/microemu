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
package org.microemu.app.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;
import org.microemu.log.Logger;
import nanoxml.XMLElement;

/**
 * @author vlads
 *
 */
public class MidletURLReference implements XMLItem {

  private String name;

  private String url;

  public MidletURLReference() {
    super();
  }

  /**
   * @param name MIDlet name
   * @param url URL to locate this URL
   */
  public MidletURLReference(final String name, final String url) {
    super();
    this.name = name;
    this.url = url;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof MidletURLReference)) { return false; }
    return ((MidletURLReference)obj).url.equals(url);
  }

  @Override
  public String toString() {
    // make the text presentation shorter.
    URL u;
    try {
      u = new URL(url);
    }
    catch (final MalformedURLException e) {
      Logger.error(e);
      return url;
    }
    final StringBuffer b = new StringBuffer();

    final String scheme = u.getProtocol();
    if (scheme.equals("file") || scheme.startsWith("http")) {
      b.append(scheme).append("://");
      if (u.getHost() != null) {
        b.append(u.getHost());
      }
      final Vector pathComponents = new Vector();
      final String pathSeparator = "/";
      final StringTokenizer st = new StringTokenizer(u.getPath(), pathSeparator);
      while (st.hasMoreTokens()) {
        pathComponents.add(st.nextToken());
      }
      if (pathComponents.size() > 3) {
        b.append(pathSeparator);
        b.append(pathComponents.get(0));
        b.append(pathSeparator).append("...").append(pathSeparator);
        b.append(pathComponents.get(pathComponents.size() - 2));
        b.append(pathSeparator);
        b.append(pathComponents.get(pathComponents.size() - 1));
      }
      else {
        b.append(u.getPath());
      }

    }
    else {
      b.append(url);
    }
    if (name != null) {
      b.append(" - ");
      b.append(name);
    }
    return b.toString();
  }

  @Override
  public void read(final XMLElement xml) {
    name = xml.getChildString("name", "");
    url = xml.getChildString("url", "");
  }

  @Override
  public void save(final XMLElement xml) {
    xml.removeChildren();
    xml.addChild("name", name);
    xml.addChild("url", url);
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

}
