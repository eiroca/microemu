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

import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface RecordControl extends Control {

  public abstract void setRecordStream(OutputStream outputstream);

  public abstract void setRecordLocation(String s) throws IOException, MediaException;

  public abstract String getContentType();

  public abstract void startRecord();

  public abstract void stopRecord();

  public abstract void commit() throws IOException;

  public abstract int setRecordSizeLimit(int i) throws MediaException;

  public abstract void reset() throws IOException;
}
