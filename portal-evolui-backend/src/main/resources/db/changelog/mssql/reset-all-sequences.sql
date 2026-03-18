DECLARE
@schemaName SYSNAME,
    @tableName SYSNAME,
    @sequenceName SYSNAME,
    @nextValue BIGINT,
    @sql NVARCHAR(MAX);

DECLARE sequence_cursor CURSOR FOR
SELECT
    s.name AS schema_name,
    t.name AS table_name,
    seq.name AS sequence_name
FROM sys.sequences seq
         JOIN sys.tables t
              ON seq.name = t.name + '_sequence'
         JOIN sys.schemas s
              ON s.schema_id = seq.schema_id
                  AND t.schema_id = s.schema_id;

OPEN sequence_cursor;

FETCH NEXT FROM sequence_cursor INTO @schemaName, @tableName, @sequenceName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'SELECT @nextValue = ISNULL(MAX(ID), 0) + 1 FROM '
        + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName);

EXEC sp_executesql
        @sql,
        N'@nextValue BIGINT OUTPUT',
        @nextValue OUTPUT;

    SET @sql = N'ALTER SEQUENCE '
        + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@sequenceName)
        + N' RESTART WITH ' + CAST(@nextValue AS NVARCHAR(20));

EXEC sp_executesql @sql;

FETCH NEXT FROM sequence_cursor INTO @schemaName, @tableName, @sequenceName;
END;

CLOSE sequence_cursor;
DEALLOCATE sequence_cursor;
