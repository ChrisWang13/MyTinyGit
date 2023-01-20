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
     *              |--master (latest commitID in branch)
     *              |--OtherBranchName (latest commitID in branch)
     *            |--staging-index (File with saved Staging info)
     *      |--HEAD (ref: refs/heads/branch?)(Contents should String name of branch)
     *      |--staging (Staged folder file, real git put staged blobs in objects folder)
     *
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJ_DIR = join(GITLET_DIR, "objects");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");

    /** Pointer file to record last operation. */
    public static final File STAGING_DIR = join(GITLET_DIR, "staging");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File STAGING_INDEX = join(REFS_DIR, "staging-index");

    /** Read from STAGING_INDEX file to check Staging status. */
    private static Staging curStage = new Staging();

    /** Read from CommitID file in obj folder to check Commit status. */
    private static Commit curCommit = new Commit();

    /** Read from HEAD file to get current branch name. */
    private static String curBranchName;

    /** gitlet init function */
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already " +
                    "exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdirs();
        OBJ_DIR.mkdirs();
        REFS_DIR.mkdirs();
        HEADS_DIR.mkdirs();
        STAGING_DIR.mkdirs();
        // Init HEAD, Write String name master to HEAD File
        Utils.writeObject(HEAD, "master");
        curBranchName = getCurBranchName();
        curCommit.saveCommit(curBranchName);
        curStage.saveStaging();
    }

    /** Inputs a command that requires containing a .gitlet subdirectory */
    private static void checkGitletExists() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /** Helper function to return file reference with fileName(String).
     *  Exit when file does not exist in current folder.
     */
    private static File getFileFromCWD(String fileName) {
        File file = join(CWD, fileName);
        // File does not exist, exit!
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        return file;
    }

    /** Helper function to return String branchName in HEAD file. */
    private static String getCurBranchName() {
        return Utils.readObject(HEAD, String.class);
    }

    /** Helper function to return Staging from persistent STAGING_INDEX. */
    private static Staging getCurStage() {
        return Utils.readObject(STAGING_INDEX, Staging.class);
    }

    /** Helper function to return Commit from persistent HEAD pointer to commit. */
    private static Commit getCurCommit() {
        // Read from HEAD file for current branch name
        curBranchName = getCurBranchName();
        // File reference to HEADS
        File ref = join(HEADS_DIR, curBranchName);
        // Read from ref about current commitID
        String commitID = Utils.readObject(ref, String.class);
        // Search for commit File in object folder
        File commitFile = join(OBJ_DIR, commitID);
        // Return latest commit class in current branch
        return Utils.readObject(commitFile, Commit.class);
    }

    /** gitlet add function */
    public static void add(String fileName) {
        File addFile = getFileFromCWD(fileName);
        String filePath = CWD + "/" + fileName;
        // 1. Compare blobID of file in current Commit of this file,
        // if same, don't create new blob to save space
        curCommit = getCurCommit();
        curStage = getCurStage();
        String curCommitBlobID = curCommit.getCommitFileBlobID(filePath);
        String curBlobID = Utils.sha1(Arrays.toString(Utils.readContents(addFile)), filePath);
        if (curCommitBlobID != null && curCommitBlobID.equals(curBlobID)) {
            // System.out.println("Same contents with last commit");
            // Not staged for removal, unstage file in rmBlob
            curStage.notStaging4Removal(filePath);
            System.exit(0);
        }
        // System.out.println("Diff contents with last commit");

        // 2. Compare blobID of file in current Staging,
        // if same, don't add to Staging area
        String curStagingBlobID = curStage.getStagingFileBlobID(filePath);
        if (curStagingBlobID != null && curStagingBlobID.equals(curBlobID)) {
            // System.out.println("Same contents with last Staging");
            System.exit(0);
        }
        // System.out.println("Diff contents with last Staging");

        // 3. All checked, create matched blob with file
        Blob blob = new Blob(addFile);
        curStage.saveBlob2Staging(blob);
    }

    /** gitlet commit function. */
    public static void commit(String message) {
        // Init current status
        curStage = getCurStage();
        curBranchName = getCurBranchName();
        // Failed case
        if (curStage.isStagingEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        // create new commit with init info: parent Commit id
        Commit curCommit = getCurCommit();
        String parentID = curCommit.getID();
        Commit newCommit = new Commit(curStage, parentID, message);

        newCommit.saveCommit(curBranchName);
        // Remove Staging area
        curStage.rmStagingArea();
    }

    /** gitlet log function. */
    public static void log() {
        curCommit = getCurCommit();
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
        // Do not use getFileFromCWD, file might not exist with unix rm
        File rmFile = join(CWD, fileName);
        // rmFile.getPath() might be null, use string concatenation
        String filePath = CWD + "/" + fileName;
        curStage = getCurStage();
        curCommit = getCurCommit();
        if (curStage.isFileInStaging(filePath)) {
            // 1. Remove file from Staging area if in current Staging
            curStage.rmFileInStaging(filePath);
        } else if (curCommit.isFileInCommit(filePath)) {
            // 2. Remove file if it is in current commit (tracked in MASTER_PTR)
            // Stage the file for removal
            curStage.rmFileInStaging(filePath);
            // If removed before with unix rm cmd
            if (rmFile.exists()) {
                rmFile.delete();
            }
        } else {
            // 3. This file is neither staged nor tracked by this commit
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** gitlet status function. */
    public static void status() {
        checkGitletExists();
        // prefix of filePath
        String cwd = CWD.getPath();
        System.out.println("=== Branches ===");
        ArrayList<String> branchList= new ArrayList<>(Utils.plainFilenamesIn(HEADS_DIR));
        curBranchName = getCurBranchName();
        for (String br : branchList) {
            if (br.equals(curBranchName)) {
                System.out.println("*" + curBranchName);
                continue;
            }
            System.out.println(br);
        }
        System.out.println();
        // 2. Staged file in current Staging area
        // Use set to print in lexicographic order
        curStage = getCurStage();
        // Careful with reference!
        Set<String> addFilePathSet = curStage.getAddBlobs().keySet();
        System.out.println("=== Staged Files ===");
        for (String addFilePath: addFilePathSet) {
            if (addFilePath.startsWith(cwd)) {
                System.out.println(addFilePath.substring(cwd.length() + 1));
            }
        }
        System.out.println();
        // 3. Removed files from current staging romove area
        curCommit = getCurCommit();
        Set<String> rmFilePathSet = curStage.getRmBlobs();
        System.out.println("=== Removed Files ===");
        for (String rmFilePath: rmFilePathSet) {
            if (rmFilePath.startsWith(cwd)) {
                System.out.println(rmFilePath.substring(cwd.length() + 1));
            }
        }
        System.out.println();

        // 4. Only with regard to curCommit except removed files
        System.out.println("=== Modifications Not Staged For Commit ===");
        // setAll contains all except new created files, which is handled in untracked.
        Set<String> setAll = curCommit.getSavedBlobs().keySet();
        // setRm contains files to be removed
        Set<String> setRm = curStage.getRmBlobs();
        // Not staged for removal
        setAll.removeAll(setRm);

        File[] dirListing = CWD.listFiles();
        if (dirListing != null) {
            for (File child: dirListing) {
                // Ignore subdirectory like .gitlet
                if (child.isDirectory()) {
                    continue;
                }
                String childFilePath = child.getPath();
                // If child is not in set, then it should be "deleted"
                setAll.remove(childFilePath);
                // Compare blobID of file in current Commit of this file
                String curCommitBlobID = curCommit.getCommitFileBlobID(childFilePath);
                String curBlobID = Utils.sha1(Arrays.toString(Utils.readContents(child)),
                        childFilePath);
                if (curCommitBlobID != null && !curCommitBlobID.equals(curBlobID)) {
                    // tracked: curCommitBlobID != null
                    System.out.println(childFilePath.substring(cwd.length() + 1) + "(modified)");
                }
            }
        }

        for (String s : setAll) {
            System.out.println(s.substring(cwd.length() + 1) + "(deleted)");
        }
        System.out.println();

        // 5. Files without gitlet knowledge, only for files in CWD.
        System.out.println("=== Untracked Files ===");
        if (dirListing != null) {
            for (File child: dirListing) {
                // Ignore subdirectory like .gitlet
                if (child.isDirectory()) {
                    continue;
                }
                String childFilePath = child.getPath();
                // Get back to current head
                curCommit = getCurCommit();
                // Check tracked?
                boolean isTracked = curCommit.isFileInCommit(childFilePath);
                while (!curCommit.getFirstParentID().isEmpty()) {
                    if (isTracked) {
                        break;
                    }
                    isTracked = curCommit.isFileInCommit(childFilePath);
                    // Update curCommit with parent id file in object folder
                    String pid = curCommit.getFirstParentID();
                    File parent = join(OBJ_DIR, pid);
                    curCommit = Utils.readObject(parent, Commit.class);
                }
                // Check in staging?
                boolean isStaging = curStage.isFileInStaging(childFilePath);
                if (!isTracked && !isStaging) {
                    System.out.println(childFilePath.substring(cwd.length() + 1));
                }
            }
        }
        System.out.println();
    }


    /** gitlet branch function. */
    public static void branch(String branchName) {
        // TODO Handle duplicate branch name
        // System.out.println("A branch with that name already exists.");
        // Create a branch == create a new file and save commitID to branchFile
        curCommit = getCurCommit();
        curCommit.saveCommit(branchName);
    }

    /** gitlet checkout function. */
    public static void checkoutBranch(String branchName) {
        // TODO Fail case: branch does not exist
        // File branchName to overwrite HEAD file
        Utils.writeObject(HEAD, branchName);
    }
}
