package gitlet;

import java.io.File;
import java.io.Serializable;
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
     *      |     |--commitID File
     *            |--BlobsID File(Staged blobs)
            |--refs
                  |--heads
     *              |--master (latest commitID in branch)
     *              |--OtherBranchName (latest commitID in branch)
     *            |--staging-index (File with saved Staging info)
     *      |--HEAD (ref: refs/heads/branch?)(Contents should String name of branch)
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
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File STAGING_INDEX = join(REFS_DIR, "staging-index");

    public static final File COMMIT_ID_INDEX = join(OBJ_DIR, "commit-id-index");

    /** Read from COMMIT_ID_INDEX file to check prefix of commitID */
    public static TrieIndex prefixCommitID = new TrieIndex();

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

    private static boolean checkAddIsNeeded(String fileName) {
        File addFile = getFileFromCWD(fileName);
        String filePath = CWD + "/" + fileName;
        // 1. Compare blobID of file in current Commit of this file,
        // if same, don't create new blob to save space
        curCommit = getCurCommit();
        curStage = getCurStage();
        String curCommitBlobID = curCommit.getCommitFileBlobID(filePath);
        String curBlobID = Utils.sha1(Utils.readContents(addFile), filePath);
        if (curCommitBlobID != null && curCommitBlobID.equals(curBlobID)) {
            // System.out.println("Same contents with last commit");
            // Not staged for removal, unstage file in rmBlob
            curStage.notStaging4Removal(filePath);
            return false;
        }
        // System.out.println("Diff contents with last commit");

        // 2. Compare blobID of file in current Staging,
        // if same, don't add to Staging area
        String curStagingBlobID = curStage.getStagingFileBlobID(filePath);
        if (curStagingBlobID != null && curStagingBlobID.equals(curBlobID)) {
            // System.out.println("Same contents with last Staging");
            return false;
        }
        // System.out.println("Diff contents with last Staging");
        return true;
    }

    /** gitlet add function */
    public static void add(String fileName) {
        File addFile = getFileFromCWD(fileName);
        if (checkAddIsNeeded(fileName)) {
            // Create matched blobFile is needed.
            Blob blobFile = new Blob(addFile);
            curStage.saveBlob2Staging(blobFile);
        }
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

        Commit curCommit = getCurCommit();
        String parentID = curCommit.getID();
        // Create new commit with init info: parent Commit id
        Commit newCommit = new Commit(curStage, parentID, message);
        // Save current CommitID to branchFile
        newCommit.saveCommit(curBranchName);
        // TrieIndex for object Commit in obj folder
        if (COMMIT_ID_INDEX.exists()) {
            prefixCommitID = Utils.readObject(COMMIT_ID_INDEX, TrieIndex.class);
        }
        prefixCommitID.add(newCommit.getID());
        Utils.writeObject(COMMIT_ID_INDEX, prefixCommitID);
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
        // Prefix of filePath
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
                String curBlobID = Utils.sha1(Utils.readContents(child),
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
        // Fail case: duplicate-branch-err
        File branchFile = join(HEADS_DIR, branchName);
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        // Create a branch == create a new file and save commitID to branchFile
        curCommit = getCurCommit();
        curCommit.saveCommit(branchName);
    }

    /** Helper function to handle fail case in checkout branch. */
    private static void checkoutBranchIsFailed(String branchName) {
        // Fail case: branch does not exist
        File branchFile = Utils.join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        // Fail case: checkout current branch
        curBranchName = getCurBranchName();
        if (curBranchName.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
    }

    /** gitlet checkout function.
     *  Files are all tracked to branch. Three use cases are:
     *      oldBranch           newBranch
     *      File1               File1 (Overwrite File1 in cwd)
     *      File2               File2 is null (If File2 exist in cwd, delete File2)
     *      File3 is null       File3 (Overwrite File3 in cwd)
     * */
    public static void checkoutBranch(String branchName) {
        checkoutBranchIsFailed(branchName);
        // Old branchFileList
        Set<String> oldBrFileList = getCurCommit().getSavedBlobs().keySet();
        // Update branch with new branch(branchName)
        Utils.writeObject(HEAD, branchName);
        // New branchFileList
        Set<String> newBrFileList = getCurCommit().getSavedBlobs().keySet();
        // Case 1 and 3. Overwrite all files in newBranch
        for (String path : newBrFileList) {
            File f = new File(path);
            // Files are tracked in checkout branch. Overwrite.
            overWriteFileWithCommit(getCurCommit(), f.getName());
        }
        // Case 2, delete Set(old - new) file
        for (String path : oldBrFileList) {
            File f = new File(path);
            // Files are not tracked in checkout branch. Delete the file if in cwd.
            if (!newBrFileList.contains(path) && f.exists()) {
                f.delete();
            }
        }
    }

    /** Helper function to overwrite file with given commit. */
    private static void overWriteFileWithCommit(Commit commit, String fileName) {
        String filePath = CWD + "/" + fileName;
        if (!commit.isFileInCommit(filePath)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File oldFile = new File(filePath);
        String newBlobID = commit.getCommitFileBlobID(filePath);
        File blobFile = join(OBJ_DIR, newBlobID);
        Blob blob = Utils.readObject(blobFile, Blob.class);
        // Overwrite oldFile contents with Blob in obj folder
        Utils.writeContents(oldFile, blob.getContents());
    }

    /** gitlet checkout -- [file name] function. */
    public static void checkoutFileName(String fileName) {
        curCommit = getCurCommit();
        // Pass current commit
        overWriteFileWithCommit(curCommit, fileName);
    }

    /** gitlet checkout [commit id] -- [file name]. */
    public static void checkoutCommitID(String commitID, String fileName) {
        // commitID might be in abbreviate form
        prefixCommitID = Utils.readObject(COMMIT_ID_INDEX, TrieIndex.class);
        String realCommitID = prefixCommitID.matchSixDigit(commitID);
        // Fail case
        if (realCommitID == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        File commitFile = Utils.join(OBJ_DIR, realCommitID);
        Commit commit = readObject(commitFile, Commit.class);
        // Pass any commit
        overWriteFileWithCommit(commit, fileName);
    }
}
