package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.converter.StringListConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"identifier", "project_fk"}, name = "ux_cliente")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonView(JsonViewerPattern.Admin.class)
public class ClienteBean implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="client_sequence_gen")
    @SequenceGenerator(name="client_sequence_gen", sequenceName="client_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "identifier", nullable = false, length = 50)
    private String identifier;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(name="keywords")
    private List<String> keywords;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id", "identifier", "title", "main", "framework"})
    private ProjectBean project;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void addKeywords(String keyword) {
        if (this.keywords == null) {
            this.keywords = new ArrayList<>();
        }
        this.keywords.add(keyword);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectBean getProject() {
        return project;
    }

    public void setProject(ProjectBean project) {
        this.project = project;
    }
}
