package backend.utils;

import java.nio.ByteBuffer;

/**
 * @PROJECT_NAME: HCDB
 * @DESCRIPTION:共享内存数组
 * @Author hqc
 * @DATE: 2023/8/2 22:13
 */
public class Parser {

    //这段代码使用ByteBuffer.wrap(buf, 0, 8)方法将一个字节数组buf的前8个字节包装成一个ByteBuffer对象，
    //并通过buffer.getLong()方法获取这个ByteBuffer中的长整型数据。
    public static long parseLong(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, 8);
        return buffer.getLong();
    }
    public static byte[] long2Byte(long value) {
        // Long.SIZE / Byte.SIZE = 8,也就是开辟一个8字节的缓存空间,并将xidCounter的值放进去
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
    }

    public static byte[] short2Byte(short value) {
        return  ByteBuffer.allocate(Short.SIZE / Byte.SIZE).putShort(value).array();
    }

    public static short parseShort(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, 2);
        return buffer.getShort();
    }

    public static byte[] int2Byte(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).putInt(value).array();
    }

    public static int parseInt(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, 4);
        return buffer.getInt();
    }
}
