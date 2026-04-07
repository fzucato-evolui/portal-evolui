package br.com.evolui.portalevolui.web.util;

import org.springframework.util.StringUtils;

/**
 * Comparação semver simples (major.minor.patch numéricos). Sufixos não numéricos após patch são ignorados ({@code 1.0.0-rc1} → 1.0.0).
 */
public final class SemverUtil {

    private SemverUtil() {
    }

    public static boolean isAtLeast(String clientVersion, String minimumRequired) {
        if (!StringUtils.hasText(minimumRequired)) {
            return true;
        }
        if (!StringUtils.hasText(clientVersion)) {
            return false;
        }
        return compare(normalize(clientVersion), normalize(minimumRequired)) >= 0;
    }

    private static String normalize(String v) {
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }
        int dash = s.indexOf('-');
        if (dash > 0) {
            s = s.substring(0, dash);
        }
        return s;
    }

    /**
     * @return negativo se a &lt; b, zero se igual, positivo se a &gt; b
     */
    static int compare(String a, String b) {
        int[] pa = parse(a);
        int[] pb = parse(b);
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(pa[i], pb[i]);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static int[] parse(String v) {
        int[] p = new int[]{0, 0, 0};
        if (!StringUtils.hasText(v)) {
            return p;
        }
        String[] parts = v.split("\\.");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            String part = parts[i].replaceAll("\\D.*", "");
            if (StringUtils.hasText(part)) {
                try {
                    p[i] = Integer.parseInt(part);
                } catch (NumberFormatException ignored) {
                    p[i] = 0;
                }
            }
        }
        return p;
    }
}
