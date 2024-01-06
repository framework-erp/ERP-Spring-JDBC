package erp.springjdbc.mysql;

import erp.process.ProcessEntity;
import erp.repository.Store;

import java.util.Map;
import java.util.Set;

public class MySQLStore<E,ID> implements Store<E,ID> {

    private Class<E> entityClass;

    @Override
    public E load(ID id) {
        return null;
    }

    @Override
    public void insert(ID id, E e) {

    }

    @Override
    public void saveAll(Map<Object, Object> map, Map<Object, ProcessEntity> map1) {

    }

    @Override
    public void removeAll(Set<Object> set) {

    }
}
