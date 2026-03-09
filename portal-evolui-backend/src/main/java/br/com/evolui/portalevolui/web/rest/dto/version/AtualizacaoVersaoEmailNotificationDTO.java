package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AtualizacaoVersaoEmailNotificationDTO {
    private String title;
    private String link;
    private GithubActionConclusionEnum conclusion;
    private List<ModulesDTO> modules;
    private String author;
    private String requestDate;
    private String conclusionDate;
    private String subject;
    private List<String> destinations;
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public GithubActionConclusionEnum getConclusion() {
        return conclusion;
    }

    public void setConclusion(GithubActionConclusionEnum conclusion) {
        this.conclusion = conclusion;
    }

    public List<ModulesDTO> getModules() {
        return modules;
    }

    public void setModules(List<ModulesDTO> modules) {
        this.modules = modules;
    }

    public void addModule(ModulesDTO module) {
        if(this.modules == null) {
            this.modules = new ArrayList<>();
        }
        this.modules.add(module);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(String conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<String> destinations) {
        this.destinations = destinations;
    }

    public void addDestination(String destination) {
        if(this.destinations == null) {
            this.destinations = new ArrayList<>();
        }
        this.destinations.add(destination);
    }
    
    
    public static AtualizacaoVersaoEmailNotificationDTO fromBean(AtualizacaoVersaoBean bean, List<String> destination, String workflowLink, String bucketURL) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        AtualizacaoVersaoEmailNotificationDTO dto = new AtualizacaoVersaoEmailNotificationDTO();
        dto.setConclusion(bean.getConclusion());
        dto.setConclusionDate(sdf.format(bean.getConclusionDate().getTime()));
        dto.setRequestDate(sdf.format(bean.getRequestDate().getTime()));
        dto.setDestinations(destination);
        dto.setLink(workflowLink);
        dto.setAuthor(bean.getUser().getName());

        if (dto.getConclusion() == GithubActionConclusionEnum.success || dto.getConclusion() == GithubActionConclusionEnum.warning) {
            dto.setSubject(String.format("Atualização Versão%s [%s.%s]", dto.conclusion == GithubActionConclusionEnum.warning ? " (COM ALERTAS)" : "", bean.getEnvironment().getIdentifier().toUpperCase(), bean.getTag()));
            dto.setTitle(String.format("Nova versão atualizada%s no ambiente %s - %s. Cliente %s", dto.conclusion == GithubActionConclusionEnum.warning ? " (COM ALERTAS)" : "", bean.getEnvironment().getDescription(), bean.getTag(), bean.getEnvironment().getClient().getDescription()));
            for (AtualizacaoVersaoModuloBean module : bean.getModules()) {
                if (module.isEnabled()) {
                    ModulesDTO m = new ModulesDTO();
                    m.setName(module.getEnvironmentModule().getProjectModule().getTitle());
                    m.setVersion(module.getTag());
                    dto.addModule(m);
                }
            }
        } else {
            dto.setSubject("Falha atualização do ambiente " + bean.getEnvironment().getIdentifier());
            dto.setTitle(String.format("Falha na atualização da versão %s do ambiente %s.", bean.getTag(), bean.getEnvironment().getDescription()));
        }
        
        return dto;
    }

    public static class ModulesDTO {
        private String name;
        private String version;
        private String downloadLink;
        private String message;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDownloadLink() {
            return downloadLink;
        }

        public void setDownloadLink(String downloadLink) {
            this.downloadLink = downloadLink;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
