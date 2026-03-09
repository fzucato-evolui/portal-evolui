package br.com.evolui.healthchecker;


import br.com.evolui.healthchecker.controller.AgentJavaController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

public class HealthcheckerAgent {
    private static Logger LOGGER = LoggerFactory.getLogger(HealthcheckerAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In premain method");

        transformClass(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method");

        transformClass(inst);
    }

    private static void transformClass(Instrumentation instrumentation) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;

        // otherwise iterate all loaded classes and find what we want
        AgentJavaController dt = new AgentJavaController();
        for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {
            targetCls = clazz;
            targetClassLoader = targetCls.getClassLoader();
            dt.addTargetClass(targetCls.getName(), targetClassLoader);
        }
        instrumentation.addTransformer(dt);
    }
}
