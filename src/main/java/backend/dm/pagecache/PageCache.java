package backend.dm.pagecache;

import backend.dm.page.Page;

/**
 * @PROJECT_NAME: HCDB
 * @DESCRIPTION:
 * @Author hqc
 * @DATE: 2023/8/4 11:41
 */
public interface PageCache {

    //页面大小8k
    public static final int PAGE_SIZE = 1 << 13;//1092
    int newPage(byte[] initData);
    Page getPage(int pgno) throws Exception;
    void close();
    void release(Page page);

    void truncateByBgno(int maxPgno);
    int getPageNumber();
    void flushPage(Page pg);

}
