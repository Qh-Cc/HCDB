package backend.common;

/**
 * @PROJECT_NAME: HCDB
 * @DESCRIPTION:共享的内存数组
 * @Author Ccc
 * @DATE: 2023/8/3 11:53
 */
public class SubArray {
    public byte[] raw;
    public int start;
    public int end;

    public SubArray(byte[] raw, int start, int end) {
        this.raw = raw;
        this.start = start;
        this.end = end;
    }
}
