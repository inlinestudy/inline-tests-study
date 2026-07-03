package test;

import java.nio.ByteBuffer;

class X {

    private static int readMetroAreaCombo(ByteBuffer buffer) {
        if (buffer.remaining() < 3) {
            throw new InvalidDatabaseException("Unexpected end of data record when reading metro area");
        }
        int metroareaCombo = 0;
        for (int j = 0; j < 3; j++) {
            metroareaCombo += unsignedByteToInt(buffer.get()) << (j * 8);
        }
        return metroareaCombo;
    }

    private static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}
