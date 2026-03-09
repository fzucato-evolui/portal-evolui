package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.rest.dto.GraphqlRequestBodyDTO;
import br.com.evolui.portalevolui.web.util.GraphqlSchemaReaderUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.internal.util.StringHelper;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraphqlClientService {
    String url;
    String bearerToken;
    HttpGraphQlClient graphQlClient;
    String version;

    public static GraphqlClientService using(String url, String bearerToken)  {
        GraphqlClientService client = new GraphqlClientService();
        client.url = url;
        client.bearerToken = bearerToken;
        WebClient webClient = getClient();
        client.graphQlClient = HttpGraphQlClient.builder(webClient)
                .url(url)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .build();
        return client;
    }

    public static GraphqlClientService using(String url, String bearerToken, String version)  {
        GraphqlClientService client = new GraphqlClientService();
        client.url = url;
        client.bearerToken = bearerToken;
        client.version = version;
        WebClient webClient = getClient();
        client.graphQlClient = HttpGraphQlClient.builder(webClient)
                .url(url)
                .headers(headers ->  {
                    headers.setBearerAuth(bearerToken);
                    headers.add("API-Version", version);
                })
                .build();
        return client;
    }

    private static WebClient getClient() {
        HttpClient httpClient =
                HttpClient.create().httpResponseDecoder(spec -> spec.maxHeaderSize(32 * 1024));

        WebClient webClient =
                WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
        return webClient;
    }

    public <T> T post (String schemaFileName, LinkedHashMap<String, Object> variables, String path, Class<T> returnType) throws Exception {
        GraphQlClient.RequestSpec req = this.prepareRequestFromSchema(schemaFileName, variables);
        T resp = req.retrieve(StringHelper.isEmpty(path) ? "" : path).toEntity(returnType).block();
        return resp;
    }

    public <T> T post (GraphQlClient.RequestSpec req, String path, Class<T> returnType) throws Exception {
        T resp = req.retrieve(StringHelper.isEmpty(path) ? "" : path).toEntity(returnType).block();
        return resp;
    }

    public <T> List<T> postList (String schemaFileName, LinkedHashMap<String, Object> variables, String path, Class<T> returnType) throws Exception {
        GraphQlClient.RequestSpec req = this.prepareRequestFromSchema(schemaFileName, variables);
        List<T> resp = req.retrieve(StringHelper.isEmpty(path) ? "" : path).toEntityList(returnType).block();
        return resp;
    }

    public <T> List<T> postList (GraphQlClient.RequestSpec req, String path, Class<T> returnType) throws Exception {
        List<T> resp = req.retrieve(StringHelper.isEmpty(path) ? "" : path).toEntityList(returnType).block();
        return resp;
    }

    public GraphQlClient.RequestSpec prepareRequestFromSchema(String schemaFileName, LinkedHashMap<String, Object> variables) throws IOException {
        String query = GraphqlSchemaReaderUtil.getSchemaFromFileName(schemaFileName);
        return this.prepareRequestFromQuery(query, variables);
    }

    public GraphQlClient.RequestSpec prepareRequestFromQuery(String query, LinkedHashMap<String, Object> variables) throws IOException {
        GraphQlClient.RequestSpec req = this.graphQlClient.document(query);
        if (variables != null && !variables.isEmpty()) {
            req.variables(variables);
        }
        return req;
    }

    public String parserToQueryString(String schemaFileName, LinkedHashMap<String, Object> variables) throws IOException {
        String query = GraphqlSchemaReaderUtil.getSchemaFromFileName(schemaFileName);
        ObjectMapper mapper = new ObjectMapper();
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> v : variables.entrySet()) {
                query = query.replace("$" + v.getKey(), mapper.writeValueAsString(v.getValue()));
            }
        }
        return query;
    }

    public JsonNode postAlterarQuadro(final Long item_id, final Long board_id, String column_id, String value) throws IOException {
        WebClient webClient = WebClient.builder()
                .build();

        GraphqlRequestBodyDTO graphQLRequestBody = new GraphqlRequestBodyDTO();

        final String query = GraphqlSchemaReaderUtil.getSchemaFromFileName("changeColumn");
        final LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("item_id", item_id);
        variables.put("board_id", board_id);
        variables.put("column_id", column_id);
        variables.put("value", value);

        graphQLRequestBody.setQuery(query);
        graphQLRequestBody.setVariables(variables);
        HttpGraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient)
                .url(this.url)
                .headers(headers -> headers.setBearerAuth(this.bearerToken))

                .build();
        JsonNode node = graphQlClient.document(query).variables(variables).retrieve("").toEntity(JsonNode.class).block();
        return node;
    }

    private JsonNode getUsers(final List<String> emails, final Integer limit) throws IOException {

        WebClient webClient = WebClient.builder().build();

        GraphqlRequestBodyDTO graphQLRequestBody = new GraphqlRequestBodyDTO();

        final String query = GraphqlSchemaReaderUtil.getSchemaFromFileName("user");
        final LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("emails", emails);
        variables.put("limit", limit);

        graphQLRequestBody.setQuery(query);
        graphQLRequestBody.setVariables(variables);

        return webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + this.bearerToken)
                .bodyValue(graphQLRequestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

}
