package br.com.evolui.portalevolui.web.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "meta_project_client", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"meta_project_fk", "client_fk"}, name = "ux_meta_project_client")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MetadadosBranchClienteBean {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="mmeta_project_client_sequence_gen")
    @SequenceGenerator(name="meta_project_client_sequence_gen", sequenceName="meta_project_client_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private MetadadosBranchBean metadados;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id", "identifier"})
    private ClienteBean client;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MetadadosBranchBean getMetadados() {
        return metadados;
    }

    public void setMetadados(MetadadosBranchBean meta) {
        this.metadados = meta;
    }

    public ClienteBean getClient() {
        return client;
    }

    public void setClient(ClienteBean client) {
        this.client = client;
    }
}
