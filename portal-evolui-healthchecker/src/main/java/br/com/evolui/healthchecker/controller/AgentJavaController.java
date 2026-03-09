package br.com.evolui.healthchecker.controller;


import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class AgentJavaController implements ClassFileTransformer {

    private static Logger LOGGER = LoggerFactory.getLogger(AgentJavaController.class);

    private static final String WITHDRAW_MONEY_METHOD = "withdrawMoney";

    private LinkedHashMap<String, ClassLoader> targetClass;


    public AgentJavaController() {
        this.targetClass = new LinkedHashMap<>();
    }

    public void addTargetClass(String targetClassName, ClassLoader targetClassLoader) {
        this.targetClass.put(targetClassName, targetClassLoader);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        if (className == null || className.startsWith("java/") || className.startsWith("javax/") ||
                className.contains("AgentJavaController") || className.contains("HealthCheckerAgent") || className.contains("MonitorAgentUtil")) {
            return byteCode;
        }
        if (className.contains("$Proxy")) {
            return byteCode;
        }
        if (className.startsWith("sun/reflect")) {
            return byteCode;
        }
        if (loader == null) {
            return byteCode;
        }
        try {
            // Proceed with transformation for application classes only
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get(className.replace("/", "."));

            for (CtMethod method : ctClass.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) {
                    continue; // Skip abstract and native methods
                }
                //if (method.getName().equals("main")) {
                    method.insertBefore("{ br.com.evolui.healthchecker.util.MonitorAgentUtil.trackStart(\"" + method.getLongName() + "\"); }");
                    method.insertAfter("{ br.com.evolui.healthchecker.util.MonitorAgentUtil.trackEnd(); }", true);
                //}
            }

            byteCode = ctClass .toBytecode();
            ctClass.detach();
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        String finalTargetClassName = className.replaceAll("/", "\\."); //replace . with /
        if (!targetClass.containsKey(finalTargetClassName)) {
            //return byteCode;
        }
        ClassLoader targetClassLoader = targetClass.get(finalTargetClassName);

        if (loader != null ) {//loader.equals(targetClassLoader)) {
            LOGGER.info("[Agent] Transforming class MyAtm");
            try {
                ClassPool classPool  = ClassPool.getDefault();
                classPool.appendClassPath(new LoaderClassPath(loader)); // Add class loader path
                CtClass ctClass  = classPool .get(finalTargetClassName);

                // Modify all methods in the class
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    instrumentMethod(method, finalTargetClassName);
                }

                byteCode = ctClass .toBytecode();
                ctClass .detach();
            } catch (NotFoundException | CannotCompileException | IOException e) {
                LOGGER.error("Exception", e);
            }
        }

         */
        //analyzeStackTrace();
        return byteCode;
    }

    private void instrumentMethod(CtMethod method, String className) throws CannotCompileException {
        method.addLocalVariable("_startTime", CtClass.longType);
        method.insertBefore("_startTime = System.nanoTime();");

        method.insertAfter("System.out.println(\"[JAVASSIST] Class " + className + " Method " + method.getName() +
                " executed in \" + (System.nanoTime() - _startTime) + \" ns\");");
    }

    private void analyzeStackTrace() {
        long threadId = Thread.currentThread().getId();
        long startTime = System.nanoTime();

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        System.out.println("[THREAD " + threadId + "] Executing Stack Trace at " + Instant.now());
        Arrays.stream(stackTrace)
                .skip(3) // Skip JVM internals
                .filter(el -> !el.getClassName().contains("br.com.evolui.healthchecker.controller.AgentJavaController"))
                .forEach(System.out::println);

        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("[THREAD " + threadId + "] Execution Time: " + elapsedTime + " ns\n");
    }
}
