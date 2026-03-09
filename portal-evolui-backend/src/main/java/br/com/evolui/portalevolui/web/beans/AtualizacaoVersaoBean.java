package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.converter.StringListConverter;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@Entity
@Table(name = "version_update")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AtualizacaoVersaoBean extends VersaoBuildBaseBean implements Serializable {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="version_update_sequence_gen")
    @SequenceGenerator(name="version_update_sequence_gen", sequenceName="version_update_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties(value = {"modules"})
    private AmbienteBean environment;

    @Column(name = "workflow")
    private Long workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserBean user;

    @Column(name = "scheduler_date", nullable = true)
    private Calendar schedulerDate;

    @Column(name = "request_date", nullable = false)
    private Calendar requestDate;

    @Column(name = "conclusion_date", nullable = true)
    private Calendar conclusionDate;

    @Column(name = "status", nullable = false)
    private GithubActionStatusEnum status;

    @Column(name = "conclusion", nullable = true)
    private GithubActionConclusionEnum conclusion;

    @Column(name = "hash_token", nullable = true, length = 255, unique = true)
    private String hashToken;

    @Convert(converter = StringListConverter.class)
    @Column(name="tags", length = 5000)
    private List<String> tags;

    @OneToMany(mappedBy = "atualizacaoVersao",
            cascade = CascadeType.ALL
    )
    private List<AtualizacaoVersaoModuloBean> modules = new ArrayList<>();

    @Column(name="error")
    @Lob
    @JsonIgnore
    private String error;

    @Transient
    private String link;


    public Long getId() {
        return id;
    }

    
    public void setId(Long id) {
        this.id = id;
    }

    
    public AmbienteBean getEnvironment() {
        return environment;
    }

    
    public void setEnvironment(AmbienteBean enviroment) {
        this.environment = enviroment;
    }
    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
    }

    public Calendar getSchedulerDate() {
        return schedulerDate;
    }

    public void setSchedulerDate(Calendar schedulerDate) {
        if (schedulerDate != null) {
            schedulerDate.setTimeZone(TimeZone.getDefault());
        }
        this.schedulerDate = schedulerDate;
    }

    public Calendar getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Calendar requestDate) {
        this.requestDate = requestDate;
    }

    public Calendar getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(Calendar conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public GithubActionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GithubActionStatusEnum status) {
        this.status = status;
    }

    public GithubActionConclusionEnum getConclusion() {
        return conclusion;
    }

    public void setConclusion(GithubActionConclusionEnum conclusion) {
        this.conclusion = conclusion;
    }

    public String getHashToken() {
        return hashToken;
    }

    public void setHashToken(String hashToken) {
        this.hashToken = hashToken;
    }

    public Long getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Long workflow) {
        this.workflow = workflow;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }


    public List<AtualizacaoVersaoModuloBean> getModules() {
        return modules;
    }

    public void setModules(List<AtualizacaoVersaoModuloBean> modules) {
        if (modules != null  && !modules.isEmpty()) {
            for (AtualizacaoVersaoModuloBean b : modules) {
                b.setAtualizacaoVersao(this);
            }
        }
        this.modules = modules;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
