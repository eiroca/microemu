/**
   *  MicroEmulator
 *  Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package org.microemu.app.embedded;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.midlet.MIDletStateChangeException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.microemu.DisplayAccess;
import org.microemu.DisplayComponent;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.app.Common;
import org.microemu.app.Config;
import org.microemu.app.capture.AnimatedGifEncoder;
import org.microemu.app.ui.DisplayRepaintListener;
import org.microemu.app.ui.Message;
import org.microemu.app.ui.ResponseInterfaceListener;
import org.microemu.app.ui.StatusBarListener;
import org.microemu.app.ui.swing.DropTransferHandler;
import org.microemu.app.ui.swing.ExtensionFileFilter;
import org.microemu.app.ui.swing.RecordStoreManagerDialog;
import org.microemu.app.ui.swing.ResizeDeviceDisplayDialog;
import org.microemu.app.ui.swing.SwingAboutDialog;
import org.microemu.app.ui.swing.SwingDeviceComponent;
import org.microemu.app.ui.swing.SwingDialogWindow;
import org.microemu.app.ui.swing.SwingDisplayComponent;
import org.microemu.app.ui.swing.SwingErrorMessageDialogPanel;
import org.microemu.app.ui.swing.SwingLogConsoleDialog;
import org.microemu.app.ui.swing.SwingSelectDevicePanel;
import org.microemu.app.util.DeviceEntry;
import org.microemu.device.Device;
import org.microemu.device.DeviceDisplay;
import org.microemu.device.DeviceFactory;
import org.microemu.device.EmulatorContext;
import org.microemu.device.FontManager;
import org.microemu.device.InputMethod;
import org.microemu.device.impl.DeviceDisplayImpl;
import org.microemu.device.impl.DeviceImpl;
import org.microemu.device.impl.Rectangle;
import org.microemu.device.impl.SoftButton;
import org.microemu.device.j2se.J2SEDevice;
import org.microemu.device.j2se.J2SEDeviceDisplay;
import org.microemu.device.j2se.J2SEFontManager;
import org.microemu.device.j2se.J2SEGraphicsSurface;
import org.microemu.device.j2se.J2SEInputMethod;
import org.microemu.log.Logger;
import org.microemu.log.QueueAppender;

public class Main extends JFrame {

	private static final long serialVersionUID = 27183137807109218L;

	protected Common common;

	protected SwingSelectDevicePanel selectDevicePanel = null;
	private JFileChooser captureFileChooser = null;
	private JMenuItem menuSelectDevice;
	private JMenuItem menuStartCapture;
	private JMenuItem menuStopCapture;
	private JCheckBoxMenuItem menuMIDletNetworkConnection;
	private JCheckBoxMenuItem menuLogConsole;
	private JCheckBoxMenuItem menuRecordStoreManager;
	private JFrame scaledDisplayFrame;
	private JCheckBoxMenuItem[] zoomLevels;
	private SwingDeviceComponent devicePanel;
	private SwingLogConsoleDialog logConsoleDialog;
	private RecordStoreManagerDialog recordStoreManagerDialog;
	private QueueAppender logQueueAppender;
	private DeviceEntry deviceEntry;
	private AnimatedGifEncoder encoder;
	private JLabel statusBar = new JLabel("Status");
	private JButton resizeButton = new JButton("Resize");
	private ResizeDeviceDisplayDialog resizeDeviceDisplayDialog = null;

	protected EmulatorContext emulatorContext = new EmulatorContext() {

		private InputMethod inputMethod = new J2SEInputMethod();
		private DeviceDisplay deviceDisplay = new J2SEDeviceDisplay(this);
		private FontManager fontManager = new J2SEFontManager();

		public DisplayComponent getDisplayComponent() {
			return devicePanel.getDisplayComponent();
		}

		public InputMethod getDeviceInputMethod() {
			return inputMethod;
		}

		public DeviceDisplay getDeviceDisplay() {
			return deviceDisplay;
		}

		public FontManager getDeviceFontManager() {
			return fontManager;
		}

		public InputStream getResourceAsStream(Class origClass, String name) {
			return MIDletBridge.getCurrentMIDlet().getClass().getResourceAsStream(name);
		}

		public boolean platformRequest(final String URL) {
			new Thread(new Runnable() {
				public void run() {
					Message.info("MIDlet requests that the device handle the following URL: " + URL);
				}
			}).start();

			return false;
		}
	};

	private ActionListener menuStartCaptureListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (captureFileChooser == null) {
				ExtensionFileFilter fileFilter = new ExtensionFileFilter("GIF files");
				fileFilter.addExtension("gif");
				captureFileChooser = new JFileChooser();
				captureFileChooser.setFileFilter(fileFilter);
				captureFileChooser.setDialogTitle("Capture to GIF File...");
				captureFileChooser.setCurrentDirectory(new File(Config.getRecentDirectory("recentCaptureDirectory")));
			}

			if (captureFileChooser.showSaveDialog(Main.this) == JFileChooser.APPROVE_OPTION) {
				Config.setRecentDirectory("recentCaptureDirectory",
						captureFileChooser.getCurrentDirectory().getAbsolutePath());
				String name = captureFileChooser.getSelectedFile().getName();
				if (!name.toLowerCase().endsWith(".gif") && name.indexOf('.') == -1) {
					name = name + ".gif";
				}
				File captureFile = new File(captureFileChooser.getSelectedFile().getParentFile(), name);
				if (!allowOverride(captureFile)) {
					return;
				}

				encoder = new AnimatedGifEncoder();
				encoder.start(captureFile.getAbsolutePath());

				menuStartCapture.setEnabled(false);
				menuStopCapture.setEnabled(true);

				((SwingDisplayComponent) emulatorContext.getDisplayComponent())
						.addDisplayRepaintListener(new DisplayRepaintListener() {
							long start = 0;

							public void repaintInvoked(Object repaintObject) {
								synchronized (Main.this) {
									if (encoder != null) {
										if (start == 0) {
											start = System.currentTimeMillis();
										} else {
											long current = System.currentTimeMillis();
											encoder.setDelay((int) (current - start));
											start = current;
										}
										encoder.addFrame(((J2SEGraphicsSurface) repaintObject).getImage());
									}
								}
							}
						});
			}
		}

		private boolean allowOverride(File file) {
			if (file.exists()) {
				int answer = JOptionPane.showConfirmDialog(Main.this, "Override the file:" + file + "?", "Question?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (answer == 1 /* no */) {
					return false;
				}
			}

			return true;
		}
	};

	private ActionListener menuStopCaptureListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			menuStopCapture.setEnabled(false);

			synchronized (Main.this) {
				encoder.finish();
				encoder = null;
			}

			menuStartCapture.setEnabled(true);
		}
	};

	private ActionListener menuMIDletNetworkConnectionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			org.microemu.cldc.http.Connection.setAllowNetworkConnection(menuMIDletNetworkConnection.getState());
		}

	};

	private ActionListener menuRecordStoreManagerListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (recordStoreManagerDialog == null) {
				recordStoreManagerDialog = new RecordStoreManagerDialog(Main.this, common);
				recordStoreManagerDialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						menuRecordStoreManager.setState(false);
					}
				});
				recordStoreManagerDialog.pack();
				Rectangle window = Config.getWindow("recordStoreManager", new Rectangle(0, 0, 640, 320));
				recordStoreManagerDialog.setBounds(window.x, window.y, window.width, window.height);
			}
			recordStoreManagerDialog.setVisible(!recordStoreManagerDialog.isVisible());
		}
	};

	private ActionListener menuLogConsoleListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (logConsoleDialog == null) {
				logConsoleDialog = new SwingLogConsoleDialog(Main.this, Main.this.logQueueAppender);
				logConsoleDialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						menuLogConsole.setState(false);
					}
				});
				logConsoleDialog.pack();
				// To avoid NPE on MacOS setFocusableWindowState(false) have to be called after
				// pack()
				logConsoleDialog.setFocusableWindowState(false);
				Rectangle window = Config.getWindow("logConsole", new Rectangle(0, 0, 640, 320));
				logConsoleDialog.setBounds(window.x, window.y, window.width, window.height);
			}
			logConsoleDialog.setVisible(!logConsoleDialog.isVisible());
		}
	};

	private ActionListener menuAboutListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			SwingDialogWindow.show(Main.this, "About", new SwingAboutDialog(), false);
		}
	};

	private ActionListener menuExitListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			synchronized (Main.this) {
				if (encoder != null) {
					encoder.finish();
					encoder = null;
				}
			}

			if (logConsoleDialog != null) {
				Config.setWindow(
						"logConsole", new Rectangle(logConsoleDialog.getX(), logConsoleDialog.getY(),
								logConsoleDialog.getWidth(), logConsoleDialog.getHeight()),
						logConsoleDialog.isVisible());
			}
			if (recordStoreManagerDialog != null) {
				Config.setWindow("recordStoreManager",
						new Rectangle(recordStoreManagerDialog.getX(), recordStoreManagerDialog.getY(),
								recordStoreManagerDialog.getWidth(), recordStoreManagerDialog.getHeight()),
						recordStoreManagerDialog.isVisible());
			}
			if (scaledDisplayFrame != null) {
				Config.setWindow("scaledDisplay",
						new Rectangle(scaledDisplayFrame.getX(), scaledDisplayFrame.getY(), 0, 0), false);
			}
			Config.setWindow("main",
					new Rectangle(Main.this.getX(), Main.this.getY(), Main.this.getWidth(), Main.this.getHeight()),
					true);

			System.exit(0);
		}
	};

	private ActionListener menuSelectDeviceListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (SwingDialogWindow.show(Main.this, "Select device...", selectDevicePanel, true)) {
				if (selectDevicePanel.getSelectedDeviceEntry().equals(deviceEntry)) {
					return;
				}
				int restartMidlet = 1;
				if (MIDletBridge.getCurrentMIDlet() != common.getLauncher()) {
					restartMidlet = JOptionPane.showConfirmDialog(Main.this,
							"Changing device may trigger MIDlet to the unpredictable state and restart of MIDlet is recommended. \n"
									+ "Do you want to restart the MIDlet? All MIDlet data will be lost.",
							"Question?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				}
				if (!setDevice(selectDevicePanel.getSelectedDeviceEntry())) {
					return;
				}
				if (restartMidlet == 0) {
					try {
						common.initMIDlet(true);
					} catch (Exception ex) {
						System.err.println(ex);
					}
				} else {
					DeviceDisplay deviceDisplay = DeviceFactory.getDevice().getDeviceDisplay();
					DisplayAccess da = MIDletBridge.getMIDletAccess().getDisplayAccess();
					if (da != null) {
						da.sizeChanged();
						deviceDisplay.repaint(0, 0, deviceDisplay.getFullWidth(), deviceDisplay.getFullHeight());
					}
				}
			}
		}
	};

	private ActionListener menuScaledDisplayListener = new ActionListener() {
		private DisplayRepaintListener updateScaledImageListener;

		public void actionPerformed(ActionEvent e) {
			final JCheckBoxMenuItem selectedZoomLevelMenuItem = (JCheckBoxMenuItem) e.getSource();
			if (selectedZoomLevelMenuItem.isSelected()) {
				for (int i = 0; i < zoomLevels.length; ++i) {
					if (zoomLevels[i] != e.getSource()) {
						zoomLevels[i].setSelected(false);
					}
				}
				final int scale = Integer.parseInt(e.getActionCommand());
				if (scaledDisplayFrame != null) {
					((SwingDisplayComponent) emulatorContext.getDisplayComponent())
							.removeDisplayRepaintListener(updateScaledImageListener);
					scaledDisplayFrame.dispose();
				}
				scaledDisplayFrame = new JFrame(getTitle());
				scaledDisplayFrame.setContentPane(new JLabel(new ImageIcon()));
				updateScaledImageListener = new DisplayRepaintListener() {
					public void repaintInvoked(Object repaintObject) {
						updateScaledImage(scale, scaledDisplayFrame);
						scaledDisplayFrame.validate();
					}
				};
				scaledDisplayFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent event) {
						selectedZoomLevelMenuItem.setSelected(false);
					}
				});
				scaledDisplayFrame.getContentPane().addMouseListener(new MouseListener() {
					private MouseListener receiver = ((SwingDisplayComponent) emulatorContext.getDisplayComponent())
							.getMouseListener();

					public void mouseClicked(MouseEvent e) {
						receiver.mouseClicked(createAdaptedMouseEvent(e, scale));
					}

					public void mousePressed(MouseEvent e) {
						receiver.mousePressed(createAdaptedMouseEvent(e, scale));
					}

					public void mouseReleased(MouseEvent e) {
						receiver.mouseReleased(createAdaptedMouseEvent(e, scale));
					}

					public void mouseEntered(MouseEvent e) {
						receiver.mouseEntered(createAdaptedMouseEvent(e, scale));
					}

					public void mouseExited(MouseEvent e) {
						receiver.mouseExited(createAdaptedMouseEvent(e, scale));
					}
				});
				scaledDisplayFrame.getContentPane().addMouseMotionListener(new MouseMotionListener() {
					private MouseMotionListener receiver = ((SwingDisplayComponent) emulatorContext
							.getDisplayComponent()).getMouseMotionListener();

					public void mouseDragged(MouseEvent e) {
						receiver.mouseDragged(createAdaptedMouseEvent(e, scale));
					}

					public void mouseMoved(MouseEvent e) {
						receiver.mouseMoved(createAdaptedMouseEvent(e, scale));
					}
				});
				scaledDisplayFrame.getContentPane().addMouseWheelListener(new MouseWheelListener() {
					private MouseWheelListener receiver = ((SwingDisplayComponent) emulatorContext
							.getDisplayComponent()).getMouseWheelListener();

					public void mouseWheelMoved(MouseWheelEvent e) {
						MouseWheelEvent adaptedEvent = createAdaptedMouseWheelEvent(e, scale);
						receiver.mouseWheelMoved(adaptedEvent);
					}
				});
				scaledDisplayFrame.addKeyListener(devicePanel);

				updateScaledImage(scale, scaledDisplayFrame);
				((SwingDisplayComponent) emulatorContext.getDisplayComponent())
						.addDisplayRepaintListener(updateScaledImageListener);
				scaledDisplayFrame.setIconImage(getIconImage());
				scaledDisplayFrame.setResizable(false);
				Point location = getLocation();
				Dimension size = getSize();
				Rectangle window = Config.getWindow("scaledDisplay",
						new Rectangle(location.x + size.width, location.y, 0, 0));
				scaledDisplayFrame.setLocation(window.x, window.y);
				Config.setWindow("scaledDisplay",
						new Rectangle(scaledDisplayFrame.getX(), scaledDisplayFrame.getY(), 0, 0), false);
				scaledDisplayFrame.pack();
				scaledDisplayFrame.setVisible(true);
			} else {
				((SwingDisplayComponent) emulatorContext.getDisplayComponent())
						.removeDisplayRepaintListener(updateScaledImageListener);
				scaledDisplayFrame.dispose();
			}
		}

		private MouseEvent createAdaptedMouseEvent(MouseEvent e, int scale) {
			return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), e.getX() / scale,
					e.getY() / scale, e.getClickCount(), e.isPopupTrigger(), e.getButton());
		}

		private MouseWheelEvent createAdaptedMouseWheelEvent(MouseWheelEvent e, int scale) {
			return new MouseWheelEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), e.getX() / scale,
					e.getY() / scale, e.getClickCount(), e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(),
					e.getWheelRotation());
		}

		private void updateScaledImage(int scale, JFrame scaledLCDFrame) {
			J2SEGraphicsSurface graphicsSurface = ((SwingDisplayComponent) emulatorContext.getDisplayComponent())
					.getGraphicsSurface();

			BufferedImage img = graphicsSurface.getImage();
			BufferedImage scaledImg = new BufferedImage(img.getWidth() * scale, img.getHeight() * scale, img.getType());
			Graphics2D imgGraphics = scaledImg.createGraphics();
			imgGraphics.scale(scale, scale);
			imgGraphics.drawImage(img, 0, 0, null);

			((ImageIcon) (((JLabel) scaledLCDFrame.getContentPane()).getIcon())).setImage(scaledImg);
			((JLabel) scaledLCDFrame.getContentPane()).repaint();
		}
	};

	private StatusBarListener statusBarListener = new StatusBarListener() {
		public void statusBarChanged(String text) {
			FontMetrics metrics = statusBar.getFontMetrics(statusBar.getFont());
			statusBar.setPreferredSize(new Dimension(metrics.stringWidth(text), metrics.getHeight()));
			statusBar.setText(text);
		}
	};

	private ResponseInterfaceListener responseInterfaceListener = new ResponseInterfaceListener() {
		public void stateChanged(boolean state) {
			menuSelectDevice.setEnabled(state);
		}
	};

	private ComponentListener componentListener = new ComponentAdapter() {
		Timer timer;

		int count = 0;

		public void componentResized(ComponentEvent e) {
			count++;
			DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
			if (deviceDisplay.isResizable()) {
				setDeviceSize(deviceDisplay, devicePanel.getWidth(), devicePanel.getHeight());
				devicePanel.revalidate();
				statusBarListener.statusBarChanged(
						"New size: " + deviceDisplay.getFullWidth() + "x" + deviceDisplay.getFullHeight());
				synchronized (statusBarListener) {
					if (timer == null) {
						timer = new Timer();
					}
					timer.schedule(new CountTimerTask(count) {
						public void run() {
							if (counter == count) {
								Config.setDeviceEntryDisplaySize(deviceEntry,
										new Rectangle(0, 0, devicePanel.getWidth(), devicePanel.getHeight()));
								statusBarListener.statusBarChanged("");
								timer.cancel();
								timer = null;
							}
						}
					}, 2000);
				}
			}
		}
	};

	private WindowAdapter windowListener = new WindowAdapter() {
		public void windowClosing(WindowEvent ev) {
			menuExitListener.actionPerformed(null);
		}

		public void windowIconified(WindowEvent ev) {
			MIDletBridge.getMIDletAccess(MIDletBridge.getCurrentMIDlet()).pauseApp();
		}

		public void windowDeiconified(WindowEvent ev) {
			try {
				MIDletBridge.getMIDletAccess(MIDletBridge.getCurrentMIDlet()).startApp();
			} catch (MIDletStateChangeException ex) {
				System.err.println(ex);
			}
		}
	};

	public Main() {
		this(null);
	}

	public Main(DeviceEntry defaultDevice) {

		this.logQueueAppender = new QueueAppender(1024);
		Logger.addAppender(logQueueAppender);

		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");

		JMenuItem menuItem = new JMenuItem("Exit");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(menuExitListener);
		menuFile.add(menuItem);

		JMenu menuOptions = new JMenu("Options");

		menuSelectDevice = new JMenuItem("Select device...");
		menuSelectDevice.addActionListener(menuSelectDeviceListener);
		menuOptions.add(menuSelectDevice);

		JMenu menuScaleLCD = new JMenu("Scaled display");
		menuOptions.add(menuScaleLCD);
		zoomLevels = new JCheckBoxMenuItem[3];
		for (int i = 0; i < zoomLevels.length; ++i) {
			zoomLevels[i] = new JCheckBoxMenuItem("x " + (i + 2));
			zoomLevels[i].setActionCommand("" + (i + 2));
			zoomLevels[i].addActionListener(menuScaledDisplayListener);
			menuScaleLCD.add(zoomLevels[i]);
		}

		menuStartCapture = new JMenuItem("Start capture to GIF...");
		menuStartCapture.addActionListener(menuStartCaptureListener);
		menuOptions.add(menuStartCapture);

		menuStopCapture = new JMenuItem("Stop capture");
		menuStopCapture.setEnabled(false);
		menuStopCapture.addActionListener(menuStopCaptureListener);
		menuOptions.add(menuStopCapture);

		menuMIDletNetworkConnection = new JCheckBoxMenuItem("MIDlet Network access");
		menuMIDletNetworkConnection.setState(true);
		menuMIDletNetworkConnection.addActionListener(menuMIDletNetworkConnectionListener);
		menuOptions.add(menuMIDletNetworkConnection);

		menuRecordStoreManager = new JCheckBoxMenuItem("Record Store Manager");
		menuRecordStoreManager.setState(false);
		menuRecordStoreManager.addActionListener(menuRecordStoreManagerListener);
		menuOptions.add(menuRecordStoreManager);

		menuLogConsole = new JCheckBoxMenuItem("Log console");
		menuLogConsole.setState(false);
		menuLogConsole.addActionListener(menuLogConsoleListener);
		menuOptions.add(menuLogConsole);

		menuOptions.addSeparator();
		JCheckBoxMenuItem menuShowMouseCoordinates = new JCheckBoxMenuItem("Mouse coordinates");
		menuShowMouseCoordinates.setState(false);
		menuShowMouseCoordinates.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				devicePanel.switchShowMouseCoordinates();
			}
		});
		menuOptions.add(menuShowMouseCoordinates);

		JMenu menuHelp = new JMenu("Help");
		JMenuItem menuAbout = new JMenuItem("About");
		menuAbout.addActionListener(menuAboutListener);
		menuHelp.add(menuAbout);

		menuBar.add(menuFile);
		menuBar.add(menuOptions);
		menuBar.add(menuHelp);
		setJMenuBar(menuBar);

		setTitle("MicroEmulator");

		this.setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/org/microemu/icon.png")));

		addWindowListener(windowListener);

		Config.loadConfig(defaultDevice, emulatorContext);
		Logger.setLocationEnabled(Config.isLogConsoleLocationEnabled());

		Rectangle window = Config.getWindow("main", new Rectangle(0, 0, 160, 120));
		this.setLocation(window.x, window.y);

		getContentPane().add(createContents(getContentPane()), "Center");

		selectDevicePanel = new SwingSelectDevicePanel(emulatorContext);

		this.common = new Common(emulatorContext);
		this.common.setStatusBarListener(statusBarListener);
		this.common.setResponseInterfaceListener(responseInterfaceListener);
		this.common.loadImplementationsFromConfig();

		this.resizeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (resizeDeviceDisplayDialog == null) {
					resizeDeviceDisplayDialog = new ResizeDeviceDisplayDialog();
				}
				DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
				resizeDeviceDisplayDialog.setDeviceDisplaySize(deviceDisplay.getFullWidth(),
						deviceDisplay.getFullHeight());
				if (SwingDialogWindow.show(Main.this, "Enter new size...", resizeDeviceDisplayDialog, true)) {
					setDeviceSize(deviceDisplay, resizeDeviceDisplayDialog.getDeviceDisplayWidth(),
							resizeDeviceDisplayDialog.getDeviceDisplayHeight());
					pack();
					devicePanel.requestFocus();
				}
			}
		});

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout());
		statusPanel.add(statusBar, "West");
		statusPanel.add(this.resizeButton, "East");

		getContentPane().add(statusPanel, "South");

		Message.addListener(new SwingErrorMessageDialogPanel(this));

		devicePanel.setTransferHandler(new DropTransferHandler());
	}

	protected Component createContents(Container parent) {
		devicePanel = new SwingDeviceComponent();
		devicePanel.addKeyListener(devicePanel);
		addKeyListener(devicePanel);

		return devicePanel;
	}

	public boolean setDevice(DeviceEntry entry) {
		if (DeviceFactory.getDevice() != null) {
			// ((J2SEDevice) DeviceFactory.getDevice()).dispose();
		}
		final String errorTitle = "Error creating device";
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			if (entry.getFileName() != null) {
				URL[] urls = new URL[1];
				urls[0] = new File(Config.getConfigPath(), entry.getFileName()).toURI().toURL();
				classLoader = Common.createExtensionsClassLoader(urls);
			}
			// TODO font manager have to be moved from emulatorContext into device
			emulatorContext.getDeviceFontManager().init();

			Device device = DeviceImpl.create(emulatorContext, classLoader, entry.getDescriptorLocation(),
					J2SEDevice.class);
			this.deviceEntry = entry;

			DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) device.getDeviceDisplay();
			if (deviceDisplay.isResizable()) {
				Rectangle size = Config.getDeviceEntryDisplaySize(entry);
				if (size != null) {
					setDeviceSize(deviceDisplay, size.width, size.height);
				}
			}
			common.setDevice(device);
			updateDevice();
			return true;
		} catch (MalformedURLException e) {
			Message.error(errorTitle, errorTitle + ", " + Message.getCauseMessage(e), e);
		} catch (IOException e) {
			Message.error(errorTitle, errorTitle + ", " + Message.getCauseMessage(e), e);
		} catch (Throwable e) {
			Message.error(errorTitle, errorTitle + ", " + Message.getCauseMessage(e), e);
		}
		return false;
	}

	protected void setDeviceSize(DeviceDisplayImpl deviceDisplay, int width, int height) {
		// move the soft buttons
		int menuh = 0;
		Enumeration<?> en = DeviceFactory.getDevice().getSoftButtons().elements();
		while (en.hasMoreElements()) {
			SoftButton button = (SoftButton) en.nextElement();
			Rectangle paintable = button.getPaintable();
			paintable.y = height - paintable.height;
			menuh = paintable.height;
		}
		// resize the display area
		deviceDisplay.setDisplayPaintable(new Rectangle(0, 0, width, height - menuh));
		deviceDisplay.setDisplayRectangle(new Rectangle(0, 0, width, height));
		((SwingDisplayComponent) devicePanel.getDisplayComponent()).init();
		// update display
		MIDletAccess ma = MIDletBridge.getMIDletAccess();
		if (ma == null) {
			return;
		}
		DisplayAccess da = ma.getDisplayAccess();
		if (da != null) {
			da.sizeChanged();
			deviceDisplay.repaint(0, 0, deviceDisplay.getFullWidth(), deviceDisplay.getFullHeight());
		}
	}

	protected void updateDevice() {
		devicePanel.init();
		if (((DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay()).isResizable()) {
			setResizable(true);
			resizeButton.setVisible(true);
		} else {
			setResizable(false);
			resizeButton.setVisible(false);
		}

		pack();

		devicePanel.requestFocus();
	}

	public static void run(List<String> params) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			Logger.error(ex);
		}

		final Main app = new Main();

		if (app.common.initParams(params, app.selectDevicePanel.getSelectedDeviceEntry(), J2SEDevice.class)) {
			app.deviceEntry = app.selectDevicePanel.getSelectedDeviceEntry();
			DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
			if (deviceDisplay.isResizable()) {
				Rectangle size = Config.getDeviceEntryDisplaySize(app.deviceEntry);
				if (size != null) {
					app.setDeviceSize(deviceDisplay, size.width, size.height);
				}
			}
		}
		app.updateDevice();

		app.validate();
		app.setVisible(true);

		if (Config.isWindowOnStart("logConsole")) {
			app.menuLogConsoleListener.actionPerformed(null);
			app.menuLogConsole.setSelected(true);
		}
		if (Config.isWindowOnStart("recordStoreManager")) {
			app.menuRecordStoreManagerListener.actionPerformed(null);
			app.menuRecordStoreManager.setSelected(true);
		}

		app.common.initMIDlet(true);
		app.addComponentListener(app.componentListener);
		app.responseInterfaceListener.stateChanged(true);

	}

	private abstract class CountTimerTask extends TimerTask {

		protected int counter;

		public CountTimerTask(int counter) {
			this.counter = counter;
		}

	}

}
