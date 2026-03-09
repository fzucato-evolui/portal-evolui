package br.com.evolui.portalevolui.web.repository.log_aws;

import br.com.evolui.portalevolui.web.beans.LogAWSActionBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.repository.dto.LogAWSActionFilterDTO;
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

public class LogAWSActionCustomRepositoryImpl implements LogAWSActionCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<LogAWSActionBean> filter(LogAWSActionFilterDTO filter) throws SQLException {
        List<Predicate> predicates = new ArrayList<>();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LogAWSActionBean> cr = cb.createQuery(LogAWSActionBean.class);

        Metamodel m = entityManager.getMetamodel();
        EntityType<LogAWSActionBean> Log_ = m.entity(LogAWSActionBean.class);
        EntityType<LogAWSActionBean> User_ = m.entity(LogAWSActionBean.class);

        Root<LogAWSActionBean> root = cr.from(LogAWSActionBean.class);
        Join<LogAWSActionBean, UserBean> user = root.join("user", JoinType.INNER);

        if (filter != null) {
            UtilRepository util = new UtilRepository().using(this.entityManager);
            if (filter.getId() != null && filter.getId() > 0L) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }
            if (filter.getLogDateFrom() != null) {
                Calendar c = filter.getLogDateFrom();
                c.set(Calendar.HOUR, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                predicates.add(cb.greaterThanOrEqualTo(root.get("logDate"), c));
            }
            if (filter.getLogDateTo() != null) {
                Calendar c = filter.getLogDateTo();
                c.set(Calendar.HOUR, 23);
                c.set(Calendar.MINUTE, 59);
                c.set(Calendar.SECOND, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("logDate"), c));
            }
            if (StringUtils.hasText(filter.getUserEmail())) {
                predicates.add(util.filterStringLikeIgnoreCaseAndAccents(user, "email", filter.getUserEmail()));
            }
            if (StringUtils.hasText(filter.getUserName())) {
                predicates.add(util.filterStringLikeIgnoreCaseAndAccents(user, "name", filter.getUserName()));
            }
            if (filter.getActionType() != null) {
                predicates.add(cb.equal(root.get("actionType"), filter.getActionType()));
            }

        }
        cr.where(cb.and(predicates.toArray(new Predicate[0])));
        cr.select(root);

        Query<LogAWSActionBean> query = (Query<LogAWSActionBean>) this.entityManager.createQuery(cr);
        return query.getResultList();
    }
}
