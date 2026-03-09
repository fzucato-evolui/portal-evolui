package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.web.json.deserializer.LinkedHashMapDeserializer;
import br.com.evolui.portalevolui.web.rest.dto.monday.MondayColumnDTO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashMap;

public class MondayVersionGenerationConfigDTO {
    private boolean enabled;
    private String taskBoardId;
    private String boardId;
    private String groupId;
    private MondayColumnDTO columnProduct;
    private MondayColumnDTO columnMajorMinor;
    private MondayColumnDTO columnPatch;
    private MondayColumnDTO columnBuild;
    private MondayColumnDTO columnStatus;
    private MondayColumnDTO columnResponsable;
    private MondayColumnDTO columnVersionType;
    private MondayColumnDTO columnItemsStatus;
    private MondayColumnDTO columnItemsIncluded;
    private MondayColumnDTO columnGenerationDate;
    private MondayColumnDTO columnGenerationHour;
    @JsonDeserialize(using = LinkedHashMapDeserializer.class)
    private LinkedHashMap<String, String> allowedStatusValues;
    @JsonDeserialize(using = LinkedHashMapDeserializer.class)
    private LinkedHashMap<String, String> allowedItemStatusValues;

    private LinkedHashMap<String, String> mappedStatusValues;
    private LinkedHashMap<String, String> mappedItemStatusValues;


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTaskBoardId() {
        return taskBoardId;
    }

    public void setTaskBoardId(String taskBoardId) {
        this.taskBoardId = taskBoardId;
    }

    public LinkedHashMap<String, String> getAllowedItemStatusValues() {
        return allowedItemStatusValues;
    }

    public void setAllowedItemStatusValues(LinkedHashMap<String, String> allowedItemStatusValues) {
        this.allowedItemStatusValues = allowedItemStatusValues;
    }

    public LinkedHashMap<String, String> getAllowedStatusValues() {
        return allowedStatusValues;
    }

    public void setAllowedStatusValues(LinkedHashMap<String, String> allowedStatusValues) {
        this.allowedStatusValues = allowedStatusValues;
    }

    public LinkedHashMap<String, String> getMappedStatusValues() {
        return mappedStatusValues;
    }

    public void setMappedStatusValues(LinkedHashMap<String, String> mappedStatusValues) {
        this.mappedStatusValues = mappedStatusValues;
    }

    public LinkedHashMap<String, String> getMappedItemStatusValues() {
        return mappedItemStatusValues;
    }

    public void setMappedItemStatusValues(LinkedHashMap<String, String> mappedItemStatusValues) {
        this.mappedItemStatusValues = mappedItemStatusValues;
    }

    public MondayColumnDTO getColumnProduct() {
        return columnProduct;
    }

    public void setColumnProduct(MondayColumnDTO columnProduct) {
        this.columnProduct = columnProduct;
    }

    public MondayColumnDTO getColumnMajorMinor() {
        return columnMajorMinor;
    }

    public void setColumnMajorMinor(MondayColumnDTO columnMajorMinor) {
        this.columnMajorMinor = columnMajorMinor;
    }

    public MondayColumnDTO getColumnPatch() {
        return columnPatch;
    }

    public void setColumnPatch(MondayColumnDTO columnPatch) {
        this.columnPatch = columnPatch;
    }

    public MondayColumnDTO getColumnBuild() {
        return columnBuild;
    }

    public void setColumnBuild(MondayColumnDTO columnBuild) {
        this.columnBuild = columnBuild;
    }

    public MondayColumnDTO getColumnStatus() {
        return columnStatus;
    }

    public void setColumnStatus(MondayColumnDTO columnStatus) {
        this.columnStatus = columnStatus;
    }

    public MondayColumnDTO getColumnResponsable() {
        return columnResponsable;
    }

    public void setColumnResponsable(MondayColumnDTO columnResponsable) {
        this.columnResponsable = columnResponsable;
    }

    public MondayColumnDTO getColumnVersionType() {
        return columnVersionType;
    }

    public void setColumnVersionType(MondayColumnDTO columnVersionType) {
        this.columnVersionType = columnVersionType;
    }

    public MondayColumnDTO getColumnItemsStatus() {
        return columnItemsStatus;
    }

    public void setColumnItemsStatus(MondayColumnDTO columnItemsStatus) {
        this.columnItemsStatus = columnItemsStatus;
    }

    public MondayColumnDTO getColumnItemsIncluded() {
        return columnItemsIncluded;
    }

    public void setColumnItemsIncluded(MondayColumnDTO columnItemsIncluded) {
        this.columnItemsIncluded = columnItemsIncluded;
    }

    public MondayColumnDTO getColumnGenerationDate() {
        return columnGenerationDate;
    }

    public void setColumnGenerationDate(MondayColumnDTO columnGenerationDate) {
        this.columnGenerationDate = columnGenerationDate;
    }

    public MondayColumnDTO getColumnGenerationHour() {
        return columnGenerationHour;
    }

    public void setColumnGenerationHour(MondayColumnDTO columnGenerationHour) {
        this.columnGenerationHour = columnGenerationHour;
    }
}
