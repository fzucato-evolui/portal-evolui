package br.com.evolui.portalevolui.web.rest.dto.monday;

import java.util.List;

public class MondayColumnValueDTO {
    private String value;
    private String text;
    private List<MondayPersonAndTeamDTO> persons_and_teams;
    private String display_value;
    private MondayColumnDTO column;
    private List<String> linked_item_ids;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<MondayPersonAndTeamDTO> getPersons_and_teams() {
        return persons_and_teams;
    }

    public void setPersons_and_teams(List<MondayPersonAndTeamDTO> persons_and_teams) {
        this.persons_and_teams = persons_and_teams;
    }

    public String getDisplay_value() {
        return display_value;
    }

    public void setDisplay_value(String display_value) {
        this.display_value = display_value;
    }

    public MondayColumnDTO getColumn() {
        return column;
    }

    public void setColumn(MondayColumnDTO column) {
        this.column = column;
    }

    public List<String> getLinked_item_ids() {
        return linked_item_ids;
    }

    public void setLinked_item_ids(List<String> linked_item_ids) {
        this.linked_item_ids = linked_item_ids;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
