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
package org.microemu.cldc.ssl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.microedition.io.SecureConnection;
import javax.microedition.io.SecurityInfo;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.microemu.cldc.CertificateImpl;
import org.microemu.cldc.ClosedConnection;
import org.microemu.cldc.SecurityInfoImpl;

public class Connection extends org.microemu.cldc.socket.SocketConnection implements SecureConnection, ClosedConnection {

  private SecurityInfo securityInfo;

  public Connection() {
    securityInfo = null;
  }

  @Override
  public javax.microedition.io.Connection open(final String name) throws IOException {
    if (!org.microemu.cldc.http.Connection.isAllowNetworkConnection()) { throw new IOException("No network"); }
    final int portSepIndex = name.lastIndexOf(':');
    final int port = Integer.parseInt(name.substring(portSepIndex + 1));
    final String host = name.substring("ssl://".length(), portSepIndex);

    // TODO validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {

          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          @Override
          public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
          }

          @Override
          public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
          }
        }
    };

    try {
      final SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      final SSLSocketFactory factory = sc.getSocketFactory();
      socket = factory.createSocket(host, port);
    }
    catch (final NoSuchAlgorithmException ex) {
      throw new IOException(ex.toString());
    }
    catch (final KeyManagementException ex) {
      throw new IOException(ex.toString());
    }

    return this;
  }

  @Override
  public void close() throws IOException {
    // TODO fix differences between Java ME and Java SE

    socket.close();
  }

  @Override
  public SecurityInfo getSecurityInfo() throws IOException {
    if (securityInfo == null) {
      final SSLSession session = ((SSLSocket)socket).getSession();
      final Certificate[] certs = session.getPeerCertificates();
      if (certs.length == 0) { throw new IOException(); }
      securityInfo = new SecurityInfoImpl(session.getCipherSuite(), session.getProtocol(), new CertificateImpl((X509Certificate)certs[0]));
    }

    return securityInfo;
  }

}
