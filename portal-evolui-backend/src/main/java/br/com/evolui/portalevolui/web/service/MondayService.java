package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.VersaoBuildBaseBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.exceptions.MondayValidateException;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.MondayConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.enums.MondayColumnTypesEnum;
import br.com.evolui.portalevolui.web.rest.dto.monday.*;
import br.com.evolui.portalevolui.web.rest.dto.version.GeracaoVersaoMondayNotificationDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MondayService {
    private MondayConfigDTO config;
    private GraphqlClientService client;
    private final String version = "2023-10";

    @Autowired
    private SystemConfigRepository configRepository;

    public MondayUserDTO getUserByEmail(String email) throws Exception {
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("emails", Arrays.asList(email));
        variables.put("limit", 1);
        List<MondayUserDTO> users = this.client.postList("queryUsers", variables, "users", MondayUserDTO.class);
        if (users == null || users.isEmpty()) {
            return null;
        } else {
            return users.get(0);
        }
    }

    public List<MondayBoardDTO> listBoards() throws Exception {
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        List<MondayBoardDTO> dto = client.postList("queryBoards", variables, "boards", MondayBoardDTO.class);
        if (dto == null || dto.isEmpty()) {
            return null;
        } else {
            return dto;
        }
    }

    public List<MondayGroupDTO> listGroupsBoard() throws Exception {
        String boardId = this.getVersionBoardId();
        if (StringUtils.hasText(boardId)) {
            return this.listGroupsBoard(boardId);
        }
        return null;
    }
    public List<MondayGroupDTO> listGroupsBoard(String boardId) throws Exception {
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("boardId", Arrays.asList(boardId));
        List<MondayGroupDTO> dto = client.postList("queryGroupsBoard", variables, "boards[0].groups", MondayGroupDTO.class);
        if (dto == null || dto.isEmpty()) {
            return null;
        } else {
            return dto;
        }
    }

    public List<MondayColumnDTO> listColumnsBoard() throws Exception {
        String boardId = this.getVersionBoardId();
        if (StringUtils.hasText(boardId)) {
            return this.listColumnsBoard(boardId, null, null);
        }
        return null;
    }

    public List<MondayColumnDTO> listColumnsTaskBoard() throws Exception {
        String boardId = this.getTaskBoardId();
        if (StringUtils.hasText(boardId)) {
            return this.listColumnsBoard(boardId, null, null);
        }
        return null;
    }

    public List<MondayColumnDTO> listColumnsBoard(String boardId, List<String> columnIds, List<MondayColumnTypesEnum> types) throws Exception {
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("boardId", Arrays.asList(boardId));
        if (columnIds != null && !columnIds.isEmpty()) {
            variables.put("columnIds", columnIds);
        }
        if (types != null && !types.isEmpty()) {
            variables.put("types", types);
        }
        List<MondayColumnDTO> dto = client.postList("queryColumnsBoard", variables, "boards[0].columns", MondayColumnDTO.class);
        if (dto == null || dto.isEmpty()) {
            return null;
        } else {
            return dto;
        }
    }

    public LinkedHashMap<String, String> listPossibleUpdateValues() throws Exception {
        String boardId = this.getVersionBoardId();
        String columnStatus = this.getStatusColumnId();
        if (StringUtils.hasText(boardId) && StringUtils.hasText(columnStatus)) {
            List<MondayColumnDTO> columns = this.listColumnsBoard(boardId, Arrays.asList(columnStatus), null);
            if (columns != null && !columns.isEmpty()) {
                return columns.get(0).getPossibleValues();
            }
        }
        return null;
    }

    public LinkedHashMap<String, String> listPossibleItemsStatusValues() throws Exception {
        String boardId = this.getTaskBoardId();
        String columnStatus = this.getItemsStatusColumnId();
        if (StringUtils.hasText(boardId) && StringUtils.hasText(columnStatus)) {
            List<MondayColumnDTO> columns = this.listColumnsBoard(boardId, Arrays.asList(columnStatus), null);
            if (columns != null && !columns.isEmpty()) {
                return columns.get(0).getPossibleValues();
            }
        }
        return null;
    }

    public String validateVersion(VersaoBuildBaseBean version, String product) throws Exception {
        if (!this.versionGerenarionIsEnabled()) {
            return null;
        }
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("board_id", Arrays.asList(this.getConfig().getVersionGenerationConfig().getBoardId()));
        variables.put("group_id", Arrays.asList(this.getConfig().getVersionGenerationConfig().getGroupId()));
        MondayBoardDTO board = client.post("queryVersionGenerationBoard", variables, "boards[0]", MondayBoardDTO.class);
        if (board == null || !StringUtils.hasText(board.getId())) {
            throw new MondayValidateException("Quadro " + this.getConfig().getVersionGenerationConfig().getBoardId() + " não foi encontrado");
        }
        MondayGroupDTO group = board.getGroups().get(0);
        if (group == null || !StringUtils.hasText(group.getId())) {
            throw new MondayValidateException("Grupo " + this.getConfig().getVersionGenerationConfig().getGroupId() + " não foi encontrado");
        }
        if (group.getItems_page() == null || group.getItems_page().getItems() == null || group.getItems_page().getItems().isEmpty()) {
            throw new MondayValidateException("Nenhum item encontrado no grupo");
        }
        MondayValidateException validateException = new MondayValidateException();
        // Validar todas as colunas
        {
            MondayItemPageDTO firstItem =  group.getItems_page().getItems().get(0);
            MondayColumnValueDTO columnProduct = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnProduct().getId())).findFirst().orElse(null);
            if (columnProduct == null) {
                validateException.addError("Coluna de produto não foi encontrada");
            }
            MondayColumnValueDTO columnMajorMinor = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnMajorMinor().getId())).findFirst().orElse(null);
            if (columnMajorMinor == null) {
                validateException.addError("Coluna de versão (major/minor) não foi encontrada");
            }
            MondayColumnValueDTO columnPatch = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnPatch().getId())).findFirst().orElse(null);
            if (columnPatch == null) {
                validateException.addError("Coluna de subversão (patch) não foi encontrada");
            }
            MondayColumnValueDTO columnBuild = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnBuild().getId())).findFirst().orElse(null);
            if (columnBuild == null) {
                validateException.addError("Coluna de build não foi encontrada");
            }
            MondayColumnValueDTO columnStatus = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnStatus().getId())).findFirst().orElse(null);
            if (columnStatus == null) {
                validateException.addError("Coluna de situação de geração não foi encontrada");
            }
            MondayColumnValueDTO columnResponsable = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnResponsable().getId())).findFirst().orElse(null);
            if (columnResponsable == null) {
                validateException.addError("Coluna de gerador (responsável) não foi encontrada");
            }
            MondayColumnValueDTO columnVersionType = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnVersionType().getId())).findFirst().orElse(null);
            if (columnVersionType == null) {
                validateException.addError("Coluna tipo de geração (release) não foi encontrada");
            }
            MondayColumnValueDTO columnItemsIncluded = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnItemsIncluded().getId())).findFirst().orElse(null);
            if (columnItemsIncluded == null) {
                validateException.addError("Coluna dos itens incluídos na build não foi encontrada");
            }
            MondayColumnValueDTO columnDate = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnGenerationDate().getId())).findFirst().orElse(null);
            if (columnDate == null) {
                validateException.addError("Coluna data de geração não foi encontrada");
            }
            MondayColumnValueDTO columnHour = firstItem.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnGenerationHour().getId())).findFirst().orElse(null);
            if (columnHour == null) {
                validateException.addError("Coluna hora de geração não foi encontrada");
            }
        }
        if (validateException.getErrors() != null && !validateException.getErrors().isEmpty()) {
            throw validateException;
        }

        List<MondayItemPageDTO> itens = group.getItems_page().getItems().stream().filter(x -> {

            MondayColumnValueDTO columnProduct = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnProduct().getId())).findFirst().orElse(null);
            MondayColumnValueDTO columnMajorMinor = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnMajorMinor().getId())).findFirst().orElse(null);
            MondayColumnValueDTO columnPatch = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnPatch().getId())).findFirst().orElse(null);
            MondayColumnValueDTO columnBuild = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnBuild().getId())).findFirst().orElse(null);


            if (validateException.getErrors() != null && !validateException.getErrors().isEmpty()) {
                return false;
            }

            String majorMinor = version.getMajor() + "." + version.getMinor();
            return (StringUtils.hasText(columnProduct.getDisplay_value()) && columnProduct.getDisplay_value().matches("(?i).*" + product + ".*") || StringUtils.hasText(columnProduct.getValue()) && columnProduct.getValue().matches("(?i).*" + product + ".*")) &&
                    (StringUtils.hasText(columnMajorMinor.getDisplay_value()) && columnMajorMinor.getDisplay_value().matches("(?i).*" + majorMinor + ".*") || StringUtils.hasText(columnMajorMinor.getValue()) && columnMajorMinor.getValue().matches("(?i).*" + majorMinor + ".*")) &&
                    (StringUtils.hasText(columnPatch.getDisplay_value()) && columnPatch.getDisplay_value().matches("(?i).*" + version.getPatch() + ".*") || StringUtils.hasText(columnPatch.getValue()) && columnPatch.getValue().matches("(?i).*" + version.getPatch() + ".*")) &&
                    (StringUtils.hasText(columnBuild.getDisplay_value()) && columnBuild.getDisplay_value().matches("(?i).*" + version.getBuild() + ".*") || StringUtils.hasText(columnBuild.getValue()) && columnBuild.getValue().matches("(?i).*" + version.getBuild() + ".*"));

            /*
            return (StringUtils.hasText(columnProduct.getDisplay_value()) && columnProduct.getDisplay_value().matches("(?i).*" + produto + ".*") || StringUtils.hasText(columnProduct.getValue()) && columnProduct.getValue().matches("(?i).*" + produto + ".*")) &&
                    (StringUtils.hasText(columnMajorMinor.getDisplay_value()) && columnMajorMinor.getDisplay_value().matches("(?i).*" + majorMinor + ".*") || StringUtils.hasText(columnMajorMinor.getDisplay_value()) && columnMajorMinor.getValue().matches("(?i).*" + majorMinor + ".*"));
             */
        }).collect(Collectors.toList());

        if (itens == null || itens.isEmpty()) {
            throw new MondayValidateException(String.format("Nenhum item encontrado para o produto %s e versão %s", product, version.getTag()));
        }
        if (itens.size() != 1) {
            throw new MondayValidateException(String.format("Mais de um item encontrado para o produto %s e versão %s", product, version.getTag()));
        }
        MondayItemPageDTO item = itens.get(0);

        MondayColumnValueDTO columnStatus = item.getColumn_values().stream().filter(x -> x.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnStatus().getId())).findFirst().orElse(null);
        MondayColumnValueDTO columnItemsIncluded = item.getColumn_values().stream().filter(x -> x.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsIncluded().getId())).findFirst().orElse(null);

        if (!this.getConfig().getVersionGenerationConfig().getAllowedStatusValues().containsValue(columnStatus.getText())) {
            validateException.addError(String.format("A situação %s não é permitida na geração de build", columnStatus.getText()));
        }

        List<String> linkedItemns = columnItemsIncluded.getLinked_item_ids();
        if (linkedItemns == null || linkedItemns.isEmpty()) {
            validateException.addError("Não existe nenhum item incluído na build");
        }
        else {
            List<MondayItemPageDTO> lItems = new ArrayList<>();
            for (int i = 0; i < linkedItemns.size()/100 + 1; i++) { // O maximo de consultas é 100
                int startIndex = i * 100;
                int endIndex = startIndex + 100;
                if (endIndex >= linkedItemns.size()) {
                    endIndex = linkedItemns.size();
                }
                variables = new LinkedHashMap<>();
                List<String> sublist = linkedItemns.subList(startIndex, endIndex);
                if (sublist.size() > 0) {
                    variables.put("item_ids", sublist);
                    lItems.addAll(client.postList("queryItemsStatusColumns", variables, "items", MondayItemPageDTO.class));
                }
            }

            boolean columnStatusItemExists = lItems.stream().allMatch(x -> x.getColumn_values().stream().anyMatch(y ->
                            y.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsStatus().getId()) ||
                            y.getColumn().getTitle().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsStatus().getTitle())
                    ));
            if (!columnStatusItemExists) {
                validateException.addError("Coluna de status dos itens incluídos na build não foi encontrada em algum dos itens");
            }
            else {
                List<MondayItemPageDTO> notAllowedItems = lItems.stream().filter(x -> {
                    MondayColumnValueDTO columnStatusItem = x.getColumn_values().stream().filter(y ->
                                    y.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsStatus().getId()) ||
                                    y.getColumn().getTitle().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsStatus().getTitle())
                            ).findFirst().get();
                    return !this.getConfig().getVersionGenerationConfig().getAllowedItemStatusValues().containsValue(columnStatusItem.getText());

                }).collect(Collectors.toList());
                if (!notAllowedItems.isEmpty()) {
                    String mensagem = "Existem %s itens em desenvolvimento e que não podem entrar na build:";
                    if (notAllowedItems.size() == 1) {
                        mensagem = "Existe %s item em desenvolvimento e que não pode entrar na build:";
                    }
                    String sitens = String.join("\r\n", IntStream.range(1, notAllowedItems.size() + 1)
                                    .mapToObj(i -> org.apache.commons.lang3.StringUtils.leftPad(i + "", 2, "0") + ") " + notAllowedItems.get(i -1).getName())
                                            .collect(Collectors.toList()));
                    validateException.addError(String.format(mensagem + "\r\n%s",
                            notAllowedItems.size(),
                            sitens));
                }
            }

        }
        if (validateException.getErrors() != null && !validateException.getErrors().isEmpty()) {
            throw validateException;
        }
        return item.getId();
    }
    public void sendVersionSync(GeracaoVersaoMondayNotificationDTO dto) throws Exception {
        this.sendVersion(dto);
    }

    @Async
    public void sendVersionAsync(GeracaoVersaoMondayNotificationDTO dto) throws Exception {
        this.sendVersion(dto);
    }
    private void sendVersion(GeracaoVersaoMondayNotificationDTO dto) throws Exception {
        if (!this.versionGerenarionIsEnabled()) {
            return;
        }
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("item_ids", Arrays.asList(dto.getMondayId()));
        MondayItemPageDTO item = client.post("queryItems", variables, "items[0]", MondayItemPageDTO.class);

        List<String> linkedItems = item.getColumn_values().stream().filter(x -> x.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsIncluded().getId())).findFirst().get().getLinked_item_ids();
        //List<String> pendingBuildItems = this.getItemsFromProductSmallerBuild(dto.getProduct(), new VersaoBuildBaseBean(dto.getTag()));
        //linkedItems = linkedItems.stream().filter(x -> !pendingBuildItems.contains(x)).collect(Collectors.toList());
        List<String> itemMutation = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (linkedItems != null && !linkedItems.isEmpty()) {
            List<MondayItemPageDTO> lItems = new ArrayList<>();
            for (int i = 0; i < linkedItems.size()/100 + 1; i++) {
                int startIndex = i * 100;
                int endIndex = startIndex + 100;
                if (endIndex >= linkedItems.size()) {
                    endIndex = linkedItems.size();
                }
                variables = new LinkedHashMap<>();
                List<String> sublist = linkedItems.subList(startIndex, endIndex);
                if (sublist.size() > 0) {
                    variables.put("item_ids", sublist);
                    lItems.addAll(client.postList("queryItemsStatusColumns", variables, "items", MondayItemPageDTO.class));
                }
            }

            for (MondayItemPageDTO i : lItems) {

                MondayColumnValueDTO itemStatusColumn = i.getColumn_values().stream().filter(x ->
                        x.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsStatus().getId()) ||
                        x.getColumn().getTitle().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsStatus().getTitle())
                ).findFirst().get();
                //MondayStatusColumnValueDTO itemStatusColumnValue = mapper.readValue(itemStatusColumn.getValue(), MondayStatusColumnValueDTO.class);
                Map.Entry<String, String> e = this.getConfig().getVersionGenerationConfig().getAllowedItemStatusValues().entrySet().stream().filter(x -> x.getValue().equalsIgnoreCase(itemStatusColumn.getText())).findFirst().orElse(null);
                if (e == null) {
                    continue;
                }
                String newItemStatusIndex = this.getConfig().getVersionGenerationConfig().getMappedItemStatusValues().get(e.getKey());
                if (!StringUtils.hasText(newItemStatusIndex)) {
                    continue;
                }
                String newItemStatusValue = this.getConfig().getVersionGenerationConfig().getAllowedItemStatusValues().get(newItemStatusIndex);
                if (!StringUtils.hasText(newItemStatusValue)) {
                    continue;
                }
                variables = new LinkedHashMap<>();
                //variables.put("mutation_id", "updateItem" + itemMutation.size());
                variables.put("item_id", i.getId());
                variables.put("board_id", i.getBoard().getId());
                variables.put("column_id", itemStatusColumn.getColumn().getId());
                variables.put("column_value", newItemStatusValue);
                String query = client.parserToQueryString("updateItemStatus", variables).toString();
                query = query.replace("$mutation_id", "updateItem" + itemMutation.size());
                itemMutation.add(query);
            }
        }
        variables = new LinkedHashMap<>();
        variables.put("emails", Arrays.asList(dto.getAuthorEmail()));
        variables.put("limit", 1);
        List<MondayUserDTO> users = client.postList("queryUsers", variables, "users", MondayUserDTO.class);

        variables = new LinkedHashMap<>();
        variables.put("board_id", item.getBoard().getId());
        variables.put("item_id", item.getId());
        LinkedHashMap<String, Object> columnValues = new LinkedHashMap<>();

        MondayColumnValueDTO statusColumn = item.getColumn_values().stream().filter(x -> x.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnStatus().getId())).findFirst().get();
        MondayStatusColumnValueDTO statusColumnValue = mapper.readValue(statusColumn.getValue(), MondayStatusColumnValueDTO.class);
        String newStatusValue = this.getConfig().getVersionGenerationConfig().getMappedStatusValues().get(statusColumnValue.getIndex());
        columnValues.put(this.getConfig().getVersionGenerationConfig().getColumnStatus().getId(), newStatusValue);
        Calendar conclusionDate = dto.getConclusionDate();
        columnValues.put(this.getConfig().getVersionGenerationConfig().getColumnGenerationDate().getId(), new SimpleDateFormat("yyyy-MM-dd").format(conclusionDate.getTime()));
        columnValues.put(this.getConfig().getVersionGenerationConfig().getColumnGenerationHour().getId(), new MondayHourDTO(conclusionDate.get(Calendar.HOUR_OF_DAY), conclusionDate.get(Calendar.MINUTE)));
        columnValues.put(this.getConfig().getVersionGenerationConfig().getColumnResponsable().getId(), users.get(0).getId().toString());


        variables.put("column_values", mapper.writeValueAsString(columnValues));
        if (!itemMutation.isEmpty()) {
            String mutationQuery = client.parserToQueryString("updateVersionValues", null);
            mutationQuery = mutationQuery.replace("#itemnsMutation", String.join("\r\n", itemMutation));
            String plain = client.parserToQueryString("updateVersionValues", variables);
            GraphQlClient.RequestSpec req = client.prepareRequestFromQuery(mutationQuery, variables);
            client.post(req, "", JsonNode.class);
        } else {
            client.post("updateVersionValues", variables, "", JsonNode.class);
        }
    }

    private List<String> getItemsFromProductSmallerBuild(String product, VersaoBuildBaseBean build) throws Exception {
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("board_id", Arrays.asList(this.getConfig().getVersionGenerationConfig().getBoardId()));
        variables.put("group_id", Arrays.asList(this.getConfig().getVersionGenerationConfig().getGroupId()));
        MondayBoardDTO board = client.post("queryVersionGenerationBoard", variables, "boards[0]", MondayBoardDTO.class);
        MondayGroupDTO group = board.getGroups().get(0);

        List<MondayItemPageDTO> itens = group.getItems_page().getItems().stream().filter(x -> {

            MondayColumnValueDTO columnProduct = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnProduct().getId())).findFirst().orElse(null);
            MondayColumnValueDTO columnMajorMinor = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnMajorMinor().getId())).findFirst().orElse(null);
            MondayColumnValueDTO columnPatch = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnPatch().getId())).findFirst().orElse(null);
            MondayColumnValueDTO columnBuild = x.getColumn_values().stream().filter(y -> y.getColumn().getId().equals(this.getConfig().getVersionGenerationConfig().getColumnBuild().getId())).findFirst().orElse(null);

            if ((StringUtils.hasText(columnProduct.getDisplay_value()) && columnProduct.getDisplay_value().matches("(?i).*" + product + ".*") ||
                    StringUtils.hasText(columnProduct.getValue()) && columnProduct.getValue().matches("(?i).*" + product + ".*")) == false) {
                return false;
            }

            String tag = "";
            if (StringUtils.hasText(columnMajorMinor.getDisplay_value()) && columnMajorMinor.getDisplay_value().replaceAll("(?![0-9\\.]).", "").matches("[0-9]{1,}\\.[0-9]{1,}")) {
                tag = columnMajorMinor.getDisplay_value().replaceAll("(?![0-9\\.]).", "");
            }
            else if (StringUtils.hasText(columnMajorMinor.getValue()) && columnMajorMinor.getValue().replaceAll("(?![0-9\\.]).", "").matches("[0-9]{1,}\\.[0-9]{1,}")) {
                tag = columnMajorMinor.getValue().replaceAll("(?![0-9\\.]).", "");
            }

            if (StringUtils.hasText(columnPatch.getDisplay_value()) && columnPatch.getDisplay_value().replaceAll("(?![0-9]).", "").matches("[0-9]{1,}")) {
                tag += "." + columnPatch.getDisplay_value().replaceAll("(?![0-9]).", "");
            }
            else if (StringUtils.hasText(columnPatch.getValue()) && columnPatch.getValue().replaceAll("(?![0-9]).", "").matches("[0-9]{1,}")) {
                tag += "." + columnPatch.getValue().replaceAll("(?![0-9]).", "");
            }

            if (StringUtils.hasText(columnBuild.getDisplay_value()) && columnBuild.getDisplay_value().replaceAll("(?![0-9]).", "").matches("[0-9]{1,}")) {
                tag += "." + columnBuild.getDisplay_value().replaceAll("(?![0-9]).", "");
            }
            else if (StringUtils.hasText(columnBuild.getValue()) && columnBuild.getValue().replaceAll("(?![0-9]).", "").matches("[0-9]{1,}")) {
                tag += "." + columnBuild.getValue().replaceAll("(?![0-9]).", "");
            }

            VersaoBuildBaseBean compareVersion = new VersaoBuildBaseBean(tag);
            return compareVersion.compareTo(build) < 0;


        }).collect(Collectors.toList());
        List<String> ids = new ArrayList<>();
        if (itens != null && !itens.isEmpty()) {
            for (MondayItemPageDTO i : itens) {
                MondayColumnValueDTO columnItemsIncluded = i.getColumn_values().stream().filter(x -> x.getColumn().getId().equalsIgnoreCase(this.getConfig().getVersionGenerationConfig().getColumnItemsIncluded().getId())).findFirst().get();
                if (columnItemsIncluded.getLinked_item_ids() != null & !columnItemsIncluded.getLinked_item_ids().isEmpty()) {
                    ids.addAll(columnItemsIncluded.getLinked_item_ids());
                }
            }
        }
        return ids;
    }

    public String getLinkItem(String id) throws Exception {
        this.client = GraphqlClientService.using(this.getConfig().getEndpoint(), this.getConfig().getToken(), this.version);
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("item_ids", Arrays.asList(id));
        MondayItemPageDTO item = client.post("queryItems", variables, "items[0]", MondayItemPageDTO.class);
        if (item != null) {
            String boardId = item.getBoard().getId();
            URIBuilder builder = new URIBuilder(String.format("%s/boards/%s/pulses/%s", this.getConfig().getPage(), boardId, id));
            return builder.toString();
        }
        return null;
    }

    public boolean initialize(Object... param) {
        return this.getConfig() != null && this.getConfig().getEnabled() != null && this.getConfig().getEnabled().booleanValue();
    }

    public void dispose() {
        this.config = null;
    }

    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.MONDAY).orElse(null);
    }

    public MondayConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (MondayConfigDTO) c.getConfig();
            }
        }
        return config;
    }

    public void setConfig(MondayConfigDTO dto) {
        this.config = dto;
    }

    public String getVersionBoardId() {
        if (this.getConfig().getVersionGenerationConfig() != null) {
            return this.getConfig().getVersionGenerationConfig().getBoardId();
        }
        return null;
    }

    public String getTaskBoardId() {
        if (this.getConfig().getVersionGenerationConfig() != null) {
            return this.getConfig().getVersionGenerationConfig().getTaskBoardId();
        }
        return null;
    }

    public String getStatusColumnId() {
        if (this.getConfig().getVersionGenerationConfig() != null) {
            return this.getConfig().getVersionGenerationConfig().getColumnStatus().getId();
        }
        return null;
    }

    public String getItemsStatusColumnId() {
        if (this.getConfig().getVersionGenerationConfig() != null) {
            return this.getConfig().getVersionGenerationConfig().getColumnItemsStatus().getId();
        }
        return null;
    }

    public boolean versionGerenarionIsEnabled() {
        try {
            boolean ret = this.initialize(null) && this.getConfig().getVersionGenerationConfig() != null && this.getConfig().getVersionGenerationConfig().isEnabled();
            return ret;
        }
        finally {
            this.dispose();
        }
    }
}
