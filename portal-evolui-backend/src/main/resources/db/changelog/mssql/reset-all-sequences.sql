DECLARE @sql NVARCHAR(MAX) = '';

SELECT @sql = @sql +
              'ALTER SEQUENCE ' + s.name + '.' + seq.name +
              ' RESTART WITH ' + CAST(ISNULL(MAX(t.ID),0)+1 AS NVARCHAR) + ';'
FROM sys.sequences seq
         JOIN sys.tables t ON seq.name = t.name + '_sequence'
         JOIN sys.schemas s ON s.schema_id = seq.schema_id
GROUP BY s.name, seq.name;

EXEC sp_executesql @sql;
