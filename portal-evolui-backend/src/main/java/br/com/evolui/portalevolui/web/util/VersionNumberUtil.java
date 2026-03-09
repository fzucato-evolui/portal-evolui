package br.com.evolui.portalevolui.web.util;

public class VersionNumberUtil {
    private Integer major;
    private Integer minor;
    private Integer patch;
    private String build;
    public VersionNumberUtil (String version) {
        String[] vs = version.split("\\.");
        for (int i = 0; i < vs.length; i++) {
            if (i == 0) {
                this.setMajor(Integer.parseInt(vs[i]));
            } else if (i == 1) {
                this.setMinor(Integer.parseInt(vs[i]));
            } else if (i == 2) {
                this.setPatch(Integer.parseInt(vs[i]));
            } else if (i == 3) {
                this.setBuild(vs[i]);
            }
        }
    }

    public VersionNumberUtil (Integer major, Integer minor, Integer patch, String build) {
        this.setMajor(major);
        this.setMinor(minor);
        this.setPatch(patch);
        this.setBuild(build);
    }

    public String getBranch() {
        return String.format("%s.%s.%s", this.getMajor(), this.getMinor(), this.getPatch());
    }

    public String getCompleteVersion() {
        return String.format("%s.%s.%s.%s", this.getMajor(), this.getMinor(), this.getPatch(), this.getBuild());
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

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }
}
