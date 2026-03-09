package br.com.evolui.portalevolui.web.util;

import java.net.URLEncoder;
import java.util.Map;

public class FunctionsUtil {
    public static String urlEncodeUTF8(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }
    public static String mapToQueryString(Map<?,?> map) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())
            ));
        }
        return sb.toString();
    }
}
