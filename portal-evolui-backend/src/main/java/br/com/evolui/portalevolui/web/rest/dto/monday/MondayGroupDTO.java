package br.com.evolui.portalevolui.web.rest.dto.monday;

public class MondayGroupDTO {
    private String title;
    private String id;
    private MondayItemsPageDTO items_page;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MondayItemsPageDTO getItems_page() {
        return items_page;
    }

    public void setItems_page(MondayItemsPageDTO items_page) {
        this.items_page = items_page;
    }
}
