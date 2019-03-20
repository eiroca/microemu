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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.microemu.app.util.MRUListListener;

/**
 * @author vlads
 *
 */
public class JMRUMenu extends JMenu implements MRUListListener {

  private static final long serialVersionUID = 1L;

  public static class MRUActionEvent extends ActionEvent {

    private static final long serialVersionUID = 1L;

    Object sourceMRU;

    public MRUActionEvent(final Object sourceMRU, final ActionEvent orig) {
      super(orig.getSource(), orig.getID(), orig.getActionCommand(), orig.getWhen(), orig.getModifiers());
      this.sourceMRU = sourceMRU;
    }

    public Object getSourceMRU() {
      return sourceMRU;
    }

  }

  public JMRUMenu(final String s) {
    super(s);
  }

  @Override
  public void listItemChanged(final Object item) {
    final String label = item.toString();
    for (int i = 0; i < getItemCount(); i++) {
      if (getItem(i).getText().equals(label)) {
        remove(i);
        break;
      }
    }
    final AbstractAction a = new AbstractAction(label) {

      private static final long serialVersionUID = 1L;
      Object sourceMRU = item;

      @Override
      public void actionPerformed(final ActionEvent e) {
        JMRUMenu.this.fireActionPerformed(new MRUActionEvent(sourceMRU, e));
      }
    };

    final JMenuItem menu = new JMenuItem(a);
    this.insert(menu, 0);
  }

  /**
   * Do not create new Event
   */
  @Override
  protected void fireActionPerformed(final ActionEvent event) {
    final Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ActionListener.class) {
        ((ActionListener)listeners[i + 1]).actionPerformed(event);
      }
    }
  }

}
