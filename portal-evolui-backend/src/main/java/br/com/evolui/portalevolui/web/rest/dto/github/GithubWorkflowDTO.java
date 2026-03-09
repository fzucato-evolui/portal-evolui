package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;

import java.util.Calendar;

public class GithubWorkflowDTO {
    private Long id;
    private GithubActionStatusEnum status;
    private GithubActionConclusionEnum conclusion;
    private Calendar updated_at;
    private Actor actor;
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GithubActionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GithubActionStatusEnum status) {
        this.status = status;
    }

    public GithubActionConclusionEnum getConclusion() {
        return conclusion;
    }

    public void setConclusion(GithubActionConclusionEnum conclusion) {
        this.conclusion = conclusion;
    }

    public Calendar getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Calendar updated_at) {
        this.updated_at = updated_at;
    }

    public Actor getActor() {
        return actor;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public class Actor {
        private Long id;
        private String login;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }
    }
}
