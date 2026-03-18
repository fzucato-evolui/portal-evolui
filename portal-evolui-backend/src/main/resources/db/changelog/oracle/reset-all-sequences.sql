DECLARE
v_next_id NUMBER;
    v_dummy NUMBER;
BEGIN
FOR r IN (
        SELECT
            t.table_name,
            s.sequence_name,
            s.last_number
        FROM user_sequences s
        JOIN user_tables t
            ON s.sequence_name = t.table_name || '_SEQUENCE'
    ) LOOP
        EXECUTE IMMEDIATE
            'SELECT NVL(MAX(ID), 0) + 1 FROM ' || r.table_name
            INTO v_next_id;

        IF v_next_id <> r.last_number THEN
            EXECUTE IMMEDIATE
                'ALTER SEQUENCE ' || r.sequence_name || ' INCREMENT BY ' || (v_next_id - r.last_number);

EXECUTE IMMEDIATE
    'SELECT ' || r.sequence_name || '.NEXTVAL FROM dual'
    INTO v_dummy;

EXECUTE IMMEDIATE
    'ALTER SEQUENCE ' || r.sequence_name || ' INCREMENT BY 1';
END IF;
END LOOP;
END;
/
