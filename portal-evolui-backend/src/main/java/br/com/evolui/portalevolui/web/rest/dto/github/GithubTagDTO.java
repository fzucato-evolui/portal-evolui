package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubTagDTO {
    private String name;
    private Commit commit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public class Commit {
        private String sha;

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }
    }
}
