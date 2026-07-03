package test;

public class W {
    private void writeShort(int s, byte[] buf, int offset) {
        buf[offset] = (byte) (s & 0xff); 
        buf[offset + 1] = (byte) ((s >> 8) & 0xff); 
    }
}
