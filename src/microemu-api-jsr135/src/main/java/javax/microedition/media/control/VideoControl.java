/**
 * MicroEmulator
 * 
 * Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
 * 
 * It is licensed under the following two licenses as alternatives:
 * 
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 
 * 2. Apache License (the "AL") Version 2.0
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
 */
package javax.microedition.media.control;

import javax.microedition.media.MediaException;

public interface VideoControl extends GUIControl {

  public static final int USE_DIRECT_VIDEO = 1;

  public abstract Object initDisplayMode(int i, Object obj);

  public abstract void setDisplayLocation(int i, int j);

  public abstract int getDisplayX();

  public abstract int getDisplayY();

  public abstract void setVisible(boolean flag);

  public abstract void setDisplaySize(int i, int j) throws MediaException;

  public abstract void setDisplayFullScreen(boolean flag) throws MediaException;

  public abstract int getSourceWidth();

  public abstract int getSourceHeight();

  public abstract int getDisplayWidth();

  public abstract int getDisplayHeight();

  public abstract byte[] getSnapshot(String s) throws MediaException;

}
