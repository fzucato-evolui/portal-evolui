package br.com.evolui.portalevolui.web.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "version_update_module", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"environment_module_fk", "version_update_fk"}, name = "ux_version_update_module")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AtualizacaoVersaoModuloBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "version_update_module_sequence_gen")
    @SequenceGenerator(name = "version_update_module_sequence_gen", sequenceName = "version_update_module_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_module_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AmbienteModuloBean environmentModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_update_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private AtualizacaoVersaoBean atualizacaoVersao;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "execute_commands", nullable = false)
    private Boolean executeUpdateCommands;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AtualizacaoVersaoBean getAtualizacaoVersao() {
        return atualizacaoVersao;
    }

    public void setAtualizacaoVersao(AtualizacaoVersaoBean atualizacaoVersao) {
        this.atualizacaoVersao = atualizacaoVersao;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getExecuteUpdateCommands() {
        return executeUpdateCommands;
    }

    public void setExecuteUpdateCommands(Boolean executeUpdateCommands) {
        this.executeUpdateCommands = executeUpdateCommands;
    }

    public AmbienteModuloBean getEnvironmentModule() {
        return environmentModule;
    }

    public void setEnvironmentModule(AmbienteModuloBean ambienteModulo) {
        this.environmentModule = ambienteModulo;
    }
}
