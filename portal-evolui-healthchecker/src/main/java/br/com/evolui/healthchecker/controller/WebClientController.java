package br.com.evolui.healthchecker.controller;


import br.com.evolui.healthchecker.exceptions.WebClientStatusException;
import br.com.evolui.portalevolui.shared.dto.HealthCheckerConfigDTO;
import br.com.evolui.portalevolui.shared.dto.HealthCheckerDTO;
import br.com.evolui.portalevolui.shared.dto.HealthCheckerModuleConfigDTO;
import br.com.evolui.portalevolui.shared.dto.LoginDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.LinkedHashMap;

public class WebClientController {
    private String user;
    private String password;
    private String host;
    private HealthCheckerConfigDTO config;

    private final String LOGIN_ENDPOINT = "/api/public/auth/login-health-checker";
    private final String DELETE_HEALTCHECKER_ENDPOINT = "/api/admin/health-checker/delete-from-machine";
    private final String HEALTCHECKER_ENDPOINT = "/api/admin/health-checker/check";
    public WebClientController(HealthCheckerConfigDTO config) {

        this.user = config.getLogin().getLogin();
        this.password = config.getLogin().getPassword();
        this.host = config.getHost();
        this.config = config;
    }

    public String getToken() throws Exception {
        LoginDTO<Long> dto = new LoginDTO();
        dto.setLogin(this.user);
        dto.setPassword(this.password);
        dto.setExtraInfo(this.config.getId());
        LinkedHashMap<String, Object> map = this.consume(this.LOGIN_ENDPOINT,HttpMethod.POST, dto, null, null, new TypeReference<LinkedHashMap<String,Object>>() {}, null);
        return map.get("accessToken").toString();
    }

    public void deleteHealthChecker(String token) throws Exception {
        if (!StringUtils.hasText(token)) {
            token = this.getToken();
        }
        this.consume(this.DELETE_HEALTCHECKER_ENDPOINT, HttpMethod.POST, this.config, null, null, null, token);
    }

    public void saveChecker(String token, HealthCheckerDTO body) throws Exception {
        if (!StringUtils.hasText(token)) {
            token = this.getToken();
        }
        this.consume(this.HEALTCHECKER_ENDPOINT, HttpMethod.POST, body, null, null, null, token);
    }

    public String doHealthCheck(HealthCheckerModuleConfigDTO dto) throws Exception {
        CloseableHttpClient client = this.getClient(dto.getClientCertificate() != null && dto.getClientCertificate().getFile() != null ? dto.getClientCertificate().getKeystoreFactory(): null, dto.isBypassCertificate());
        URI url = new URI(dto.getCommandAddress());
        HttpRequestBase request = new HttpGet(url);

        CloseableHttpResponse response = client.execute(request);
        LinkedHashMap<String, Object> formattedResponse = new LinkedHashMap<>();
        formattedResponse.put("STATUS", response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        if (StringUtils.hasText(result)) {
            formattedResponse.put("CONTENT", result);
        } else {
            formattedResponse.put("CONTENT", "");
        }
        formattedResponse.put("HEADERS", response.getAllHeaders());
        return new ObjectMapper().writeValueAsString(formattedResponse);

    }

    private <T> T consume(String endpoint, HttpMethod method, Object body, String query, Class<T> type, TypeReference<T> typeList, String token) throws Exception {
        CloseableHttpClient client = this.getClient();
        URI url = new URI(this.host);
        URI urlEndpoint = new URI(url.getScheme(), url.getUserInfo(), url.getHost(), url.getPort(), endpoint, query, null);
        HttpRequestBase request = null;

        if (method == HttpMethod.GET) {
            request = new HttpGet(urlEndpoint);
        } else if (method == HttpMethod.POST) {
            request = new HttpPost(urlEndpoint);
        } else if (method == HttpMethod.PUT) {
            request = new HttpPut(urlEndpoint);
        } else if (method == HttpMethod.DELETE) {
            request = new HttpDelete(urlEndpoint);
        }
        if (body != null) {
            String json = new ObjectMapper().writeValueAsString(body);
            ((HttpEntityEnclosingRequestBase)request).setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        }
        if (StringUtils.hasText(token)) {
            request.addHeader("Authorization", "Bearer " + token);
        }

        CloseableHttpResponse response = client.execute(request);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            throw new WebClientStatusException(response.getStatusLine());
        }
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        if (type != null) {
            T readValue = new ObjectMapper().readValue(result, type);
            return readValue;
        } else if (typeList != null){
            T readValue = new ObjectMapper().readValue(result, typeList);
            return readValue;
        } else {
            return null;
        }


    }

    private CloseableHttpClient getClient() throws Exception {
        return getClient(null);
    }

    private CloseableHttpClient getClient(KeyManagerFactory kmf) throws Exception {
        return getClient(kmf, true);
    }

    private CloseableHttpClient getClient(KeyManagerFactory kmf, boolean bypassCertificate) throws Exception {
        final SSLContext sslContext = UtilController.getSSLContext(kmf, bypassCertificate);
        return HttpClientBuilder.create()
                .setSSLContext(sslContext)
                .setConnectionManager(
                        new PoolingHttpClientConnectionManager(
                                RegistryBuilder.<ConnectionSocketFactory>create()
                                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                                        .register("https", new SSLConnectionSocketFactory(sslContext,
                                                NoopHostnameVerifier.INSTANCE))
                                        .build()
                        ))
                .build();
    }
}
