package br.com.evolui.portalevolui.web.repository.geracao_versao;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeracaoVersaoRepository extends JpaRepository<GeracaoVersaoBean, Long>, GeracaoVersaoCustomRepository {
    List<GeracaoVersaoBean> findAllByStatusNotAndProjectIdentifier(GithubActionStatusEnum status, String project);
    List<GeracaoVersaoBean> findAllByProjectIdentifier(String project);
    Optional<GeracaoVersaoBean> findByHashTokenAndWorkflowAndStatusNot(String token, Long workflowId, GithubActionStatusEnum status);
    Optional<GeracaoVersaoBean> findByStatusNot(GithubActionStatusEnum status);
    long countByStatusNotAndProjectIdentifier(GithubActionStatusEnum status, String project);
    Optional<GeracaoVersaoBean> findByWorkflow(Long workflowId);
    List<GeracaoVersaoBean> findAllByBranchAndProjectIdentifierOrderByBuildDesc(String branch, String project);
    List<GeracaoVersaoBean> findAllByRequestDateBeforeAndStatus(Calendar limitDate, GithubActionStatusEnum status);
}
