package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.ClienteBean;

import java.util.ArrayList;
import java.util.List;

public class GithubClienteDTO {
    private String identifier;
    private List<String> keywords;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void addKeyword(String keyword) {
        if (this.keywords == null) {
            this.keywords = new ArrayList<>();
        }
        this.keywords.add(keyword);
    }

    public static GithubClienteDTO fromBean(ClienteBean bean) {
        if (bean == null) {
            return null;
        }
        GithubClienteDTO dto = new GithubClienteDTO();
        dto.setIdentifier(bean.getIdentifier());
        if (bean.getKeywords() != null) {
            for(String keyword : bean.getKeywords()) {
                dto.addKeyword(keyword);
            }
        }
        return dto;
    }
}
