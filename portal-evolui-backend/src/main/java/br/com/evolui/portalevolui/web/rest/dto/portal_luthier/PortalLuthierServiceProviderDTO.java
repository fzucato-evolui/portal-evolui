package br.com.evolui.portalevolui.web.rest.dto.portal_luthier;

public class PortalLuthierServiceProviderDTO {
    private Long id;
    private String name;
    private String classname;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }
}
