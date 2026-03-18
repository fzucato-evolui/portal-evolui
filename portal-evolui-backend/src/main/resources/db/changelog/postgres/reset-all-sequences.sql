DO $$
DECLARE
r RECORD;
BEGIN
FOR r IN
SELECT
    tbl.table_name,
    seq.sequence_schema,
    seq.sequence_name
FROM information_schema.sequences seq
         JOIN information_schema.tables tbl
              ON tbl.table_schema = seq.sequence_schema
                  AND seq.sequence_name = tbl.table_name || '_sequence'
WHERE seq.sequence_schema = 'public'
  AND tbl.table_type = 'BASE TABLE'
    LOOP
        EXECUTE format(
            'SELECT setval(%L, COALESCE(MAX(id), 0) + 1, false) FROM %I.%I',
            r.sequence_schema || '.' || r.sequence_name,
            'public',
            r.table_name
        );
END LOOP;
END;
$$;
