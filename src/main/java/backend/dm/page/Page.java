package backend.dm.page;

/**
 * @author: Ccc
 * @description:页的接口
 * @date: 2023/8/4 11:22
 **/
public interface Page {
    void lock();
    void unlock();
    void release();
    void setDirty(boolean dirty);
    boolean isDirty();
    int getPageNumber();
    byte[] getData();
}
