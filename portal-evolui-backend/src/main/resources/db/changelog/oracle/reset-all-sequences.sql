BEGIN
FOR r IN (
        SELECT s.sequence_name, s.last_number, NVL(MAX(t.ID),0) AS max_id
        FROM user_sequences s
        JOIN user_tables t ON s.sequence_name = t.table_name || '_SEQUENCE'
        GROUP BY s.sequence_name, s.last_number
    ) LOOP
        EXECUTE IMMEDIATE
            'ALTER SEQUENCE ' || r.sequence_name || ' INCREMENT BY ' || (r.max_id + 1 - r.last_number);
EXECUTE IMMEDIATE 'SELECT ' || r.sequence_name || '.nextval FROM dual';
EXECUTE IMMEDIATE 'ALTER SEQUENCE ' || r.sequence_name || ' INCREMENT BY 1';
END LOOP;
END;
/
