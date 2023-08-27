package backend.tm;

import common.Error;
import backend.utils.Panic;
import backend.utils.Parser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @PROJECT_NAME: HCDB
 * @DESCRIPTION:事务实现接口
 * @Author Ccc
 * @DATE: 2023/8/2 21:34
 */
public class TransactionManagerImpl implements TransactionManager {

    //先定义一些必要的常量:
    //XID文件头长度
    static final int LEN_XID_HEADER_LENGTH = 8;
    //每个事务的占用长度
    private static final int XID_FIELD_SIZE = 1;
    //事务的三种状态
    private static final byte FIELD_TRAN_ACTIVE = 0;//事务正在进行,尚未结束

    private static final byte FIELD_TRAN_COMMITTED = 1;//事务已提交

    private static final byte FIELD_TRAN_ABORTED = 2;//事务已回滚

    //超级事务,永远为commited状态
    public static final long SUPER_XID = 0;

    //XID文件后缀
    static final String XID_SUFFIX = ".xid";

    //RandomAccessFile是Java中用于对文件进行随机访问的类。它可以在文件中任意位置读取或写入数据，而不仅限于顺序访问。
    private RandomAccessFile file;

    //FileChannel是Java NIO中用于对文件进行读写操作的通道。它提供了高效的文件操作方法，可以在文件中进行随机访问、读取和写入数据。
    private FileChannel fc;

    //XID文件管理事务的总数
    private long xidCounter;

    private Lock counterLock;


    TransactionManagerImpl(RandomAccessFile raf, FileChannel fc) {
        this.file = raf;
        this.fc = fc;
        //ReentrantLock是Java中的一个可重入锁（Reentrant Lock），它提供了与synchronized关键字相似的功能，但更加灵活和强大。
        counterLock = new ReentrantLock();
        checkXIDCounter();
    }

    /**
     * 检查XID文件是否合法
     * 读取XID_FILE_HEADER中的xidcounter，根据它计算文件的理论长度，对比实际长度
     */
    private void checkXIDCounter() {
        long fileLen = 0;
        //如果在获取文件长度时发生异常，则会触发 Panic.panic(Error.BadXIDFileException) 方法，强制停机。
        try {
            fileLen = file.length();
        } catch (IOException e1) {
            Panic.panic(Error.BadXIDFileException);
        }
        //接下来，通过比较文件长度与 LEN_XID_HEADER_LENGTH 的大小，判断文件是否满足最小长度要求。
        //如果文件长度小于 LEN_XID_HEADER_LENGTH，则同样会触发 Panic.panic(Error.BadXIDFileException) 方法，强制停机。
        if(fileLen < LEN_XID_HEADER_LENGTH) {
            Panic.panic(Error.BadXIDFileException);
        }
        //然后，使用 ByteBuffer.allocate(LEN_XID_HEADER_LENGTH) 创建一个指定长度的字节缓冲区，
        //并通过 fc.position(0) 将文件通道的位置设置为 0，再通过 fc.read(buf) 从文件通道读取数据到缓冲区。
        // 如果在读取过程中发生异常，则会触发 Panic.panic(e) 方法，强制停机。
        ByteBuffer buf = ByteBuffer.allocate(LEN_XID_HEADER_LENGTH);
        try {
            fc.position(0);
            fc.read(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }

        //最后，通过调用 getXidPosition(this.xidCounter + 1) 方法获取事务 XID 在文件中的偏移位置，并将其赋值给 end 变量。
        // 如果计算得到的 end 值与文件长度不相等，则同样会触发 Panic.panic(Error.BadXIDFileException) 方法，强制停机。
        this.xidCounter = Parser.parseLong(buf.array());
        long end = getXidPosition(this.xidCounter + 1);
        if(end != fileLen) {
            Panic.panic(Error.BadXIDFileException);
        }
    }

    // 根据事务xid取得其在xid文件中对应的位置
    private long getXidPosition(long xid) {
        return LEN_XID_HEADER_LENGTH + (xid-1)*XID_FIELD_SIZE;
    }


    //开始一个事务,并返回XID
    public long begin() {
        //获取锁
        counterLock.lock();
        try {
            //创建事务的XID
            long xid = xidCounter + 1;
            //将这个事务的状态改为正在进行
            updateStatus(xid, FIELD_TRAN_ABORTED);
            incrXIDCounter();
            return xid;
        }finally {
            //解锁
            counterLock.unlock();
        }

    }



    private void updateStatus(long xid, byte status) {
        long offset = getXidPosition(xid);

        //创建这个事务的状态字节数组,长度为1
        byte[] tmp = new byte[XID_FIELD_SIZE];
        tmp[0] = status;
        //创建一个字节缓冲区
        ByteBuffer buf = ByteBuffer.wrap(tmp);

        try{
            //将指针移向offst位置
            fc.position(offset);
            //写入事务状态
            fc.write(buf);
        }catch (IOException e){
            Panic.panic(e);
        }

        try{
            //作用是将文件通道对应的数据强制刷新到磁盘上的存储介质，但不保证立即写入磁盘。
            //参数 false 表示不需要将文件的元数据（metadata）也一同刷新到磁盘。
            fc.force(false);
        }catch (IOException e){
            Panic.panic(e);
        }
    }

    //XID文件事务管理总数加一,将XidCounter加一,并更新XID Header
    private void incrXIDCounter() {
        xidCounter++;
        //更新XID Header
        ByteBuffer buf = ByteBuffer.wrap(Parser.long2Byte(xidCounter));
        try{
            fc.position(0);
            fc.write(buf);
        }catch (IOException e){
            Panic.panic(e);
        }

        try{
            fc.force(false);
        }catch (IOException e){
            Panic.panic(e);
        }

    }

    public void commit(long xid) {
        //修改事务状态为已提交
        updateStatus(xid, FIELD_TRAN_COMMITTED);
    }

    public void abort(long xid) {
        //修改事务状态为已回滚
        updateStatus(xid, FIELD_TRAN_ABORTED);
    }

    public boolean isActive(long xid) {
        if(xid == SUPER_XID) return false;
        return checkStatus(xid, FIELD_TRAN_ACTIVE);
    }

    public boolean isCommitted(long xid) {
        if(xid == SUPER_XID) return false;
        return checkStatus(xid, FIELD_TRAN_COMMITTED);
    }

    public boolean isAborted(long xid) {
        if(xid == SUPER_XID) return false;
        return checkStatus(xid, FIELD_TRAN_ABORTED);
    }

    // 检测XID事务是否处于status状态
    private boolean checkStatus(long xid, byte status) {
        long offset = getXidPosition(xid);
        ByteBuffer buf = ByteBuffer.wrap(new byte[XID_FIELD_SIZE]);
        try {
            fc.position(offset);
            fc.read(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return buf.array()[0] == status;
    }

    public void close() {
        try {
            fc.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
}
