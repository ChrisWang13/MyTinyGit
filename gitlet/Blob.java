package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * Blob is an abstraction above basic file metadata.
 * In staging area, One blob match with one staged file.
 * Always init Blob with a file.
 */

public class Blob implements Serializable {

    /** Specifier of this blob, by SHA-1 hashing. */
    public String blobID;

    /** One blob match with one file. */
    private File file;

    /** Path of this file in fileName.getPath(). */
    private String filePath;

    /** Byte representation of file. */
    private byte[] contents;

    Blob(File file) {
        this.file = file;
        this.filePath = file.getPath();
        this.contents = Utils.readContents(file);
        this.blobID = Utils.sha1(filePath, getFileName());
    }

    public String getBlobID() {
        return blobID;
    }

    public String getFileName() {
        return file.getName();
    }

}
