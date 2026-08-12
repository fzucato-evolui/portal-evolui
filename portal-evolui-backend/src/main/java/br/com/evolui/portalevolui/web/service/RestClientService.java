package br.com.evolui.portalevolui.web.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Map;


public class RestClientService {

    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

    /**
     * SSLContext, connection manager e RestTemplate são compartilhados por todas as chamadas.
     * Aceita qualquer certificado de propósito: além do GitHub, este client atende APIs internas
     * com certificado autoassinado.
     */
    private static final SSLContext TRUST_ALL_SSL_CONTEXT = buildTrustAllSslContext();
    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER = buildConnectionManager(TRUST_ALL_SSL_CONTEXT);
    private static final RestTemplate SHARED_REST_TEMPLATE = buildRestTemplate(false);
    private static final RestTemplate SHARED_REST_TEMPLATE_REMOVE_AUTH_ON_REDIRECT = buildRestTemplate(true);

    String url;
    String bearerToken;
    RestTemplate restTemplate;
    boolean pureURL = false;

    public RestClientService () {

    }
    public static RestClientService using(String url, String bearerToken) throws Exception {
        RestClientService client = new RestClientService();
        client.url = url;
        client.bearerToken = bearerToken;
        client.restTemplate = getRestTemplateBypassingHostNameVerifcation();
        return client;
    }
    public static RestClientService using(String url, boolean pureURL, String bearerToken) throws Exception {
        RestClientService client = new RestClientService();
        client.url = url;
        client.pureURL = pureURL;
        client.bearerToken = bearerToken;
        client.restTemplate = getRestTemplateBypassingHostNameVerifcation();
        return client;
    }

    public static RestClientService using(String url, boolean pureURL, String bearerToken, boolean removeAuthorizationHeaderOnRedirect) throws Exception {
        RestClientService client = new RestClientService();
        client.url = url;
        client.pureURL = pureURL;
        client.bearerToken = bearerToken;
        client.restTemplate = getRestTemplateBypassingHostNameVerifcation(removeAuthorizationHeaderOnRedirect);
        return client;
    }

    private static SSLContext buildTrustAllSslContext() {
        try {
            return SSLContextBuilder.create()
                    .loadTrustMaterial((X509Certificate[] certificateChain, String authType) -> true)  // <--- accepts each certificate
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível inicializar o SSLContext trust-all", e);
        }
    }

    private static PoolingHttpClientConnectionManager buildConnectionManager(SSLContext sslContext) {
        Registry<ConnectionSocketFactory> socketRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(URIScheme.HTTPS.getId(), new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                .register(URIScheme.HTTP.getId(), new PlainConnectionSocketFactory())
                .build();
        return new PoolingHttpClientConnectionManager(socketRegistry);
    }

    private static RestTemplate buildRestTemplate(boolean removeAuthorizationOnRedirect) {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setConnectionManager(CONNECTION_MANAGER)
                .setConnectionManagerShared(true)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                        .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                        .build());
        if (removeAuthorizationOnRedirect) {
            builder.addRequestInterceptorFirst((httpRequest, entityDetails, httpContext) -> {
                if (httpContext instanceof HttpClientContext) {
                    if (((HttpClientContext) httpContext).getRedirectLocations().size() > 0) {
                        httpRequest.removeHeader(httpRequest.getHeader("Authorization"));
                    }
                }
            });
        }
        HttpClient httpClient = builder.build();

        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                ((MappingJackson2HttpMessageConverter) converter).setObjectMapper(mapper);
            }
        }
        return restTemplate;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String doRequest (HttpMethod httpMethod, Object body, Object...parametros) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(this.bearerToken)) {
            headers.add("Authorization", "Bearer " + this.bearerToken);
        }

        HttpEntity entity = new HttpEntity(body, headers);
        ResponseEntity<String> response = null;
        if (!pureURL) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            for (Object parametro : parametros) {
                if (parametro instanceof Map.Entry) {
                    Map.Entry pair = (Map.Entry) parametro;
                    if (pair.getValue() != null) {
                        builder.queryParam(pair.getKey().toString(), pair.getValue());
                    }
                } else {
                    builder.pathSegment(parametro.toString());
                }
            }

            response = restTemplate.exchange(builder.toUriString(), httpMethod, entity, String.class);
        } else {
            URI uri = new URI(url);
            response = restTemplate.exchange(uri, httpMethod, entity, String.class);
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception(response.getStatusCode().toString());
        }
        return response.getBody();

    }

    public static RestTemplate getRestTemplateBypassingHostNameVerifcation() throws Exception {
        return getRestTemplateBypassingHostNameVerifcation(false);
    }

    public static RestTemplate getRestTemplateBypassingHostNameVerifcation(boolean removeAuthorizationOnRedirect) throws Exception {
        return removeAuthorizationOnRedirect ? SHARED_REST_TEMPLATE_REMOVE_AUTH_ON_REDIRECT : SHARED_REST_TEMPLATE;
    }

}
