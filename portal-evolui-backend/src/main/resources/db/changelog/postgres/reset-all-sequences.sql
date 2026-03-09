DO $$
DECLARE
r RECORD;
BEGIN
FOR r IN
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
    LOOP
BEGIN
EXECUTE format(
        'SELECT setval(''%I_sequence'', coalesce(max(id),0)+1, false) FROM %I',
        r.table_name,
        r.table_name
        );
EXCEPTION WHEN undefined_table THEN
            -- Ignora se a tabela não existir ou não tiver coluna id
END;
END LOOP;
END;
$$;
