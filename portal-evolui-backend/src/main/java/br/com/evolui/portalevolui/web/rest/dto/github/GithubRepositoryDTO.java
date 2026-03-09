package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubRepositoryDTO {
    private String owner;
    private String repo;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }
}
