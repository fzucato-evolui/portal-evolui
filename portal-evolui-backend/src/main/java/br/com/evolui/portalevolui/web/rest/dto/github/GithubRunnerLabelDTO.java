package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.rest.dto.enums.GithubRunnerLabelTypeEnum;

public class GithubRunnerLabelDTO {
    private Integer id;
    private String name;
    private GithubRunnerLabelTypeEnum type;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GithubRunnerLabelTypeEnum getType() {
        return type;
    }

    public void setType(GithubRunnerLabelTypeEnum type) {
        this.type = type;
    }
}
