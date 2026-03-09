package br.com.evolui.portalevolui.web.rest.dto.monday;

import java.util.List;

public class MondayItemsPageDTO {
    private List<MondayItemPageDTO> items;

    public List<MondayItemPageDTO> getItems() {
        return items;
    }

    public void setItems(List<MondayItemPageDTO> items) {
        this.items = items;
    }
}
