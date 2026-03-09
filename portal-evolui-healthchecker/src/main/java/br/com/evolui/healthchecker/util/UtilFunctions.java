package br.com.evolui.healthchecker.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class UtilFunctions {
    public static String exceptionToString(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    public static String normalizePath(String path) {
        return path.replaceAll("[/\\\\]+", "/")  // Substitui múltiplas / ou \ por uma única /
                .replaceAll("^/+", "")        // Remove barras no início
                .replaceAll("/+$", "")        // Remove barras no final
                .toLowerCase();
    }
}
