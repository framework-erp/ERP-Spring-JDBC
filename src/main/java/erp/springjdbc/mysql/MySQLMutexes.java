package erp.springjdbc.mysql;

import erp.repository.Mutexes;
import erp.springjdbc.CurrentTimeMillisClock;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
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
    private String mutexesTableName;
    private CurrentTimeMillisClock clock = CurrentTimeMillisClock.getInstance();
    private boolean mock;
    private volatile boolean initialized;

    public MySQLMutexes(JdbcTemplate jdbcTemplate, Class entityClass, String entityIDField, String entityTableName, long maxLockTime) {
        if (jdbcTemplate == null) {
            mock = true;
            return;
        }
        this.jdbcTemplate = jdbcTemplate;
        this.entityClass = entityClass;
        this.entityIDField = entityIDField;
        this.mutexesTableName = "mutexes_" + entityTableName;
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
                createMutexesTableIfNotExists();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            initialized = true;
        }
    }

    private void createMutexesTableIfNotExists() throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS " + mutexesTableName + " (";
        Field idField = entityClass.getDeclaredField(entityIDField);
        String idFieldType = idField.getType().getName();
        String idMySQLDataType = MySQLUtil.getMySQLDataType(idFieldType);
        sql += "id " + idMySQLDataType + "  PRIMARY KEY,";
        sql += "locked INT,"; //0:未锁定 1:已锁定
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
        if (mock) {
            return 1;
        }
        initIfNecessary();
        long currTime = clock.now();
        long unlockTime = currTime - maxLockTime;
        //通过UPDATE来获得锁
        String sql = "UPDATE " + mutexesTableName + " SET locked = ?, lockProcess = ?, lockTime = ? " +
                "WHERE id = ? AND (locked = ? OR lockTime < ?)";
        int result = jdbcTemplate.update(sql, 1, processName, currTime, id, 0, unlockTime);
        if (result == 1) {
            return 1;
        }
        //判断锁是否存在
        sql = "SELECT * FROM " + mutexesTableName + " WHERE id = ?";
        try {
            Mutex mutex = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Mutex.class), id);
            if (mutex == null) {
                return -1;
            }
        } catch (EmptyResultDataAccessException e) {
            return -1;
        }
        return 0;
    }

    /**
     * 返回false那就是已创建了
     */
    @Override
    public boolean newAndLock(ID id, String processName) {
        if (mock) {
            return true;
        }
        initIfNecessary();
        String sql = "INSERT INTO " + mutexesTableName + " (id, locked, lockProcess, lockTime) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, id, 1, processName, clock.now());
        } catch (DataAccessException e) {
            return false;
        }
        return true;
    }

    @Override
    public void unlockAll(Set<Object> ids) {
        if (mock) {
            return;
        }
        initIfNecessary();
        String sql = "UPDATE " + mutexesTableName + " SET locked = ? WHERE id = ?";
        List<Object[]> updateParameters = new ArrayList<>();
        for (Object id : ids) {
            updateParameters.add(new Object[]{0, id});
        }
        jdbcTemplate.batchUpdate(sql, updateParameters);
    }

    @Override
    public String getLockProcess(ID id) {
        if (mock) {
            return null;
        }
        initIfNecessary();
        String sql = "SELECT lockProcess FROM " + mutexesTableName + " WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, id);
    }

    @Override
    public void removeAll(Set<Object> ids) {
        if (mock) {
            return;
        }
        initIfNecessary();
        String sql = "DELETE FROM " + mutexesTableName + " WHERE id = ?";
        List<Object[]> removeParameters = new ArrayList<>();
        for (Object id : ids) {
            removeParameters.add(new Object[]{id});
        }
        jdbcTemplate.batchUpdate(sql, removeParameters);
    }
}
