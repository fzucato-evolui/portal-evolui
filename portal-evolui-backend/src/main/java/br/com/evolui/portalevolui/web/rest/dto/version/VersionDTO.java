package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Admin.class)
public class VersionDTO {
    private String version;
    private String commit;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    @JsonIgnore
    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return  null;
        }
    }

    @JsonIgnore
    public static VersionDTO fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, VersionDTO.class);
        } catch (Exception e) {
            return  null;
        }
    }
}
