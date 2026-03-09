package br.com.evolui.portalevolui.web.repository.atualizacao_versao;

import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtualizacaoVersaoRepository extends JpaRepository<AtualizacaoVersaoBean, Long>, AtualizacaoVersaoCustomRepository {
    List<AtualizacaoVersaoBean> findAllByEnvironmentProjectIdentifier(String id);
    List<AtualizacaoVersaoBean> findAllByStatusNot(GithubActionStatusEnum status);
    List<AtualizacaoVersaoBean> findAllByStatusNotAndEnvironmentProjectIdentifierAndWorkflowIsNotNull(GithubActionStatusEnum status, String project);
    Optional<AtualizacaoVersaoBean> findByHashTokenAndWorkflowAndStatusNot(String token,
                                                                           Long workflowId, GithubActionStatusEnum status);
    Optional<AtualizacaoVersaoBean> findByStatusNot(GithubActionStatusEnum status);
    long countByStatusNotAndEnvironmentProjectIdentifierAndWorkflowIsNotNull(GithubActionStatusEnum status, String project);
    Optional<AtualizacaoVersaoBean> findByWorkflow(Long workflowId);
    long countByStatusNotAndEnvironmentIdAndIdNot(GithubActionStatusEnum status, Long idAmbiente, Long id);

    List<AtualizacaoVersaoBean> findAllBySchedulerDateAfterAndStatus(Calendar scheduler, GithubActionStatusEnum status);

    List<AtualizacaoVersaoBean> findAllByConclusionAndEnvironmentIdOrderByConclusionDateDesc(GithubActionConclusionEnum conclusion, Long idAmbiente);

    List<AtualizacaoVersaoBean> findAllByConclusionAndEnvironmentProjectIdentifierOrderByConclusionDateDesc(GithubActionConclusionEnum conclusion, String project);

    List<AtualizacaoVersaoBean> findAllByRequestDateBeforeAndStatus(Calendar limitDate, GithubActionStatusEnum status);
}
