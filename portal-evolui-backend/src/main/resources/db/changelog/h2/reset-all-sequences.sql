CREATE ALIAS RESET_SEQUENCES AS $$
import java.sql.*;

@CODE
void reset(Connection conn) throws SQLException {
    String sql =
        "SELECT t.TABLE_NAME, s.SEQUENCE_NAME " +
        "FROM INFORMATION_SCHEMA.SEQUENCES s " +
        "JOIN INFORMATION_SCHEMA.TABLES t " +
        "  ON t.TABLE_SCHEMA = s.SEQUENCE_SCHEMA " +
        " AND s.SEQUENCE_NAME = t.TABLE_NAME || '_SEQUENCE' " +
        "WHERE s.SEQUENCE_SCHEMA = 'PUBLIC' " +
        "  AND t.TABLE_SCHEMA = 'PUBLIC' " +
        "  AND t.TABLE_TYPE = 'BASE TABLE'";

    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            String sequenceName = rs.getString("SEQUENCE_NAME");

            long nextValue = 1L;

            try (Statement maxStmt = conn.createStatement();
                 ResultSet maxRs = maxStmt.executeQuery(
                     "SELECT COALESCE(MAX(ID), 0) + 1 FROM " + tableName)) {
                if (maxRs.next()) {
                    nextValue = maxRs.getLong(1);
}
            }

            try (Statement alterStmt = conn.createStatement()) {
                alterStmt.execute(
                    "ALTER SEQUENCE " + sequenceName + " RESTART WITH " + nextValue);
}
        }
    }
}
$$;
CALL RESET_SEQUENCES();
DROP ALIAS RESET_SEQUENCES;
