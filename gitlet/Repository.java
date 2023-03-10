package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.nio.charset.StandardCharsets;

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

    /** Helper function to return Commit with given branchName. */
    private static Commit getCommit(String branchName) {
        // File reference to branch
        File ref = join(HEADS_DIR, branchName);
        // Read from ref about branch commitID
        String brCommitID = Utils.readObject(ref, String.class);
        // Search for commit File in object folder
        File brCommitFile = join(OBJ_DIR, brCommitID);
        // Return latest commit class in current branch
        return Utils.readObject(brCommitFile, Commit.class);
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
        // Create new commit with init info: parent Commit id
        Commit newCommit = new Commit(curCommit, null, curStage, message);
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
        while (!curCommit.getFirstParentID().equals("")) {
            // Print info
            curCommit.printLogInfo();
            // Update curCommit with parent id file in object folder
            String pid = curCommit.getFirstParentID();
            File parent = join(OBJ_DIR, pid);
            curCommit = Utils.readObject(parent, Commit.class);
        }
        // Only initial commit, print info
        assert (curCommit.getFirstParentID().equals(""));
        curCommit.printLogInfo();
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
            // 2. Remove file if it is in current commit, stage the file for removal
            curStage.rmFileInStaging(filePath);
            curCommit.untrackFileInCommit(filePath);
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
        ArrayList<String> branchList = new ArrayList<>(Utils.plainFilenamesIn(HEADS_DIR));
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
        curStage = getCurStage();
        Set<String> addSet = curStage.getAddBlobs().keySet();
        System.out.println("=== Staged Files ===");
        for (String filePath: addSet) {
            System.out.println(filePath.substring(cwd.length() + 1));
        }
        System.out.println();

        // 3. Removed files from current staging remove area
        curCommit = getCurCommit();
        Set<String> rmSet = curStage.getRmBlobs();
        System.out.println("=== Removed Files ===");
        for (String filePath: rmSet) {
            System.out.println(filePath.substring(cwd.length() + 1));
        }
        System.out.println();

        // 4. Modifications Not Staged For Commit
        System.out.println("=== Modifications Not Staged For Commit ===");
        // commitAll contains files to be tracked
        Map<String, String> commitAll = curCommit.getSavedBlobs();
        // setAdd contains files to be staged
        Map<String, String> stageAdd = curStage.getAddBlobs();
        Set<String> stageRm = curStage.getRmBlobs();
        // Read cwd fileName
        ArrayList<String> cwdFileName = new ArrayList<>(Utils.plainFilenamesIn(CWD));
        Set<String> modified = new TreeSet<>();
        Set<String> deleted = new TreeSet<>();
        // Case 1 and 4
        for (String filePath : commitAll.keySet()) {
            String fileName = filePath.substring(cwd.length() + 1);
            File f = join(CWD, fileName);
            if (f.exists()) {
                String curBlobID = Utils.sha1(Utils.readContents(f), filePath);
                if (!commitAll.get(filePath).equals(curBlobID) && !stageAdd.containsKey(filePath)) {
                    // case 1: Tracked in current Commit, contents changed with prev commit, but not staged
                    modified.add(fileName);
                }
            }
            else if (!stageRm.contains(filePath)) {
                // case 4: Tracked in current Commit, File not in CWD, Not staged for removal
                deleted.add(fileName);
            }
        }
        // Case 2 and 3
        for (String filePath : stageAdd.keySet()) {
            String fileName = filePath.substring(cwd.length() + 1);
            File f = join(CWD, fileName);
            if (f.exists()) {
                String curBlobID = Utils.sha1(Utils.readContents(f), filePath);
                if (!stageAdd.get(filePath).equals(curBlobID)) {
                    // case 2: In current Staging, File in CWD, contents changed
                    modified.add(fileName);
                }
            }
            else {
                // case 3: In current Staging, File not in CWD
                deleted.add(fileName);
            }
        }

        for (String s : modified) {
            System.out.println(s + "(modified)");
        }
        for (String s : deleted) {
            System.out.println(s + "(deleted)");
        }
        System.out.println();

        // 5. Files without gitlet knowledge, only for files in CWD.
        System.out.println("=== Untracked Files ===");
        for (String fileName : cwdFileName) {
            String filePath = CWD + "/" + fileName;
            // Check tracked?
            boolean isTracked = curCommit.isFileInCommit(filePath);
            // Check in staging?
            boolean isStaging = curStage.isFileInStaging(filePath);
            if (!isTracked && !isStaging) {
                System.out.println(fileName);
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
        // Fail case: File untracked in current branch and would be overwritten by checkout
        // Real git do not clear staging area, and stage all files that is checkout out
        // Real git won't do checkout that would overwrite or undo changes on staged files
        Set<String> curBrFileList = getCurCommit().getSavedBlobs().keySet();
        List<String> list = Utils.plainFilenamesIn(CWD);
        for (String fileName: list) {
            String filePath = CWD + "/" + fileName;
            if (!curBrFileList.contains(filePath)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** gitlet checkout function.
     *  Files are all tracked to branch. Three use cases are:
     *      curBranch           newBranch
     *      File1               File1 (Overwrite File1 in cwd)
     *      File2               File2 is null (If File2 exist in cwd, delete File2)
     *      File3 is null       File3 (Overwrite File3 in cwd)
     * */
    public static void checkoutBranch(String branchName) {
        checkoutBranchIsFailed(branchName);
        // current branchFileList
        Set<String> curBrFileList = getCurCommit().getSavedBlobs().keySet();
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
        for (String path : curBrFileList) {
            File f = new File(path);
            // Files are not tracked in checkout branch. Delete the file if in cwd.
            if (!newBrFileList.contains(path) && f.exists()) {
                f.delete();
            }
        }
        // Clear staging area, unless checkout branch is current branch
        curStage = getCurStage();
        curStage.rmStagingArea();
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

    /** Helper function to get parent CommitID in distance order. */
    private static List<String> getBfsList(Commit commit) {
        Queue<String> q = new ArrayDeque<>();
        List<String> res = new ArrayList<>();
        Commit cur = commit;
        res.add(cur.getID());
        while (!cur.getFirstParentID().equals("")) {
            String p1 = cur.getFirstParentID();
            String p2 = cur.getMergeParentID();
            if (p1 != null && !q.contains(p1)) {
                q.add(p1);
            }
            if (p2 != null && !q.contains(p2)) {
                q.add(p2);
            }
            String head = q.poll();
            res.add(head);
            File f = join(OBJ_DIR, head);
            cur = Utils.readObject(f, Commit.class);
        }
        return res;
    }

    /** Helper function to get split point Commit object. */
    private static Commit getSplitPointCommit(Commit a, Commit b) {
        List<String> parentA = getBfsList(a);
        Set<String> parentB = b.getParents();
        String resID = null;
        for (String pid : parentA) {
            if (parentB.contains(pid)) {
                resID = pid;
                break;
            }
        }
        // Read commit with resID
        File commitFile = Utils.join(OBJ_DIR, resID);
        return readObject(commitFile, Commit.class);
    }

    /** gitlet merge function. */
    public static void merge(String branchName) {
        // Special merge case 1: Split point is same as given branch
        // Special merge case 2: Fast forward merge
        Commit splitPoint = getSplitPointCommit(getCurCommit(), getCommit(branchName));
        curCommit = getCurCommit();
        curStage = getCurStage();
        curBranchName = getCurBranchName();
        Commit brCommit = getCommit(branchName);
        if (splitPoint.getID().equals(brCommit.getID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint.getID().equals(getCurCommit().getID())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        // Update Update tracked file in mergeCommit.
        Map<String, String> updateMerge = new HashMap<>();
        Map<String, String> delMerge = new HashMap<>();
        // Use set to union all fileNames in splitCommit, curCommit and brCommit
        Set<String> all = new HashSet<>();
        all.addAll(splitPoint.getSavedBlobs().keySet());
        all.addAll(curCommit.getSavedBlobs().keySet());
        all.addAll(brCommit.getSavedBlobs().keySet());

        for (String filePath : all) {
            File f = new File(filePath);
            String cwd = CWD.getPath();
            String fileName = filePath.substring(cwd.length() + 1);
            // If found in history, check if blob match (modified? deleted?) in other commit
            String splitID = splitPoint.getCommitFileBlobID(filePath);
            String curID = curCommit.getCommitFileBlobID(filePath);
            String brID = brCommit.getCommitFileBlobID(filePath);
            if (splitID.equals(curID)) {
                // Case 6: remove the file and untrack the file
                // Present in splitCommit and unmodified at curCommit, absent in brCommit
                if (brID.equals("")) {
                    delMerge.put(filePath, curID);
                    f.delete();
                    curCommit.untrackFileInCommit(filePath);
                    curCommit.saveCommit(curBranchName);
                } else {
                    // Checkout the file in brCommit (Not empty) and stage the file
                    // case 1: Present in splitCommit, not modified in curCommit, modified in brCommit
                    // Case 5: Not present in splitCommit, not present in curCommit, present in brCommit
                    checkoutCommitID(brCommit.getID(), fileName);
                    add(fileName);
                }
            } else if (!splitID.equals(brID) && !curID.equals(brID)) {
                // !splitID.equals(curID) && !splitID.equals(brID) && !curID.equals(brID)
                // curCommit and brCommit are modified in different way
                String curContents = "";
                String brContents = "";

                if (curID != "") {
                    File blobFile = join(OBJ_DIR, curID);
                    Blob blob = Utils.readObject(blobFile, Blob.class);
                    curContents = new String(blob.getContents(), StandardCharsets.UTF_8);
                }
                if (brID != "") {
                    File blobFile = join(OBJ_DIR, brID);
                    Blob blob = Utils.readObject(blobFile, Blob.class);
                    brContents = new String(blob.getContents(), StandardCharsets.UTF_8);
                }
                String conflictContents = "<<<<<<< HEAD" + "\n" + curContents  + "=======" + "\n"
                       + brContents + ">>>>>>>" + "\n";
                Utils.writeContents(f, conflictContents);
                Blob blob = new Blob(f);
                updateMerge.put(filePath, blob.getBlobID());
                add(fileName);
                System.out.println("Encountered a merge conflict.");
            }
        }
        String commitMsg = "Merged " + branchName + " into " + getCurBranchName() + ".";
        // Make merge commit
        Commit mergeCommit = new Commit(curCommit, brCommit, curStage, commitMsg);
        // Update tracked file in mergeCommit.
        mergeCommit.updateMergeCommitFile(updateMerge, delMerge);
        // Save current CommitID to branchFile
        mergeCommit.saveCommit(curBranchName);
        curStage.rmStagingArea();
    }

}
