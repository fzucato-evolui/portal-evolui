package br.com.evolui.portalevolui.web.repository.user;

import br.com.evolui.portalevolui.web.repository.enums.DatabaseTypeEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.hibernate.internal.SessionImpl;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.concurrent.atomic.AtomicReference;

public class UtilRepository {
    private EntityManager entityManager;
    private CriteriaBuilder cb;
    public UtilRepository using(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.cb = this.entityManager.getCriteriaBuilder();
        return this;
    }

    public Predicate filterStringLikeIgnoreCaseAndAccents(From table, String field, String value) throws SQLException {
        Expression<String> func = null;
        if (this.checkDatabase() == DatabaseTypeEnum.H2 || this.checkDatabase() == DatabaseTypeEnum.POSTGRES) {
            func = cb.function("translate", String.class, cb.upper(table.<String>get(field)),
                    cb.literal("ŠŽšžŸÁÇÉÍÓÚÀÈÌÒÙÂÊÎÔÛÃÕËÜÏÖÑÝåáçéíóúàèìòùâêîôûãõëüïöñýÿ"), cb.literal("SZszYACEIOUAEIOUAEIOUAOEUIONYaaceiouaeiouaeiouaoeuionyy"));
        }
        return cb.like(func, "%" + removerAcentos(value).toUpperCase() + "%");

    }

    String filterStringLikeIgnoreCaseAndAccents(String table, String field, String value) throws SQLException {
        if (!StringUtils.hasText(table)) {
            table = "";
        } else {
            table = table + ".";
        }
        String filter = "%s LIKE '%%%s%%'";
        if (this.checkDatabase() == DatabaseTypeEnum.H2) {
            filter = String.format(filter, "TRANSLATE(UPPER(" + table + field + "), 'ŠŽšžŸÁÇÉÍÓÚÀÈÌÒÙÂÊÎÔÛÃÕËÜÏÖÑÝåáçéíóúàèìòùâêîôûãõëüïöñýÿ', " +
                    "'SZszYACEIOUAEIOUAEIOUAOEUIONYaaceiouaeiouaeiouaoeuionyy')", removerAcentos(value).toUpperCase() );
        }
        return filter;

    }

    Predicate filterStringEqualIgnoreCase(Root table, String field, String value) throws SQLException {
        return cb.equal(cb.upper(table.get(field)), value.toUpperCase());
    }

    String filterStringEqualIgnoreCase(String table, String field, String value) throws SQLException {
        if (!StringUtils.hasText(table)) {
            table = "";
        } else {
            table = table + ".";
        }
        String filter = "%s LIKE '%%%s%%'";

        filter = String.format(filter, "UPPER(" + table + field + ")",  removerAcentos(value).toUpperCase());
        return filter;
    }

    public DatabaseTypeEnum checkDatabase() throws SQLException {
        SessionImpl hibernateSession = entityManager.unwrap(SessionImpl.class);
        AtomicReference<String> productName = new AtomicReference<>();
         hibernateSession.doWork(x -> productName.set(x.getMetaData().getDatabaseProductName()));
        return DatabaseTypeEnum.fromValue(productName.get());
    }

    public static String removerAcentos(String texto) {
        if (texto == null) {
            return "";
        }
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return texto;
    }
}
