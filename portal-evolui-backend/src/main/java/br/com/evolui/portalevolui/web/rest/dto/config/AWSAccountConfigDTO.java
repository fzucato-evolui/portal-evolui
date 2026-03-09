package br.com.evolui.portalevolui.web.rest.dto.config;

public class AWSAccountConfigDTO {
    private Boolean enabled;
    private Boolean main;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketVersions;
    private String bucketTempDump;
    private String bucketLocalMountPath;
    private AWSRunnerConfigDTO runnerVersions;
    private AWSRunnerConfigDTO runnerTests;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getMain() {
        return main;
    }

    public void setMain(Boolean main) {
        this.main = main;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketVersions() {
        return bucketVersions;
    }

    public void setBucketVersions(String bucketVersions) {
        this.bucketVersions = bucketVersions;
    }

    public AWSRunnerConfigDTO getRunnerVersions() {
        return runnerVersions;
    }

    public void setRunnerVersions(AWSRunnerConfigDTO runnerVersions) {
        this.runnerVersions = runnerVersions;
    }

    public AWSRunnerConfigDTO getRunnerTests() {
        return runnerTests;
    }

    public void setRunnerTests(AWSRunnerConfigDTO runnerTests) {
        this.runnerTests = runnerTests;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucketTempDump() {
        return bucketTempDump;
    }

    public void setBucketTempDump(String bucketTempDump) {
        this.bucketTempDump = bucketTempDump;
    }

    public String getBucketLocalMountPath() {
        return bucketLocalMountPath;
    }

    public void setBucketLocalMountPath(String bucketLocalMountPath) {
        this.bucketLocalMountPath = bucketLocalMountPath;
    }
}
