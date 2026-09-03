package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.CICDBean;
import br.com.evolui.portalevolui.web.beans.CICDModuloBean;
import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GithubCICDDTO extends GithubBasicInputDTO {
    private CompileTypeEnum compileType;
    private String runner;
    private String identifier;
    private String repository;
    private List<GithubMetadadosDTO> metadados;
    private LinkedHashMap<String, GithubCICDModuleResultDTO> modules;

    public List<GithubMetadadosDTO> getMetadados() {
        return metadados;
    }

    public void setMetadados(List<GithubMetadadosDTO> metadados) {
        this.metadados = metadados;
    }

    public void addMetadados(GithubMetadadosDTO metadados) {
        if (this.metadados == null) {
            this.metadados = new ArrayList<>();
        }
        this.metadados.add(metadados);
    }

    public String getRunner() {
        return runner;
    }

    public void setRunner(String runner) {
        this.runner = runner;
    }

    public LinkedHashMap<String, GithubCICDModuleResultDTO> getModules() {
        return modules;
    }

    public void setModules(LinkedHashMap<String, GithubCICDModuleResultDTO> modules) {
        this.modules = modules;
    }

    public void addModule (String key, GithubCICDModuleResultDTO module) {
        if (this.modules == null) {
            this.modules = new LinkedHashMap<>();
        }
        this.modules.put(key, module);
    }

    public void addModule (CICDModuloBean b) {
        if (this.modules == null) {
            this.modules = new LinkedHashMap<>();
        }
        GithubCICDModuleResultDTO dto = new GithubCICDModuleResultDTO();
        dto.setEnabled(b.isEnabled());
        dto.setTag(b.getTag());
        dto.setRepository(b.getRepository());
        dto.setBranch(b.getRepositoryBranch());
        dto.setIncludeTests(b.isIncludeTests());
        dto.setRelativePath(b.getRelativePath());
        dto.setCommit(b.getHashCommit());
        this.modules.put(b.getProjectModule().getIdentifier(), dto);
    }

    public static GithubCICDDTO fromBean(CICDBean bean,
                                         String runner,
                                         String webhook,
                                         List<MetadadosBranchBean> testMetadados) {
        GithubCICDDTO dto = new GithubCICDDTO();
        BeanUtils.copyProperties(bean, dto);

        dto.setRunner(runner);
        dto.setWebhook(webhook);
        dto.setUser(new GithubUserDTO(bean.getUser().getName(), bean.getUser().getEmail()));
        dto.setIdentifier(bean.getProject().getIdentifier());

        if (testMetadados != null) {
            for (MetadadosBranchBean meta : testMetadados) {
                dto.addMetadados(GithubMetadadosDTO.fromBean(meta));
            }
        }

        for(CICDModuloBean b : bean.getModules()) {
            dto.addModule(b);
        }
        return dto;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public CompileTypeEnum getCompileType() {
        return compileType;
    }

    public void setCompileType(CompileTypeEnum compileType) {
        this.compileType = compileType;
    }
}
