package br.com.evolui.portalevolui.web.repository.user;

import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.repository.dto.UserBeanFilterDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.Query;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserCustomRepositoryImpl implements UserCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<UserBean> filter(UserBeanFilterDTO filter) throws SQLException {
        List<Predicate> predicates = new ArrayList<>();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserBean> cr = cb.createQuery(UserBean.class);
        Root<UserBean> root = cr.from(UserBean.class);

        if (filter != null) {
            UtilRepository util = new UtilRepository().using(this.entityManager);
            if (filter.getId() != null && filter.getId() > 0L) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }
            if (StringUtils.hasText(filter.getLogin())) {
                predicates.add(util.filterStringEqualIgnoreCase(root, "login", filter.getLogin()));
            }
            if (StringUtils.hasText(filter.getName())) {
                predicates.add(util.filterStringLikeIgnoreCaseAndAccents(root, "name", filter.getName()));
            }
            if (StringUtils.hasText(filter.getEmail())) {
                predicates.add(util.filterStringLikeIgnoreCaseAndAccents(root, "email", filter.getEmail()));
            }
            if (filter.getProfile() != null) {
                predicates.add(cb.equal(root.get("profile"), filter.getProfile()));
            }
            if (filter.getEnabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), filter.getEnabled()));
            }

        }
        cr.where(cb.and(predicates.toArray(new Predicate[0])));
        cr.select(root);

        Query<UserBean> query = (Query<UserBean>) this.entityManager.createQuery(cr);
        return query.getResultList();
    }
}
