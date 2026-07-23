# Plano futuro: múltiplas regiões por conta AWS

> Status: **não iniciado**. Este documento é um registro de investigação/design para quando essa
> funcionalidade for priorizada — nenhum código foi alterado por causa dele.

## Motivação

O usuário tem, na mesma conta AWS, instâncias RDS espalhadas em 2 regiões diferentes. Hoje a tela de
configuração (`config-system-aws`) só permite configurar **uma região por conta**, então só uma das
regiões fica visível/gerenciável pelo Portal Evolui.

## Estado atual (levantado em 2026-07-23)

- `AWSAccountConfigDTO.region` (backend) e `AWSAccountConfigModel.region` (frontend) são uma única
  `String`, sem validação de formato, sem lista de regiões conhecidas.
- Em `portal-evolui-backend/.../service/AWSService.java`, a região é lida em **4 pontos
  centralizados**, todos com o mesmo padrão — `Region.of(this.getConfig().getRegion())` — dentro dos
  builders privados de client do AWS SDK:
  - `getEc2Client()`
  - `getRdsClient()`
  - `getWorkspaceClient()`
  - `getS3Client()` (usa a região também para os buckets `bucketVersions`/`bucketTempDump`/
    `bucketLocalMountPath`)
- Não existe cache de client nem de região — cada chamada cria e fecha um client novo. Superfície
  pequena e centralizada, o que é uma boa notícia para uma futura migração.
- `AWSService.getConfig()` resolve **uma única `AWSAccountConfigDTO`** a partir de um
  `ThreadLocal<Map.Entry<String /*account*/, AWSConfigDTO>>` — o contexto atual é "por conta", nunca
  "por região". `initialize(accountKey)` só troca a conta ativa na thread.
- **RDS/EC2/Workspace listam hoje só na região única da conta ativa.** `listRds()` cria um único
  `RdsClient` e chama `describeDBInstances()` sem qualquer loop por região. O mesmo vale para
  `listEc2()`/`listWorkspaces()`.
- Os DTOs de retorno (`RDSDTO`, `EC2DTO`, `WorkspaceDTO`, `BucketDTO`) só carregam `account` — não
  têm campo `region`. Isso significa que, **mesmo depois de listar recursos em múltiplas regiões**,
  ações subsequentes (`start`/`stop`/`reboot`) não teriam hoje como saber em qual região o recurso
  está, a não ser que esses DTOs ganhem esse campo.
- **Backup/restore de RDS não depende de região da mesma forma**:
  - Postgres (`ActionRDSPostgresHelperService`) usa JDBC direto no endpoint do RDS (já resolvido,
    hostname) — não depende de região para o backup em si. Só a parte de upload/download do dump em
    S3 depende da região configurada na conta (`service.initialize(bean.getDumpFile().getAccount())`).
  - Oracle (`ActionRDSOracleHelperService`) usa `RDSADMIN.RDSADMIN_S3_TASKS.UPLOAD_TO_S3`/
    `DOWNLOAD_FROM_S3`, executado **dentro do próprio RDS** via IAM role — já é agnóstico de região do
    lado da aplicação.
- **Já existe um padrão maduro de "iterar múltiplas contas e agregar por chave"**, usado em pelo menos
  4 lugares: `RDSAWSAdminRestController.get()`, `EC2AWSAdminRestController.get()`,
  `WorkspaceAWSAdminRestController.get()` e `AWSActionService.getDatabases()` — todos fazem
  `for (Map.Entry<String, AWSAccountConfigDTO> e : accountConfigs.entrySet())`, chamando
  `service.initialize(accountKey)` e agregando resultados num `LinkedHashMap<String, List<XxxDTO>>`.
  Esse é o modelo natural a replicar numa dimensão extra (conta × região), mas exigiria um nível
  adicional de loop, não é uma mudança pontual.

## Decisões já combinadas com o usuário (a reaproveitar quando este plano for retomado)

- **Buckets S3 continuam atrelados a uma única região "principal" da conta.** `bucketVersions`,
  `bucketTempDump` e `bucketLocalMountPath` **não** viram um conjunto por região — mesmo que a conta
  passe a ter múltiplas regiões para RDS/EC2/Workspace, os buckets de versionamento/dump seguem
  resolvidos pela região única/principal já existente.

## Perguntas em aberto (não decididas — adiadas nesta rodada)

1. **Escopo de recursos**: as regiões extras valem só para RDS (o caso de uso relatado), ou também
   para EC2 e Workspaces (que usam exatamente o mesmo padrão de client-por-região)?
2. **UI de cadastro**: como o usuário vai adicionar as regiões extras na tela — uma lista simples de
   texto livre com botão de adicionar/remover (mesmo padrão já usado no File Map de
   `ambiente-modal`), ou um select/multi-select com a lista fixa de códigos de região AWS conhecidos
   (mais seguro contra erro de digitação, mas exige manter essa lista no frontend)?
3. **Ações start/stop**: uma vez que um recurso apareça listado numa região não-principal da conta,
   a tela precisa de alguma indicação visual de região, e o `start`/`stop`/`reboot` precisa levar essa
   informação de volta ao backend (hoje só manda `account`).

## Pontos de código que precisariam mudar (mapeamento por arquivo)

| Arquivo | Mudança |
|---|---|
| `AWSAccountConfigDTO.java` (backend) | `region: String` → manter como região principal + nova lista de regiões adicionais (nome a definir, ex. `additionalRegions: List<String>`) |
| `AWSAccountConfigModel` (`system-config.model.ts`, frontend) | mesmo formato espelhado do DTO |
| `AWSService.java` — `getEc2Client()`, `getRdsClient()`, `getWorkspaceClient()` | passam a aceitar um parâmetro de região explícito, em vez de sempre ler `getConfig().getRegion()` |
| `AWSService.java` — `getS3Client()` | **sem mudança de comportamento** (continua usando a região principal da conta, por decisão já tomada) |
| `AWSService.java` — `listEc2()`, `getEc2(id)`, `listRds()`, `getRDS(id)`, `listWorkspaces()`, `getWorspace(id)` | precisam iterar as regiões configuradas da conta ativa (loop interno) e agregar resultados |
| `RDSDTO`, `EC2DTO`, `WorkspaceDTO` | ganham campo `region` (hoje só têm `account`) |
| `RDSAWSAdminRestController`, `EC2AWSAdminRestController`, `WorkspaceAWSAdminRestController`, `AWSActionService.getDatabases()` | loop hoje é só por conta; precisaria virar conta × região, ou a responsabilidade de iterar regiões migra pra dentro do `AWSService` |
| `start()`/`stop()`/`reboot()` nos controllers acima | passam a receber/repassar também a região do recurso, não só a conta |
| `config-system-aws.component.ts`/`.html` (frontend) | novo campo/lista de regiões adicionais por conta, conforme decisão da pergunta em aberto #2 |

## Observação de compatibilidade

Qualquer migração de `region: String` para incluir uma lista precisa manter o campo de região
principal existente compatível com o que já está salvo em produção (contas hoje só têm 1 valor de
`region`) — a região principal atual deve virar automaticamente a "região principal" no novo formato,
sem exigir que o usuário reconfigure nada nas contas já existentes.
