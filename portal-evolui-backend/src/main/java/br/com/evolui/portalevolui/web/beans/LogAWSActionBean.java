package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.enums.AWSActionTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.Calendar;

@Entity
@Table(name = "log_aws_action")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LogAWSActionBean implements Serializable {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="log_aws_action_sequence_gen")
    @SequenceGenerator(name="log_aws_action_sequence_gen", sequenceName="log_aws_action_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "log_date", nullable = false)
    private Calendar logDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserBean user;

    @Column(name = "action_type", nullable = false)
    private AWSActionTypeEnum actionType;

    @Column(name = "instance_id", nullable = false, length = 2000)
    private String instance;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Calendar getLogDate() {
        return logDate;
    }

    public void setLogDate(Calendar logDate) {
        this.logDate = logDate;
    }

    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
    }

    public AWSActionTypeEnum getActionType() {
        return actionType;
    }

    public void setActionType(AWSActionTypeEnum actionType) {
        this.actionType = actionType;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }
}
