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

    /** One blob match with one file */
    File file;

    /** Path of this file in fileName.getPath(). */
    String filePath;

    /** Byte representation of file. */
    private byte[] contents;

    Blob(File file) {
        this.file = file;
        this.filePath = file.getPath();
        this.contents = Utils.readContents(file);
    }

    public String getFileName() {
        if (file.isDirectory()) {
            System.out.println("This is a directory! Not a file!");
            System.exit(0);
        }
        return file.getName();
    }

}
