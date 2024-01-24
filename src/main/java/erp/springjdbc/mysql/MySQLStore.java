package erp.springjdbc.mysql;

import com.alibaba.fastjson2.JSON;
import erp.process.ProcessEntity;
import erp.repository.Store;
import erp.repository.impl.mem.MemStore;
import erp.springjdbc.NestedPOJOJSONRowMapper;
import erp.util.Unsafe;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.*;

interface EntityFieldGetter {
    Object getField(Object entity);
}

public class MySQLStore<E, ID> implements Store<E, ID> {

    private JdbcTemplate jdbcTemplate;
    private String entityIDField;
    private Class<E> entityClass;
    private NestedPOJOJSONRowMapper<E> rowMapper;
    private Map<String, EntityFieldGetter> entityFieldGetterMap = new HashMap<>();
    private List<String> entityFieldNames = new ArrayList<>();
    private String insertSQL;
    private String updateSQL;
    private String deleteSQL;
    private MemStore<E, ID> mockStore;

    public MySQLStore(JdbcTemplate jdbcTemplate, Class<E> entityClass, String entityIDField) {
        if (jdbcTemplate == null) {
            initAsMock();
            return;
        }
        this.entityClass = entityClass;
        createTableIfNotExists(jdbcTemplate, entityClass, entityIDField);

        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            EntityFieldGetter entityFieldGetter = createEntityFieldGetter(field);
            String fieldName = field.getName();
            entityFieldGetterMap.put(fieldName, entityFieldGetter);
            entityFieldNames.add(fieldName);
        }

        insertSQL = createInsertSQL(entityClass);
        updateSQL = createUpdateSQL(entityClass, entityIDField);
        deleteSQL = "DELETE FROM " + entityClass.getSimpleName() + " WHERE " + entityIDField + "=?";

