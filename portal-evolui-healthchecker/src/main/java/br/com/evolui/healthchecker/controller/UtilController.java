package br.com.evolui.healthchecker.controller;


import br.com.evolui.healthchecker.Main;
import br.com.evolui.portalevolui.shared.dto.HealthCheckerConfigDTO;
import br.com.evolui.portalevolui.shared.util.GeradorTokenPortalEvolui;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;

public class UtilController {
    private static boolean isWindows = System.getProperty("os.name")
            .toLowerCase().startsWith("windows");
    public static SSLContext getSSLContext() throws Exception {
        return getSSLContext(null);
    }

    public static SSLContext getSSLContext(KeyManagerFactory kmf) throws Exception {
        return getSSLContext(null, true);
    }

    public static SSLContext getSSLContext(KeyManagerFactory kmf, boolean bypassCertificate) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        if (!bypassCertificate) {
            trustAllCerts = null;
        }
        SSLContext sc = SSLContext.getInstance("SSL");
        if (kmf == null) {
            sc.init(null, trustAllCerts, null);
        } else {
            sc.init(kmf.getKeyManagers(), trustAllCerts, null);
        }
        return sc;
    }

    public static HealthCheckerConfigDTO readConfiguration() throws Exception {
        Path p = Paths.get(getAbsolutePath(), "configuration.hck");
        if (fileExists(p)) {
            String encrypt = new String(Files.readAllBytes(p), "UTF-8");
            String decript = GeradorTokenPortalEvolui.decrypt(encrypt);
            return new ObjectMapper().readValue(decript, HealthCheckerConfigDTO.class);
        }

        return null;

    }

    public static void saveConfigFile(HealthCheckerConfigDTO config) throws Exception {
        Path p = Paths.get(getAbsolutePath(), "configuration.hck");
        String encrypt = GeradorTokenPortalEvolui.encrypt(new ObjectMapper().writeValueAsString(config));
        Files.write(p, encrypt.getBytes(StandardCharsets.UTF_8));
    }

    public static void deleteConfigFile() {
        Path p = Paths.get(getAbsolutePath(), "configuration.hck");
        try {
            Files.delete(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAbsolutePath() {
        String absolutePath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
        if (absolutePath.startsWith("/")) {
            if (isWindows) {
                absolutePath = absolutePath.substring(1);
            }
        }
        return absolutePath;
    }

    public static boolean fileExists(Path path) {
        return Files.exists(path);
    }
}
