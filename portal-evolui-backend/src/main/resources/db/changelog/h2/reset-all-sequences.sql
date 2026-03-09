CREATE ALIAS RESET_SEQUENCES AS $$
import java.sql.*;
@CODE
void reset(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'")) {
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            try (Statement alterStmt = conn.createStatement()) {
                // H2 suporta subqueries no RESTART WITH em versões recentes
                alterStmt.execute("ALTER SEQUENCE " + tableName + "_SEQUENCE RESTART WITH (SELECT COALESCE(MAX(ID), 0) + 1 FROM " + tableName + ")");
} catch (SQLException e) {
                // Ignora tabelas que não possuem a sequência correspondente
            }
        }
    }
}
$$;
CALL RESET_SEQUENCES();
DROP ALIAS RESET_SEQUENCES;
