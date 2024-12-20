package erp.springjdbc.mysql;

import erp.repository.SingletonEntity;
import erp.repository.SingletonRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

public class MySQLSingletonRepository<E> extends SingletonRepository<E> {
    private static MySQLRepository<SingletonEntity, String> SINGLETON_CONTAINER;

    private static void init(JdbcTemplate jdbcTemplate) {
        if (SINGLETON_CONTAINER == null) {
            synchronized (MySQLSingletonRepository.class) {
                if (SINGLETON_CONTAINER == null) {
                    SINGLETON_CONTAINER = new MySQLRepository<>(jdbcTemplate, SingletonEntity.class,
                            "MySQLSingletonRepository");
                }
            }
        }
    }

    protected MySQLSingletonRepository(JdbcTemplate jdbcTemplate) {
        init(jdbcTemplate);
        this.singletonEntitiesContainer = SINGLETON_CONTAINER;
    }

    public MySQLSingletonRepository(JdbcTemplate jdbcTemplate, String repositoryName) {
        super(repositoryName);
        init(jdbcTemplate);
        this.singletonEntitiesContainer = SINGLETON_CONTAINER;
    }

    public MySQLSingletonRepository(JdbcTemplate jdbcTemplate, E entity) {
        super(entity);
        init(jdbcTemplate);
        this.singletonEntitiesContainer = SINGLETON_CONTAINER;
        ensureEntity(entity);
    }

    public MySQLSingletonRepository(JdbcTemplate jdbcTemplate, E entity, String repositoryName) {
        super(repositoryName);
        init(jdbcTemplate);
        this.singletonEntitiesContainer = SINGLETON_CONTAINER;
        ensureEntity(entity);
    }

    private void ensureEntity(E entity) {
        SingletonEntity singletonEntity = new SingletonEntity();
        singletonEntity.setName(name);
        singletonEntity.setEntity(entity);
        try {
            this.singletonEntitiesContainer.getStore().insert(name, singletonEntity);
        } catch (DuplicateKeyException e) {
            //什么也不用做
            System.out.println("DuplicateKeyException");
        }
    }

}