        this.jdbcTemplate = jdbcTemplate;
        this.entityIDField = entityIDField;
        rowMapper = new NestedPOJOJSONRowMapper<>(entityClass);
    }

    private void initAsMock() {
        mockStore = new MemStore<E, ID>();
    }

    private boolean isMock() {
        return mockStore != null;
    }

    private String createUpdateSQL(Class<E> entityClass, String entityIDField) {
        String updateSQL = "UPDATE " + entityClass.getSimpleName() + " SET ";
        for (String entityField : entityFieldNames) {
            updateSQL += entityField + "=?,";
        }
        updateSQL = updateSQL.substring(0, updateSQL.length() - 1) + " WHERE " + entityIDField + "=?";
        return updateSQL;
    }

    private String createInsertSQL(Class<E> entityClass) {
        String insertSQL = "INSERT INTO " + entityClass.getSimpleName() + " (";
        for (String entityField : entityFieldNames) {
            insertSQL += entityField + ",";
        }
        insertSQL = insertSQL.substring(0, insertSQL.length() - 1) + ") VALUES (";
        for (int i = 0; i < entityFieldNames.size(); i++) {
            insertSQL += "?,";
        }
        insertSQL = insertSQL.substring(0, insertSQL.length() - 1) + ")";
        return insertSQL;
    }

    private EntityFieldGetter createEntityFieldGetter(Field field) {
        EntityFieldGetter entityFieldGetter = null;
        Class<?> fieldType = field.getType();
        long fieldOffset = Unsafe.getFieldOffset(field);
        if (fieldType.equals(byte.class)) {
            entityFieldGetter = (e) -> Unsafe.getByteFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(short.class)) {
            entityFieldGetter = (e) -> Unsafe.getShortFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(char.class)) {
            entityFieldGetter = (e) -> Unsafe.getCharFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(int.class)) {
            entityFieldGetter = (e) -> Unsafe.getIntFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(long.class)) {
            entityFieldGetter = (e) -> Unsafe.getLongFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(float.class)) {
            entityFieldGetter = (e) -> Unsafe.getFloatFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(double.class)) {
            entityFieldGetter = (e) -> Unsafe.getDoubleFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(boolean.class)) {
            entityFieldGetter = (e) -> Unsafe.getBooleanFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(String.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Byte.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Short.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Character.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Integer.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Long.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Float.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Double.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else if (fieldType.equals(Boolean.class)) {
            entityFieldGetter = (e) -> Unsafe.getObjectFieldOfObject(e, fieldOffset);
        } else {
            entityFieldGetter = (e) -> JSON.toJSONString(Unsafe.getObjectFieldOfObject(e, fieldOffset));
        }
        return entityFieldGetter;
    }

    private void createTableIfNotExists(JdbcTemplate jdbcTemplate, Class<E> entityClass, String entityIDField) {
        Field[] fields = entityClass.getDeclaredFields();

        String tableName = entityClass.getSimpleName();
        // 构建创建表的 SQL 语句
        String createTableQuery = "CREATE TABLE IF NOT EXISTS  " + tableName + " (";
        for (Field field : fields) {
            String fieldName = field.getName();
            String fieldType = field.getType().getName();
            String mysqlDataType = MySQLUtil.getMySQLDataType(fieldType);
            if (fieldName.equals(entityIDField)) {
                mysqlDataType += " PRIMARY KEY";
            }
            createTableQuery += fieldName + " " + mysqlDataType + ",";
        }
        // 去掉末尾的逗号
        createTableQuery = createTableQuery.substring(0, createTableQuery.length() - 1) + ")";

        // 执行创建表的 SQL 语句
        jdbcTemplate.execute(createTableQuery);
    }

    @Override
    public E load(ID id) {
        if (isMock()) {
            return mockStore.load(id);
        }
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM " + entityClass.getSimpleName() + " WHERE " +
                    entityIDField +
                    " = " + id, rowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void insert(ID id, E entity) {
        if (isMock()) {
            mockStore.insert(id, entity);
            return;
        }
        Object[] args = getFieldsValue(entity);
        jdbcTemplate.update(insertSQL, args);
    }

    private Object[] getFieldsValue(Object entity) {
        Object[] fieldsValue = new Object[entityFieldNames.size()];
        for (int i = 0; i < entityFieldNames.size(); i++) {
            fieldsValue[i] = entityFieldGetterMap.get(entityFieldNames.get(i)).getField(entity);
        }
        return fieldsValue;
    }

    @Override
    public void saveAll(Map<Object, Object> entitiesToInsert, Map<Object, ProcessEntity> entitiesToUpdate) {
        if (isMock()) {
            mockStore.saveAll(entitiesToInsert, entitiesToUpdate);
            return;
        }
        List<Object[]> insertParameters = new ArrayList<>();
        for (Object entity : entitiesToInsert.values()) {
            insertParameters.add(getFieldsValue(entity));
        }
        jdbcTemplate.batchUpdate(insertSQL, insertParameters);

        List<Object[]> updateParameters = new ArrayList<>();
        for (Map.Entry<Object, ProcessEntity> entry : entitiesToUpdate.entrySet()) {
            Object entity = entry.getValue().getEntity();
            Object[] fieldsValue = getFieldsValue(entity);
            Object[] args = new Object[fieldsValue.length + 1];
            System.arraycopy(fieldsValue, 0, args, 0, fieldsValue.length);
            args[args.length - 1] = entry.getKey();
            updateParameters.add(args);
        }
        jdbcTemplate.batchUpdate(updateSQL, updateParameters);
    }

    @Override
    public void removeAll(Set<Object> ids) {
        if (isMock()) {
            mockStore.removeAll(ids);
            return;
        }
        List<Object[]> removeParameters = new ArrayList<>();
        for (Object id : ids) {
            removeParameters.add(new Object[]{id});
        }
        jdbcTemplate.batchUpdate(deleteSQL, removeParameters);
    }
}