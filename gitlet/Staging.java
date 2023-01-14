package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Repository.*;

/** Staging use ADD_STAGE file to persist staging info.
 *
 */
public class Staging implements Serializable {

    /** Shared list to store blobs, add file to storeBlobs in Staging area. */
    public List<Blob> storeBlobs = new ArrayList<>();

    /** Save blob to .gitlet/staging/blob.getFileName() */
    public void saveBlob2Staging(Blob blob) {
        storeBlobs.add(blob);
        // Write staged file to staging folder.
        // blobID is fileName and filePath, used only for this file.
        // Overwrite entry if already staged
        String newFileName = blob.blobID;
        File newFile = Utils.join(STAGING_DIR, newFileName);
        Utils.writeObject(newFile, blob);
    }

    /** Remove Staging area and write it to ADDSTAGE_PTR for persistence. */
    public void removeStagingArea() {
        storeBlobs.clear();
        // Save cleared storeBlob list to ADDSTAGE_PTR
        Utils.writeObject(ADDSTAGE_PTR, this);
    }
}
