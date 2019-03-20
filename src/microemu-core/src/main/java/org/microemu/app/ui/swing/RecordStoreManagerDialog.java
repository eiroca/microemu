/**
 * MicroEmulator Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreNotOpenException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.microemu.RecordStoreManager;
import org.microemu.app.Common;
import org.microemu.app.Config;
import org.microemu.app.util.FileRecordStoreManager;
import org.microemu.log.Logger;
import org.microemu.util.ExtendedRecordListener;
import org.microemu.util.MemoryRecordStoreManager;

public class RecordStoreManagerDialog extends JFrame {

  private static final long serialVersionUID = 1L;

  private Common common;

  private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.S");

  private final JLabel recordStoreTypeLabel = new JLabel();

  private final JLabel suiteNameLabel = new JLabel();

  private RecordStoreChangePanel recordStoreChangePanel = null;

  private final DefaultTableModel modelTable = new DefaultTableModel();

  private final JTable logTable = new JTable(modelTable);

  private final JScrollPane logScrollPane = new JScrollPane(logTable);

  private final ActionListener recordStoreTypeChangeListener = e -> {
    if (recordStoreChangePanel == null) {
      recordStoreChangePanel = new RecordStoreChangePanel(common);
    }
    if (SwingDialogWindow.show(RecordStoreManagerDialog.this, "Change Record Store...", recordStoreChangePanel,
        true)) {
      final String recordStoreName = recordStoreChangePanel.getSelectedRecordStoreName();
      if (!recordStoreName.equals(common.getRecordStoreManager().getName())) {
        if (JOptionPane.showConfirmDialog(RecordStoreManagerDialog.this,
            "Changing record store type requires MIDlet restart. \n"
                + "Do you want to proceed? All MIDlet data will be lost.",
            "Question?",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
          for (int i = modelTable.getRowCount() - 1; i >= 0; i--) {
            modelTable.removeRow(i);
          }
          RecordStoreManager manager;
          if (recordStoreName.equals("File record store")) {
            manager = new FileRecordStoreManager();
          }
          else {
            manager = new MemoryRecordStoreManager();
          }
          common.setRecordStoreManager(manager);
          Config.setRecordStoreManagerClassName(manager.getClass().getName());
          refresh();
          try {
            common.initMIDlet(true);
          }
          catch (final Exception ex) {
            Logger.error(ex);
          }
        }
      }
    }
  };

  public RecordStoreManagerDialog(final Frame owner, final Common common) {
    super("Record Store Manager");

    this.common = common;

    setIconImage(owner.getIconImage());
    setFocusableWindowState(false);

    setLayout(new BorderLayout());

    refresh();

    final JButton recordStoreTypeChangeButton = new JButton("Change...");
    recordStoreTypeChangeButton.addActionListener(recordStoreTypeChangeListener);

    final JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new GridBagLayout());
    final GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(3, 3, 3, 3);

    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.fill = GridBagConstraints.NONE;
    c.weightx = 0.0;
    c.weighty = 0.0;
    headerPanel.add(new JLabel("Record store type:"), c);

    c.gridx = 1;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1.0;
    c.weighty = 0.0;
    headerPanel.add(recordStoreTypeLabel, c);

    c.gridx = 2;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.fill = GridBagConstraints.NONE;
    c.weightx = 0.0;
    c.weighty = 0.0;
    headerPanel.add(recordStoreTypeChangeButton, c);

    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.fill = GridBagConstraints.NONE;
    c.weightx = 0.0;
    c.weighty = 0.0;
    headerPanel.add(new JLabel("Suite name:"), c);

    c.gridx = 1;
    c.gridy = 1;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1.0;
    c.weighty = 0.0;
    headerPanel.add(suiteNameLabel, c);

    modelTable.addColumn("Timestamp");
    modelTable.addColumn("Action type");
    modelTable.addColumn("Record store name");
    modelTable.addColumn("Details");
    logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

      private final Color SUPER_LIGHT_GRAY = new Color(240, 240, 240);

      @Override
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        if ((row % 2) == 0) {
          setBackground(Color.WHITE);
        }
        else {
          setBackground(SUPER_LIGHT_GRAY);
        }

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }

      private static final long serialVersionUID = 1L;

    });
    logTable.setShowGrid(false);
    logScrollPane.setAutoscrolls(true);

    final JTabbedPane viewPanel = new JTabbedPane();
    // viewPanel.addTab("Records view", new JLabel("Records view"));
    viewPanel.addTab("Log view", logScrollPane);

    getContentPane().add(headerPanel, BorderLayout.NORTH);
    getContentPane().add(viewPanel, BorderLayout.CENTER);
  }

  public void refresh() {
    recordStoreTypeLabel.setText(common.getRecordStoreManager().getName());

    suiteNameLabel.setText(common.getLauncher().getSuiteName());

    common.getRecordStoreManager().setRecordListener(new ExtendedRecordListener() {

      @Override
      public void recordEvent(final int type, final long timestamp, final RecordStore recordStore, final int recordId) {
        String eventMessageType = null;

        switch (type) {
          case ExtendedRecordListener.RECORD_ADD:
            eventMessageType = "added";
            break;
          case ExtendedRecordListener.RECORD_READ:
            eventMessageType = "read";
            break;
          case ExtendedRecordListener.RECORD_CHANGE:
            eventMessageType = "changed";
            break;
          case ExtendedRecordListener.RECORD_DELETE:
            eventMessageType = "deleted";
            break;
        }

        try {
          modelTable.addRow(new Object[] {
              dateFormat.format(new Date(timestamp)),
              "record " + eventMessageType, recordStore.getName(), "recordId = " + recordId
          });
        }
        catch (final RecordStoreNotOpenException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        logTable.scrollRectToVisible(logTable.getCellRect(modelTable.getRowCount() - 1, 0, true));
      }

      @Override
      public void recordStoreEvent(final int type, final long timestamp, final String recordStoreName) {
        String eventMessageType = null;

        switch (type) {
          case ExtendedRecordListener.RECORDSTORE_OPEN:
            eventMessageType = "opened";
            break;
          case ExtendedRecordListener.RECORDSTORE_CLOSE:
            eventMessageType = "closed";
            break;
          case ExtendedRecordListener.RECORDSTORE_DELETE:
            eventMessageType = "deleted";
            break;
        }

        modelTable.addRow(new Object[] {
            dateFormat.format(new Date(timestamp)), "store " + eventMessageType,
            recordStoreName, null
        });
        logTable.scrollRectToVisible(logTable.getCellRect(modelTable.getRowCount() - 1, 0, true));
      }

      @Override
      public void recordAdded(final RecordStore recordStore, final int recordId) {
        // already handled by recordEvent
      }

      @Override
      public void recordChanged(final RecordStore recordStore, final int recordId) {
        // already handled by recordEvent
      }

      @Override
      public void recordDeleted(final RecordStore recordStore, final int recordId) {
        // already handled by recordEvent
      }

    });
  }

}
