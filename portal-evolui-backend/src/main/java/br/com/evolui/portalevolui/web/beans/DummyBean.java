package br.com.evolui.portalevolui.web.beans;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Entity
public class DummyBean {

    @Id
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
