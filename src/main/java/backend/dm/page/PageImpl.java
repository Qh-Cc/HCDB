package backend.dm.page;




import backend.dm.pagecache.PageCache;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @PROJECT_NAME: HCDB
 * @DESCRIPTION:页的实现类
 * @Author hqc
 * @DATE: 2023/8/4 11:21
 */
public class PageImpl implements Page {
    private int pageNumber;         //页号

    private byte[] data;            //页的数据
    /*
    脏页面（Dirty Page）是数据库管理系统中的一个概念，指的是在内存缓冲区中已经被修改过但尚未写回到磁盘的数据页。
    当数据库进行写操作时，为了提高性能和减少磁盘IO次数，通常会将数据先写入内存缓冲区（Buffer Cache），
    而不是直接写入磁盘。这样可以利用内存的高速读写特性来加快数据访问速度。
    然而，如果这些被修改过的数据页没有及时写回到磁盘，就会出现脏页面。脏页面可能是由于事务的更新操作、缓存刷新策略或者系统崩溃等原因导致的。
     */
    private boolean dirty;          //是否是脏页面

    private Lock lock;

    /*
    这里保存了一个 PageCache（还未定义）的引用，用来方便在拿到 Page 的引用时可以快速对这个页面的缓存进行释放操作。
     */
    private PageCache pc;

    public PageImpl(int pageNumber, byte[] data, PageCache pc) {
        this.pageNumber = pageNumber;
        this.data = data;
        this.pc = pc;
        lock = new ReentrantLock();
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public void release() {
        pc.release(this);
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public int getPageNumber() {
        return pageNumber;
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
