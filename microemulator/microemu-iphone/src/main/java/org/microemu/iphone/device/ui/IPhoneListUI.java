package org.microemu.iphone.device.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

import joc.Message;
import obc.CGRect;
import obc.NSIndexPath;
import obc.UITableView;
import obc.UITableViewCell;
import obc.UIToolbar;
import obc.UIView;

import org.microemu.device.ui.ListUI;
import org.microemu.iphone.MicroEmulator;

public class IPhoneListUI extends AbstractUI implements ListUI {

	static final int UIViewAutoresizingFlexibleHeight =  1 << 4;

	static final int UIViewAutoresizingFlexibleWidth =  1 << 1;
	
	private List list;

	private UITableView tableView;

	private Command selectCommand=List.SELECT_COMMAND;
	
	private UIView view;
	
	public IPhoneListUI(MicroEmulator microEmulator, List list) {
		super(microEmulator, list);
		this.list = list;
	}

	@Message
	public final int tableView$numberOfRowsInSection$(UITableView table, int sectionIndex) {
		return list.size();
	}

	@Message
	public final UITableViewCell tableView$cellForRowAtIndexPath$(UITableView table, NSIndexPath indexPath) {
		UITableViewCell cell = (UITableViewCell) table.dequeueReusableCellWithIdentifier$("reuse");
		if (cell == null) {
			cell = new UITableViewCell().init();
			cell.setReuseIdentifier$("reuse");
		}
		cell.setText$(list.getString(indexPath.row()));
		return cell;
	}

	@Message
	public final void tableView$didSelectRowAtIndexPath$(UITableView table, NSIndexPath indexPath) {
		System.out.println(list.getString(indexPath.row()) + " selected");
		list.setSelectedIndex(indexPath.row(),true);
		if(commandListener!=null&&selectCommand!=null)
			commandListener.commandAction(selectCommand, list);
	}

	public int append(String stringPart, Image imagePart) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getSelectedIndex() {
		return list.getSelectedIndex();
	}

	public String getString(int elementNum) {
		return list.getString(elementNum);
	}

	public void setSelectCommand(Command command) {
		System.out.println("setSelectCommand"+command);
		this.selectCommand=command;
	}


	public void hideNotify() {
	}

	public void invalidate() {
		// TODO Auto-generated method stub

	}

	public void showNotify() {
		System.out.println("showNotify");

		if (view==null) {
			view = new UIView().initWithFrame$(microEmulator.getWindow().bounds());
			tableView = new UITableView().initWithFrame$style$(
					new CGRect(0, 0, microEmulator.getWindow().bounds().size.width,
							microEmulator.getWindow().bounds().size.height - TOOLBAR_HEIGHT), 0);
			view.addSubview$(tableView);
			toolbar = (UIToolbar) new UIToolbar().initWithFrame$(new CGRect(0,
					microEmulator.getWindow().bounds().size.height - TOOLBAR_HEIGHT, microEmulator.getWindow().bounds().size.width,
					TOOLBAR_HEIGHT));
			view.addSubview$(toolbar);
			updateToolbar();
		}
		tableView.setDataSource$(this);
		tableView.setDelegate$(this);
		tableView.reloadData();

		view.retain();
		microEmulator.getWindow().addSubview$(view);
	}
	
	@Override
	protected void finalize() throws Throwable {
		tableView.release();
	}
}

