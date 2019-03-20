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
 */

package org.microemu.cldc.http;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.microedition.io.HttpConnection;
import org.microemu.microedition.io.ConnectionImplementation;

public class Connection implements HttpConnection, ConnectionImplementation {

  protected URLConnection cn;

  protected boolean connected = false;

  protected static boolean allowNetworkConnection = true;

  @Override
  public javax.microedition.io.Connection openConnection(final String name, final int mode, final boolean timeouts) throws IOException {
    if (!Connection.isAllowNetworkConnection()) { throw new IOException("No network"); }
    URL url;
    try {
      url = new URL(name);
    }
    catch (final MalformedURLException ex) {
      throw new IOException(ex.toString());
    }
    cn = url.openConnection();
    cn.setDoOutput(true);
    // J2ME do not follow redirects. Test this url
    // http://www.microemu.org/test/r/
    if (cn instanceof HttpURLConnection) {
      ((HttpURLConnection)cn).setInstanceFollowRedirects(false);
    }
    return this;
  }

  @Override
  public void close() throws IOException {
    if (cn == null) { return; }

    if (cn instanceof HttpURLConnection) {
      ((HttpURLConnection)cn).disconnect();
    }

    cn = null;
  }

  @Override
  public String getURL() {
    if (cn == null) { return null; }

    return cn.getURL().toString();
  }

  @Override
  public String getProtocol() {
    return "http";
  }

  @Override
  public String getHost() {
    if (cn == null) { return null; }

    return cn.getURL().getHost();
  }

  @Override
  public String getFile() {
    if (cn == null) { return null; }

    return cn.getURL().getFile();
  }

  @Override
  public String getRef() {
    if (cn == null) { return null; }

    return cn.getURL().getRef();
  }

  @Override
  public String getQuery() {
    if (cn == null) { return null; }

    // return cn.getURL().getQuery();
    return null;
  }

  @Override
  public int getPort() {
    if (cn == null) { return -1; }

    final int port = cn.getURL().getPort();
    if (port == -1) { return 80; }
    return port;
  }

  @Override
  public String getRequestMethod() {
    if (cn == null) { return null; }

    if (cn instanceof HttpURLConnection) {
      return ((HttpURLConnection)cn).getRequestMethod();
    }
    else {
      return null;
    }
  }

  @Override
  public void setRequestMethod(final String method) throws IOException {
    if (cn == null) { throw new IOException(); }

    if (method.equals(HttpConnection.POST)) {
      cn.setDoOutput(true);
    }

    if (cn instanceof HttpURLConnection) {
      ((HttpURLConnection)cn).setRequestMethod(method);
    }
  }

  @Override
  public String getRequestProperty(final String key) {
    if (cn == null) { return null; }

    return cn.getRequestProperty(key);
  }

  @Override
  public void setRequestProperty(final String key, final String value) throws IOException {
    if ((cn == null) || connected) { throw new IOException(); }

    cn.setRequestProperty(key, value);
  }

  @Override
  public int getResponseCode() throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    if (cn instanceof HttpURLConnection) {
      return ((HttpURLConnection)cn).getResponseCode();
    }
    else {
      return -1;
    }
  }

  @Override
  public String getResponseMessage() throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    if (cn instanceof HttpURLConnection) {
      return ((HttpURLConnection)cn).getResponseMessage();
    }
    else {
      return null;
    }
  }

  @Override
  public long getExpiration() throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getExpiration();
  }

  @Override
  public long getDate() throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getDate();
  }

  @Override
  public long getLastModified() throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getLastModified();
  }

  @Override
  public String getHeaderField(final String name) throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getHeaderField(name);
  }

  @Override
  public int getHeaderFieldInt(final String name, final int def) throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getHeaderFieldInt(name, def);
  }

  @Override
  public long getHeaderFieldDate(final String name, final long def) throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getHeaderFieldDate(name, def);
  }

  @Override
  public String getHeaderField(final int n) throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getHeaderField(getImplIndex(n));
  }

  @Override
  public String getHeaderFieldKey(final int n) throws IOException {
    if (cn == null) { throw new IOException(); }
    if (!connected) {
      cn.connect();
      connected = true;
    }

    return cn.getHeaderFieldKey(getImplIndex(n));
  }

  private int getImplIndex(int index) {
    if ((cn.getHeaderFieldKey(0) == null) && (cn.getHeaderField(0) != null)) {
      index++;
    }
    return index;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    if (cn == null) { throw new IOException(); }

    connected = true;

    return cn.getInputStream();
  }

  @Override
  public DataInputStream openDataInputStream() throws IOException {
    return new DataInputStream(openInputStream());
  }

  @Override
  public OutputStream openOutputStream() throws IOException {
    if (cn == null) { throw new IOException(); }

    connected = true;

    return cn.getOutputStream();
  }

  @Override
  public DataOutputStream openDataOutputStream() throws IOException {
    return new DataOutputStream(openOutputStream());
  }

  @Override
  public String getType() {
    try {
      return getHeaderField("content-type");
    }
    catch (final IOException ex) {
      return null;
    }
  }

  @Override
  public String getEncoding() {
    try {
      return getHeaderField("content-encoding");
    }
    catch (final IOException ex) {
      return null;
    }
  }

  @Override
  public long getLength() {
    try {
      return getHeaderFieldInt("content-length", -1);
    }
    catch (final IOException ex) {
      return -1;
    }
  }

  public static boolean isAllowNetworkConnection() {
    return Connection.allowNetworkConnection;
  }

  public static void setAllowNetworkConnection(final boolean allowNetworkConnection) {
    Connection.allowNetworkConnection = allowNetworkConnection;
  }

}
