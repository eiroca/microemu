/**
 * MicroEmulator Copyright (C) 2001-2005 Bartek Teodorczyk <barteo@barteo.net>
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

package org.microemu.util;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import org.microemu.MicroEmulator;
import org.microemu.RecordStoreManager;

public class MemoryRecordStoreManager implements RecordStoreManager {

  private final Hashtable recordStores = new Hashtable();

  private ExtendedRecordListener recordListener = null;

  @Override
  public void init(final MicroEmulator emulator) {
  }

  @Override
  public String getName() {
    return "Memory record store";
  }

  @Override
  public void deleteRecordStore(final String recordStoreName) throws RecordStoreNotFoundException, RecordStoreException {
    final RecordStoreImpl recordStoreImpl = (RecordStoreImpl)recordStores.get(recordStoreName);
    if (recordStoreImpl == null) { throw new RecordStoreNotFoundException(recordStoreName); }
    if (recordStoreImpl.isOpen()) { throw new RecordStoreException(); }
    recordStores.remove(recordStoreName);

    fireRecordStoreListener(ExtendedRecordListener.RECORDSTORE_DELETE, recordStoreName);
  }

  @Override
  public RecordStore openRecordStore(final String recordStoreName, final boolean createIfNecessary)
      throws RecordStoreNotFoundException {
    RecordStoreImpl recordStoreImpl = (RecordStoreImpl)recordStores.get(recordStoreName);
    if (recordStoreImpl == null) {
      if (!createIfNecessary) { throw new RecordStoreNotFoundException(recordStoreName); }
      recordStoreImpl = new RecordStoreImpl(this, recordStoreName);
      recordStores.put(recordStoreName, recordStoreImpl);
    }
    recordStoreImpl.setOpen(true);
    if (recordListener != null) {
      recordStoreImpl.addRecordListener(recordListener);
    }

    fireRecordStoreListener(ExtendedRecordListener.RECORDSTORE_OPEN, recordStoreName);

    return recordStoreImpl;
  }

  @Override
  public String[] listRecordStores() {
    String[] result = null;

    int i = 0;
    for (final Enumeration e = recordStores.keys(); e.hasMoreElements();) {
      if (result == null) {
        result = new String[recordStores.size()];
      }
      result[i] = (String)e.nextElement();
      i++;
    }

    return result;
  }

  @Override
  public void deleteRecord(final RecordStoreImpl recordStoreImpl, final int recordId) {
  }

  @Override
  public void loadRecord(final RecordStoreImpl recordStoreImpl, final int recordId) {
  }

  @Override
  public void saveRecord(final RecordStoreImpl recordStoreImpl, final int recordId) {
  }

  public void init() {
    deleteStores();
  }

  @Override
  public void deleteStores() {
    if (recordStores != null) {
      recordStores.clear();
    }
  }

  @Override
  public int getSizeAvailable(final RecordStoreImpl recordStoreImpl) {
    // FIXME returns too much
    return (int)Runtime.getRuntime().freeMemory();
  }

  @Override
  public void setRecordListener(final ExtendedRecordListener recordListener) {
    this.recordListener = recordListener;
  }

  @Override
  public void fireRecordStoreListener(final int type, final String recordStoreName) {
    if (recordListener != null) {
      recordListener.recordStoreEvent(type, System.currentTimeMillis(), recordStoreName);
    }
  }
}
