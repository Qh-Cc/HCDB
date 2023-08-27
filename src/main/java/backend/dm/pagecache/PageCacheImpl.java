package backend.dm.pagecache;

import backend.common.AbstractCache;
import backend.dm.page.Page;
import backend.dm.page.PageImpl;
import backend.utils.Panic;
import common.Error;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @PROJECT_NAME: HCDB
 * @DESCRIPTION:页面缓存实现类
 * @Author Ccc
 * @DATE: 2023/8/4 11:45
 */
public class PageCacheImpl extends AbstractCache<Page> implements PageCache {

    private static final int MEM_MIN_LIM = 10;
    public static final String DB_SUFFIX = ".db";
    RandomAccessFile file;
    FileChannel fc;
    private Lock fileLock;
    private AtomicInteger pageNumbers;

    public PageCacheImpl(RandomAccessFile file, FileChannel fileChannel,int maxResource) {
        super(maxResource);
        if(maxResource < MEM_MIN_LIM) {
            Panic.panic(Error.MemTooSmallException);
        }
        long length = 0;
        try {
            length = file.length();
        } catch (IOException e) {
            Panic.panic(e);
        }
        this.file = file;
        this.fc = fileChannel;
        this.fileLock = new ReentrantLock();

        //刚开始不懂为什么会这样设计,想着如果这样设计,那么文件长度小于8k也就是8192的时候,页号不就为0吗
        //查了才知道,文件的length是以字节为单位,也就是说如果length等于1,那么文件就只有一字节
        //既然你是一个db文件,而如果你连一页的大小都没有,
        //那么根本就没有页号,只有你的长度除以页的大小大于1那么才有页号
        //计算机基础很不好体现出来了
        this.pageNumbers = new AtomicInteger((int)length / PAGE_SIZE);
    }

    @Override
    public int newPage(byte[] initData) {
        //每新增一个页,都会赋予一个页号,并且自增
        int pgno = pageNumbers.incrementAndGet();

        //TODO 为什么这里不放进缓存里,是因为新增的页没有数据吗?
        Page pg = new PageImpl(pgno, initData, null);
        flush(pg);  // 新建的页面需要立刻写回
        return pgno;
    }


    //当资源不在缓存时,读取db文件的数据,并已页的形式返回
    @Override
    protected Page getFromFileForCache(long key) throws Exception {
        int pgno = (int)key;
        long offset = PageCacheImpl.pageOffset(pgno);
        //开辟一个8K的缓存空间
        ByteBuffer buf = ByteBuffer.allocate(PAGE_SIZE);
        fileLock.lock();
        try {
            //移动到对应的页,读8K的数据
            fc.position(offset);
            fc.read(buf);
        } catch(IOException e) {
            Panic.panic(e);
        }
        fileLock.unlock();
        //返回一个页回去,并把这个页放进缓存里
        return new PageImpl(pgno,buf.array(),this);
    }


    @Override
    protected void releaseFromCacheForFile(Page pg) {
        if(pg.isDirty()) {
            flush(pg);
            pg.setDirty(false);
        }
    }

    private void flush(Page pg) {
        int pgno = pg.getPageNumber();
        long offset = pageOffset(pgno);
        ByteBuffer buf = ByteBuffer.wrap(pg.getData());
        fileLock.lock();
        try{
            fc.position(offset);
            fc.write(buf);
        }catch (IOException e){
            Panic.panic(e);
        }finally {
            fileLock.unlock();
        }
    }



    @Override
    public Page getPage(int pgno) throws Exception {
        return get((long)pgno);
    }

    @Override
    public void close() {
        super.close();
        try {
            fc.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    @Override
    public void release(Page page) {
        release((long)page.getPageNumber());
    }

    @Override
    public void truncateByBgno(int maxPgno) {

    }

    @Override
    public void flushPage(Page pg) {

    }
    @Override
    public int getPageNumber() {
        return pageNumbers.intValue();
    }

    private static long pageOffset(int pgno) {
        return (pgno-1) * PAGE_SIZE;
    }
}
