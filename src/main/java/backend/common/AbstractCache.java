package backend.common;

import common.Error;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @PROJECT_NAME: HCDB
 * @DESCRIPTION:所有缓存虚拟类
 * @Author hqc
 * @DATE: 2023/8/3 11:53
 */
public abstract class AbstractCache<T> {
    private HashMap<Long, T> cache;                     // 实际缓存的数据
    private HashMap<Long, Integer> references;          // 元素的引用个数
    private HashMap<Long, Boolean> getting;             // 正在获取某资源的线程

    private int maxResource;                            // 缓存的最大缓存资源数
    private int count = 0;                              // 缓存中元素的个数
    private Lock lock;                                  // 锁

    public AbstractCache(int maxResource) {
        this.maxResource = maxResource;
        cache = new HashMap<>();
        references = new HashMap<>();
        getting = new HashMap<>();
        lock = new ReentrantLock();
    }

    public T get(long key) throws Exception{
        while(true){
            //获取锁
            lock.lock();
            //如果有其他线程正在获取当前资源的话
            //释放锁并等待一段时间后再次尝试获取锁。这样做的目的是为了避免线程长时间的忙等待，减少CPU资源的浪费。
            if(getting.containsKey(key)){
                lock.unlock();
                try{
                    Thread.sleep(1);
                }catch (InterruptedException e){
                    e.printStackTrace();
                    continue;
                }
                continue;
            }

            //如果获取的key在缓存里,那么直接返回
            if(cache.containsKey(key)){
                T obj = cache.get(key);
                references.put(key,references.get(key)+1);
                lock.unlock();
                return obj;
            }

            //如果不在缓存,那么就要开始从数据源中获取数据,并在getting集合中添加key
            if(maxResource > 0 && count == maxResource){
                lock.unlock();
                throw Error.CacheFullException;
            }

            count++;
            getting.put(key,true);
            lock.unlock();
            break;
        }

        //从数据源中获取数据
        T obj = null;

        try{
            obj = getFromFileForCache(key);
        }catch (Exception e) {

            lock.lock();
            count--;
            getting.remove(key);
            lock.unlock();
            throw e;
        }

        lock.lock();
        getting.remove(key);
        cache.put(key,obj);
        //第一次放在缓存里,所以引用次数为1
        references.put(key,1);
        lock.unlock();
        return obj;
    }
    /**
     * 强行释放一个缓存
     */
    protected void release(long key) {
        lock.lock();
        try {
            int ref = references.get(key)-1;
            if(ref == 0) {
                T obj = cache.get(key);
                releaseFromCacheForFile(obj);
                references.remove(key);
                cache.remove(key);
                count --;
            } else {
                references.put(key, ref);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭缓存，写回所有资源
     */
    protected void close() {
        lock.lock();
        try {
            Set<Long> keys = cache.keySet();
            for (long key : keys) {
                T obj = cache.get(key);
                releaseFromCacheForFile(obj);
                references.remove(key);
                cache.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * 当资源不在缓存时的获取行为
     */
    protected abstract T getFromFileForCache(long key) throws Exception;
    /**
     * 当资源被驱逐时的写回行为
     */
    protected abstract void releaseFromCacheForFile(T obj);
}

