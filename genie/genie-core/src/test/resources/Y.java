package test;

import java.io.IOException;

class Y {

    private static final int SEGMENT_RECORD_LENGTH = 3;
    private int[] databaseSegments;

    private synchronized void init() throws IOException {
        byte[] buf = new byte[SEGMENT_RECORD_LENGTH];
        for (int j = 0; j < SEGMENT_RECORD_LENGTH; j++) {
            databaseSegments[0] += (unsignedByteToInt(buf[j]) << (j * 8));
        }
    }

    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}