package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.hibernate.internal.util.StringHelper;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MappedSuperclass
@JsonView(JsonViewerPattern.Admin.class)
public class VersaoBranchBaseBean implements Serializable {
    @JsonIgnore
    @Column(name = "major", nullable = false)
    protected Integer major;

    @JsonIgnore
    @Column(name = "minor", nullable = false)
    protected Integer minor;

    @JsonIgnore
    @Column(name = "patch", nullable = false)
    protected Integer patch;

    @Column(name = "branch", nullable = false, length = 50)
    protected String branch;

    @Transient
    protected final Pattern pattern = Pattern.compile("-.*$");

    public VersaoBranchBaseBean() {

    }
    public VersaoBranchBaseBean(String version) {
        this.parseVersion(version);
    }

    public VersaoBranchBaseBean(Integer major, Integer minor, Integer patch) {
        this.setMajor(major);
        this.setMinor(minor);
        this.setPatch(patch);
        this.getBranch();
    }

    public String getBranch() {
        if (StringHelper.isEmpty(this.branch)) {
            if (this.getMajor().equals(99) && this.getMinor().equals(99) && this.getPatch().equals(99)) {
                this.branch = "master";
            } else {
                this.branch = String.format("%s.%s.%s",
                        this.getMajor(),
                        this.getMinor(),
                        this.getPatch());
            }
        }
        return this.branch;
    }

    public void setBranch(String branch) {
        this.parseVersion(branch);
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public Integer getPatch() {
        return patch;
    }

    public void setPatch(Integer patch) {
        this.patch = patch;
    }

 
    protected String extractQualifier(String version) {
        Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            return matcher.group().substring(1);
        }
        return null;
    }

    protected String extractWithoutQualifier(String version) {
        Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            return version.substring(0, matcher.start());

        }
        return version;
    }

    private void parseVersion(String version) {
        if (version.equals("master")) {
            version = "99.99.99";
        }
        String withoutQualifier = extractWithoutQualifier(version);
        String[] vs = withoutQualifier.split("\\.");
        for (int i = 0; i < vs.length; i++) {
            if (i == 0) {
                this.setMajor(Integer.parseInt(vs[i]));
            } else if (i == 1) {
                this.setMinor(Integer.parseInt(vs[i]));
            } else if (i == 2) {
                this.setPatch(Integer.parseInt(vs[i]));
            }
        }
        this.branch = null;
        this.getBranch();
    }
}
