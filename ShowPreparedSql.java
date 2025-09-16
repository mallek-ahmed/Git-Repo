import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class ShowPreparedSql {
    public static void main(String[] args) throws Exception {
        // à adapter : remplacer host, port, service, user, password
        String url = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";
        String user = "scott";
        String password = "tiger";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(
                     "select * from emp where deptno = ? and job = ?")) {

            ps.setInt(1, 10);
            ps.setString(2, "CLERK");

            // affichera la requête avec les “?”
            System.out.println(ps.toString());
        }
    }
}
