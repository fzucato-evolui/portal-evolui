package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.GeracaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GithubGeracaoVersaoDTO extends GithubBasicInputDTO {
    private CompileTypeEnum compileType;
    private String runner;
    private String repository;
    private String identifier;
    private List<GithubMetadadosDTO> metadados;
    private LinkedHashMap<String, GithubGeracaoVersaoModuleResultDTO> modules;

    public CompileTypeEnum getCompileType() {
        return compileType;
    }

    public void setCompileType(CompileTypeEnum compileType) {
        this.compileType = compileType;
    }

    public String getRunner() {
        return runner;
    }

    public void setRunner(String runner) {
        this.runner = runner;
    }

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

    public LinkedHashMap<String, GithubGeracaoVersaoModuleResultDTO> getModules() {
        return modules;
    }

    public void setModules(LinkedHashMap<String, GithubGeracaoVersaoModuleResultDTO> modules) {
        this.modules = modules;
    }

    public void addModule (String key, GithubGeracaoVersaoModuleResultDTO module) {
        if (this.modules == null) {
            this.modules = new LinkedHashMap<>();
        }
        this.modules.put(key, module);
    }

    public void addModule (GeracaoVersaoModuloBean module) {
        if (this.modules == null) {
            this.modules = new LinkedHashMap<>();
        }
        GithubGeracaoVersaoModuleResultDTO dto = new GithubGeracaoVersaoModuleResultDTO();
        dto.setEnabled(module.isEnabled());
        dto.setTag(module.getTag());
        dto.setBranch(module.getRepositoryBranch());
        dto.setRepository(module.getRepository());
        dto.setCommit(module.getCommit());
        dto.setRelativePath(module.getRelativePath());
        this.modules.put(module.getProjectModule().getIdentifier(), dto);
    }

    public static GithubGeracaoVersaoDTO fromBean(GeracaoVersaoBean bean,
                                                  List<MetadadosBranchBean> metadadosBean,
                                                  String runner,
                                                  String webhook) {
        GithubGeracaoVersaoDTO dto = new GithubGeracaoVersaoDTO();
        BeanUtils.copyProperties(bean, dto);
        if (metadadosBean != null) {
            for (MetadadosBranchBean meta : metadadosBean) {
                dto.addMetadados(GithubMetadadosDTO.fromBean(meta));
            }
        }

        dto.setRunner(runner);
        dto.setWebhook(webhook);
        dto.setUser(new GithubUserDTO(bean.getUser().getName(), bean.getUser().getEmail()));
        dto.setRepository(bean.getProject().getRepository());
        dto.setIdentifier(bean.getProject().getIdentifier());
        for(GeracaoVersaoModuloBean b : bean.getModules()) {
            dto.addModule(b);
        }
        return dto;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
