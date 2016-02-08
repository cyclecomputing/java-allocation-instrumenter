// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.monitoring.runtime.instrumentation;

import privateorg.objectweb.asm.ClassVisitor;
import privateorg.objectweb.asm.MethodVisitor;
import privateorg.objectweb.asm.Opcodes;
import privateorg.objectweb.asm.commons.JSRInlinerAdapter;
import privateorg.objectweb.asm.commons.LocalVariablesSorter;

/**
 * Instruments bytecodes that allocate heap memory to call a recording hook. A
 * <code>ClassVisitor</code> that processes methods with a
 * <code>AllocationMethodAdapter</code> to instrument heap allocations.
 * 
 * @author jeremymanson@google.com (Jeremy Manson)
 * @author fischman@google.com (Ami Fischman) (Original Author)
 */
class AllocationClassAdapter extends ClassVisitor {
    private final String recorderClass;
    private final String recorderMethod;

    public AllocationClassAdapter(ClassVisitor cv, String recorderClass,
            String recorderMethod) {
        super(Opcodes.ASM5, cv);
        this.recorderClass = recorderClass;
        this.recorderMethod = recorderMethod;
    }

    /**
     * For each method in the class being instrumented, <code>visitMethod</code>
     * is called and the returned MethodVisitor is used to visit the method.
     * Note that a new MethodVisitor is constructed for each method.
     */
    @Override
    public MethodVisitor visitMethod(int access, String base, String desc,
            String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, base, desc, signature,
                exceptions);

        if (mv != null) {
            // We need to compute stackmaps (see
            // AllocationInstrumenter#instrument). This can't really be
            // done for old bytecode that contains JSR and RET instructions.
            // So, we remove JSRs and RETs.
            JSRInlinerAdapter jsria = new JSRInlinerAdapter(mv, access, base,
                    desc, signature, exceptions);
            AllocationMethodAdapter aimv = new AllocationMethodAdapter(jsria,
                    recorderClass, recorderMethod);
            LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc,
                    aimv);
            aimv.lvs = lvs;
            mv = lvs;
        }
        return mv;
    }
}
