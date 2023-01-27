package gitlet;

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.Serializable;

import static gitlet.Repository.*;


/** Represents a gitlet commit object.
 *  Commit is set to be Serializable to store in object folder.
 *  @author ChrisWang13
 */

public class Commit implements Serializable {
    
    /** The initial Commit message. */
    private String message = "initial commit";

    /** ID of each commit, generated by SHA-1 hash. */
    private final String ID;

    /** String format of current time. */
    private final String timeStamp;

    /** First parent commitID for logging. */
    private String firstParentID = "";

    /** Second parent commitID found in merge commits. */
    private String mergeParentID = null;

    /** Update parent commitFiles with staging info. */
    private Map<String, String> savedBlobs;

    /** Record non-duplicate parent CommitID for BFS getLCA merging. */
    private Set<String> parents;

     /** Create initial commit with default message. */
    public Commit() {
        this.savedBlobs = new TreeMap<>();
        // Unix epoch time
        this.timeStamp = dateToTimeStamp(new Date(0));
        this.ID = setID();
        // Update parents, include itself
        parents = new HashSet<>();
        parents.add(ID);
    }

    /** Create new commit with designed parentsID and message. */
    public Commit(Commit parentCommit, Commit brCommit, Staging stage, String message) {
        this.savedBlobs = setSavedBlobs(parentCommit, stage);
        this.message = message;
        this.firstParentID = parentCommit.getID();
        this.timeStamp = dateToTimeStamp(new Date());
        this.ID = setID();
        this.parents = new HashSet<>(parentCommit.getParents());
        parents.add(ID);
        if (brCommit != null) {
            this.mergeParentID = brCommit.getID();
            parents.addAll(brCommit.getParents());
        }
    }

    /** Copy parent commit info to this commit and update with staging info. */
    private Map<String, String> setSavedBlobs(Commit parentCommit, Staging stage) {
        // All types of commit are copied from parent commit (include merge commit)
        Map<String, String> res = parentCommit.getSavedBlobs();
        Set<String> rmStage = stage.getRmBlobs();
        // Update with addStaging and rmStaging info
        res.putAll(stage.getAddBlobs());
        for (String filePath: rmStage) {
            res.remove(filePath);
        }
        return res;
    }

    /** Update current MergeCommit with conflict info and removed info. */
    public void updateMergeCommitFile(Map<String, String> updateMerge, Map<String, String> delMerge) {
        assert this.mergeParentID != null;
        this.savedBlobs.putAll(updateMerge);
        for (String s : delMerge.keySet()) {
            this.savedBlobs.remove(s);
        }
    }

    /** Formatter helper function to return String format of timeStamp. */
    private String dateToTimeStamp(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        return dateFormat.format(date);
    }

    /** SHA-1 hash to generate ID for this commit. */
    private String setID() {
        return Utils.sha1(savedBlobs.toString(),
                firstParentID, message, timeStamp);
    }

    /** Save current commit to objects folder and save current commitID in branch head. */
    public void saveCommit(String branchName) {
        // Save in obj folder
        File commitFile = Utils.join(OBJ_DIR, this.ID);
        Utils.writeObject(commitFile, this);
        // Save commitID to branch file
        File branchFile = Utils.join(HEADS_DIR, branchName);
        Utils.writeObject(branchFile, this.ID);
    }

    /** Check addBlob HashMap to see map exists. */
    public boolean isFileInCommit(String filePath) {
        return savedBlobs.containsKey(filePath);
    }

    /** Untrack file in current Commit, file is untracked after this operation. */
    public void untrackFileInCommit(String filePath) {
        this.savedBlobs.remove(filePath);
    }
    
    /** Get value of <FilePath, ShA1-Hash> pair, */
    public String getCommitFileBlobID(String filePath) {
        if (this.isFileInCommit(filePath)) {
            return this.savedBlobs.get(filePath);
        }
        // File not found in this commit
        return "";
    }

    /** Get HashSet of parents' CommitID and check in BFS merging. */
    public Set<String> getParents() {
        return new HashSet<>(parents);
    }

    /** Return private ID. */
    public String getID() {
        return ID;
    }

    /** Return firstParentID to create new Commit. */
    public String getFirstParentID() {
        return firstParentID;
    }

    /** Return mergeParentID to BFS in merge. */
    public String getMergeParentID() {
        return mergeParentID;
    }
    /** Return private timeStamp. */
    public String getTimeStamp() {
        return timeStamp;
    }

    /** Return private commit message. */
    public String getMessage() {
        return message;
    }

    /** Return a new copied saveBlobs. */
    public Map<String, String> getSavedBlobs() {
        return new TreeMap<>(savedBlobs);
    }

    /** Helper function to print log info of this commit. */
    public void printLogInfo() {
        System.out.println("===");
        System.out.println("commit " + this.getID());
        if (this.mergeParentID != null) {
            System.out.println("Merge: " + firstParentID.substring(0, 7) + " " + mergeParentID.substring(0, 7));
        }
        System.out.println("Date: " + this.getTimeStamp());
        System.out.println(this.getMessage() + '\n');
    }
}
