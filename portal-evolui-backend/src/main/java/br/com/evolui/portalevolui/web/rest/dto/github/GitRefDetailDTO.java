package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.Calendar;

public class GitRefDetailDTO {
    private String name;
    private String sha;
    private String author;
    private String message;
    private Calendar date;

    public GitRefDetailDTO() {}

    public GitRefDetailDTO(String name, String sha, String author, String message, Calendar date) {
        this.name = name;
        this.sha = sha;
        this.author = author;
        this.message = message;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }
}
