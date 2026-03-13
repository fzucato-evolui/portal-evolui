package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.enums.CICDReportStatusTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubCICDModuleResultDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "cicd_project_module", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_module_fk", "cicd_project_fk"}, name = "ux_cicd_project_module")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CICDModuloBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cicd_project_module_sequence_gen")
    @SequenceGenerator(name = "cicd_project_module_sequence_gen", sequenceName = "cicd_project_module_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_module_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id", "identifier", "title"})
    private ProjectModuleBean projectModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cicd_project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private CICDBean cicd;

    @Column(name = "checkrun")
    private Long checkrun;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "include_tests", nullable = false)
    private boolean includeTests;

    @Column(name = "status")
    private CICDReportStatusTypeEnum status;

    @Column(name = "build_sumary")
    private String buildSumary;

    @Column(name = "test_sumary")
    private String testSumary;

    @Column(name = "fatal_error")
    private Boolean fatalError;

    @Transient
    private String repositoryBranch;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectModuleBean getProjectModule() {
        return projectModule;
    }

    public void setProjectModule(ProjectModuleBean projectModule) {
        this.projectModule = projectModule;
    }

    public CICDBean getCicd() {
        return cicd;
    }

    public void setCicd(CICDBean cicd) {
        this.cicd = cicd;
    }

    public Long getCheckrun() {
        return checkrun;
    }

    public void setCheckrun(Long checkrun) {
        this.checkrun = checkrun;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CICDReportStatusTypeEnum getStatus() {
        return status;
    }

    public void setStatus(CICDReportStatusTypeEnum status) {
        this.status = status;
    }

    public GithubCICDModuleResultDTO.Summary getBuildSumary() {
        if (StringUtils.hasText(this.buildSumary)) {
            return GithubCICDModuleResultDTO.Summary.fromJson(this.buildSumary);
        }
        return null;
    }

    public void setBuildSumary(GithubCICDModuleResultDTO.Summary buildSumary) {
        if (buildSumary != null) {
            this.buildSumary = buildSumary.toJson();
        }
    }

    public GithubCICDModuleResultDTO.Summary getTestSumary() {
        if (StringUtils.hasText(this.testSumary)) {
            return GithubCICDModuleResultDTO.Summary.fromJson(this.testSumary);
        }
        return null;
    }

    public void setTestSumary(GithubCICDModuleResultDTO.Summary testSumary) {
        if (testSumary != null) {
            this.testSumary = testSumary.toJson();
        }

    }

    public Boolean getFatalError() {
        return fatalError;
    }

    public void setFatalError(Boolean fatalError) {
        this.fatalError = fatalError;
    }

    public boolean isIncludeTests() {
        return includeTests;
    }

    public void setIncludeTests(boolean includeTests) {
        this.includeTests = includeTests;
    }

    public String getRepositoryBranch() {
        return repositoryBranch;
    }

    public void setRepositoryBranch(String repositoryBranch) {
        this.repositoryBranch = repositoryBranch;
    }
}
