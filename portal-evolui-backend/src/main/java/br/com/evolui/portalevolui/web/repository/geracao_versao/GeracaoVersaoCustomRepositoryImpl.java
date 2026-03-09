package br.com.evolui.portalevolui.web.repository.geracao_versao;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.GeracaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.repository.dto.geracao_versao.GeracaoVersaoFilterDTO;
import br.com.evolui.portalevolui.web.repository.user.UtilRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.query.Query;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GeracaoVersaoCustomRepositoryImpl implements GeracaoVersaoCustomRepository {

    @PersistenceContext
    protected EntityManager entityManager;
    protected CriteriaBuilder cb;
    protected CriteriaQuery<GeracaoVersaoBean> cr;
    protected Metamodel m;
    protected Root<GeracaoVersaoBean> root;
    protected Join<GeracaoVersaoBean, UserBean> user;
    protected Join<GeracaoVersaoBean, ProjectBean> project;
    protected Join<GeracaoVersaoBean, GeracaoVersaoModuloBean> modules;
    protected UtilRepository util;

    protected void initialize() {
        this.cb = entityManager.getCriteriaBuilder();
        this.cr = cb.createQuery(GeracaoVersaoBean.class);
        this.m = entityManager.getMetamodel();
        this.root = cr.from(GeracaoVersaoBean.class);
        this.user = root.join("user", JoinType.INNER);
        this.project = root.join("project", JoinType.INNER);
        this.modules = root.join("modules", JoinType.INNER);
    }
    @Override
    public List<GeracaoVersaoBean> filter(String produto, GeracaoVersaoFilterDTO filter) throws SQLException {
        this.initialize();
        List<Predicate> predicates = this.createBasicPredicates(produto, filter);

        if (filter != null) {

            if (StringUtils.hasText(filter.getVersion())) {
                predicates.add(cb.or(
                        util.filterStringLikeIgnoreCaseAndAccents(modules, "tag", filter.getVersion()),
                        util.filterStringLikeIgnoreCaseAndAccents(root, "tag", filter.getVersion())
                        ));
            }
            /*
            if (filter.getIncludeTributos() != null) {
                predicates.add(cb.equal(root.get("includeTributos"), filter.getIncludeTributos()));
            }
            if (filter.getIncludePortalPrincipal() != null) {
                predicates.add(cb.equal(root.get("includePortalPrincipal"), filter.getIncludePortalPrincipal()));
            }
            if (filter.getIncludePortalImobiliario() != null) {
                predicates.add(cb.equal(root.get("includePortalImobiliario"), filter.getIncludePortalImobiliario()));
            }
            if (filter.getIncludePortalContribuinte() != null) {
                predicates.add(cb.equal(root.get("includePortalContribuinte"), filter.getIncludePortalContribuinte()));
            }
            if (filter.getIncludePortalSocioeconomico() != null) {
                predicates.add(cb.equal(root.get("includePortalSocioeconomico"), filter.getIncludePortalSocioeconomico()));
            }
            if (filter.getIncludeItbiOnline() != null) {
                predicates.add(cb.equal(root.get("includeItbiOnline"), filter.getIncludeItbiOnline()));
            }
            if (filter.getIncludeIntegradorIss() != null) {
                predicates.add(cb.equal(root.get("includeIntegradorIss"), filter.getIncludeIntegradorIss()));
            }
            
             */

        }
        cr.where(cb.and(predicates.toArray(new Predicate[0])));
        cr.distinct(true);
        cr.select(root);

        Query<GeracaoVersaoBean> query = (Query<GeracaoVersaoBean>) this.entityManager.createQuery(cr);

        return query.getResultList();
    }

    public List<Predicate> createBasicPredicates(String target, GeracaoVersaoFilterDTO filter) throws SQLException {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(project.get("identifier"), target));
        if (filter != null) {
            this.util = new UtilRepository().using(this.entityManager);

            if (filter.getRequestDateFrom() != null) {
                Calendar c = filter.getRequestDateFrom();
                c.set(Calendar.HOUR, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestDate"), c));
            }
            if (filter.getRequestDateTo() != null) {
                Calendar c = filter.getRequestDateTo();
                c.set(Calendar.HOUR, 23);
                c.set(Calendar.MINUTE, 59);
                c.set(Calendar.SECOND, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("requestDate"), c));
            }
            if (StringUtils.hasText(filter.getUserEmail())) {
                predicates.add(util.filterStringLikeIgnoreCaseAndAccents(user, "email", filter.getUserEmail()));
            }
            if (StringUtils.hasText(filter.getUserName())) {
                predicates.add(util.filterStringLikeIgnoreCaseAndAccents(user, "name", filter.getUserName()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getConclusion() != null) {
                predicates.add(cb.equal(root.get("conclusion"), filter.getConclusion()));
            }
        }
        cr.orderBy(cb.desc(root.get("id")));
        return predicates;
    }

}
