SELECT utc.table_name
FROM user_tab_columns utc
WHERE UPPER(utc.column_name) = UPPER('NOM_COLONNE_RECHERCHEE')
ORDER BY utc.table_name;

SELECT utc.table_name
FROM user_tab_columns utc
JOIN user_tables ut
  ON ut.table_name = utc.table_name
WHERE UPPER(utc.column_name) = UPPER('NOM_COLONNE_RECHERCHEE')
ORDER BY utc.table_name;


WITH relevant_parent_constraints AS (
    SELECT
        uc.table_name,
        uc.constraint_name,
        uc.constraint_type
    FROM user_constraints uc
    JOIN user_cons_columns ucc
      ON ucc.constraint_name = uc.constraint_name
     AND ucc.table_name      = uc.table_name
    WHERE uc.constraint_type IN ('P', 'U')
      AND ucc.column_name = UPPER('CODE_REF')
),
fk_edges AS (
    SELECT
        p.table_name              AS parent_table,
        p.constraint_name         AS parent_constraint,
        c.table_name              AS child_table,
        c.constraint_name         AS child_fk_constraint,
        LISTAGG(pc.column_name, ', ')
            WITHIN GROUP (ORDER BY pc.position) AS parent_columns,
        LISTAGG(cc.column_name, ', ')
            WITHIN GROUP (ORDER BY cc.position) AS child_columns
    FROM user_constraints p
    JOIN user_cons_columns pc
      ON pc.constraint_name = p.constraint_name
     AND pc.table_name      = p.table_name
    JOIN user_constraints c
      ON c.r_constraint_name = p.constraint_name
     AND c.constraint_type   = 'R'
    JOIN user_cons_columns cc
      ON cc.constraint_name = c.constraint_name
     AND cc.table_name      = c.table_name
     AND cc.position        = pc.position
    WHERE p.constraint_type IN ('P', 'U')
    GROUP BY
        p.table_name,
        p.constraint_name,
        c.table_name,
        c.constraint_name
),
edges_with_next_parent AS (
    SELECT
        e.parent_table,
        e.parent_constraint,
        e.parent_columns,
        e.child_table,
        e.child_fk_constraint,
        e.child_columns,
        rp.constraint_name AS next_parent_constraint
    FROM fk_edges e
    LEFT JOIN relevant_parent_constraints rp
      ON rp.table_name = e.child_table
)
SELECT
    LEVEL AS niveau,
    e.parent_table,
    e.parent_constraint,
    e.parent_columns,
    e.child_table,
    e.child_fk_constraint,
    e.child_columns,
    e.next_parent_constraint,
    LTRIM(SYS_CONNECT_BY_PATH(
        e.parent_constraint || ' -> ' || e.child_fk_constraint,
        ' | '
    ), ' | ') AS chemin_contraintes
FROM edges_with_next_parent e
START WITH e.parent_constraint = UPPER('PK_T_PARENT')
CONNECT BY NOCYCLE PRIOR e.next_parent_constraint = e.parent_constraint
ORDER BY niveau, parent_constraint, child_table, child_fk_constraint;


