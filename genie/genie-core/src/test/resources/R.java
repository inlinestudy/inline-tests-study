package test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.List;

public class R {
    /**
     * Returns information about the database.
     *
     * @return database info.
     */
    public synchronized DatabaseInfo getDatabaseInfo() {
        if (databaseInfo != null) {
            return databaseInfo;
        }
        try {
            _check_mtime();
            boolean hasStructureInfo = false;
            byte[] delim = new byte[3];
            // Advance to part of file where database info is stored.
            file.seek(file.length() - 3);
            for (int i = 0; i < STRUCTURE_INFO_MAX_SIZE; i++) {
                int read = file.read(delim);
                if (read == 3 && (delim[0] & 0xFF) == 255
                        && (delim[1] & 0xFF) == 255 && (delim[2] & 0xFF) == 255) {
                    hasStructureInfo = true;
                    break;
                }
                file.seek(file.getFilePointer() - 4);

            }
            if (hasStructureInfo) {
                file.seek(file.getFilePointer() - 6);
            } else {
                // No structure info, must be pre Sep 2002 database, go back to
                // end.
                file.seek(file.length() - 3);
            }
            // Find the database info string.
            for (int i = 0; i < DATABASE_INFO_MAX_SIZE; i++) {
                file.readFully(delim);
                if (delim[0] == 0 && delim[1] == 0 && delim[2] == 0) {
                    byte[] dbInfo = new byte[i];
                    file.readFully(dbInfo);
                    // Create the database info object using the string.
                    databaseInfo = new DatabaseInfo(new String(dbInfo, charset));
                    return databaseInfo;
                }
                file.seek(file.getFilePointer() - 4);
            }
        } catch (IOException e) {
            throw new InvalidDatabaseException("Error reading database info", e);
        }
        return new DatabaseInfo("");
    }
}
