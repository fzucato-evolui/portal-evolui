package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.dto.HealthCheckerAlertDTO;
import br.com.evolui.portalevolui.shared.dto.HealthCheckerConfigDTO;
import br.com.evolui.portalevolui.shared.dto.HealthCheckerSimpleSystemInfoDTO;
import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;
import br.com.evolui.portalevolui.web.config.WebSocketConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.StringUtils;

import javax.naming.NoPermissionException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "health_checker")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonView(JsonViewerPattern.Admin.class)
public class HealthCheckerBean implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "health_checker_sequence_gen")
    @SequenceGenerator(name = "health_checker_sequence_gen", sequenceName = "health_checker_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true)
    private String identifier;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserBean user;

    @Column(name = "last_health_date")
    private Calendar lastHealthDate;

    @Column(name = "last_update")
    private Calendar lastUpdate;

    @Column(name = "health")
    private Boolean health;

    @Lob
    @Column(name = "system_info", columnDefinition = "TEXT", nullable = false)
    private String systemInfo;

    @Lob
    @Column(name = "config", columnDefinition = "TEXT", nullable = false)
    @JsonView(JsonViewerPattern.Super.class)
    @Basic(fetch = FetchType.LAZY)
    private String config;

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean online;

    @OneToMany(mappedBy = "healthChecker",
            cascade = CascadeType.ALL
    )
    private List<HealthCheckerModuloBean> modules = new ArrayList<>();

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

    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
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

    public HealthCheckerSimpleSystemInfoDTO getSystemInfo() {
        return HealthCheckerSimpleSystemInfoDTO.fromJson(this.systemInfo);
    }

    public void setSystemInfo(HealthCheckerSimpleSystemInfoDTO systemInfo) {
        this.systemInfo = systemInfo.toJson();
    }

    public HealthCheckerConfigDTO getConfig() {
        return HealthCheckerConfigDTO.fromJson(this.config);
    }

    public void setConfig(HealthCheckerConfigDTO config) {
        this.config = config.toJson();
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public List<HealthCheckerModuloBean> getModules() {
        return modules;
    }

    public void setModules(List<HealthCheckerModuloBean> modules) {
        if (modules != null && !modules.isEmpty()) {
            for(HealthCheckerModuloBean b : modules) {
                b.setHealthChecker(this);
            }
        }
        this.modules = modules;
    }

    public void addModule(HealthCheckerModuloBean module) {
        if (this.modules == null) {
            this.modules = new ArrayList<>();
        }
        module.setHealthChecker(this);
        this.modules.add(module);
    }

    public List<HealthCheckerAlertDTO> getAlerts() {
        if (StringUtils.hasText(this.alerts)) {
            try {
                return new ObjectMapper().readValue(this.alerts, new TypeReference<List<HealthCheckerAlertDTO>>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return  null;
    }

    public void setAlerts(List<HealthCheckerAlertDTO> alerts) {
        if (alerts != null) {
            try {
                this.alerts = new ObjectMapper().writeValueAsString(alerts);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.alerts = null;
        }
    }

    @PostLoad
    private void postLoadFunction() {
        if (WebSocketConfig.connectedDevices.containsKey(this.identifier)) {
            this.online = true;
        } else {
            this.online = false;
        }
    }

    @PrePersist
    @PreUpdate
    private void beforeAnyUpdate() throws NoPermissionException {
        if (this.user.getProfile() != ProfileEnum.ROLE_HEALTHCHECKER) {
            throw new NoPermissionException("Apenas usuário com perfil HEALTHCHECKER podem ser atribuídos");
        }
    }


}
