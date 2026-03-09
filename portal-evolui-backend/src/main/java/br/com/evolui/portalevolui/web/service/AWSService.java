package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.listener.ProgressStatusListener;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.aws.BucketDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.EC2DTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.WorkspaceDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSAccountConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.enums.BucketFileTypeEnum;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigService;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import org.hibernate.internal.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.workspaces.WorkSpacesClient;
import software.amazon.awssdk.services.workspaces.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class AWSService implements ISystemConfigService {
    @Autowired
    private SystemConfigRepository configRepository;

    private static final ThreadLocal<Map.Entry<String, AWSConfigDTO>> threadLocalContext = new ThreadLocal<>();

    public List<EC2DTO> listEc2() {
        Ec2Client client = this.getEc2Client();
        String nextToken = null;
        try {
            List<EC2DTO> dtos = new ArrayList<>();
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(30).nextToken(nextToken).build();
                DescribeInstancesResponse response = client.describeInstances(request);
                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        Filter filter = Filter.builder()
                                .name("resource-id")
                                .values(instance.instanceId())
                                .build();
                        DescribeTagsResponse tags = client.describeTags(DescribeTagsRequest.builder().filters(filter).build());
                        EC2DTO dto = new EC2DTO();
                        dto.setId(instance.instanceId());
                        if (tags != null && tags.hasTags()) {
                            TagDescription tag = tags.tags().stream().filter(x -> x.key().equals("Name")).findFirst().orElse(null);
                            if (tag != null) {
                                dto.setName(tag.value());
                            }
                        }
                        dto.setInstanceState(instance.state().nameAsString());
                        dto.setInstanceType(instance.instanceType().name());
                        dto.setOs(instance.platformDetails());
                        dto.setPublicIp(instance.publicIpAddress());
                        dto.setPrivateIp(instance.privateIpAddress());
                        dto.setAccount(this.getAccount());
                        dtos.add(dto);

                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);
            return dtos;
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public EC2DTO getEc2(String id) {
        Ec2Client client = this.getEc2Client();
        String nextToken = null;
        try {
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().instanceIds(id).nextToken(nextToken).build();
                DescribeInstancesResponse response = client.describeInstances(request);
                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        Filter filter = Filter.builder()
                                .name("resource-id")
                                .values(instance.instanceId())
                                .build();
                        DescribeTagsResponse tags = client.describeTags(DescribeTagsRequest.builder().filters(filter).build());
                        EC2DTO dto = new EC2DTO();
                        dto.setId(instance.instanceId());
                        if (tags != null && tags.hasTags()) {
                            TagDescription tag = tags.tags().stream().filter(x -> x.key().equals("Name")).findFirst().orElse(null);
                            if (tag != null) {
                                dto.setName(tag.value());
                            }
                        }
                        dto.setInstanceState(instance.state().nameAsString());
                        dto.setInstanceType(instance.instanceType().name());
                        dto.setOs(instance.platformDetails());
                        dto.setPublicIp(instance.publicIpAddress());
                        dto.setPrivateIp(instance.privateIpAddress());
                        dto.setAccount(this.getAccount());
                        return dto;

                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);
            return null;
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void startEc2(String id) {
        Ec2Client client = this.getEc2Client();
        try {
            StartInstancesRequest request = StartInstancesRequest.builder()
                    .instanceIds(id)
                    .build();

            client.startInstances(request);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void stopEc2(String id) {
        Ec2Client client = this.getEc2Client();
        try {
            StopInstancesRequest request = StopInstancesRequest.builder()
                    .instanceIds(id)
                    .build();

            client.stopInstances(request);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void rebootEc2(String id) {
        Ec2Client client = this.getEc2Client();
        try {
            RebootInstancesRequest request = RebootInstancesRequest.builder()
                    .instanceIds(id)
                    .build();

            client.rebootInstances(request);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public List<RDSDTO> listRds() {
        RdsClient client = this.getRdsClient();
        Ec2Client clientEC2 = this.getEc2Client();
        String nextToken = null;
        try {
            List<RDSDTO> dtos = new ArrayList<>();
            DescribeDbInstancesResponse response = client.describeDBInstances();
            List<NetworkInterface> networkInterfaces = clientEC2.describeNetworkInterfaces().networkInterfaces();
            for (DBInstance instance : response.dbInstances()) {

                RDSDTO dto = new RDSDTO();
                dto.setId(instance.dbInstanceIdentifier());
                dto.setInstanceState(instance.dbInstanceStatus());
                dto.setInstanceType(instance.dbInstanceClass());
                dto.setPort(instance.endpoint().port());
                dto.setDbName(instance.dbName());
                dto.setEngine(instance.engine());
                dto.setEndpoint(instance.endpoint().address());
                dto.setAccount(this.getAccount());
                dto.setArn(instance.dbInstanceArn());

                String securityGroupId = instance.vpcSecurityGroups().get(0).vpcSecurityGroupId();
                String zone = instance.availabilityZone();
                String vpcId = instance.dbSubnetGroup().vpcId();
                NetworkInterface networkInterface = networkInterfaces.stream().filter(x ->
                                x.vpcId().equals(vpcId) &&
                                        x.availabilityZone().equals(zone) &&
                                        (x.groups().stream().filter(y-> y.groupId().equals(securityGroupId)).findFirst().orElse(null)) != null)
                        .findFirst().orElse(null);
                if (networkInterface != null) {
                    dto.setPrivateDns(networkInterface.privateDnsName());
                    dto.setPrivateIpAddress(networkInterface.privateIpAddress());
                    if (networkInterface.association() != null) {
                        NetworkInterfaceAssociation association = networkInterface.association();
                        dto.setPublicIpAddress(association.publicIp());
                        dto.setPublicDns(association.publicDnsName());
                    }
                }
                dtos.add(dto);

            }
            return dtos;
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public RDSDTO getRDS(String id) {
        RdsClient client = this.getRdsClient();
        Ec2Client clientEC2 = this.getEc2Client();
        DescribeDbInstancesRequest describeDbInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(id).build();
        List<DBInstance> instances = client.describeDBInstances(describeDbInstancesRequest).dbInstances();
        for(DBInstance instance : instances) {
            RDSDTO dto = new RDSDTO();
            dto.setId(instance.dbInstanceIdentifier());
            dto.setInstanceState(instance.dbInstanceStatus());
            dto.setInstanceType(instance.dbInstanceClass());
            dto.setPort(instance.endpoint().port());
            dto.setDbName(instance.dbName());
            dto.setEngine(instance.engine());
            dto.setEndpoint(instance.endpoint().address());
            dto.setArn(instance.dbInstanceArn());
            String securityGroupId = instance.vpcSecurityGroups().get(0).vpcSecurityGroupId();
            String zone = instance.availabilityZone();
            String vpcId = instance.dbSubnetGroup().vpcId();

            Filter filterGroupId = Filter.builder()
                    .name("group-id")
                    .values(securityGroupId)
                    .build();
            Filter filterZone = Filter.builder()
                    .name("availability-zone")
                    .values(zone)
                    .build();
            Filter filterVPCId = Filter.builder()
                    .name("vpc-id")
                    .values(vpcId)
                    .build();
            DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest = DescribeNetworkInterfacesRequest.builder().filters(filterGroupId, filterZone, filterVPCId).build();
            List<NetworkInterface> networkInterfaces = clientEC2.describeNetworkInterfaces(describeNetworkInterfacesRequest).networkInterfaces();
            if (networkInterfaces != null && networkInterfaces.size() > 0) {
                NetworkInterface networkInterface = networkInterfaces.get(0);
                dto.setPrivateDns(networkInterface.privateDnsName());
                dto.setPrivateIpAddress(networkInterface.privateIpAddress());
                if (networkInterface.association() != null) {
                    NetworkInterfaceAssociation association = networkInterface.association();
                    dto.setPublicIpAddress(association.publicIp());
                    dto.setPublicDns(association.publicDnsName());
                }
            }
            dto.setAccount(this.getAccount());
            return dto;
        }
        return null;
    }

    public void startRds(String id) {
        RdsClient client = this.getRdsClient();
        try {
            StartDbInstanceRequest startDbInstanceRequest = StartDbInstanceRequest.builder()
                    .dbInstanceIdentifier(id)
                    .build();

            client.startDBInstance(startDbInstanceRequest);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void stoptRds(String id) {
        RdsClient client = this.getRdsClient();
        try {
            StopDbInstanceRequest startDbInstanceRequest = StopDbInstanceRequest.builder()
                    .dbInstanceIdentifier(id)
                    .build();

            client.stopDBInstance(startDbInstanceRequest);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public List<WorkspaceDTO> listWorkspaces() {
        WorkSpacesClient client = this.getWorkspaceClient();
        Ec2Client clientEC2 = this.getEc2Client();
        List<WorkspaceDTO> dtos = new ArrayList<>();
        List<Workspace> wks = new ArrayList<>();
        //List<NetworkInterface> networkInterfaces = clientEC2.describeNetworkInterfaces().networkInterfaces();
        String nextToken = null;
        do {
            DescribeWorkspacesRequest request = DescribeWorkspacesRequest.builder()
                    .nextToken(nextToken)
                    .build();

            DescribeWorkspacesResponse response = client.describeWorkspaces(request);

            wks.addAll(response.workspaces());

            nextToken = response.nextToken();
        } while (nextToken != null);
        for(Workspace w : wks) {
            WorkspaceDTO dto = new WorkspaceDTO();
            dto.setId(w.workspaceId());
            dto.setComputerName(w.computerName());
            dto.setUserName(w.userName());
            dto.setState(w.stateAsString());
            dto.setPrivateIpAddress(w.ipAddress());
            WorkspaceProperties prop = w.workspaceProperties();
            dto.setRootVolumeSizeGib(prop.rootVolumeSizeGib());
            dto.setUserVolumeSizeGib(prop.userVolumeSizeGib());
            dto.setRunningMode(prop.runningMode());
            dto.setOs(prop.operatingSystemNameAsString());
            dto.setPlatform(prop.computeTypeNameAsString());
            dto.setProtocol(prop.protocolsAsStrings()
                    .stream()
                    .findFirst()
                    .orElse("UNKNOWN"));

            Filter filter = Filter.builder()
                    .name("addresses.private-ip-address")
                    .values(dto.getPrivateIpAddress())
                    .build();
            DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest = DescribeNetworkInterfacesRequest.builder().filters(filter).build();
            List<NetworkInterface> networkInterfaces = clientEC2.describeNetworkInterfaces(describeNetworkInterfacesRequest).networkInterfaces();
            if (networkInterfaces != null && networkInterfaces.size() > 0) {
                NetworkInterface networkInterface = networkInterfaces.get(0);
                dto.setPrivateDns(networkInterface.privateDnsName());
                if (networkInterface.association() != null) {
                    NetworkInterfaceAssociation association = networkInterface.association();
                    dto.setPublicIpAddress(association.publicIp());
                    dto.setPublicDns(association.publicDnsName());
                }
            }
            dto.setAccount(this.getAccount());
            dtos.add(dto);
        }
        return dtos;
    }

    public WorkspaceDTO getWorspace(String id) {
        WorkSpacesClient client = this.getWorkspaceClient();
        Ec2Client clientEC2 = this.getEc2Client();
        DescribeWorkspacesRequest describeWorkspacesRequest = DescribeWorkspacesRequest.builder().workspaceIds(id).build();
        List<Workspace> wks = client.describeWorkspaces(describeWorkspacesRequest).workspaces();
        for(Workspace w : wks) {
            WorkspaceDTO dto = new WorkspaceDTO();
            dto.setId(w.workspaceId());
            dto.setComputerName(w.computerName());
            dto.setUserName(w.userName());
            dto.setState(w.stateAsString());
            dto.setPrivateIpAddress(w.ipAddress());
            WorkspaceProperties prop = w.workspaceProperties();
            dto.setRootVolumeSizeGib(prop.rootVolumeSizeGib());
            dto.setUserVolumeSizeGib(prop.userVolumeSizeGib());
            dto.setRunningMode(prop.runningMode());
            dto.setOs(prop.operatingSystemNameAsString());
            dto.setPlatform(prop.computeTypeNameAsString());
            dto.setProtocol(prop.protocolsAsStrings()
                    .stream()
                    .findFirst()
                    .orElse("UNKNOWN"));
            Filter filter = Filter.builder()
                    .name("addresses.private-ip-address")
                    .values(dto.getPrivateIpAddress())
                    .build();
            DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest = DescribeNetworkInterfacesRequest.builder().filters(filter).build();
            List<NetworkInterface> networkInterfaces = clientEC2.describeNetworkInterfaces(describeNetworkInterfacesRequest).networkInterfaces();
            if (networkInterfaces != null && networkInterfaces.size() > 0) {
                NetworkInterface networkInterface = networkInterfaces.get(0);
                dto.setPrivateDns(networkInterface.privateDnsName());
                if (networkInterface.association() != null) {
                    NetworkInterfaceAssociation association = networkInterface.association();
                    dto.setPublicIpAddress(association.publicIp());
                    dto.setPublicDns(association.publicDnsName());
                }
            }
            dto.setAccount(this.getAccount());
            return dto;
        }
        return null;
    }

    public void startWorkspace(String id) {
        WorkSpacesClient client = this.getWorkspaceClient();
        try {
            StartRequest requestId = StartRequest.builder().workspaceId(id).build();
            StartWorkspacesRequest startWorkspacesRequest = StartWorkspacesRequest.builder()
                    .startWorkspaceRequests(requestId)
                    .build();
            client.startWorkspaces(startWorkspacesRequest);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void stopWorkspace(String id) {
        WorkSpacesClient client = this.getWorkspaceClient();
        try {
            StopRequest requestId = StopRequest.builder().workspaceId(id).build();
            StopWorkspacesRequest stopWorkspacesRequest = StopWorkspacesRequest.builder()
                    .stopWorkspaceRequests(requestId)
                    .build();
            client.stopWorkspaces(stopWorkspacesRequest);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void rebootWorkspace(String id) {
        WorkSpacesClient client = this.getWorkspaceClient();
        try {
            RebootRequest requestId = RebootRequest.builder().workspaceId(id).build();
            RebootWorkspacesRequest startWorkspacesRequest = RebootWorkspacesRequest.builder()
                    .rebootWorkspaceRequests(requestId)
                    .build();
            client.rebootWorkspaces(startWorkspacesRequest);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void deleteVersionBucketFolder(String product, String version) {
        String versionPath = String.format("%s/versao/%s", product, version);
        AmazonS3 client = this.getS3Client();
        ObjectListing objectList = client.listObjects( this.getConfig().getBucketVersions(), versionPath );
        List<S3ObjectSummary> objectSummeryList =  objectList.getObjectSummaries();
        if (objectSummeryList.size() > 0) {
            String[] keysList = new String[objectSummeryList.size()];
            int count = 0;
            for (S3ObjectSummary summery : objectSummeryList) {
                keysList[count++] = summery.getKey();
            }
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(this.getConfig().getBucketVersions()).withKeys(keysList);
            client.deleteObjects(deleteObjectsRequest);
        }

    }
    public void deleteCICDResults(LinkedHashMap<Long, String> productKeys) {
        List<String> keys = new ArrayList<>();
        AmazonS3 client = this.getS3Client();
        for(Map.Entry<Long, String> e : productKeys.entrySet()) {
            String key = String.format("%s/ci_cd/%s", e.getValue(), e.getKey());
            ObjectListing objectList = client.listObjects( this.getConfig().getBucketVersions(), key );
            List<S3ObjectSummary> objectSummeryList =  objectList.getObjectSummaries();
            if (objectSummeryList.size() > 0) {
                for (S3ObjectSummary summery : objectSummeryList) {
                    keys.add(summery.getKey());
                }
            }

        }

        if (keys.size() > 0) {
            String[] keysList = keys.toArray(new String[0]);
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(this.getConfig().getBucketVersions()).withKeys(keysList);
            client.deleteObjects(deleteObjectsRequest);
        }

    }

    public String getLinkCICDReport(String target, Long id) {
        return String.format("https://%s.s3.amazonaws.com/%s/ci_cd/%s/REPORT.json",
                this.getConfig().getBucketVersions(), target, id);
    }

    public String getLinkBucketVersoes() {
        return String.format("https://%s.s3.amazonaws.com", this.getConfig().getBucketVersions());
    }

    @Override
    public boolean initialize(Object... account) {
        if (account != null && account.length > 0) {
            setThreadLocalAccount(account[0].toString());
        }
        AWSAccountConfigDTO accountConfig = this.getConfig();
        return accountConfig != null && (accountConfig.getEnabled() == null || accountConfig.getEnabled().booleanValue());
    }

    @Override
    public void dispose() {
        threadLocalContext.remove(); // Limpa o contexto de thread
    }

    @Override
    @Transactional(readOnly = true)
    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.AWS).orElse(null);
    }

    @Transactional(readOnly = true, propagation=REQUIRES_NEW)
    public AWSAccountConfigDTO getConfig() {
        // Verifica se o contexto da thread já foi definido
        Map.Entry<String, AWSConfigDTO> context = threadLocalContext.get();

        if (context == null || context.getKey() == null) {
            // Se não houver contexto, carrega a configuração padrão
            AWSConfigDTO config = this.getAllConfigs();
            if (config == null) {
                return null;
            }

            // Define o contexto da thread com o account principal
            Map.Entry<String, AWSAccountConfigDTO> mainAccountEntry = config.getMainAccount();
            if (mainAccountEntry != null) {
                setThreadLocalAccount(mainAccountEntry.getKey());
                context = threadLocalContext.get(); // Atualiza o contexto com a account principal
            }
        }

        // Obtém a configuração a partir do contexto
        if (context == null || !context.getValue().getAccountConfigs().containsKey(context.getKey())) {
            return null;
        }

        // Verifica se a chave de acesso da conta está vazia
        if (StringHelper.isEmpty(context.getValue().getAccountConfigs().get(context.getKey()).getAccessKey())) {
            return null;
        }

        return context.getValue().getAccountConfigs().get(context.getKey());
    }

    @Transactional(readOnly = true)
    public AWSConfigDTO getAllConfigs() {
        SystemConfigBean c = this.getSystemConfig();
        if (c != null) {
            return (AWSConfigDTO) c.getConfig();
        }
        return null;
    }

    private Ec2Client getEc2Client() {
        Region region = Region.of(this.getConfig().getRegion());

        String accessKey = this.getConfig().getAccessKey();
        String secretKey = this.getConfig().getSecretKey();

        StaticCredentialsProvider provider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .credentialsProvider(provider)
                .build();
        return ec2;
    }

    private RdsClient getRdsClient() {
        Region region = Region.of(this.getConfig().getRegion());

        String accessKey = this.getConfig().getAccessKey();
        String secretKey = this.getConfig().getSecretKey();

        StaticCredentialsProvider provider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        RdsClient rdsClient = RdsClient.builder()
                .region(region)
                .credentialsProvider(provider)
                .build();
        return rdsClient;
    }

    private WorkSpacesClient getWorkspaceClient() {
        Region region = Region.of(this.getConfig().getRegion());

        String accessKey = this.getConfig().getAccessKey();
        String secretKey = this.getConfig().getSecretKey();

        StaticCredentialsProvider provider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        WorkSpacesClient client = WorkSpacesClient.builder()
                .region(region)
                .credentialsProvider(provider)
                .build();
        return client;
    }

    private AmazonS3 getS3Client() {
        Region region = Region.of(this.getConfig().getRegion());

        String accessKey = this.getConfig().getAccessKey();
        String secretKey = this.getConfig().getSecretKey();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withRegion(region.id())
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
        return client;
    }

    public List<BucketDTO> retrieveBucketFiles(BucketDTO parent) {
        String path = null;
        if (parent != null && StringUtils.hasText(parent.getPath())) {
            path = parent.getPath();
        }
        AmazonS3 client = this.getS3Client();
        List<BucketDTO> result = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            List<Bucket> buckets = client.listBuckets();
            for (Bucket bucket : buckets) {
                result.add(new BucketDTO(this.getAccount(), bucket.getName(), BucketFileTypeEnum.BUCKET, "/" + bucket.getName(), getBucketArn(bucket.getName(), "")));
            }
        } else {
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

            String bucketName = normalizedPath.split("/")[0];
            String prefix = normalizedPath.length() > bucketName.length() ? normalizedPath.substring(bucketName.length() + 1) : "";

            if (!prefix.isEmpty() && !prefix.endsWith("/")) {
                prefix = prefix + "/";
            }

            // Request to list objects in the bucket with the prefix
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withDelimiter("/");

            ListObjectsV2Result resultObj = client.listObjectsV2(request);

            // Add directories (common prefixes) with ARN
            for (String commonPrefix : resultObj.getCommonPrefixes()) {
                String directoryName = commonPrefix.replaceAll("/$", "").substring(prefix.length()); // Pega só o nome do diretório
                result.add(new BucketDTO(
                        this.getAccount(), directoryName,
                        BucketFileTypeEnum.DIRECTORY,
                        "/" + bucketName + "/" + commonPrefix,
                        getBucketArn(bucketName, commonPrefix)
                ));
            }

            // Add files (object summaries) with ARN
            for (S3ObjectSummary objectSummary : resultObj.getObjectSummaries()) {
                if (!objectSummary.getKey().endsWith("/")) { // Exclude folders
                    String fileName = objectSummary.getKey().substring(prefix.length()); // Pega só o nome do arquivo
                    result.add(new BucketDTO(
                            this.getAccount(), fileName,
                            BucketFileTypeEnum.FILE,
                            "/" + bucketName + "/" + objectSummary.getKey(),
                            getBucketArn(bucketName, objectSummary.getKey())
                    ));
                }
            }
        }

        return result;

    }

    public Future<?> uploadFileToS3(BucketDTO dto, String filePath) {
        return this.uploadFileToS3(dto, filePath, null, null);
    }

    public Future<?> uploadFileToS3(BucketDTO dto, String filePath, ProgressStatusListener listener, AtomicBoolean cancelFlag) {
        AmazonS3 s3 = this.getS3Client();
        File file = new File(filePath);

        TransferManager transferManager = TransferManagerBuilder.standard()
                .withS3Client(s3)
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            try {
                PutObjectRequest request = new PutObjectRequest(dto.getBucket(), dto.getKey(), file);
                Upload upload = transferManager.upload(request);

                upload.addProgressListener(new ProgressListener() {
                    @Override
                    public void progressChanged(ProgressEvent progressEvent) {
                        if (cancelFlag.get()) {
                            upload.abort();  // Cancela o upload real
                        } else if (progressEvent.getEventType() == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                            double percent = (progressEvent.getBytesTransferred() * 100.0) / file.length();
                            if (listener != null) listener.onProgress(percent);
                        }
                    }
                });

                upload.waitForCompletion();  // Aguarda até o upload terminar

            } catch (Exception e) {
                throw new RuntimeException("Erro ao fazer upload para S3: " + e.getMessage(), e);
            } finally {
                transferManager.shutdownNow();
                executor.shutdown();
            }
        });

    }

    public Future<?> downloadFileFromS3(BucketDTO dto, String destinationPath) {
        return this.downloadFileFromS3(dto, destinationPath, null, null);
    }

    public Future<?> downloadFileFromS3(BucketDTO dto, String destinationPath, ProgressStatusListener listener, AtomicBoolean cancelFlag) {
        AmazonS3 s3 = this.getS3Client();

        return Executors.newSingleThreadExecutor().submit(() -> {
            try {
                S3Object s3Object = s3.getObject(dto.getBucket(), dto.getKey());
                long totalBytes = s3Object.getObjectMetadata().getContentLength();
                InputStream inputStream = s3Object.getObjectContent();
                File destinationFile = new File(destinationPath);

                try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long downloadedBytes = 0;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        // Verifica se o cancelamento foi solicitado
                        if (cancelFlag.get()) {
                            return;
                        }

                        outputStream.write(buffer, 0, bytesRead);

                        if (listener != null) {
                            downloadedBytes += bytesRead;
                            double percent = (downloadedBytes * 100.0) / totalBytes;
                            listener.onProgress(percent);
                        }
                    }

                }

            } catch (Exception e) {
                throw new RuntimeException("Erro ao fazer download do arquivo do S3: " + e.getMessage(), e);
            }
        });
    }

    public Future<?> uploadHugeStreamToS3(BucketDTO dto, InputStream inputStream, int chunkSize,
                                          ProgressStatusListener listener, AtomicBoolean cancelFlag) {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            AmazonS3 s3 = this.getS3Client();
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(dto.getBucket(), dto.getKey());
            InitiateMultipartUploadResult initResponse = s3.initiateMultipartUpload(initRequest);

            List<PartETag> partETags = new ArrayList<>();
            int partNumber = 1;
            long totalUploaded = 0;

            try (BufferedInputStream bufferedStream = new BufferedInputStream(inputStream)) {
                boolean isLastPart = false;

                while (!isLastPart) {
                    byte[] buffer = new byte[chunkSize];
                    int bytesRead = 0;

                    // Preenche o buffer até o chunkSize ou EOF
                    while (bytesRead < chunkSize) {
                        int read = bufferedStream.read(buffer, bytesRead, chunkSize - bytesRead);
                        if (read == -1) break;
                        bytesRead += read;
                    }

                    if (bytesRead == 0) break; // EOF no início do loop

                    // Detecta se essa é a última parte
                    bufferedStream.mark(1);
                    int nextByte = bufferedStream.read();
                    if (nextByte == -1) {
                        isLastPart = true;
                    } else {
                        bufferedStream.reset();
                    }

                    // Verifica regra do S3: partes intermediárias não podem ser menores que 5MB
                    if (bytesRead < 5 * 1024 * 1024 && !isLastPart) {
                        throw new RuntimeException("Parte " + partNumber + " menor que 5MB e não é a última. Upload inválido para S3.");
                    }

                    ByteArrayInputStream partStream = new ByteArrayInputStream(buffer, 0, bytesRead);

                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName(dto.getBucket())
                            .withKey(dto.getKey())
                            .withUploadId(initResponse.getUploadId())
                            .withPartNumber(partNumber++)
                            .withInputStream(partStream)
                            .withPartSize(bytesRead);

                    UploadPartResult uploadResult = s3.uploadPart(uploadRequest);
                    partETags.add(uploadResult.getPartETag());

                    totalUploaded += bytesRead;

                    if (listener != null) {
                        listener.onProgress(totalUploaded);
                        listener.onTotalBytes(totalUploaded);
                        listener.onReadBytes(buffer, bytesRead);
                    }

                    if (cancelFlag.get()) {
                        throw new InterruptedException("Upload cancelado pelo usuário.");
                    }
                }

                CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
                        dto.getBucket(), dto.getKey(), initResponse.getUploadId(), partETags);
                s3.completeMultipartUpload(compRequest);

            } catch (Exception e) {
                s3.abortMultipartUpload(new AbortMultipartUploadRequest(
                        dto.getBucket(), dto.getKey(), initResponse.getUploadId()));
                throw new RuntimeException("Erro no upload pg_dump multipart para S3", e);
            } finally {
                executor.shutdown();
            }
        });
    }

    public InputStream getFileStreamFromS3(BucketDTO dto) {
        AmazonS3 s3 = this.getS3Client();
        try {
            S3Object s3Object = s3.getObject(dto.getBucket(), dto.getKey());
            return s3Object.getObjectContent();
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return null; // Arquivo não encontrado
            }
            throw e; // Lança a exceção para tratamento posterior
        }
    }

    /**
     * Retorna um InputStream com reconexão automática via range requests.
     * Se a conexão S3 cair no meio da leitura, retoma a partir da última posição
     * com exponential backoff. Ideal para arquivos grandes (dezenas/centenas de GB).
     */
    public InputStream getResumableFileStreamFromS3(BucketDTO dto, int maxRetries, long baseRetryDelayMs) {
        AmazonS3 s3 = this.getS3Client();
        try {
            return new ResumableS3InputStream(s3, dto.getBucket(), dto.getKey(), maxRetries, baseRetryDelayMs);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    public boolean bucketFileExists(BucketDTO dto) {
        AmazonS3 s3 = this.getS3Client();
        try {
            S3Object object = s3.getObject(dto.getBucket(), dto.getKey());
            return object != null;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return false; // Arquivo não encontrado
            }
            throw e; // Lança a exceção para tratamento posterior
        }
    }
    private String getBucketArn(String bucketName, String key) {
        return "arn:aws:s3:::" + bucketName + "/" + key;
    }

    private void setThreadLocalAccount(String account) {
        AWSConfigDTO config = this.getAllConfigs();
        if (config != null) {
            threadLocalContext.set(new AbstractMap.SimpleEntry<>(account, config));
        }
    }

    private String getAccount() {
        Map.Entry<String, AWSConfigDTO> context = threadLocalContext.get();
        if (context == null) {
            return null;
        }
        return context.getKey();
    }

    /**
     * InputStream que lê de um objeto S3 com reconexão automática via range requests.
     * Se a conexão HTTP cair no meio da leitura, retoma a partir da última posição
     * lida com sucesso, usando o header Range do S3.
     *
     * Transparente para os consumidores — BufferedInputStream, GZIPInputStream e
     * pg_restore continuam lendo normalmente sem perceber a reconexão.
     */
    public class ResumableS3InputStream extends InputStream {

        private static final Logger logger = LoggerFactory.getLogger(ResumableS3InputStream.class);

        private final AmazonS3 s3;
        private final String bucket;
        private final String key;
        private final long totalSize;
        private final int maxRetries;
        private final long baseRetryDelayMs;

        private long position = 0;
        private InputStream currentStream;
        private boolean closed = false;

        public ResumableS3InputStream(AmazonS3 s3, String bucket, String key, int maxRetries, long baseRetryDelayMs) {
            this.s3 = s3;
            this.bucket = bucket;
            this.key = key;
            this.maxRetries = maxRetries;
            this.baseRetryDelayMs = baseRetryDelayMs;

            ObjectMetadata metadata = s3.getObjectMetadata(bucket, key);
            this.totalSize = metadata.getContentLength();

            this.currentStream = openStream(0);
            logger.info("ResumableS3InputStream aberto: bucket={}, key={}, tamanho={} bytes ({} GB)",
                    bucket, key, totalSize, String.format("%.2f", totalSize / (1024.0 * 1024.0 * 1024.0)));
        }

        private InputStream openStream(long fromPosition) {
            if (fromPosition >= totalSize) {
                return null;
            }
            GetObjectRequest request = new GetObjectRequest(bucket, key);
            if (fromPosition > 0) {
                request.setRange(fromPosition, totalSize - 1);
                logger.info("Retomando leitura S3 a partir da posição {} de {} ({} % concluído)",
                        fromPosition, totalSize, String.format("%.2f", (fromPosition * 100.0) / totalSize));
            }
            S3Object s3Object = s3.getObject(request);
            return s3Object.getObjectContent();
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int n = read(b, 0, 1);
            return n == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) throw new IOException("Stream fechado");
            if (position >= totalSize) return -1;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    if (currentStream == null) {
                        currentStream = openStream(position);
                        if (currentStream == null) return -1;
                    }
                    int n = currentStream.read(b, off, len);
                    if (n > 0) {
                        position += n;
                    }
                    return n;
                } catch (IOException e) {
                    logger.warn("Erro na leitura S3 na posição {} de {} (tentativa {}/{}): {}",
                            position, totalSize, attempt + 1, maxRetries + 1, e.getMessage());
                    closeCurrentStream();

                    if (attempt >= maxRetries) {
                        throw new IOException(String.format(
                                "Falha após %d tentativas na posição %d de %d: %s",
                                maxRetries + 1, position, totalSize, e.getMessage()), e);
                    }

                    try {
                        long delay = baseRetryDelayMs * (1L << Math.min(attempt, 5)); // exponential backoff, max 32x base
                        logger.info("Aguardando {} ms antes de reconectar ao S3...", delay);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrompido durante retry", ie);
                    }
                }
            }
            return -1; // unreachable
        }

        private void closeCurrentStream() {
            if (currentStream != null) {
                try { currentStream.close(); } catch (IOException ignored) {}
                currentStream = null;
            }
        }

        @Override
        public void close() throws IOException {
            closed = true;
            closeCurrentStream();
        }

        public long getPosition() {
            return position;
        }

        public long getTotalSize() {
            return totalSize;
        }
    }
}
