package erp.springjdbc.mysql;

import erp.repository.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

public class MySQLRepository<E, ID> extends Repository<E, ID> {

    protected MySQLRepository(JdbcTemplate jdbcTemplate) {
        String tableName = entityType.getSimpleName();
        this.store = new MySQLStore<>(jdbcTemplate, entityType, entityIDField, tableName);
        this.mutexes = new MySQLMutexes<>(jdbcTemplate, entityType, entityIDField, tableName, 30000L);
    }

    public MySQLRepository(JdbcTemplate jdbcTemplate, Class<E> entityType) {
        super(entityType);
        String tableName = entityType.getSimpleName();
        this.store = new MySQLStore<>(jdbcTemplate, entityType, entityIDField, tableName);
        this.mutexes = new MySQLMutexes<>(jdbcTemplate, entityType, entityIDField, tableName, 30000L);
    }

    public MySQLRepository(JdbcTemplate jdbcTemplate, Class<E> entityType, String repositoryName) {
        super(entityType, repositoryName);
        String tableName = repositoryName;
        this.store = new MySQLStore<>(jdbcTemplate, entityType, entityIDField, tableName);
        this.mutexes = new MySQLMutexes<>(jdbcTemplate, entityType, entityIDField, tableName, 30000L);
    }

}
