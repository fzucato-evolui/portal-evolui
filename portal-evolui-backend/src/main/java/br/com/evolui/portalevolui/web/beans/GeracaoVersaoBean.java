package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@Entity
@Table(name = "version_generation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GeracaoVersaoBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="version_generation_sequence_gen")
    @SequenceGenerator(name="vversion_generation_sequence_gen", sequenceName="version_generation_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties(value = {"modules", "identifier", "repository"})
    private ProjectBean project;

    @OneToMany(mappedBy = "geracaoVersao",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<GeracaoVersaoModuloBean> modules = new ArrayList<>();

    @Column(name = "workflow")
    private Long workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserBean user;

    @Column(name = "request_date", nullable = false)
    private Calendar requestDate;

    @Column(name = "scheduler_date", nullable = true)
    private Calendar schedulerDate;

    @Column(name = "conclusion_date", nullable = true)
    private Calendar conclusionDate;

    @Column(name = "status", nullable = false)
    private GithubActionStatusEnum status;

    @Column(name = "conclusion", nullable = true)
    private GithubActionConclusionEnum conclusion;

    @Column(name = "compile_type", nullable = false)
    private CompileTypeEnum compileType;

    @Column(name = "hash_token", unique = true)
    private String hashToken;

    @Column(name="error")
    @Lob
    @JsonIgnore
    private String error;

    @Column(name = "monday_id")
    private String mondayId;

    @Transient
    private String link;

    @Transient
    private String mondayLink;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<GeracaoVersaoModuloBean> getModules() {
        return modules;
    }

    public void setModules(List<GeracaoVersaoModuloBean> modules) {
        if (modules != null && !modules.isEmpty()) {
            for(GeracaoVersaoModuloBean b : modules) {
                b.setGeracaoVersao(this);
            }
        }
        this.modules = modules;
    }

    public Long getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Long workflow) {
        this.workflow = workflow;
    }

    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
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

    public CompileTypeEnum getCompileType() {
        return compileType;
    }

    public void setCompileType(CompileTypeEnum compileType) {
        this.compileType = compileType;
    }

    public String getHashToken() {
        return hashToken;
    }

    public void setHashToken(String hashToken) {
        this.hashToken = hashToken;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ProjectBean getProject() {
        return project;
    }

    public void setProject(ProjectBean project) {
        this.project = project;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMondayId() {
        return mondayId;
    }

    public void setMondayId(String mondayId) {
        this.mondayId = mondayId;
    }

    public String getMondayLink() {
        return mondayLink;
    }

    public void setMondayLink(String mondayLink) {
        this.mondayLink = mondayLink;
    }
}
