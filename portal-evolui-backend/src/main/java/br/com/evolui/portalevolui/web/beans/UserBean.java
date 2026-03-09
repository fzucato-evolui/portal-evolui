package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;
import br.com.evolui.portalevolui.web.beans.enums.UserTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.UserConfigDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"login"}, name = "ux_users_login"),
        @UniqueConstraint(columnNames = {"email"}, name = "ux_users_email")
})
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JsonView(JsonViewerPattern.Public.class)
public class UserBean implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="users_sequence_gen")
    @SequenceGenerator(name="users_sequence_gen", sequenceName="users_sequence", initialValue = 3, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "email", nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password")
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "profile", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProfileEnum profile;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Lob
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "config_json")
    @JsonIgnore
    private String configJson;

    @Transient
    private String image;

    @Transient
    private String base64Image;

    @Transient
    private UserTypeEnum userType;

    @Transient
    private String newPassword;

    @Transient
    private UserConfigDTO config;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProfileEnum getProfile() {
        return profile;
    }

    public void setProfile(ProfileEnum profile) {
        this.profile = profile;
    }

    public String getImage() {
        if (this.image == null) {
            return "/server-files/users/" + this.id + ".png?timeStamp=" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        }
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public UserTypeEnum getUserType() {
        return userType;
    }

    public void setUserType(UserTypeEnum userType) {
        this.userType = userType;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configs) {
        this.configJson = configs;
    }

    public UserConfigDTO getConfig() throws Exception {
        if (this.config == null && StringUtils.hasText(this.configJson)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.config = mapper.readValue(this.configJson, UserConfigDTO.class);
        }
        return config;
    }

    public void setConfig(UserConfigDTO config) throws Exception {
        this.config = config;
        if (config != null) {
            this.configJson = new ObjectMapper().writeValueAsString(this.config);
        }
    }
}
