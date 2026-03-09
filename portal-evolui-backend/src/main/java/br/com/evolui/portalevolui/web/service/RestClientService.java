package br.com.evolui.portalevolui.web.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLContext;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Map;


public class RestClientService {

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
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception(response.getStatusCode().toString());
        }
        return response.getBody();

    }

    public static RestTemplate getRestTemplateBypassingHostNameVerifcation() throws Exception {
        return getRestTemplateBypassingHostNameVerifcation(false);
    }
    public static RestTemplate getRestTemplateBypassingHostNameVerifcation(boolean removeAuthorizationOnRedirect) throws Exception {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial((X509Certificate[] certificateChain, String authType) -> true)  // <--- accepts each certificate
                .build();

        Registry<ConnectionSocketFactory> socketRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(URIScheme.HTTPS.getId(), new SSLConnectionSocketFactory(sslContext))
                .register(URIScheme.HTTP.getId(), new PlainConnectionSocketFactory())
                .build();

        HttpClientBuilder builder = HttpClientBuilder.create()
                .setConnectionManager(new PoolingHttpClientConnectionManager(socketRegistry))
                .setConnectionManagerShared(true);
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
        RestTemplate restTemplate = new RestTemplate( new SimpleClientHttpRequestFactory(){
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod ) {
                //connection.setInstanceFollowRedirects(false);
                int ze = 0;
            }
        } );
        restTemplate = new RestTemplate( requestFactory);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof  MappingJackson2HttpMessageConverter) {
                ((MappingJackson2HttpMessageConverter)converter).setObjectMapper(mapper);
            }
        }

        return restTemplate;
    }

}
