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

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface MIDIControl extends Control {

  public static final int NOTE_ON = 144;
  public static final int CONTROL_CHANGE = 176;

  public abstract boolean isBankQuerySupported();

  public abstract int[] getProgram(int i) throws MediaException;

  public abstract int getChannelVolume(int i);

  public abstract void setProgram(int i, int j, int k);

  public abstract void setChannelVolume(int i, int j);

  public abstract int[] getBankList(boolean flag) throws MediaException;

  public abstract int[] getProgramList(int i) throws MediaException;

  public abstract String getProgramName(int i, int j) throws MediaException;

  public abstract String getKeyName(int i, int j, int k) throws MediaException;

  public abstract void shortMidiEvent(int i, int j, int k);

  public abstract int longMidiEvent(byte abyte0[], int i, int j);

}
