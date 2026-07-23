---
title: Plano de migração de versionamento (legado 4 posições + novo 3 posições)
description: Plano oficial no repositório para guiar implementação no idp-evolui e no workflow-regente-full, alinhado ao contrato docs/contracts/versioning-workflow-contract.md.
---

# Plano: Migração de versionamento com contrato cross-repo

## Objetivo

Implementar versionamento dinâmico por projeto, mantendo compatibilidade com o formato legado de 4 posições e habilitando o novo formato de 3 posições, com rollout coordenado entre:

- `idp-evolui` (emissor/gestor de versões);
- `workflow-regente-full` (consumidor para comparação, geração de artefatos e atualização de ambiente).

Referência obrigatória de contrato:

- [`docs/contracts/versioning-workflow-contract.md`](../contracts/versioning-workflow-contract.md)

## Escopo funcional

- Suportar dois formatos:
  - legado: `major.minor.patch.build[-QUALIFIER]`;
  - novo: `major.minor.patch[-QUALIFIER]`.
- No novo formato:
  - `stable` inicia com `patch = 0`;
  - `patch` incrementa `patch`;
  - `rc/beta/alpha` usam `patch` com timestamp + qualifier.
- Aplicar bloqueios de compileType (`rc`, `beta`, `alpha`) conforme hierarquia definida.
- Impor regra de transição de formato:
  - só permitir troca quando a próxima geração avançar o par `major.minor`.

## Entregáveis

- Configuração `SystemConfigTypeEnum.VERSIONING` no backend com JSON por projeto.
- Refatoração de parsing/composição de versões para coexistência 3/4 posições.
- Ajustes de validação e geração no backend.
- Ajustes de agrupamento/sugestão/validação no frontend.
- Evolução do payload dos workflows com metadados de contrato/formato.
- Atualização do `workflow-regente-full` para suporte dual (novo + legado).

## Plano de execução

## Fase 1 — Contrato e compatibilidade (cross-repo)

- Validar e congelar o contrato em [`docs/contracts/versioning-workflow-contract.md`](../contracts/versioning-workflow-contract.md).
- Definir `contractVersion` inicial da mudança.
- Garantir fallback legado no consumidor (`workflow-regente-full`) antes de alterar emissor.

## Fase 2 — Backend idp-evolui

- Criar suporte de configuração `VERSIONING` por projeto.
- Integrar parser/serializer de `SystemConfig` para o novo DTO.
- Resolver formato do projeto no início dos fluxos de:
  - geração de versão;
  - cicd;
  - atualização de versão.
- Refatorar lógica em beans de versão para suportar 3/4 posições.
- Implementar validação de transição baseada no histórico (`major.minor`).

## Fase 3 — Frontend idp-evolui

- Ajustar `EvoluiVersionModel` para parsing/comparação em ambos formatos.
- Ajustar telas de geração/listagem para não assumir estrutura fixa antiga.
- Expor config de `VERSIONING` com fallback legado.

## Fase 4 — Workflow-regente-full

- Atualizar parser para ler `contractVersion` e `versioningFormat`.
- Selecionar estratégia por formato:
  - `LEGACY_4_PARTS`;
  - `SEMVER_3_PARTS`.
- Manter fallback para payload antigo sem metadados.
- Aplicar lógica coerente em comparação e geração de artefatos.

## Fase 5 — Rollout e estabilização

- Executar ordem de rollout:
  1. `workflow-regente-full` com suporte dual;
  2. `idp-evolui` enviando novo contrato;
  3. janela de convivência com monitoramento;
  4. endurecimento opcional após estabilização.

## Matriz resumida de impacto

- Backend:
  - `SystemConfigTypeEnum`, `SystemConfigBean`, `SystemConfigBeanDeserializer`;
  - `VersaoBranchBaseBean`, `VersaoBuildBaseBean`, `GeracaoVersaoAdminRestController`;
  - DTOs de workflow e serviços de dispatch.
- Frontend:
  - `evolui-version.model.ts`;
  - telas de geração/listagem de versão;
  - modelos de configuração de sistema.
- Workflow:
  - parser dos DTOs de versão e decisões de comparação/artefato.

## Critérios de aceite

- Projeto com formato legado permanece funcionando sem regressão.
- Projeto com formato novo gera versões conforme regras aprovadas.
- Troca de formato respeita bloqueio de avanço `major.minor`.
- Payload contém metadados de contrato/formato quando esperado.
- `workflow-regente-full` processa payload novo e antigo na mesma janela de release.

## Handoff para agentes

Sempre instruir o agente a:

1. Ler primeiro:
   - [`docs/contracts/versioning-workflow-contract.md`](../contracts/versioning-workflow-contract.md)
   - [`docs/plans/versioning-format-migration-plan.md`](./versioning-format-migration-plan.md)
2. Implementar alterações deste repositório mantendo compatibilidade retroativa.
3. Atualizar documentação de contrato/plano sempre que alterar payload.
4. Reportar no final:
   - o que mudou;
   - como ficou a compatibilidade legado/novo;
   - quais critérios de aceite foram atendidos.

---

Ao concluir alterações de código, a pessoa humana deve compilar e validar localmente os fluxos impactados (build, testes e cenários funcionais pertinentes).

