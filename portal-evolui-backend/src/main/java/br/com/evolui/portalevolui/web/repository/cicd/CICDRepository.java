package br.com.evolui.portalevolui.web.repository.cicd;

import br.com.evolui.portalevolui.web.beans.CICDBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Repository
public interface CICDRepository extends JpaRepository<CICDBean, Long>, CICDCustomRepository {
    List<CICDBean> findAllByStatusNotAndBranchAndProjectIdentifier(GithubActionStatusEnum status, String branch, String project);
    List<CICDBean> findAllByProjectIdentifier(String project);
    Optional<CICDBean> findByHashTokenAndWorkflowAndStatusNot(String token, Long workflowId, GithubActionStatusEnum status);
    List<CICDBean> findAllByStatusNotAndProjectIdentifier(GithubActionStatusEnum status, String project);
    long countByStatusNotAndBranchAndProjectId(GithubActionStatusEnum status, String branch, Long project);
    Optional<CICDBean> findByWorkflow(Long workflowId);
    @Query("select m.commit from CICDModuloBean m where m.commit is not null and m.projectModule.id=:productId and m.branch=:branch order by m.id desc")
    List<Object> findLastCommitModuleBranch(Pageable limit, @Param("productId") Long productId, @Param("branch") String branch);
    List<CICDBean> findAllByRequestDateBefore(Calendar limitDate);
    List<CICDBean> findAllByRequestDateBeforeAndStatus(Calendar limitDate, GithubActionStatusEnum status);
    List<CICDBean> findAllByConclusionDateAfterOrderByProjectIdAscBranchAscConclusionDateAsc(Calendar limitDate);

}
