package backend.dm.pageIndex;

import backend.dm.pagecache.PageCache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
    /*lists = {
         list1 = {
                   pageInfo1 = {
                                pgno1,204
                               },
                   pageInfo2 = {
                                pgno2,204
                               }
                  },
         list2 = {
                    pageInfo3 = {
                                pgno,204*2
                                }
                  }
     }
    */

public class PageIndex {
    // 将一页划成40个区间
    private static final int INTERVALS_NO = 40;
    private static final int THRESHOLD = PageCache.PAGE_SIZE / INTERVALS_NO;//204

    private Lock lock;
    private List<PageInfo>[] lists;

    @SuppressWarnings("unchecked")
    public PageIndex() {
        lock = new ReentrantLock();
        lists = new List[INTERVALS_NO+1];
        for (int i = 0; i < INTERVALS_NO+1; i ++) {
            lists[i] = new ArrayList<>();
        }
    }

    //lists数组的每一个下标的List都会存着以下标为倍数的PageInfo,
    //那么调用select方法的时候,就是通过传入你需要的spaceSize然后除以204得到下标,
    //如果这个下标的List有值就返回,并删除这个PageInfo
    public void add(int pgno, int freeSpace) {
        lock.lock();
        try {
            int number = freeSpace / THRESHOLD;
            lists[number].add(new PageInfo(pgno, freeSpace));
        } finally {
            lock.unlock();
        }
    }

    public PageInfo select(int spaceSize) {
        lock.lock();
        try {
            int number = spaceSize / THRESHOLD;
            if(number < INTERVALS_NO) number ++;
            while(number <= INTERVALS_NO) {
                if(lists[number].size() == 0) {
                    number ++;
                    continue;
                }
                return lists[number].remove(0);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

}
