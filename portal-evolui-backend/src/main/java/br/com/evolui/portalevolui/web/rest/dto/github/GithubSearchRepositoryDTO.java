package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.List;

public class GithubSearchRepositoryDTO {
    private Long total_count;
    private boolean incomplete_results;
    private List<GithubDetailedRepositoryDTO> items;

    public Long getTotal_count() {
        return total_count;
    }

    public void setTotal_count(Long total_count) {
        this.total_count = total_count;
    }

    public boolean isIncomplete_results() {
        return incomplete_results;
    }

    public void setIncomplete_results(boolean incomplete_results) {
        this.incomplete_results = incomplete_results;
    }

    public List<GithubDetailedRepositoryDTO> getItems() {
        return items;
    }

    public void setItems(List<GithubDetailedRepositoryDTO> items) {
        this.items = items;
    }
}
