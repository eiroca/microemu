/**
 * MicroEmulator Copyright (C) 2009 Bartek Teodorczyk <barteo@barteo.net>
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
package org.microemu.util;

import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;

public interface ExtendedRecordListener extends RecordListener {

  int RECORD_ADD = 1;
  int RECORD_READ = 2;
  int RECORD_CHANGE = 3;
  int RECORD_DELETE = 4;
  int RECORDSTORE_OPEN = 8;
  int RECORDSTORE_CLOSE = 9;
  int RECORDSTORE_DELETE = 10;

  void recordEvent(int type, long timestamp, RecordStore recordStore, int recordId);

  void recordStoreEvent(int type, long timestamp, String recordStoreName);

}
