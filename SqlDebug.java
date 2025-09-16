package util;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Utilitaires de debug JDBC (Oracle-friendly).
 * - Capture des paramètres via Proxy PreparedStatement
 * - Extraction du SQL "template" (avec ?) par réflexion si Oracle
 * - Rendu "lisible" du SQL avec valeurs (pour debug uniquement)
 */
public final class SqlDebug {

    private SqlDebug() {}

    /* ======== API PUBLIQUE ======== */

    /** Crée un PreparedStatement proxy qui capture les paramètres. */
    public static PreparedStatement wrap(Connection cn, String sql) throws SQLException {
        PreparedStatement delegate = cn.prepareStatement(sql);
        return wrap(delegate);
    }

    /** Enveloppe un PreparedStatement existant pour capturer les paramètres. */
    public static PreparedStatement wrap(PreparedStatement delegate) {
        Objects.requireNonNull(delegate, "delegate");
        ParamStore store = new ParamStore();
        InvocationHandler h = (proxy, method, args) -> {
            String name = method.getName();
            // Intercepter tous les setXxx(index, value, [type...])
            if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                int idx = (Integer) args[0];
                store.record(name, idx, Arrays.copyOfRange(args, 1, args.length));
            }
            // clearParameters()
            if (name.equals("clearParameters")) {
                store.clear();
            }
            // Exposer la boutique via méthode spéciale (pas dans l'API JDBC)
            if (name.equals("__sqldebug_params")) {
                return store;
            }
            return method.invoke(delegate, args);
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                h
        );
    }

    /** Renvoie le SQL "template" (avec ?) si possible, sinon null. */
    public static String sqlTemplate(PreparedStatement ps) {
        // Essai via Oracle unwrap + méthodes internes, ou champ sqlObject
        String sql = tryOracleExtract(ps);
        if (sql != null) return sql;
        // À défaut, on retourne toString() si ça contient "sql :"
        String ts = String.valueOf(ps);
        int i = ts.indexOf("sql :");
        if (i >= 0) return ts.substring(i + 5).trim();
        return null;
    }

    /** Renvoie le SQL rendu avec les paramètres capturés (pour lecture). */
    public static String sqlRendered(PreparedStatement ps) {
        String template = sqlTemplate(ps);
        if (template == null) return null;
        ParamStore params = grabParams(ps);
        if (params == null) return template;
        return render(template, params.orderedValues());
    }

    /** Infos de connexion (produit, versions, URL, user…). */
    public static DbInfo dbInfo(PreparedStatement ps) {
        try {
            Connection c = ps.getConnection();
            DatabaseMetaData md = c.getMetaData();
            return new DbInfo(
                    md.getDatabaseProductName(),
                    md.getDatabaseProductVersion(),
                    md.getDriverName(),
                    md.getDriverVersion(),
                    md.getURL(),
                    md.getUserName(),
                    safe(c::getSchema),
                    c.getAutoCommit(),
                    c.isReadOnly(),
                    isoToText(c.getTransactionIsolation())
            );
        } catch (SQLException e) {
            return new DbInfo(null, null, null, null, null, null, null, null, null, null);
        }
    }

    /* ======== Rendu SQL ======== */

    /** Remplace chaque ? par la valeur correspondante (échappage basique). */
    public static String render(String template, List<Object> values) {
        StringBuilder out = new StringBuilder();
        int q = 0, i = 0, n = template.length();
        for (; i < n; i++) {
            char ch = template.charAt(i);
            if (ch == '?') {
                Object val = (q < values.size()) ? values.get(q++) : null;
                out.append(literal(val));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    /* ======== Internes ======== */

    private static String tryOracleExtract(PreparedStatement ps) {
        try {
            Object target = ps;
            try {
                Class<?> iface = Class.forName("oracle.jdbc.OraclePreparedStatement");
                if (ps.isWrapperFor(iface)) target = ps.unwrap(iface);
            } catch (Throwable ignore) {}

            // Méthodes internes possibles
            String s = callHidden(target, "getOriginalSql", "getSQL", "getSql", "getPreparedSql");
            if (s != null) return s;

            // Champ interne sqlObject
            Object sqlObj = getField(target, "sqlObject");
            if (sqlObj != null) {
                s = callHidden(sqlObj, "getOriginalSql", "getSqlText", "getText", "toString");
            }
            return s;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String callHidden(Object obj, String... names) {
        for (String n : names) {
            try {
                Method m = obj.getClass().getDeclaredMethod(n);
                m.setAccessible(true);
                Object val = m.invoke(obj);
                if (val instanceof String) return (String) val;
                if (val != null) return val.toString();
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object getField(Object obj, String name) {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static ParamStore grabParams(PreparedStatement ps) {
        try {
            Method m = ps.getClass().getMethod("__sqldebug_params");
            return (ParamStore) m.invoke(ps);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String isoToText(int iso) {
        return switch (iso) {
            case Connection.TRANSACTION_NONE -> "NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> null;
        };
    }

    private static <T> T safe(SqlSupplier<T> s) {
        try { return s.get(); } catch (Throwable t) { return null; }
    }

    @FunctionalInterface private interface SqlSupplier<T> { T get() throws Exception; }

    /* ======== Paramètres capturés ======== */

    /** Stockage simple : index -> valeur (dernier assign gagnant). */
    private static final class ParamStore {
        private final Map<Integer,Object> values = new LinkedHashMap<>();
        void record(String method, int idx, Object[] args) {
            // setNull(int, int)
            if ("setNull".equals(method) && args.length >= 1) {
                values.put(idx, new TypedNull(args[0] instanceof Integer ? (Integer) args[0] : null));
                return;
            }
            // setObject(int, value, [type])
            if ("setObject".equals(method) && args.length >= 1) {
                values.put(idx, args[0]);
                return;
            }
            // Autres setXxx : premier arg = valeur
            values.put(idx, args.length > 0 ? args[0] : null);
        }
        void clear() { values.clear(); }
        List<Object> orderedValues() {
            if (values.isEmpty()) return List.of();
            int max = values.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<Object> out = new ArrayList<>(Collections.nCopies(max, null));
            for (Map.Entry<Integer,Object> e : values.entrySet()) {
                int i = e.getKey() - 1;
                if (i >= 0 && i < out.size()) out.set(i, e.getValue());
            }
            return out;
        }
    }

    private record TypedNull(Integer sqlType) {}

    /* ======== Formatting des littéraux (DEBUG) ======== */

    private static String literal(Object v) {
        if (v == null) return "NULL";
        if (v instanceof TypedNull tn) {
            return "NULL" + (tn.sqlType() != null ? "::" + jdbcTypeName(tn.sqlType()) : "");
        }
        if (v instanceof String s) return "'" + s.replace("'", "''") + "'";
        if (v instanceof Character c) return "'" + (c == '\'' ? "''" : c) + "'";
        if (v instanceof java.sql.Date d) return "'" + d.toString() + "'";
        if (v instanceof java.sql.Time t) return "'" + t.toString() + "'";
        if (v instanceof java.sql.Timestamp ts) return "'" + ts.toString() + "'";
        if (v instanceof LocalDate d) return "'" + d + "'";
        if (v instanceof LocalDateTime dt) return "'" + dt + "'";
        if (v instanceof OffsetDateTime odt) return "'" + odt + "'";
        if (v instanceof ZonedDateTime zdt) return "'" + zdt + "'";
        if (v instanceof byte[] b) return "hextoraw('" + toHex(b) + "')";
        if (v instanceof Boolean bo) return bo ? "1" : "0"; // pour lecture
        if (v instanceof BigDecimal bd) return bd.toPlainString();
        if (v instanceof Number n) return n.toString();
        return "'" + v.toString().replace("'", "''") + "'";
    }

    private static String jdbcTypeName(int t) {
        return switch (t) {
            case Types.VARCHAR -> "VARCHAR";
            case Types.CHAR -> "CHAR";
            case Types.INTEGER -> "INTEGER";
            case Types.BIGINT -> "BIGINT";
            case Types.DECIMAL -> "DECIMAL";
            case Types.NUMERIC -> "NUMERIC";
            case Types.DATE -> "DATE";
            case Types.TIMESTAMP -> "TIMESTAMP";
            case Types.BLOB -> "BLOB";
            case Types.CLOB -> "CLOB";
            default -> String.valueOf(t);
        };
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b: bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    /* ======== DTO ======== */
    public record DbInfo(
            String databaseProductName,
            String databaseProductVersion,
            String driverName,
            String driverVersion,
            String url,
            String userName,
            String schema,
            Boolean autoCommit,
            Boolean readOnly,
            String transactionIsolation
    ) {}
}
