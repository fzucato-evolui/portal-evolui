package br.com.evolui.portalevolui.web.rest.dto.monday;

import java.util.List;

public class MondayBoardDTO {
    private String name;
    private String id;
    private List<MondayGroupDTO> groups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<MondayGroupDTO> getGroups() {
        return groups;
    }

    public void setGroups(List<MondayGroupDTO> groups) {
        this.groups = groups;
    }
}
