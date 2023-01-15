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
        // blobID is fileName and filePath, used only for this file.
        // Overwrite entry if already staged
        String newFileName = blob.blobID;
        File newFile = Utils.join(STAGING_DIR, newFileName);
        Utils.writeObject(newFile, blob);
        this.saveStaging();
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
