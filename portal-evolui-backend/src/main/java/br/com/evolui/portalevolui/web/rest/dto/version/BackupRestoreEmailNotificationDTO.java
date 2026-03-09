package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.beans.enums.ActionRDSTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class BackupRestoreEmailNotificationDTO {
    private String title;
    private GithubActionConclusionEnum conclusion;
    private String author;
    private String requestDate;
    private String conclusionDate;
    private String subject;
    private List<String> destinations;
    private String rds;
    private String dump;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
    
    public static BackupRestoreEmailNotificationDTO fromBean(ActionRDSBean bean, List<String> destination) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        BackupRestoreEmailNotificationDTO dto = new BackupRestoreEmailNotificationDTO();
        dto.setConclusion(bean.getConclusion());
        dto.setConclusionDate(sdf.format(bean.getConclusionDate().getTime()));
        dto.setRequestDate(sdf.format(bean.getRequestDate().getTime()));
        dto.setDestinations(destination);
        dto.setAuthor(bean.getUser().getName());
        if (dto.getConclusion() == GithubActionConclusionEnum.success) {
            dto.setSubject(String.format("%s de Banco de Dados", bean.getActionType().value()));
            dto.setTitle(String.format("Novo %s de Banco de Dados", bean.getActionType().value().toLowerCase()));
            dto.setRds(String.format("%s/%s", bean.getRds().getEndpoint(), bean.getActionType() == ActionRDSTypeEnum.BACKUP ? bean.getSourceDatabase() : bean.getDestinationDatabase()));
            dto.setDump(String.format("Conta %s - %s", bean.getDumpFile().getAccount(), bean.getDumpFile().getPath()));
        }
        else if (dto.getConclusion() == GithubActionConclusionEnum.warning) {
            dto.setSubject(String.format("%s de Banco de Dados COM ALERTAS", bean.getActionType().value()));
            dto.setTitle(String.format("Novo %s de Banco de Dados COM ALERTAS", bean.getActionType().value().toLowerCase()));
            dto.setRds(String.format("%s/%s", bean.getRds().getEndpoint(), bean.getActionType() == ActionRDSTypeEnum.BACKUP ? bean.getSourceDatabase() : bean.getDestinationDatabase()));
            dto.setDump(String.format("Conta %s - %s", bean.getDumpFile().getAccount(), bean.getDumpFile().getPath()));
        }
        else {
            dto.setSubject(String.format("Falha no %s de Banco de Dados", bean.getActionType().value()));
            dto.setRds(String.format("%s/%s", bean.getRds().getEndpoint(), bean.getActionType() == ActionRDSTypeEnum.BACKUP ? bean.getSourceDatabase() : bean.getDestinationDatabase()));
            dto.setDump(String.format("Conta %s - %s", bean.getDumpFile().getAccount(), bean.getDumpFile().getPath()));
        }
        
        return dto;
    }

    public GithubActionConclusionEnum getConclusion() {
        return conclusion;
    }

    public void setConclusion(GithubActionConclusionEnum conclusion) {
        this.conclusion = conclusion;
    }

    public String getRds() {
        return rds;
    }

    public void setRds(String rds) {
        this.rds = rds;
    }

    public String getDump() {
        return dump;
    }

    public void setDump(String dump) {
        this.dump = dump;
    }
}
