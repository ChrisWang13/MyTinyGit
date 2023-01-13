package gitlet;

import java.io.File;
import java.util.*;

import gitlet.Repository.*;

import static gitlet.Repository.STAGING_DIR;
import static gitlet.Utils.serialize;


/**
 * All methods in Staging should be static
 */

public class Staging {

    /** Shared list to store blobs, add file to storeBlobs in Staging area. */
    static List<Blob> storeBlobs = new ArrayList<>();

    /** Save blob to .gitlet/staging/blob.getFileName() */
    public static void saveBlob2Staging(Blob blob) {
        storeBlobs.add(blob);
        String newFileName = blob.getFileName();
        File newFile = Utils.join(STAGING_DIR, newFileName);
        Utils.writeObject(newFile, serialize(blob));
    }

    /** Clear the staging area after one complete commit. */
    public static void clearStaging() {
        storeBlobs.clear();
    }
}
