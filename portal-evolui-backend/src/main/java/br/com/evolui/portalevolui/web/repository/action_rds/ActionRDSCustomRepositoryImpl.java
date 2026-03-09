package br.com.evolui.portalevolui.web.repository.action_rds;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.repository.dto.ActionRDSFilterDTO;
import br.com.evolui.portalevolui.web.repository.user.UtilRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.query.Query;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ActionRDSCustomRepositoryImpl implements ActionRDSCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ActionRDSBean> filter(ActionRDSFilterDTO filter) throws SQLException {
        List<Predicate> predicates = new ArrayList<>();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ActionRDSBean> cr = cb.createQuery(ActionRDSBean.class);

        Metamodel m = entityManager.getMetamodel();
        EntityType<ActionRDSBean> Log_ = m.entity(ActionRDSBean.class);
        EntityType<ActionRDSBean> User_ = m.entity(ActionRDSBean.class);

        Root<ActionRDSBean> root = cr.from(ActionRDSBean.class);
        Join<ActionRDSBean, UserBean> user = root.join("user", JoinType.INNER);

        if (filter != null) {
            UtilRepository util = new UtilRepository().using(this.entityManager);
            if (filter.getId() != null && filter.getId() > 0L) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }
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
            if (StringUtils.hasText(filter.getRds())) {
                predicates.add(util.filterStringLikeIgnoreCaseAndAccents(root, "rds", filter.getRds()));
            }
            if (filter.getActionType() != null) {
                predicates.add(cb.equal(root.get("actionType"), filter.getActionType()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getConclusion() != null) {
                predicates.add(cb.equal(root.get("conclusion"), filter.getConclusion()));
            }

        }
        cr.where(cb.and(predicates.toArray(new Predicate[0])));
        cr.orderBy(cb.desc(root.get("requestDate")));
        cr.select(root);

        Query<ActionRDSBean> query = (Query<ActionRDSBean>) this.entityManager.createQuery(cr);
        return query.getResultList();
    }
}
