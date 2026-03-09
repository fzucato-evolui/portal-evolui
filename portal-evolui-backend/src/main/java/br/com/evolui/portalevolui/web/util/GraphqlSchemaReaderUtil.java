package br.com.evolui.portalevolui.web.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

public final class GraphqlSchemaReaderUtil {

    public static String getSchemaFromFileName(final String filename) throws IOException {
        String text = IOUtils.toString(GraphqlSchemaReaderUtil.class.getClassLoader().getResourceAsStream("graphql/" + filename + ".graphql"),
                "UTF-8");
        return text;
    }
}
