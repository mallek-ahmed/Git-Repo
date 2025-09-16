package util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;

public final class OracleSqlExtractor {

    private OracleSqlExtractor() {
        // utilitaire statique : pas d’instance
    }

    /**
     * Essaie d'extraire le SQL d'un PreparedStatement Oracle (avec les ?).
     * Retourne null si rien trouvé.
     */
    public static String extract(PreparedStatement ps) {
        if (ps == null) return null;
        try {
            // unwrap éventuel vers l'interface Oracle
            Object target = ps;
            try {
                Class<?> iface = Class.forName("oracle.jdbc.OraclePreparedStatement");
                if (ps.isWrapperFor(iface)) {
                    target = ps.unwrap(iface);
                }
            } catch (Throwable ignore) { }

            // tenter des méthodes cachées
            String s = callHiddenMethod(target,
                    "getOriginalSql", "getSQL", "getSql", "getPreparedSql");
            if (s != null) return s;

            // sinon chercher un champ "sqlObject"
            s = fromSqlObject(target);
            if (s != null) return s;

            // dernier recours : toString (souvent juste la classe + hash)
            return target.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String callHiddenMethod(Object obj, String... names) {
        for (String n : names) {
            try {
                Method m = obj.getClass().getDeclaredMethod(n);
                m.setAccessible(true);
                Object val = m.invoke(obj);
                if (val instanceof String) return (String) val;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String fromSqlObject(Object obj) {
        Object sqlObj = getField(obj, "sqlObject");
        if (sqlObj == null) return null;
        return callHiddenMethod(sqlObj,
                "getOriginalSql", "getSqlText", "getText", "toString");
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
}
