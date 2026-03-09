package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "cicd_project")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CICDBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="cicd_project_sequence_gen")
    @SequenceGenerator(name="cicd_project_sequence_gen", sequenceName="cicd_project_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private ProjectBean project;

    @Column(name = "workflow")
    private Long workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserBean user;

    @Column(name = "request_date", nullable = false)
    private Calendar requestDate;

    @Column(name = "conclusion_date", nullable = true)
    private Calendar conclusionDate;

    @Column(name = "status", nullable = false)
    private GithubActionStatusEnum status;

    @Column(name = "conclusion", nullable = true)
    private GithubActionConclusionEnum conclusion;

    @Column(name = "hash_token", unique = true)
    private String hashToken;

    @Column(name="error")
    @Lob
    @JsonIgnore
    private String error;

    @OneToMany(mappedBy = "cicd",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CICDModuloBean> modules = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectBean getProject() {
        return project;
    }

    public void setProject(ProjectBean project) {
        this.project = project;
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

    public String getHashToken() {
        return hashToken;
    }

    public void setHashToken(String hashToken) {
        this.hashToken = hashToken;
    }

    public List<CICDModuloBean> getModules() {
        return modules;
    }

    public void setModules(List<CICDModuloBean> modules) {
        if (modules != null && !modules.isEmpty()) {
            for(CICDModuloBean b : modules) {
                b.setCicd(this);
            }
        }
        this.modules = modules;
    }

    public void addModule(CICDModuloBean module) {
        if (modules == null) {
            modules = new ArrayList<>();

        }
        module.setCicd(this);
        this.modules.add(module);
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
