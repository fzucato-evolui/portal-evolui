package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.dto.HealthCheckerModuleConfigDTO;
import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Calendar;

@Entity
@Table(name = "health_checker_modulo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonView(JsonViewerPattern.Admin.class)
public class HealthCheckerModuloBean {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "health_checker_modulo_sequence_gen")
    @SequenceGenerator(name = "health_checker_modulo_sequence_gen", sequenceName = "health_checker_modulo_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true)
    private String identifier;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_check_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private HealthCheckerBean healthChecker;

    @Column(name = "last_health_date")
    private Calendar lastHealthDate;

    @Column(name = "last_update")
    private Calendar lastUpdate;

    @Column(name = "health")
    private Boolean health;

    @Lob
    @Column(name = "config", columnDefinition = "TEXT", nullable = false)
    @JsonView(JsonViewerPattern.Super.class)
    @Basic(fetch = FetchType.LAZY)
    private String config;

    @Lob
    @Column(name = "alerts", columnDefinition = "TEXT")
    @JsonView(JsonViewerPattern.Super.class)
    @Basic(fetch = FetchType.LAZY)
    private String alerts;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HealthCheckerBean getHealthChecker() {
        return healthChecker;
    }

    public void setHealthChecker(HealthCheckerBean healthChecker) {
        this.healthChecker = healthChecker;
    }

    public Calendar getLastHealthDate() {
        return lastHealthDate;
    }

    public void setLastHealthDate(Calendar lastHealthDate) {
        this.lastHealthDate = lastHealthDate;
    }

    public Calendar getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Calendar lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Boolean getHealth() {
        return health;
    }

    public void setHealth(Boolean health) {
        this.health = health;
    }

    public HealthCheckerModuleConfigDTO getConfig() {
        return HealthCheckerModuleConfigDTO.fromJson(this.config);
    }

    public void setConfig(HealthCheckerModuleConfigDTO config) {
        this.config = config.toJson();
    }

    public String getAlerts() {
        return alerts;
    }

    public void setAlerts(String alerts) {
        this.alerts = alerts;
    }
}
