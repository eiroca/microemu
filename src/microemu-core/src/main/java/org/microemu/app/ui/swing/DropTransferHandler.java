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
package org.microemu.app.ui.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import org.microemu.app.Common;
import org.microemu.app.ui.Message;
import org.microemu.app.util.IOUtils;
import org.microemu.log.Logger;

/**
 * @author vlads
 *
 */
public class DropTransferHandler extends TransferHandler {

  private static final long serialVersionUID = 1L;

  private static DataFlavor uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String", null);

  private static boolean debugImport = false;

  @Override
  public int getSourceActions(final JComponent c) {
    return TransferHandler.COPY;
  }

  @Override
  public boolean canImport(final JComponent comp, final DataFlavor transferFlavors[]) {
    for (int i = 0; i < transferFlavors.length; i++) {
      final Class representationclass = transferFlavors[i].getRepresentationClass();
      // URL from Explorer or Firefox, KDE
      if ((representationclass != null) && URL.class.isAssignableFrom(representationclass)) {
        if (DropTransferHandler.debugImport) {
          Logger.debug("acepted ", transferFlavors[i]);
        }
        return true;
      }
      // Drop from Windows Explorer
      if (DataFlavor.javaFileListFlavor.equals(transferFlavors[i])) {
        if (DropTransferHandler.debugImport) {
          Logger.debug("acepted ", transferFlavors[i]);
        }
        return true;
      }
      // Drop from GNOME
      if (DataFlavor.stringFlavor.equals(transferFlavors[i])) {
        if (DropTransferHandler.debugImport) {
          Logger.debug("acepted ", transferFlavors[i]);
        }
        return true;
      }
      if (DropTransferHandler.uriListFlavor.equals(transferFlavors[i])) {
        if (DropTransferHandler.debugImport) {
          Logger.debug("acepted ", transferFlavors[i]);
        }
        return true;
      }
      //          String mimePrimaryType = transferFlavors[i].getPrimaryType();
      //			String mimeSubType = transferFlavors[i].getSubType();
      //			if ((mimePrimaryType != null) && (mimeSubType != null)) {
      //				if (mimePrimaryType.equals("text") && mimeSubType.equals("uri-list")) {
      //					Logger.debug("acepted ", transferFlavors[i]);
      //					return true;
      //				}
      //			}
      if (DropTransferHandler.debugImport) {
        Logger.debug(i + " unknown import ", transferFlavors[i]);
      }
    }
    if (DropTransferHandler.debugImport) {
      Logger.debug("import rejected");
    }
    return false;
  }

  @Override
  public boolean importData(final JComponent comp, final Transferable t) {
    final DataFlavor[] transferFlavors = t.getTransferDataFlavors();
    for (int i = 0; i < transferFlavors.length; i++) {
      // Drop from Windows Explorer
      if (DataFlavor.javaFileListFlavor.equals(transferFlavors[i])) {
        Logger.debug("importing", transferFlavors[i]);
        try {
          final List fileList = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
          if (fileList.get(0) instanceof File) {
            final File f = (File)fileList.get(0);
            if (Common.isMIDletUrlExtension(f.getName())) {
              Common.openMIDletUrlSafe(IOUtils.getCanonicalFileURL(f));
            }
            else {
              Message.warn("Unable to open " + f.getAbsolutePath() + ", Only JAD files are acepted");
            }
          }
          else {
            Logger.debug("Unknown object in list ", fileList.get(0));
          }
        }
        catch (final UnsupportedFlavorException e) {
          Logger.debug(e);
        }
        catch (final IOException e) {
          Logger.debug(e);
        }
        return true;
      }

      // Drop from GNOME Firefox
      if (DataFlavor.stringFlavor.equals(transferFlavors[i])) {
        Object data;
        try {
          data = t.getTransferData(DataFlavor.stringFlavor);
        }
        catch (final UnsupportedFlavorException e) {
          continue;
        }
        catch (final IOException e) {
          continue;
        }
        if (data instanceof String) {
          Logger.debug("importing", transferFlavors[i]);
          final String path = getPathString((String)data);
          if (Common.isMIDletUrlExtension(path)) {
            Common.openMIDletUrlSafe(path);
          }
          else {
            Message.warn("Unable to open " + path + ", Only JAD files are acepted");
          }
          return true;
        }
      }
      // Drop from GNOME Nautilus
      if (DropTransferHandler.uriListFlavor.equals(transferFlavors[i])) {
        Object data;
        try {
          data = t.getTransferData(DropTransferHandler.uriListFlavor);
        }
        catch (final UnsupportedFlavorException e) {
          continue;
        }
        catch (final IOException e) {
          continue;
        }
        if (data instanceof String) {
          Logger.debug("importing", transferFlavors[i]);
          final String path = getPathString((String)data);
          if (Common.isMIDletUrlExtension(path)) {
            Common.openMIDletUrlSafe(path);
          }
          else {
            Message.warn("Unable to open " + path + ", Only JAD files are acepted");
          }
          return true;
        }
      }
      if (DropTransferHandler.debugImport) {
        Logger.debug(i + " unknown importData ", transferFlavors[i]);
      }
    }
    // This is the second best option since it works incorrectly  on Max OS X making url like this [file://localhost/users/work/app.jad]
    for (final DataFlavor transferFlavor : transferFlavors) {
      final Class representationclass = transferFlavor.getRepresentationClass();
      // URL from Explorer or Firefox, KDE
      if ((representationclass != null) && URL.class.isAssignableFrom(representationclass)) {
        Logger.debug("importing", transferFlavor);
        try {
          final URL jadUrl = (URL)t.getTransferData(transferFlavor);
          final String urlString = jadUrl.toExternalForm();
          if (Common.isMIDletUrlExtension(urlString)) {
            Common.openMIDletUrlSafe(urlString);
          }
          else {
            Message.warn("Unable to open " + urlString + ", Only JAD url are acepted");
          }
        }
        catch (final UnsupportedFlavorException e) {
          Logger.debug(e);
        }
        catch (final IOException e) {
          Logger.debug(e);
        }
        return true;
      }
    }
    return false;
  }

  private String getPathString(final String path) {
    if (path == null) { return null; }
    final StringTokenizer st = new StringTokenizer(path.trim(), "\n\r");
    if (st.hasMoreTokens()) { return st.nextToken(); }
    return path;
  }

}
