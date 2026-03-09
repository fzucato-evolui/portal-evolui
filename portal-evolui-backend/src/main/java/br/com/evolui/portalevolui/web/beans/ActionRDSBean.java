package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.dto.AmbienteFileMapConfigDTO;
import br.com.evolui.portalevolui.web.beans.enums.ActionRDSRemapTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.ActionRDSTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.rest.dto.aws.BucketDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.*;

@Entity
@Table(name = "action_rds")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ActionRDSBean {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="action_rds_sequence_gen")
    @SequenceGenerator(name="action_rds_sequence_gen", sequenceName="action_rds_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "action_type", nullable = false)
    private ActionRDSTypeEnum actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserBean user;

    @Column(name = "scheduler_date", nullable = true)
    private Calendar schedulerDate;

    @Column(name = "request_date", nullable = false)
    private Calendar requestDate;

    @Column(name = "conclusion_date", nullable = true)
    private Calendar conclusionDate;

    @Column(name = "status", nullable = false)
    private GithubActionStatusEnum status;

    @Column(name = "conclusion", nullable = true)
    private GithubActionConclusionEnum conclusion;

    @Column(name = "restore_key", length = 2000)
    private String restoreKey;

    @Column(name="error")
    @Lob
    @JsonIgnore
    private String error;

    @Column(name = "dump_file", nullable = false, length = 4000)
    private String dumpFile;

    @Column(name = "destination_database")
    private String destinationDatabase;

    @Column(name = "destination_password")
    private String destinationPassword;

    @Column(name = "source_database")
    private String sourceDatabase;

    @Column(name = "rds", nullable = false, length = 4000)
    private String rds;

    @Column(name = "exclude_blobs")
    private Boolean excludeBlobs;

    @Column(name="remaps")
    @Lob
    private String remaps;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
    }

    public Calendar getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Calendar requestDate) {
        this.requestDate = requestDate;
    }

    public Calendar getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(Calendar conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public GithubActionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GithubActionStatusEnum status) {
        this.status = status;
    }

    public GithubActionConclusionEnum getConclusion() {
        return conclusion;
    }

    public void setConclusion(GithubActionConclusionEnum conclusion) {
        this.conclusion = conclusion;
    }

    public String getRestoreKey() {
        return restoreKey;
    }

    public void setRestoreKey(String restoreKey) {
        this.restoreKey = restoreKey;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public BucketDTO getDumpFile() {
        if (dumpFile != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return new ObjectMapper().readValue(dumpFile, BucketDTO.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public void setDumpFile(BucketDTO dumpFile) {
        if (dumpFile != null) {
            try {
                this.dumpFile = new ObjectMapper().writeValueAsString(dumpFile);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getDestinationDatabase() {
        return destinationDatabase;
    }

    public void setDestinationDatabase(String destinationDatabase) {
        this.destinationDatabase = destinationDatabase;
    }

    public String getSourceDatabase() {
        return sourceDatabase;
    }

    public void setSourceDatabase(String sourceDatabase) {
        this.sourceDatabase = sourceDatabase;
    }

    public RDSDTO getRds() {
        if (rds != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return new ObjectMapper().readValue(rds, RDSDTO.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public void setRds(RDSDTO rds) {
        if (rds != null) {
            try {
                this.rds = new ObjectMapper().writeValueAsString(rds);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ActionRDSTypeEnum getActionType() {
        return actionType;
    }

    public void setActionType(ActionRDSTypeEnum actionType) {
        this.actionType = actionType;
    }

    public Boolean getExcludeBlobs() {
        if (excludeBlobs == null) {
            excludeBlobs = false;
        }
        return excludeBlobs;
    }

    public void setExcludeBlobs(Boolean excludeBlobs) {
        this.excludeBlobs = excludeBlobs;
    }

    public Calendar getSchedulerDate() {
        return schedulerDate;
    }

    public void setSchedulerDate(Calendar schedulerDate) {
        this.schedulerDate = schedulerDate;
    }

    public LinkedHashMap<ActionRDSRemapTypeEnum, List<AmbienteFileMapConfigDTO>> getRemaps() {
        if (remaps != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                try {
                    // Primeiro tenta deserializar com o novo formato (List)
                    return mapper.readValue(remaps, new TypeReference<LinkedHashMap<ActionRDSRemapTypeEnum, List<AmbienteFileMapConfigDTO>>>() {});
                } catch (JsonProcessingException e) {
                    // Se falhar, tenta o formato antigo e converte para o novo
                    LinkedHashMap<ActionRDSRemapTypeEnum, AmbienteFileMapConfigDTO> oldFormat = 
                        mapper.readValue(remaps, new TypeReference<LinkedHashMap<ActionRDSRemapTypeEnum, AmbienteFileMapConfigDTO>>() {});
                    
                    // Converte formato antigo para o novo
                    LinkedHashMap<ActionRDSRemapTypeEnum, List<AmbienteFileMapConfigDTO>> newFormat = new LinkedHashMap<>();
                    for (Map.Entry<ActionRDSRemapTypeEnum, AmbienteFileMapConfigDTO> entry : oldFormat.entrySet()) {
                        List<AmbienteFileMapConfigDTO> list = new ArrayList<>();
                        if (entry.getValue() != null) {
                            list.add(entry.getValue());
                        }
                        newFormat.put(entry.getKey(), list);
                    }
                    return newFormat;
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return new LinkedHashMap<>();
    }

    public void setRemaps(LinkedHashMap<ActionRDSRemapTypeEnum, List<AmbienteFileMapConfigDTO>> remaps) {
        if (remaps != null) {
            try {
                this.remaps = new ObjectMapper().writeValueAsString(remaps);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getDestinationPassword() {
        return destinationPassword;
    }

    public void setDestinationPassword(String destinationPassword) {
        this.destinationPassword = destinationPassword;
    }
}
