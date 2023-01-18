package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * Blob is an abstraction above basic file metadata.
 * In staging area, One blob match with one staged file.
 * Always init Blob with a file.
 */

public class Blob implements Serializable {

    /** Specifier of this blob, by SHA-1 hashing. */
    private final String blobID;

    /** Path of this file in fileName.getPath(). */
    private final String filePath;

    /** String representation of file. */
    private final String contents;

    /** One blob match with one file. */
    Blob(File file) {
        this.filePath = file.getPath();
        this.contents = Arrays.toString(Utils.readContents(file));
        this.blobID = Utils.sha1(contents, filePath);
    }

    /** Return blobID as value of HashMap. */
    public String getBlobID() {
        return blobID;
    }

    /** Return filePath, use substring to get fileName. */
    public String getFilePath() {
        return filePath;
    }

}
