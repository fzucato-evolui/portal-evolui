package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;

import java.util.Calendar;

public class GeracaoVersaoMondayNotificationDTO {
    private String mondayId;
    private String authorEmail;
    private Calendar conclusionDate;
    private String product;
    private String tag;

    public String getMondayId() {
        return mondayId;
    }

    public void setMondayId(String mondayId) {
        this.mondayId = mondayId;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public Calendar getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(Calendar conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public static GeracaoVersaoMondayNotificationDTO fromBean(GeracaoVersaoBean bean) {
        GeracaoVersaoMondayNotificationDTO dto = new GeracaoVersaoMondayNotificationDTO();
        dto.setMondayId(bean.getMondayId());
        dto.setConclusionDate(bean.getConclusionDate());
        dto.setAuthorEmail(bean.getUser().getEmail());
        dto.setProduct(bean.getProject().getIdentifier());
        dto.setTag(bean.getTag());
        return dto;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
