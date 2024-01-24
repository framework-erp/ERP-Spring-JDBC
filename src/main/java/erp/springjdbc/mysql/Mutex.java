package erp.springjdbc.mysql;

public class Mutex<ID> {
    private ID id;
    private boolean locked;
    private long lockTime;
    private String lockProcess;

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getLockTime() {
        return lockTime;
    }

    public void setLockTime(long lockTime) {
        this.lockTime = lockTime;
    }

    public String getLockProcess() {
        return lockProcess;
    }

    public void setLockProcess(String lockProcess) {
        this.lockProcess = lockProcess;
    }
}
