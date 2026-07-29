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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.workspaces.WorkSpacesClient;
import software.amazon.awssdk.services.workspaces.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class AWSService implements ISystemConfigService {
    private static final Logger logger = LoggerFactory.getLogger(AWSService.class);

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
                dto.setRegion(this.getConfig().getRegion());
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
            dto.setRegion(this.getConfig().getRegion());
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
        String bucket = this.getConfig().getBucketVersions();
        try (S3Client client = this.getS3Client()) {
            ListObjectsV2Response objectList = client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(versionPath)
                    .build());
            List<ObjectIdentifier> keys = new ArrayList<>();
            for (S3Object summery : objectList.contents()) {
                keys.add(ObjectIdentifier.builder().key(summery.key()).build());
            }
            if (keys.size() > 0) {
                client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(keys).build())
                        .build());
            }
        }

    }
    public void deleteCICDResults(LinkedHashMap<Long, String> productKeys) {
        List<ObjectIdentifier> keys = new ArrayList<>();
        String bucket = this.getConfig().getBucketVersions();
        try (S3Client client = this.getS3Client()) {
            for(Map.Entry<Long, String> e : productKeys.entrySet()) {
                String key = String.format("%s/ci_cd/%s", e.getValue(), e.getKey());
                ListObjectsV2Response objectList = client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(key)
                        .build());
                for (S3Object summery : objectList.contents()) {
                    keys.add(ObjectIdentifier.builder().key(summery.key()).build());
                }

            }

            if (keys.size() > 0) {
                client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(keys).build())
                        .build());
            }
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

    private StaticCredentialsProvider getCredentialsProvider() {
        String accessKey = this.getConfig().getAccessKey();
        String secretKey = this.getConfig().getSecretKey();
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Client getS3Client() {
        return this.getS3Client(this.getConfig().getRegion());
    }

    // crossRegionAccessEnabled também aqui, no caminho de dados: a navegação de buckets consegue
    // alcançar buckets de qualquer região, e a tela permite (corretamente) escolher um bucket de
    // outra conta desde que a região case com a do RDS. Sem isso, um bucket fora da região
    // configurada na conta lista bem mas falha com 301 na hora do upload/download.
    private S3Client getS3Client(String regionId) {
        return S3Client.builder()
                .region(Region.of(regionId))
                .crossRegionAccessEnabled(true)
                .credentialsProvider(this.getCredentialsProvider())
                .build();
    }

    private S3AsyncClient getS3AsyncClient() {
        return S3AsyncClient.builder()
                .region(Region.of(this.getConfig().getRegion()))
                .crossRegionAccessEnabled(true)
                .credentialsProvider(this.getCredentialsProvider())
                .multipartEnabled(true)
                .build();
    }

    /**
     * Cliente usado para navegar buckets. Diferente das demais operações, `listBuckets()` é global:
     * devolve os buckets da conta em todas as regiões, não só na região configurada. Por isso o
     * cliente de navegação precisa conseguir falar com buckets fora da região da conta, senão o S3
     * responde 301 (PermanentRedirect) ao listar o conteúdo de um bucket de outra região.
     */
    private S3Client getS3GlobalClient() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .crossRegionAccessEnabled(true)
                .credentialsProvider(this.getCredentialsProvider())
                .build();
    }

    public List<BucketDTO> retrieveBucketFiles(BucketDTO parent) {
        String path = null;
        if (parent != null && StringUtils.hasText(parent.getPath())) {
            path = parent.getPath();
        }
        List<BucketDTO> result = new ArrayList<>();
        try (S3Client client = this.getS3GlobalClient()) {
            if (path == null || path.isEmpty()) {
                // O ListBuckets do SDK v2 já devolve a região de cada bucket, então não é preciso
                // consultar getBucketLocation bucket a bucket (o que era lento). A região importa
                // porque o backup/restore nativo do RDS exige bucket e instância na mesma região.
                // O paginator cuida da paginação (o token de saída é o próprio continuationToken).
                // ATENÇÃO ao maxBuckets: a AWS só inclui BucketRegion na resposta "if the request
                // contains at least one valid parameter". Sem nenhum parâmetro, bucketRegion()
                // volta null para todos os buckets e o filtro de região da tela deixa de funcionar
                // em silêncio. Ver https://docs.aws.amazon.com/AmazonS3/latest/API/API_Bucket.html
                for (Bucket bucket : client.listBucketsPaginator(ListBucketsRequest.builder()
                        .maxBuckets(1000)
                        .build()).buckets()) {
                    BucketDTO dto = new BucketDTO(this.getAccount(), bucket.name(), BucketFileTypeEnum.BUCKET, "/" + bucket.name(), getBucketArn(bucket.name(), ""));
                    dto.setRegion(bucket.bucketRegion());
                    result.add(dto);
                }
            } else {
                String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

                String bucketName = normalizedPath.split("/")[0];
                String prefix = normalizedPath.length() > bucketName.length() ? normalizedPath.substring(bucketName.length() + 1) : "";

                if (!prefix.isEmpty() && !prefix.endsWith("/")) {
                    prefix = prefix + "/";
                }

                // Request to list objects in the bucket with the prefix
                ListObjectsV2Response resultObj = client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .delimiter("/")
                        .build());

                // Diretórios e arquivos não recebem região: só os itens de tipo BUCKET são
                // filtrados por região na tela, e o caminho de dados resolve bucket fora da região
                // da conta pelo próprio client (crossRegionAccessEnabled). Consultar
                // getBucketLocation aqui seria uma chamada de rede por navegação, sem consumidor.

                // Add directories (common prefixes) with ARN
                for (CommonPrefix common : resultObj.commonPrefixes()) {
                    String commonPrefix = common.prefix();
                    String directoryName = commonPrefix.replaceAll("/$", "").substring(prefix.length()); // Pega só o nome do diretório
                    result.add(new BucketDTO(
                            this.getAccount(), directoryName,
                            BucketFileTypeEnum.DIRECTORY,
                            "/" + bucketName + "/" + commonPrefix,
                            getBucketArn(bucketName, commonPrefix)
                    ));
                }

                // Add files (object summaries) with ARN
                for (S3Object objectSummary : resultObj.contents()) {
                    if (!objectSummary.key().endsWith("/")) { // Exclude folders
                        String fileName = objectSummary.key().substring(prefix.length()); // Pega só o nome do arquivo
                        result.add(new BucketDTO(
                                this.getAccount(), fileName,
                                BucketFileTypeEnum.FILE,
                                "/" + bucketName + "/" + objectSummary.key(),
                                getBucketArn(bucketName, objectSummary.key())
                        ));
                    }
                }
            }
        }

        return result;

    }

    public Future<?> uploadFileToS3(BucketDTO dto, String filePath) {
        return this.uploadFileToS3(dto, filePath, null, null);
    }

    public Future<?> uploadFileToS3(BucketDTO dto, String filePath, ProgressStatusListener listener, AtomicBoolean cancelFlag) {
        File file = new File(filePath);

        // Client e TransferManager são criados aqui, na thread do chamador, porque getConfig()
        // depende do ThreadLocal de conta — dentro da lambda ele cairia na conta principal.
        S3AsyncClient asyncClient = this.getS3AsyncClient();
        S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(asyncClient)
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            try {
                // O TransferListener é registrado junto com o request, então precisa de um holder
                // para poder cancelar o próprio upload de dentro do callback.
                AtomicReference<FileUpload> uploadRef = new AtomicReference<>();
                TransferListener progressListener = new TransferListener() {
                    @Override
                    public void bytesTransferred(Context.BytesTransferred context) {
                        if (cancelFlag != null && cancelFlag.get()) {
                            FileUpload current = uploadRef.get();
                            if (current != null) {
                                current.completionFuture().cancel(true);
                            }
                            return;
                        }
                        if (listener != null) {
                            context.progressSnapshot().ratioTransferred()
                                    .ifPresent(ratio -> listener.onProgress(ratio * 100.0));
                        }
                    }
                };

                FileUpload upload = transferManager.uploadFile(UploadFileRequest.builder()
                        .source(file)
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(dto.getBucket())
                                .key(dto.getKey())
                                .build())
                        .addTransferListener(progressListener)
                        .build());
                uploadRef.set(upload);
                // Recheca após publicar a referência: um cancelamento pedido antes disso não teria
                // sido atendido pelo listener (que ainda veria uploadRef vazio), e um arquivo
                // pequeno pode gerar um único evento de progresso.
                if (cancelFlag != null && cancelFlag.get()) {
                    upload.completionFuture().cancel(true);
                }

                // get() em vez de join(): join() é ininterruptível, então o cancel(true) que o
                // chamador faz no Future era ignorado e a thread ficava presa aqui (sem nunca
                // rodar o finally, vazando TransferManager e client) quando a transferência
                // travava sem novos eventos de progresso.
                try {
                    upload.completionFuture().get();
                } catch (InterruptedException ie) {
                    upload.completionFuture().cancel(true);
                    Thread.currentThread().interrupt();
                    throw ie;
                }

            } catch (Exception e) {
                throw new RuntimeException("Erro ao fazer upload para S3: " + e.getMessage(), e);
            } finally {
                transferManager.close();
                asyncClient.close();
                executor.shutdown();
            }
        });

    }

    public Future<?> downloadFileFromS3(BucketDTO dto, String destinationPath) {
        return this.downloadFileFromS3(dto, destinationPath, null, null);
    }

    public Future<?> downloadFileFromS3(BucketDTO dto, String destinationPath, ProgressStatusListener listener, AtomicBoolean cancelFlag) {
        S3Client s3 = this.getS3Client();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            try (ResponseInputStream<GetObjectResponse> inputStream = s3.getObject(GetObjectRequest.builder()
                    .bucket(dto.getBucket())
                    .key(dto.getKey())
                    .build())) {
                long totalBytes = inputStream.response().contentLength();
                File destinationFile = new File(destinationPath);

                try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long downloadedBytes = 0;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        // Verifica se o cancelamento foi solicitado
                        if (cancelFlag != null && cancelFlag.get()) {
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
            } finally {
                s3.close();
                executor.shutdown();
            }
        });
    }

    public Future<?> uploadHugeStreamToS3(BucketDTO dto, InputStream inputStream, int chunkSize,
                                          ProgressStatusListener listener, AtomicBoolean cancelFlag) {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Criado na thread do chamador: getConfig() lê o ThreadLocal de conta, que não existe na
        // thread do executor (lá cairia silenciosamente na conta principal).
        S3Client s3 = this.getS3Client();

        return executor.submit(() -> {
            // O checksum é declarado explicitamente (CRC32) em vez de depender do default do SDK:
            // desde a 2.30 o v2 calcula checksum por parte automaticamente, e nesse caso o
            // CompleteMultipartUpload exige o checksum de cada parte de volta — sem isso ele falha
            // com 400 InvalidPart só no fim do upload. Ver aws/aws-sdk-java-v2#6518.
            CreateMultipartUploadResponse initResponse = s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
                    .bucket(dto.getBucket())
                    .key(dto.getKey())
                    .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                    .build());

            List<CompletedPart> partETags = new ArrayList<>();
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

                    int currentPart = partNumber++;
                    UploadPartResponse uploadResult = s3.uploadPart(UploadPartRequest.builder()
                                    .bucket(dto.getBucket())
                                    .key(dto.getKey())
                                    .uploadId(initResponse.uploadId())
                                    .partNumber(currentPart)
                                    .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                    .build(),
                            RequestBody.fromInputStream(partStream, bytesRead));
                    partETags.add(CompletedPart.builder()
                            .partNumber(currentPart)
                            .eTag(uploadResult.eTag())
                            .checksumCRC32(uploadResult.checksumCRC32())
                            .build());

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

                s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                        .bucket(dto.getBucket())
                        .key(dto.getKey())
                        .uploadId(initResponse.uploadId())
                        .multipartUpload(CompletedMultipartUpload.builder().parts(partETags).build())
                        .build());

            } catch (Exception e) {
                s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                        .bucket(dto.getBucket())
                        .key(dto.getKey())
                        .uploadId(initResponse.uploadId())
                        .build());
                throw new RuntimeException("Erro no upload pg_dump multipart para S3", e);
            } finally {
                s3.close();
                executor.shutdown();
            }
        });
    }

    /**
     * Retorna um InputStream com reconexão automática via range requests.
     * Se a conexão S3 cair no meio da leitura, retoma a partir da última posição
     * com exponential backoff. Ideal para arquivos grandes (dezenas/centenas de GB).
     */
    public InputStream getResumableFileStreamFromS3(BucketDTO dto, int maxRetries, long baseRetryDelayMs) {
        S3Client s3 = this.getS3Client();
        try {
            // O stream devolvido passa a ser dono do client e o fecha no seu próprio close(),
            // porque ele continua usando o client depois deste método retornar.
            return new ResumableS3InputStream(s3, dto.getBucket(), dto.getKey(), maxRetries, baseRetryDelayMs);
        } catch (NoSuchKeyException e) {
            s3.close();
            return null;
        } catch (S3Exception e) {
            s3.close();
            if (e.statusCode() == 404) {
                return null;
            }
            throw e;
        } catch (RuntimeException e) {
            s3.close();
            throw e;
        }
    }

    public boolean bucketFileExists(BucketDTO dto) {
        try (S3Client s3 = this.getS3Client()) {
            // headObject em vez de getObject: só precisamos saber se existe, e o getObject antigo
            // abria o corpo do objeto sem fechá-lo.
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(dto.getBucket())
                    .key(dto.getKey())
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false; // Arquivo não encontrado
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false; // Arquivo não encontrado
            }
            throw e; // Lança a exceção para tratamento posterior
        }
    }

    /**
     * Verifica se o objeto no S3 começa com os magic bytes do gzip (0x1F 0x8B),
     * baixando apenas os 2 primeiros bytes via Range request.
     */
    public boolean isGzipObject(String bucket, String key) {
        try (S3Client s3 = this.getS3Client();
             InputStream in = s3.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .range("bytes=0-1")
                .build())) {
            byte[] head = new byte[2];
            int read = 0;
            while (read < 2) {
                int n = in.read(head, read, 2 - read);
                if (n < 0) break;
                read += n;
            }
            return read == 2 && (head[0] & 0xFF) == 0x1F && (head[1] & 0xFF) == 0x8B;
        } catch (IOException e) {
            logger.warn("Falha ao inspecionar magic bytes de s3://{}/{} : {}", bucket, key, e.getMessage());
            return false;
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

        private final S3Client s3;
        private final String bucket;
        private final String key;
        private final long totalSize;
        private final int maxRetries;
        private final long baseRetryDelayMs;

        private long position = 0;
        private InputStream currentStream;
        private boolean closed = false;

        public ResumableS3InputStream(S3Client s3, String bucket, String key, int maxRetries, long baseRetryDelayMs) {
            this.s3 = s3;
            this.bucket = bucket;
            this.key = key;
            this.maxRetries = maxRetries;
            this.baseRetryDelayMs = baseRetryDelayMs;

            HeadObjectResponse metadata = s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            this.totalSize = metadata.contentLength();

            this.currentStream = openStream(0);
            logger.info("ResumableS3InputStream aberto: bucket={}, key={}, tamanho={} bytes ({} GB)",
                    bucket, key, totalSize, String.format("%.2f", totalSize / (1024.0 * 1024.0 * 1024.0)));
        }

        private InputStream openStream(long fromPosition) {
            if (fromPosition >= totalSize) {
                return null;
            }
            GetObjectRequest.Builder request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            if (fromPosition > 0) {
                request.range("bytes=" + fromPosition + "-" + (totalSize - 1));
                logger.info("Retomando leitura S3 a partir da posição {} de {} ({} % concluído)",
                        fromPosition, totalSize, String.format("%.2f", (fromPosition * 100.0) / totalSize));
            }
            return s3.getObject(request.build());
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
                    } else if (n == -1 && position < totalSize) {
                        // Conexão encerrada "limpa" antes do fim do objeto: sem esta checagem isso
                        // parecia EOF e o consumidor (pg_restore) recebia um dump truncado em
                        // silêncio. Virar IOException joga o fluxo no retry/resume abaixo.
                        throw new IOException(String.format(
                                "Stream S3 encerrado prematuramente em %d de %d bytes", position, totalSize));
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
            // Este stream é dono do S3Client (ver getResumableFileStreamFromS3): sem fechar aqui,
            // cada restore deixaria para trás um client com seu pool de conexões.
            try {
                s3.close();
            } catch (Exception ignored) {}
        }

        public long getPosition() {
            return position;
        }

        public long getTotalSize() {
            return totalSize;
        }
    }
}
