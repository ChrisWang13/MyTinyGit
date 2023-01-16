package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *  @author ChrisWang13
 */
public class Repository {

    /*
     *   .gitlet
     *      |--objects
     *      |     |--commit
            |--refs
                  |--heads
     *              |--master (File with saved Commit info)
     *              |--OtherBranchName (File with saved Commit info)
     *            |--addStage (File with saved Staging info)
     *      |--HEAD (File, read current head of branch) (refs/heads/branch?)
     *      |--staging (staged folder file, real git put staged blobs in objects folder)
     *
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJ_DIR = join(GITLET_DIR, "objects");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    public static final File STAGING_DIR = join(GITLET_DIR, "staging");

    /** Pointer file to record last operation. */
    // TODO: Currently HEAD is always MASTER_PTR, branch is not implemented.
    public static final File MASTER_PTR = join(HEADS_DIR, "master");
    public static final File ADDSTAGE_PTR = join(REFS_DIR, "addstage");

    /** Read from ADDSTAGE_PTR file to check Staging status. */
    public static Staging curStage = new Staging();

    /** Read from HEAD file to check Commit status. */
    public static Commit curCommit = new Commit();

    /** gitlet init function */
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdirs();
        OBJ_DIR.mkdirs();
        REFS_DIR.mkdirs();
        HEADS_DIR.mkdirs();
        STAGING_DIR.mkdirs();
        // Init commit and empty Staging area
        curCommit.saveCommit();
        curStage.saveStaging();
    }

    /** Helper function to return Staging from persistent ADDSTAGE_PTR. */
    private static Staging getCurStage() {
        return Utils.readObject(ADDSTAGE_PTR, Staging.class);
    }

    /** Helper function to return file reference with fileName(String). */
    private static File getFileFromCWD(String fileName) {
        File file = join(CWD, fileName);
        // File does not exist
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        return file;
    }

    /** Helper function to return Staging from persistent ADDSTAGE_PTR. */
    private static Commit getCurCommit() {
        return Utils.readObject(MASTER_PTR, Commit.class);
    }

    /** gitlet add function */
    public static void add(String fileName) {
        File addFile = getFileFromCWD(fileName);
        // 1. Compare blobID of file in current Commit of this file,
        // if same, don't create new blob to save space
        curCommit = getCurCommit();
        String curCommitBlobID = curCommit.getCommitFileBlobID(addFile.getPath());
        String curBlobID = Utils.sha1(Arrays.toString(Utils.readContents(addFile)), addFile.getPath());
        if (curCommitBlobID != null && curCommitBlobID.equals(curBlobID)) {
            System.out.println("Same contents with last commit");
            System.exit(0);
        }
        System.out.println("Diff contents with last commit");

        // 2. Compare blobID of file in current Staging,
        // if same, don't add to Staging area
        curStage = getCurStage();
        String curStagingBlobID = curStage.getStagingFileBlobID(addFile.getPath());
        if (curStagingBlobID != null && curStagingBlobID.equals(curBlobID)) {
            System.out.println("Same contents with last Staging");
            System.exit(0);
        }
        System.out.println("Diff contents with last Staging");

        // 3. All checked, create matched blob with file
        Blob blob = new Blob(addFile);
        curStage.saveBlob2Staging(blob);
    }

    /** gitlet commit function. */
    public static void commit(String message) {
        curStage = getCurStage();
        if (curStage.IsStagingEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        // create new commit with init info: parent Commit id
        Commit curCommit = getCurCommit();
        String parentID = curCommit.getID();
        Commit newCommit = new Commit(curStage, parentID, message);
        newCommit.saveCommit();
        // Remove Staging area
        curStage.rmStagingArea();
    }

    /** gitlet log function. */
    public static void log() {
        Commit curCommit = getCurCommit();
        // Get parentID and open file iteratively
        while (!curCommit.getFirstParentID().isEmpty()) {
            // Print info
            curCommit.printLogInfo();
            // Update curCommit with parent id file in object folder
            String pid = curCommit.getFirstParentID();
            File parent = join(OBJ_DIR, pid);
            curCommit = Utils.readObject(parent, Commit.class);
        }
        if (curCommit.getFirstParentID().isEmpty()) {
            // Only initial commit, print info
            curCommit.printLogInfo();
        }
     }

     /** gitlet rm function. */
     public static void rm(String fileName) {
         File rmFile = getFileFromCWD(fileName);
         String filePath = rmFile.getPath();
         curStage = getCurStage();
         curCommit = getCurCommit();
         // 1. Remove file from Staging area if in current Staging
         if (curStage.IsFileInStaging(filePath)) {
             curStage.rmFileInStaging(filePath);
         }
         // TODO: 2. Remove file if it is in current commit (tracked in MASTER_PTR)
         else if(curCommit.IsFileInCommit(filePath)) {
             curCommit.rmFileInCommit(filePath);
             // Delete this file
             rmFile.delete();
         }
         // 3. This file is neither staged nor tracked by this commit
         else {
             System.out.println("No reason to remove the file.");
             System.exit(0);
         }
     }

}
