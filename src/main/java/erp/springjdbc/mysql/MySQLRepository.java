package erp.springjdbc.mysql;

import erp.repository.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

public class MySQLRepository<E, ID> extends Repository<E, ID> {

    private Class<E> entityClass;

    protected MySQLRepository(JdbcTemplate jdbcTemplate) {
        try {
            this.entityClass = (Class<E>) Class.forName(entityType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("can not parse entity type", e);
        }
        this.store = new MySQLStore<>(jdbcTemplate, entityClass, entityIDField);
        this.mutexes = new MySQLMutexes<>(jdbcTemplate, entityClass, entityIDField, 30000L);
    }

    public MySQLRepository(JdbcTemplate jdbcTemplate, Class<E> entityType) {
        super(entityType.getName());
        this.store = new MySQLStore<>(jdbcTemplate, entityType, entityIDField);
        this.mutexes = new MySQLMutexes<>(jdbcTemplate, entityType, entityIDField, 30000L);
    }

}
