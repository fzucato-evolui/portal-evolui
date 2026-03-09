package br.com.evolui.portalevolui.web.repository.home;

import br.com.evolui.portalevolui.web.beans.DummyBean;
import br.com.evolui.portalevolui.web.repository.view.home.HomeBeansCountView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;

@Repository
public interface HomeRepository extends JpaRepository<DummyBean, Long> {
    @Query(value = "select\n" +
            "\t*\n" +
            "from\n" +
            "\t(\n" +
            "\tselect\n" +
            "\t\tCOUNT(*) as total,\n" +
            "\t\tcast( version_update.conclusion as text) as valor,\n" +
            "\t\t'atualizacao_versao' as countType,\n" +
            "\t\tenvironment.project_fk as produtoId\n" +
            "\tfrom\n" +
            "\t\tversion_update\n" +
            "\tinner join environment on\n" +
            "\t\tenvironment.id = version_update.environment_fk\n" +
            "\t\twhere version_update.request_date >= :versionDate\n" +
            "\tgroup by\n" +
            "\t\tenvironment.project_fk,\n" +
            "\t\tversion_update.conclusion\n" +
            "union all\n" +
            "\tselect\n" +
            "\t\tCOUNT(*) as total,\n" +
            "\t\tcast(version_generation.conclusion as text) as valor,\n" +
            "\t\t'geracao_versao' as countType,\n" +
            "\t\tversion_generation.project_fk as produtoId\n" +
            "\tfrom\n" +
            "\t\tversion_generation\n" +
            "\tinner join project on\n" +
            "\t\tproject.id = version_generation.project_fk\n" +
            "\twhere\n" +
            "\t\tversion_generation.request_date >= :versionDate \n" +
            "\tgroup by\n" +
            "\t\tversion_generation.project_fk,\n" +
            "\t\tversion_generation.conclusion\n" +
            "union all\n" +
            "\tselect\n" +
            "\t\tCOUNT(*) as total,\n" +
            "\t\tenvironment.tag as valor,\n" +
            "\t\t'ambiente' as countType,\n" +
            "\t\tenvironment.project_fk as produtoId\n" +
            "\tfrom\n" +
            "\t\tenvironment\n" +
            "\tgroup by\n" +
            "\t\tenvironment.project_fk,\n" +
            "\t\tenvironment.tag\n" +
            "union all\n" +
            "\tselect\n" +
            "\t\tCOUNT(*) as total,\n" +
            "\t\t'inativa' as valor,\n" +
            "\t\t'branch' as countType,\n" +
            "\t\tav.project_fk as produtoId\n" +
            "\tfrom\n" +
            "\t\t(\n" +
            "\t\tselect\n" +
            "\t\t\tdistinct available_version.branch,\n" +
            "\t\t\tavailable_version.project_fk\n" +
            "\t\tfrom\n" +
            "\t\t\tavailable_version) as av\n" +
            "\tleft join environment on\n" +
            "\t\tenvironment.branch = av.branch\n" +
            "\tinner join project on\n" +
            "\t\tproject.id = av.project_fk\n" +
            "\twhere\n" +
            "\t\tproject.framework is not true\n" +
            "\t\tand\n" +
            "\t\tenvironment.id is null\n" +
            "\tgroup by\n" +
            "\t\tav.project_fk\n" +
            "union all\n" +
            "\tselect\n" +
            "\t\tCOUNT(av.branch) as total,\n" +
            "\t\t'ativa' as valor,\n" +
            "\t\t'branch' as countType,\n" +
            "\t\tav.project_fk as produtoId\n" +
            "\tfrom\n" +
            "\t\t(\n" +
            "\t\tselect\n" +
            "\t\t\tdistinct available_version.branch,\n" +
            "\t\t\tavailable_version.project_fk\n" +
            "\t\tfrom\n" +
            "\t\t\tavailable_version\n" +
            "\t\tinner join environment on\n" +
            "\t\t\tenvironment.branch = available_version.branch\n" +
            "\t\t\tand available_version.project_fk = environment.project_fk) as av\n" +
            "\tgroup by\n" +
            "\t\tav.project_fk\n" +
            ")\n" +
            "order by\n" +
            "\tcountType,\n" +
            "\tprodutoId", nativeQuery=true)
    List<HomeBeansCountView> findAllBeansCount(Calendar versionDate);

}
