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
     *  add files to addBlobs in Staging area. */
    private Map<String, String> addBlobs = new TreeMap<>();

    /** Remove file in this Staging area. */
    private Set<String> rmBlobs = new TreeSet<>();

    /** Check if addBlobs list is empty. */
    public boolean isStagingEmpty() {
        return addBlobs.isEmpty() && rmBlobs.isEmpty();
    }

    /** Check if given file is in addBlobs list. */
    public boolean isFileInStaging(String filePath) {
        return addBlobs.containsKey(filePath);
    }

    /** Remove file in addBlob map.  */
    public void rmFileInStaging(String filePath) {
        if (addBlobs.containsKey(filePath)) {
            addBlobs.remove(filePath);
        } else {
            // File not in staging area
            rmBlobs.add(filePath);
        }
        this.saveStaging();
    }

    /** Save blob with info and write it to STAGING_INDEX for persistence. */
    public void saveBlob2Staging(Blob blob) {
        addBlobs.put(blob.getFilePath(), blob.getBlobID());
        // Write staged file to staging folder.
        // SHA1-Hash of filePath as staging file entry name(String),
        // easy to overwrite if file is already staged.
        String newFileName = Utils.sha1(blob.getFilePath());
        File newFile = Utils.join(STAGING_DIR, newFileName);
        // Write blobs to Staging file
        Utils.writeObject(newFile, blob);
        this.saveStaging();
    }

    /** Return BlobID of this file in current commit. */
    public String getStagingFileBlobID(String filePath) {
        if (this.isFileInStaging(filePath)) {
            return this.addBlobs.get(filePath);
        }
        // File not found in this Staging
        return null;
    }

    /** Save current Staging config to STAGING_INDEX. */
    public void saveStaging() {
        Utils.writeObject(STAGING_INDEX, this);
    }

    /** Remove Staging area and write it to STAGING_INDEX for persistence. */
    public void rmStagingArea() {
        addBlobs.clear();
        rmBlobs.clear();
        // Save cleared addBlob list to STAGING_INDEX
        this.saveStaging();
    }

    /** Not staged for removal, unstage file in rmBlobs. */
    public void notStaging4Removal(String filePath) {
        this.rmBlobs.remove(filePath);
        this.saveStaging();
    }

    /** Return a new copied rmBlobs. */
    public Set<String> getRmBlobs() {
        return new TreeSet<>(rmBlobs);
    }

    /** Return a new copied addBlobs. */
    public Map<String, String> getAddBlobs() {
        return new TreeMap<>(addBlobs);
    }
}
