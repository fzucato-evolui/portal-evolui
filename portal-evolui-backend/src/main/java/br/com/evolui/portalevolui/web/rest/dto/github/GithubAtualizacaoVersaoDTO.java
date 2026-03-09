package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.VersaoBean;
import br.com.evolui.portalevolui.web.beans.VersaoModuloBean;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GithubAtualizacaoVersaoDTO extends GithubBasicInputDTO {
    private LinkedHashMap<String, GithubAtualizacaoVersaoModuleResultDTO> modules;
    private List<LinkedHashMap<String, GithubVersaoDTO>> versions;
    private GithubAmbienteDTO enviroment;
    private String identifier;
    private String repository;

    public LinkedHashMap<String, GithubAtualizacaoVersaoModuleResultDTO> getModules() {
        return modules;
    }

    public void setModules(LinkedHashMap<String, GithubAtualizacaoVersaoModuleResultDTO> modules) {
        this.modules = modules;
    }

    public void addModule (String key, GithubAtualizacaoVersaoModuleResultDTO module) {
        if (this.modules == null) {
            this.modules = new LinkedHashMap<>();
        }
        this.modules.put(key, module);
    }

    public void addModule (String key, boolean enabled, boolean executeUpdateCommands) {
        if (this.modules == null) {
            this.modules = new LinkedHashMap<>();
        }
        GithubAtualizacaoVersaoModuleResultDTO dto = new GithubAtualizacaoVersaoModuleResultDTO();
        dto.setEnabled(enabled);
        dto.setExecuteUpdateCommands(executeUpdateCommands);
        this.modules.put(key, dto);
    }

    public List<LinkedHashMap<String, GithubVersaoDTO>> getVersions() {
        return versions;
    }

    public void setVersions(List<LinkedHashMap<String, GithubVersaoDTO>> versions) {
        this.versions = versions;
    }

    public void addVersion(VersaoBean bean) {
        if (this.versions == null) {
            this.versions = new ArrayList<>();
        }
        LinkedHashMap<String, GithubVersaoDTO> l = new LinkedHashMap<>();
        for (VersaoModuloBean mod : bean.getModules()) {
            GithubVersaoDTO v = new GithubVersaoDTO();
            v.setTag(mod.getTag());
            v.setBranch(mod.getBranch());
            v.setCommit(mod.getCommit());
            v.setRepository(mod.getRepository());
            v.setRelativePath(mod.getRelativePath());
            l.put(mod.getProjectModule().getIdentifier(), v);
        }
        this.versions.add(l);
    }

    public GithubAmbienteDTO getEnviroment() {
        return enviroment;
    }

    public void setEnviroment(GithubAmbienteDTO enviroment) {
        this.enviroment = enviroment;
    }

    public static GithubAtualizacaoVersaoDTO fromBean(AtualizacaoVersaoBean bean,
                                                      List<VersaoBean> versoes, String webhook) {
        if (bean == null) {
            return null;
        }
        GithubAtualizacaoVersaoDTO dto = new GithubAtualizacaoVersaoDTO();
        BeanUtils.copyProperties(bean, dto);
        dto.setUser(new GithubUserDTO(bean.getUser().getName(), bean.getUser().getEmail()));
        dto.setWebhook(webhook);
        dto.setIdentifier(bean.getEnvironment().getProject().getIdentifier());
        for (AtualizacaoVersaoModuloBean b : bean.getModules()) {
            dto.addModule(b.getEnvironmentModule().getProjectModule().getIdentifier(), b.isEnabled(), b.getExecuteUpdateCommands());
        }

        for(VersaoBean v : versoes) {
            dto.addVersion(v);
        }
        dto.setEnviroment(GithubAmbienteDTO.fromBean(bean.getEnvironment()));
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
}
