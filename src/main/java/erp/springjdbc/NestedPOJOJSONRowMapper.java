package erp.springjdbc;

import com.alibaba.fastjson2.JSON;
import erp.util.Unsafe;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

interface EntityFieldSetter {
    void setField(Object entity, Object value);
}

interface ResultSetColumnValueGetter {
    Object getColumnValue(ResultSet rs) throws SQLException;
}

/**
 * 扁平化的映射器，将嵌套的POJO转换为JSON字符串
 */
public class NestedPOJOJSONRowMapper<E> implements RowMapper<E> {

    private final Class<E> entityClass;
    private Map<String, ResultSetColumnValueGetter> resultSetColumnValueGetterMap = new HashMap<>();
    private Map<String, EntityFieldSetter> entityFieldSetterMap = new HashMap<>();

    public NestedPOJOJSONRowMapper(Class<E> entityClass) {
        this.entityClass = entityClass;
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            long fieldOffset = Unsafe.getFieldOffset(field);
            Class<?> fieldType = field.getType();
            ResultSetColumnValueGetter resultSetColumnValueGetter = null;
            EntityFieldSetter entityFieldSetter = null;
            if (fieldType.equals(byte.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getByte(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setByteFieldOfObject(e, fieldOffset, (Byte) v);
                };
            } else if (fieldType.equals(short.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getShort(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setShortFieldOfObject(e, fieldOffset, (Short) v);
                };
            } else if (fieldType.equals(char.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return (char) rs.getInt(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setCharFieldOfObject(e, fieldOffset, (Character) v);
                };
            } else if (fieldType.equals(int.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getInt(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setIntFieldOfObject(e, fieldOffset, (Integer) v);
                };
            } else if (fieldType.equals(float.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getFloat(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setFloatFieldOfObject(e, fieldOffset, (Float) v);
                };
            } else if (fieldType.equals(long.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getLong(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setLongFieldOfObject(e, fieldOffset, (Long) v);
                };
            } else if (fieldType.equals(double.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getDouble(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setDoubleFieldOfObject(e, fieldOffset, (Double) v);
                };
            } else if (fieldType.equals(boolean.class)) {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getBoolean(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Unsafe.setBooleanFieldOfObject(e, fieldOffset, (Boolean) v);
                };
            } else {
                resultSetColumnValueGetter = (rs) -> {
                    return rs.getString(fieldName);
                };
                entityFieldSetter = (e, v) -> {
                    Object nestedField = JSON.parseObject((String) v, fieldType);
                    Unsafe.setObjectFieldOfObject(e, fieldOffset, nestedField);
                };
            }
            resultSetColumnValueGetterMap.put(fieldName, resultSetColumnValueGetter);
            entityFieldSetterMap.put(fieldName, entityFieldSetter);
        }
    }

    @Override
    public E mapRow(ResultSet rs, int rowNum) throws SQLException {
        E entity = Unsafe.allocateInstance(entityClass);
        for (Map.Entry entry : resultSetColumnValueGetterMap.entrySet()) {
            String fieldName = (String) entry.getKey();
            ResultSetColumnValueGetter resultSetColumnValueGetter = (ResultSetColumnValueGetter) entry.getValue();
            Object columnValue = resultSetColumnValueGetter.getColumnValue(rs);
            EntityFieldSetter entityFieldSetter = entityFieldSetterMap.get(fieldName);
            entityFieldSetter.setField(entity, columnValue);
        }
        return entity;
    }
}
