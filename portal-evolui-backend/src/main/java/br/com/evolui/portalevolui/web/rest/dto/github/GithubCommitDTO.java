package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.Calendar;
import java.util.TimeZone;

public class GithubCommitDTO {
    private String sha;
    private Commit commit;
    private AuthorCommit author;
    private String html_url;

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public AuthorCommit getAuthor() {
        return author;
    }

    public void setAuthor(AuthorCommit author) {
        this.author = author;
    }

    public String getHtml_url() {
        return html_url;
    }

    public void setHtml_url(String html_url) {
        this.html_url = html_url;
    }

    public static class AuthorCommit {
        private String login;
        private Long id;
        private String avatar_url;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAvatar_url() {
            return avatar_url;
        }

        public void setAvatar_url(String avatar_url) {
            this.avatar_url = avatar_url;
        }
    }
    public static class Commit {
        private Author author;
        private String message;

        public Author getAuthor() {
            return author;
        }

        public void setAuthor(Author author) {
            this.author = author;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public static class Author {
            private String name;
            private Calendar date;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public Calendar getDate() {
                return date;
            }

            public void setDate(Calendar date) {
                date.setTimeZone(TimeZone.getDefault());
                this.date = date;
            }
        }
    }
}
