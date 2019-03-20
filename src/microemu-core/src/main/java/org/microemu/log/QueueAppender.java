/**
 * MicroEmulator Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net> Copyright (C)
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
package org.microemu.log;

import java.util.LinkedList;
import java.util.List;

public class QueueAppender implements LoggerAppender {

  private final int buferSize;

  private final List queue = new LinkedList();

  public QueueAppender(final int buferSize) {
    this.buferSize = buferSize;
  }

  @Override
  public void append(final LoggingEvent event) {
    queue.add(event);
    if (queue.size() > buferSize) {
      queue.remove(0);
    }
  }

  public LoggingEvent poll() {
    if (queue.size() == 0) { return null; }
    return (LoggingEvent)queue.remove(0);
  }

}
