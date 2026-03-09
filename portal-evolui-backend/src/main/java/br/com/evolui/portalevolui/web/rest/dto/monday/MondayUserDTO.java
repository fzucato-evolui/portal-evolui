package br.com.evolui.portalevolui.web.rest.dto.monday;

import java.util.Calendar;

public class MondayUserDTO {
    private Long id;
    private Calendar created_at;
    private String name;
    private String email;
    private MondayAccountDTO account;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Calendar getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Calendar created_at) {
        this.created_at = created_at;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public MondayAccountDTO getAccount() {
        return account;
    }

    public void setAccount(MondayAccountDTO account) {
        this.account = account;
    }
}
