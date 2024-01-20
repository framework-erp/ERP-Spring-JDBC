package erp.springjdbc.mysql;

import erp.repository.Mutexes;
import erp.springjdbc.CurrentTimeMillisClock;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MySQLMutexes<ID> implements Mutexes<ID> {

    private long maxLockTime;
    private JdbcTemplate jdbcTemplate;
    private Class entityClass;
    private String entityIDField;
    private CurrentTimeMillisClock clock = CurrentTimeMillisClock.getInstance();
    private boolean mock;
    private volatile boolean initialized;

    public MySQLMutexes(JdbcTemplate jdbcTemplate, Class entityClass, String entityIDField, long maxLockTime) {
        if (jdbcTemplate == null) {
            mock = true;
            return;
        }
        this.jdbcTemplate = jdbcTemplate;
        this.entityClass = entityClass;
        this.entityIDField = entityIDField;
        this.maxLockTime = maxLockTime;
    }

    private void initIfNecessary() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            try {
                createMutexesTableIfNotExists(jdbcTemplate, entityClass, entityIDField);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            initialized = true;
        }
    }

    private void createMutexesTableIfNotExists(JdbcTemplate jdbcTemplate, Class entityClass, String entityIDField) throws Exception {
        String mutexesTableName = "mutexes_" + entityClass.getName();
        String sql = "CREATE TABLE IF NOT EXISTS " + mutexesTableName + " (";
        Field idField = entityClass.getField(entityIDField);
        String idFieldType = idField.getType().getName();
        String idMySQLDataType = MySQLUtil.getMySQLDataType(idFieldType);
        sql += "id " + idMySQLDataType + "  PRIMARY KEY,";
        //还需要lockProcess和lockTime两个字段
        sql += "lockProcess VARCHAR(255),";
        sql += "lockTime BIGINT";
        sql += ")";
        jdbcTemplate.execute(sql);
    }

    /**
     * -1:锁不存在 0:锁失败 1:锁成功
     */
    @Override
    public int lock(ID id, String processName) {
        initIfNecessary();
        long currTime = clock.now();
        long unlockTime = currTime - maxLockTime;
        //通过UPDATE来获得锁
        String sql = "UPDATE mutexes_" + entityClass.getName() + " SET lock = ?, lockProcess = ?, lockTime = ? " +
                "WHERE id = ? AND (lock = ? OR lockTime < ?)";
        int result = jdbcTemplate.update(sql, 1, processName, currTime, id, 0, unlockTime);
        if (result == 1) {
            return 1;
        }
        //判断锁是否存在
        sql = "SELECT * FROM mutexes_" + entityClass.getName() + " WHERE id = ?";
        Mutex mutex = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Mutex.class), id);
        if (mutex == null) {
            return -1;
        }
        return 0;
    }

    /**
     * 返回false那就是已创建了
     */
    @Override
    public boolean newAndLock(ID id, String processName) {
        initIfNecessary();
        String sql = "INSERT INTO mutexes_" + entityClass.getName() + " (id, lock, lockProcess, lockTime) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, id, 1, processName, clock.now());
        } catch (DataAccessException e) {
            return false;
        }
        return true;
    }

    @Override
    public void unlockAll(Set<Object> ids) {
        initIfNecessary();
        String sql = "UPDATE mutexes_" + entityClass.getName() + " SET lock = ? WHERE id = ?";
        List<Object[]> updateParameters = new ArrayList<>();
        for (Object id : ids) {
            updateParameters.add(new Object[]{0, id});
        }
        jdbcTemplate.batchUpdate(sql, updateParameters);
    }

    @Override
    public String getLockProcess(ID id) {
        initIfNecessary();
        String sql = "SELECT lockProcess FROM mutexes_" + entityClass.getName() + " WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, id);
    }

    @Override
    public void removeAll(Set<Object> ids) {
        initIfNecessary();
        String sql = "DELETE FROM mutexes_" + entityClass.getName() + " WHERE id = ?";
        List<Object[]> removeParameters = new ArrayList<>();
        for (Object id : ids) {
            removeParameters.add(new Object[]{id});
        }
        jdbcTemplate.batchUpdate(sql, removeParameters);
    }
}
