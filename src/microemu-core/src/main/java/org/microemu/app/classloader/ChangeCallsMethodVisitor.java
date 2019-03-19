/**
 * MicroEmulator
 *
 * Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 *
 * Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package org.microemu.app.classloader;

import java.util.HashMap;
import org.microemu.Injected;
import org.microemu.app.util.MIDletThread;
import org.microemu.app.util.MIDletTimer;
import org.microemu.app.util.MIDletTimerTask;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author vlads
 *
 */
public class ChangeCallsMethodVisitor extends MethodVisitor implements Opcodes {

  private static final String INJECTED_CLASS = ChangeCallsMethodVisitor.codeName(Injected.class);

  static String NEW_SYSTEM_OUT_CLASS = ChangeCallsMethodVisitor.INJECTED_CLASS;

  static String NEW_SYSTEM_PROPERTIES_CLASS = ChangeCallsMethodVisitor.INJECTED_CLASS;

  static String NEW_RESOURCE_LOADER_CLASS = ChangeCallsMethodVisitor.INJECTED_CLASS;

  private HashMap catchInfo;

  private final InstrumentationConfig config;

  private static class CatchInformation {

    Label label;

    String type;

    public CatchInformation(final String type) {
      label = new Label();
      this.type = type;
    }
  }

  public ChangeCallsMethodVisitor(final MethodVisitor mv, final InstrumentationConfig config) {
    super(Opcodes.ASM4);
    this.mv = mv;
    this.config = config;
  }

  public static String codeName(final Class klass) {
    return klass.getName().replace('.', '/');
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
    switch (opcode) {
      case GETSTATIC:
        if ((name.equals("out")) && (owner.equals("java/lang/System"))) {
          //System.out.println("owner " + owner + " name " + name + " desc " + desc);
          // GETSTATIC System.out : PrintStream
          mv.visitFieldInsn(opcode, ChangeCallsMethodVisitor.NEW_SYSTEM_OUT_CLASS, name, desc);
          return;
        }
        if ((name.equals("err")) && (owner.equals("java/lang/System"))) {
          //System.out.println("owner " + owner + " name " + name + " desc " + desc);
          // GETSTATIC System.out : PrintStream
          mv.visitFieldInsn(opcode, ChangeCallsMethodVisitor.NEW_SYSTEM_OUT_CLASS, name, desc);
          return;
        }
        break;

    }
    mv.visitFieldInsn(opcode, owner, name, desc);
  }

  @Override
  public void visitMethodInsn(final int opcode, String owner, final String name, final String desc) {
    switch (opcode) {
      case INVOKESTATIC:
        //System.out.println("Method owner " + owner + " name " + name + " desc " + desc);
        if ((name.equals("getProperty")) && (owner.equals("java/lang/System"))) {
          // INVOKESTATIC
          // java/lang/System.getProperty(Ljava/lang/String;)Ljava/lang/String;
          mv.visitMethodInsn(opcode, ChangeCallsMethodVisitor.NEW_SYSTEM_PROPERTIES_CLASS, name, desc);
          return;
        }
        break;
      case INVOKEVIRTUAL:
        if ((name.equals("getResourceAsStream")) && (owner.equals("java/lang/Class"))) {
          // INVOKEVIRTUAL
          // java/lang/Class.getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;
          // "org/microemu/ResourceLoader", "getResourceAsStream", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;");
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, ChangeCallsMethodVisitor.NEW_RESOURCE_LOADER_CLASS, name, "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;");
          return;
        }
        else if ((name.equals("printStackTrace")) && (owner.equals("java/lang/Throwable"))) {
          // INVOKEVIRTUAL java/lang/Throwable.printStackTrace()V
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, ChangeCallsMethodVisitor.INJECTED_CLASS, name, "(Ljava/lang/Throwable;)V");
          return;
        }
        break;
      case INVOKESPECIAL:
        if ((config.isEnhanceThreadCreation()) && (name.equals("<init>"))) {
          if (owner.equals("java/util/Timer")) {
            owner = ChangeCallsMethodVisitor.codeName(MIDletTimer.class);
          }
          else if (owner.equals("java/util/TimerTask")) {
            owner = ChangeCallsMethodVisitor.codeName(MIDletTimerTask.class);
          }
          else if (owner.equals("java/lang/Thread")) {
            owner = ChangeCallsMethodVisitor.codeName(MIDletThread.class);
          }
        }
        break;
    }

    mv.visitMethodInsn(opcode, owner, name, desc);
  }

  @Override
  public void visitTypeInsn(final int opcode, String desc) {
    if ((opcode == Opcodes.NEW) && (config.isEnhanceThreadCreation())) {
      if ("java/util/Timer".equals(desc)) {
        desc = ChangeCallsMethodVisitor.codeName(MIDletTimer.class);
      }
      else if ("java/util/TimerTask".equals(desc)) {
        desc = ChangeCallsMethodVisitor.codeName(MIDletTimerTask.class);
      }
      else if ("java/lang/Thread".equals(desc)) {
        desc = ChangeCallsMethodVisitor.codeName(MIDletThread.class);
      }
    }
    mv.visitTypeInsn(opcode, desc);
  }

  @Override
  public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
    if (config.isEnhanceCatchBlock() && (type != null)) {
      if (catchInfo == null) {
        catchInfo = new HashMap();
      }
      CatchInformation newHandler = (CatchInformation)catchInfo.get(handler);
      if (newHandler == null) {
        newHandler = new CatchInformation(type);
        catchInfo.put(handler, newHandler);
      }
      mv.visitTryCatchBlock(start, end, newHandler.label, type);
    }
    else {
      mv.visitTryCatchBlock(start, end, handler, type);
    }
  }

  //TODO make this work for gMaps case
  @Override
  public void visitLabel(final Label label) {
    if (config.isEnhanceCatchBlock() && (catchInfo != null)) {
      final CatchInformation newHandler = (CatchInformation)catchInfo.get(label);
      if (newHandler != null) {
        mv.visitLabel(newHandler.label);
        // no push, just use current Throwable in stack
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ChangeCallsMethodVisitor.INJECTED_CLASS, "handleCatchThrowable", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
        // stack contains Throwable, just verify that it is right type for this handler
        mv.visitTypeInsn(Opcodes.CHECKCAST, newHandler.type);
      }
    }
    mv.visitLabel(label);
  }

}
