package br.com.evolui.portalevolui.web.repository.versao;

import br.com.evolui.portalevolui.web.beans.VersaoBean;
import br.com.evolui.portalevolui.web.beans.VersaoBranchBaseBean;
import br.com.evolui.portalevolui.web.beans.VersaoBuildBaseBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersaoRepository extends JpaRepository<VersaoBean, Long> {
    @Query("SELECT v FROM #{#entityName} v WHERE v.major = :#{#build.major} AND v.minor = :#{#build.minor} AND v.patch = :#{#build.patch} AND v.build = :#{#build.build}")
    Optional<VersaoBean> findByBuild(@Param("build") VersaoBuildBaseBean vb);

    @Query("SELECT v FROM VersaoBean v WHERE v.project.id = :#{#project} AND  v.major = :#{#build.major} AND v.minor = :#{#build.minor} AND v.patch = :#{#build.patch} AND v.build = :#{#build.build}")
    Optional<VersaoBean> findByBuildAndProjectId(@Param("build") VersaoBuildBaseBean vb, @Param("project") String project);

    @Query("SELECT v FROM #{#entityName} v WHERE v.major = :#{#branch.major} AND v.minor = :#{#branch.minor} AND v.patch = :#{#branch.patch}")
    List<VersaoBean> findAllByBranch(@Param("branch") VersaoBranchBaseBean vb);

    @Query("DELETE FROM #{#entityName} v WHERE v.major = :#{#build.major} AND v.minor = :#{#build.minor} AND v.patch = :#{#build.patch} AND v.build = :#{#build.build}")
    void deleteByBuild(@Param("build") VersaoBuildBaseBean vb);

    @Query("DELETE FROM #{#entityName} v WHERE v.major = :#{#branch.major} AND v.minor = :#{#branch.minor} AND v.patch = :#{#branch.patch}")
    void deleteAllByBranchDetailed(@Param("branch") VersaoBranchBaseBean vb);

    void deleteAllByBranchAndProjectIdentifier(String branch, String project);
    List<VersaoBean> findAllByProjectIdentifier(String project);

    List<VersaoBean> findAllByProjectIdentifierOrderByMajorDescMinorDescPatchDescVersionTypeAscBuildDesc(String project);

    Optional<VersaoBean> findByTagAndProjectIdentifier(String tag, String project);

    List<VersaoBean> findAllByBranchAndProjectIdentifier(String branch, String project);
    List<VersaoBean> findAllByBranchAndProjectIdentifierOrderByVersionTypeAscBuildDesc(String branch, String project);
    @Query("""
    select v
    from VersaoBean v
    where v.project.identifier = :project
      and (v.qualifier is null or v.qualifier = '')
    order by
      v.major desc,
      v.minor desc,
      v.patch desc,
      v.build desc
""")
    List<VersaoBean> findStableVersions(@Param("project") String project, Pageable pageable);

    default Optional<VersaoBean> findLastStableVersion(String project) {
        List<VersaoBean> results = findStableVersions(project, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    Optional<VersaoBean> findFirstByProjectIdentifierOrderByMajorDescMinorDescPatchDescBuildDesc(String project);
}
