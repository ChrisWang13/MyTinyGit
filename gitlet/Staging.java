package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Repository.*;

/** Staging use ADD_STAGE file to persist staging info.
 *
 */
public class Staging implements Serializable {

    /** HashMap to store map to Blob object(unordered), <Blob pathName, SHA1-hash of Blob>.
     *  add or remove file to storeBlobs in Staging area. */
    public Map<String, String> storeBlobs = new HashMap<>();

    /** Check if storeBlobs list is empty. */
    public boolean IsStagingEmpty() {
        if (storeBlobs.isEmpty()) {
            return true;
        }
        return false;
    }

    /** Check if given file is in storeBlobs list. */
    public boolean IsFileInStaging(String filePath) {
        if (storeBlobs.containsKey(filePath)) {
            return true;
        }
        return false;
    }

    /** Remove file in storeBlob map.  */
    public void rmFileInStaging(String filePath) {
        if (storeBlobs.containsKey(filePath)) {
            storeBlobs.remove(filePath);
        }
        this.saveStaging();
    }

    /** Save blob with info and write it to ADDSTAGE_PTR for persistence. */
    public void saveBlob2Staging(Blob blob) {
        storeBlobs.put(blob.getFilePath(), blob.getBlobID());
        // Write staged file to staging folder.
        // SHA1-Hash of filePath as staging file entry name(String),
        // easy to overwrite if file is already staged.
        String newFileName = Utils.sha1(blob.getFilePath());
        File newFile = Utils.join(STAGING_DIR, newFileName);
        Utils.writeObject(newFile, blob);
        this.saveStaging();
    }

    public String getStagingFileBlobID(String filePath) {
        if (storeBlobs.isEmpty()) {
            return null;
        }
        if (this.IsFileInStaging(filePath)) {
            String stagingName = Utils.sha1(filePath);
            // Open Staging object in Staging folder
            File obj = Utils.join(STAGING_DIR, stagingName);
            Blob blobObj = Utils.readObject(obj, Blob.class);
            return blobObj.getBlobID();
        }
        // File not found in this Staging
        return null;
    }
    /** Save current Staging config to ADDSTAGE_PTR. */
    public void saveStaging() {
        Utils.writeObject(ADDSTAGE_PTR, this);
    }

    /** Remove Staging area and write it to ADDSTAGE_PTR for persistence. */
    public void rmStagingArea() {
        storeBlobs.clear();
        // Save cleared storeBlob list to ADDSTAGE_PTR
        this.saveStaging();
    }
}
