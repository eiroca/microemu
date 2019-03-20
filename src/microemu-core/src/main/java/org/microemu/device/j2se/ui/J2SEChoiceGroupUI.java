/**
 * MicroEmulator Copyright (C) 2009 Bartek Teodorczyk <barteo@barteo.net>
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
 * @version $Id: J2SEChoiceGroupUI.java 1918 2009-01-21 12:56:43Z barteo $
 */

package org.microemu.device.j2se.ui;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Image;
import org.microemu.device.ui.ChoiceGroupUI;

public class J2SEChoiceGroupUI implements ChoiceGroupUI {

  public J2SEChoiceGroupUI(final ChoiceGroup choiceGroup, final int choiceType) {
    // TODO Auto-generated constructor stub
  }

  @Override
  public void setDefaultCommand(final Command cmd) {
    // TODO Auto-generated method stub
  }

  @Override
  public void setLabel(final String label) {
    // TODO Auto-generated method stub
  }

  @Override
  public void delete(final int elementNum) {
    // TODO Auto-generated method stub
  }

  @Override
  public void deleteAll() {
    // TODO Auto-generated method stub
  }

  @Override
  public void setSelectedIndex(final int elementNum, final boolean selected) {
    // TODO Auto-generated method stub
  }

  @Override
  public int getSelectedIndex() {
    // TODO Auto-generated method stub
    return -1;
  }

  @Override
  public void insert(final int elementNum, final String stringPart, final Image imagePart) {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean isSelected(final int elementNum) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setSelectedFlags(final boolean[] selectedArray) {
    // TODO Auto-generated method stub
  }

  @Override
  public int getSelectedFlags(final boolean[] selectedArray) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getString(final int elementNum) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void set(final int elementNum, final String stringPart, final Image imagePart) {
    // TODO Auto-generated method stub
  }

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 0;
  }

}
