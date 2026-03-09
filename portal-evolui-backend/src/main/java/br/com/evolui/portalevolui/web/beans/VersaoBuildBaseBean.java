package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.internal.util.StringHelper;

import java.io.Serializable;
import java.text.SimpleDateFormat;

@MappedSuperclass
@JsonView(JsonViewerPattern.Admin.class)
public class VersaoBuildBaseBean extends VersaoBranchBaseBean implements Serializable, Comparable<VersaoBuildBaseBean> {
    @JsonIgnore
    @Column(name = "build", nullable = false, length = 50)
    protected String build;

    @Column(name = "tag", nullable = false, length = 50)
    protected String tag;

    @Column(name = "hash_commit", length = 100)
    private String commit;

    @Column(name = "repository")
    private String repository;

    @Column(name = "relative_path", length = 2000)
    private String relativePath;

    @JsonIgnore
    @Column(name = "qualifier", nullable = true, length = 50)
    protected String qualifier;

    @Transient
    protected boolean abnormalBranch;

    @JsonIgnore
    @Transient
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

    @JsonIgnore
    @Transient
    private boolean betaQualifier;

    public VersaoBuildBaseBean() {

    }
    public VersaoBuildBaseBean(String version) {
        this.parseVersion(version);
    }

    public VersaoBuildBaseBean(Integer major, Integer minor, Integer patch, String build) {
        this.setMajor(major);
        this.setMinor(minor);
        this.setPatch(patch);
        this.setBuild(build);
        this.getTag();
        this.getBranch();
    }

    public VersaoBuildBaseBean(Integer major, Integer minor, Integer patch, String build, String qualifier) {
        this.setMajor(major);
        this.setMinor(minor);
        this.setPatch(patch);
        this.setBuild(build);
        this.setQualifier(qualifier);
        this.getTag();
        this.getBranch();
    }

    public String getTag() {
        if (StringHelper.isEmpty(this.tag)) {
            if (this.getMajor().equals(99) && this.getMinor().equals(99) && this.getPatch().equals(99) && this.getBuild().equals("999999")) {
                this.tag = "master";
            } else if (this.isAbnormalBranch()) {
                // Para branches anormais, o tag será o próprio nome da branch
                this.tag = this.getBranch();
            } else {
                this.tag = String.format("%s.%s.%s.%s%s",
                        this.getMajor(),
                        this.getMinor(),
                        this.getPatch(),
                        this.getBuild(),
                        StringHelper.isEmpty(this.getQualifier()) ? "" : "-" + this.getQualifier());
            }
        }
        return this.tag;
    }

    public void setTag(String tag) {
        this.parseVersion(tag);
    }

    public void setPureBranch(String branch) {
        this.branch = branch;
    }

    public String getBuild() {
        if (this.isValidDate(build)) {
            return build;
        }
        if (StringUtils.isNumeric(build)) {
            return Long.parseLong(build) + "";
        }
        return build;
    }

    @JsonIgnore
    public String getComparableBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
        this.tag = null;
        this.getTag();
        this.getBranch();
    }

    private void parseVersion(String version) {
        // Verificar se é uma versão especial "master"
        if (version.equals("master")) {
            version = "99.99.99.999999";
        }

        // Verificar se é uma versão que não segue o padrão numérico
        if (!version.matches("^\\d+(\\.\\d+)*(\\.\\w+)?$") && !version.contains("-")) {
            // Versão não segue o padrão - é uma branch anormal
            this.setMajor(0);
            this.setMinor(0);
            this.setPatch(0);
            this.setBuild("000000");
            this.qualifier = "";
            this.abnormalBranch = true;
            this.branch = version; // Usar a string original como nome da branch
            this.tag = version;    // E também como tag
            return;                // Sair do método, pois já configuramos tudo
        }

        // Processamento normal para versões no formato padrão
        this.abnormalBranch = false;
        this.qualifier = this.extractQualifier(version);
        if (!StringHelper.isEmpty(this.qualifier)) {
            version = this.extractWithoutQualifier(version);
        }

        String[] vs = version.split("\\.");
        for (int i = 0; i < vs.length; i++) {
            if (i == 0) {
                this.setMajor(Integer.parseInt(vs[i]));
            } else if (i == 1) {
                this.setMinor(Integer.parseInt(vs[i]));
            } else if (i == 2) {
                this.setPatch(Integer.parseInt(vs[i]));
            } else if (i == 3) {
                if (this.isValidDate(vs[i])) {
                    this.setBuild(vs[i]);
                } else if (StringUtils.isNumeric(vs[i])) {
                    this.setBuild(StringUtils.leftPad(vs[i], 6, '0'));
                } else {
                    this.setBuild(vs[i]);
                }
            }
        }
        this.tag = null;
        this.branch = null;
        this.getTag();
        this.getBranch();
    }

    @Override
    public int compareTo(VersaoBuildBaseBean o) {
        // Se uma das versões é abnormal e a outra não, a versão normal vem primeiro
        if (this.isAbnormalBranch() && !o.isAbnormalBranch()) {
            return -1;
        } else if (!this.isAbnormalBranch() && o.isAbnormalBranch()) {
            return 1;
        }

        // Se ambas são abnormais, compara strings diretamente
        if (this.isAbnormalBranch() && o.isAbnormalBranch()) {
            return this.getBranch().compareTo(o.getBranch());
        }

        // Comparação normal por componentes numéricos
        int ret = this.getMajor().compareTo(o.getMajor());
        if (ret != 0) {
            return ret;
        }
        ret = this.getMinor().compareTo(o.getMinor());
        if (ret != 0) {
            return ret;
        }
        ret = this.getPatch().compareTo(o.getPatch());
        if (ret != 0) {
            return ret;
        }
        if (this.isBetaQualifier() && !o.isBetaQualifier()) {
            return -1;
        }
        else if (!this.isBetaQualifier() && o.isBetaQualifier()) {
            return 1;
        }
        if (!StringHelper.isEmpty(this.getComparableBuild()) && !StringHelper.isEmpty(o.getComparableBuild())) {
            ret = this.getComparableBuild().compareTo(o.getComparableBuild());
            if (ret != 0) {
                return ret;
            }
        } else if (!StringHelper.isEmpty(this.getComparableBuild())) {
            return 1;
        } else if (!StringHelper.isEmpty(o.getComparableBuild())) {
            return -1;
        }
        if (!StringHelper.isEmpty(this.getQualifier()) && !StringHelper.isEmpty(o.getQualifier())) {
            return this.qualifier.compareTo(o.qualifier);
        } else if (!StringHelper.isEmpty(this.getQualifier())) {
            return 1;
        } else if (!StringHelper.isEmpty(o.getQualifier())) {
            return -1;
        }
        return 0;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    private boolean isValidDate(String value) {
        try {
            this.dateFormatter.parse(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public boolean isBetaQualifier() {
        if (!StringHelper.isEmpty(this.qualifier)) {
            return this.qualifier.equals("BETA");
        }
        return false;
    }

    public boolean isAbnormalBranch() {
        return abnormalBranch;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public SimpleDateFormat getDateFormatter() {
        return dateFormatter;
    }
}
