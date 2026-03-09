package br.com.evolui.portalevolui.web.repository.project;

import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.ProjectModuleBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectBean, Long> {
    Optional<ProjectBean> findByIdentifier(String identifier);
    Optional<ProjectBean> findByIdOrIdentifier(Long id, String identifier);
    Optional<ProjectBean> findByRepository(String repository);

    @Query("SELECT p FROM ProjectBean p WHERE " +
            "(:identifier IS NULL OR p.identifier = :identifier) " +
            "AND (:repository IS NULL OR p.repository = :repository) " +
            "AND (:description IS NULL OR LOWER(FUNCTION('TRANSLATE', p.description, 'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ', 'aaaaaeeeeiiiioooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')) LIKE :description) " +
            "AND (:title IS NULL OR LOWER(FUNCTION('TRANSLATE', p.title, 'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ', 'aaaaaeeeeiiiioooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')) LIKE :title)")
    List<ProjectBean> findByFilter(@Param("identifier") String identifier,
                                   @Param("repository") String repository,
                                   @Param("description") String description,
                                   @Param("title") String title);

    @Modifying
    @Query("UPDATE ProjectModuleBean o SET o.bond = null WHERE o.id IN :ids")
    void clearBondsByModuleIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("DELETE FROM ProjectModuleBean o WHERE o.id IN :ids")
    void deleteModulesByIds(@Param("ids") List<Long> ids);

    @Query ("SELECT p.repository from ProjectBean p where p.identifier = :identifier")
    String getRepositoryFromIdentifier( @Param("identifier") String identifier );

    @Query("SELECT p FROM ProjectModuleBean p WHERE " +
            "(:identifier IS NULL OR p.identifier = :identifier) " +
            "AND (:description IS NULL OR LOWER(FUNCTION('TRANSLATE', p.description, 'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ', 'aaaaaeeeeiiiioooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')) LIKE :description) " +
            "AND (:title IS NULL OR LOWER(FUNCTION('TRANSLATE', p.title, 'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ', 'aaaaaeeeeiiiioooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')) LIKE :title) " +
            "AND (:main IS NULL OR p.main = :main) " +
            "AND (:repository IS NULL OR p.repository = :repository) " +
            "AND (:relativePath IS NULL OR p.relativePath = :relativePath)")
    List<ProjectModuleBean> findModulesByFilter(@Param("identifier") String identifier,
                                                @Param("description") String description,
                                                @Param("title") String title,
                                                @Param("main") Boolean main,
                                                @Param("repository") String repository,
                                                @Param("relativePath") String relativePath);
    @Query ("SELECT m from ProjectModuleBean m where m.id = :id")
    Optional<ProjectModuleBean> findModuleById(Long id);
}

