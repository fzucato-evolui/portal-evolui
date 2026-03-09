package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.GeracaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class GeracaoVersaoEmailNotificationDTO {
    private String title;
    private String link;
    private boolean success;
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
    
    
    public static GeracaoVersaoEmailNotificationDTO fromBean(GeracaoVersaoBean bean, List<String> destination, String workflowLink, String bucketURL) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        GeracaoVersaoEmailNotificationDTO dto = new GeracaoVersaoEmailNotificationDTO();
        dto.setSuccess(bean.getConclusion() == GithubActionConclusionEnum.success);
        dto.setConclusionDate(sdf.format(bean.getConclusionDate().getTime()));
        dto.setRequestDate(sdf.format(bean.getRequestDate().getTime()));
        dto.setDestinations(destination);
        dto.setLink(workflowLink);
        dto.setAuthor(bean.getUser().getName());
        boolean isModular = bean.getModules().stream().noneMatch(x -> x.getProjectModule().isMain() && x.isEnabled());
        if (dto.isSuccess()) {
            if (!isModular) {
                dto.setSubject(String.format("[%s.%s]", bean.getProject().getIdentifier().toUpperCase(), bean.getTag()));
                dto.setTitle(String.format("Nova versão do %s - %s (%s)", bean.getProject().getTitle(), bean.getTag(), bean.getCompileType().name()));
            }
            else {
                dto.setSubject(String.format("[%s.%s.%s]", "MODULOS", bean.getProject().getIdentifier().toUpperCase(), bean.getTag()));
                dto.setTitle(String.format("Nova versão dos módulos do %s - %s (%s)", bean.getProject().getTitle(), bean.getTag(), bean.getCompileType().name()));
            }
            for (GeracaoVersaoModuloBean module : bean.getModules()) {
                if (module.isEnabled()) {
                    ModulesDTO m = new ModulesDTO();
                    m.setName(module.getProjectModule().getTitle());
                    m.setVersion(module.getTag());
                    dto.addModule(m);
                }
            }
        } else {
            if (!isModular) {
                dto.setSubject("Falha Versão " + bean.getProject().getTitle());
                dto.setTitle("Falha na geração de versão do " + bean.getProject().getTitle());
            }
            else {
                dto.setSubject("Falha Versão Módulos " + bean.getProject().getTitle());
                dto.setTitle("Falha na geração de versão dos módulos do " + bean.getProject().getTitle());
            }
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
