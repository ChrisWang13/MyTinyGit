package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
     *              |--master (File, save Commit info)
     *              |--OtherBranchName (File, save Commit info)
     *            |--addStage (File save Staging info)
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
    public static final File MASTER_PTR = join(HEADS_DIR, "master");
    public static final File ADDSTAGE_PTR = join(REFS_DIR, "staging");

    /** Read from staging file to check stage status. */
    public static Staging curStage = new Staging();

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
        // Initial commit
        Commit commit = new Commit();
        commit.saveCommit();
        // Create master head
        Utils.writeObject(MASTER_PTR, commit);
    }

    /** gitlet add function */
    public static void add(String fileName) {
        File addFile = join(CWD, fileName);
        // File does not exist
        if (!addFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        // Create matched blob
        Blob blob = new Blob(addFile);
        // Previous saved list<Blob>
        if (ADDSTAGE_PTR.exists()) {
            Staging oldStage = Utils.readObject(ADDSTAGE_PTR, Staging.class);
            curStage.storeBlobs = oldStage.storeBlobs;
        }
        curStage.saveBlob2Staging(blob);
        Utils.writeObject(ADDSTAGE_PTR, curStage);
    }

    /** gitlet commit function. */
    public static void commit(String message) {
        Staging oldStage = Utils.readObject(ADDSTAGE_PTR, Staging.class);
        if (oldStage.storeBlobs.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        // Remove Staging area
        oldStage.storeBlobs.clear();
        Utils.writeObject(ADDSTAGE_PTR, oldStage);
        // Record previous commit and init new commit
        Commit prevCommit = Utils.readObject(MASTER_PTR, Commit.class);
        String id = prevCommit.getID();
        List<String> parents = new ArrayList<>();
        parents.add(id);
        Commit newCommit = new Commit(parents, message);
        newCommit.saveCommit();
    }
}
