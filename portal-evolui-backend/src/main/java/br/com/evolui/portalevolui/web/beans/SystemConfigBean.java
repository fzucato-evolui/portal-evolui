package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.json.deserializer.SystemConfigBeanDeserializer;
import br.com.evolui.portalevolui.web.rest.dto.config.*;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "sysconfig", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"config_type"}, name = "ux_sysconfig_type")
})

@JsonDeserialize(using = SystemConfigBeanDeserializer.class)
public class SystemConfigBean {
    public SystemConfigBean(){};
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="system_cofig_sequence_gen")
    @SequenceGenerator(name="system_cofig_sequence_gen", sequenceName="system_cofig_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    @JsonView({JsonViewerPattern.Public.class, JsonViewerPattern.Admin.class})
    private Long id;

    @Column(name = "config_type")
    @JsonView({JsonViewerPattern.Public.class, JsonViewerPattern.Admin.class})
    private SystemConfigTypeEnum configType;

    @Lob
    @Column(name = "json", columnDefinition = "TEXT")
    @JsonIgnore
    private String json;

    @Transient
    @JsonView({JsonViewerPattern.Public.class, JsonViewerPattern.Admin.class})
    private ISystemConfigParser config;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SystemConfigTypeEnum getConfigType() {
        return configType;
    }

    public void setConfigType(SystemConfigTypeEnum configType) {
        this.configType = configType;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
        try {
            if (this.configType == SystemConfigTypeEnum.GOOGLE) {
                this.config = new GoogleConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.GENERAL) {
                this.config = new GeneralConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.AWS) {
                this.config = new AWSConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.GITHUB) {
                this.config = new GithubConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.SMTP) {
                this.config = new SMTPConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.NOTIFICATION) {
                this.config = new NotificationConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.MONDAY) {
                this.config = new MondayConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.CICD) {
                this.config = new CICDConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.AX) {
                this.config = new AXConfigDTO().parseJson(json);
            } else if (this.configType == SystemConfigTypeEnum.PORTAL_LUTHIER) {
                this.config = new PortalLuthierConfigDTO().parseJson(json);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ISystemConfigParser getConfig() {
        if (this.config == null && StringUtils.hasText(this.json)) {
            this.setJson(this.json);
        }
        return config;
    }

    public void setConfig(ISystemConfigParser config) {
        this.config = config;
        if (this.config != null) {
            try {
                this.json = config.getJson();
                if (config instanceof GoogleConfigDTO) {
                    this.configType = SystemConfigTypeEnum.GOOGLE;
                } else if (config instanceof GeneralConfigDTO) {
                    this.configType = SystemConfigTypeEnum.GENERAL;
                } else if (config instanceof AWSConfigDTO) {
                    this.configType = SystemConfigTypeEnum.AWS;
                } else if (config instanceof GithubConfigDTO) {
                    this.configType = SystemConfigTypeEnum.GITHUB;
                } else if (config instanceof SMTPConfigDTO) {
                    this.configType = SystemConfigTypeEnum.SMTP;
                } else if (config instanceof NotificationConfigDTO) {
                    this.configType = SystemConfigTypeEnum.NOTIFICATION;
                } else if (config instanceof MondayConfigDTO) {
                    this.configType = SystemConfigTypeEnum.MONDAY;
                } else if (config instanceof CICDConfigDTO) {
                    this.configType = SystemConfigTypeEnum.CICD;
                } else if (config instanceof AXConfigDTO) {
                    this.configType = SystemConfigTypeEnum.AX;
                } else if (config instanceof PortalLuthierConfigDTO) {
                    this.configType = SystemConfigTypeEnum.PORTAL_LUTHIER;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
