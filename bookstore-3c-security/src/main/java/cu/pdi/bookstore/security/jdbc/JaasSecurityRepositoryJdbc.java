package cu.pdi.bookstore.security.jdbc;

import cu.pdi.bookstore.security.HashMaker;
import cu.pdi.bookstore.security.entities.SecurityPerson;
import cu.pdi.bookstore.security.entities.SecurityRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cu.pdi.bookstore.security.HashMaker.HashAlgorithm.SHA256;

/**
 * Created by R.S.F.
 */
@Component
public class JaasSecurityRepositoryJdbc implements JaasSecurityRepository {

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final Map<String, String> queries = new HashMap<>();

    @Autowired
    public JaasSecurityRepositoryJdbc(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Optional<SecurityPerson> getUserAssociatedPerson(String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("user", username);
        params.put("pass", password);
        List<SecurityPerson> securityPeople = namedParameterJdbcTemplate.query(queries.get("userQuery"), params, (ResultSet resultSet, int i) ->
                new SecurityPerson(resultSet.getString("first_name"),
                        resultSet.getString("middle_name"),
                        resultSet.getString("last_name"))
        );
        return Optional.of(securityPeople.get(0));
    }

    @Override
    public List<SecurityRole> getUserAssociatedRoles(String username) {
        Map<String, String> params = new HashMap<>();
        params.put("user", username);

        return namedParameterJdbcTemplate.query(queries.get("roleQuery"), params, (ResultSet resultSet, int i) ->
                new SecurityRole(resultSet.getInt("id_role"),
                        resultSet.getString("role"))
        );
    }

    @Override
    public void initQueries(Map<String, ?> options) {
        this.queries.put("userQuery", (String) options.get("userQuery"));
        this.queries.put("roleQuery", (String) options.get("roleQuery"));
        this.queries.put("adminQuery", (String) options.get("adminQuery"));
        this.queries.put("adminRoleInsertQuery", (String) options.get("adminRoleInsertQuery"));
        this.queries.put("findAdminRoleQuery", (String) options.get("findAdminRoleQuery"));
        this.queries.put("adminInsertQuery", (String) options.get("adminInsertQuery"));
        this.queries.put("adminInsertWorkerQuery", (String) options.get("adminInsertWorkerQuery"));
    }

    @Override
    public void createDefaultAdminUser() {

        List<SecurityPerson> securityPeople = namedParameterJdbcTemplate.query(queries.get("adminQuery"), (ResultSet resultSet, int i) ->
                new SecurityPerson(resultSet.getString("first_name"),
                        resultSet.getString("middle_name"),
                        resultSet.getString("last_name"))
        );

        if (securityPeople.isEmpty()) {
            Integer roleId = namedParameterJdbcTemplate.getJdbcOperations()
                    .execute((StatementCallback<Integer>) statement -> {

                statement.execute(queries.get("adminRoleInsertQuery"));
                ResultSet roleResultSet = statement.executeQuery(queries.get("findAdminRoleQuery"));
                roleResultSet.next();

                return roleResultSet.getInt("id_role");
            });


            Map<String, Object> params = new HashMap<>();
            params.put("user", "admin");
            params.put("pass", HashMaker.hashedTypeFor("admin1234", SHA256));
            params.put("role", roleId);
            namedParameterJdbcTemplate.update(queries.get("adminInsertQuery"), params);

            params.clear();
            params.put("user", "admin");
            params.put("f_name", "admin");
            params.put("m_name", "admin");
            params.put("l_name", "admin");
            namedParameterJdbcTemplate.update(queries.get("adminInsertWorkerQuery"), params);
        }

    }
}
