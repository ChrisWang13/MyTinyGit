package gitlet;

import java.io.File;
import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *  @author ChrisWang13
 */
public class Repository {

    /*
     *   .gitlet
     *      |--objects
     *      |     |--commit and blob
     *      |--refs
     *      |    |--heads
     *      |         |--master (File)
     *                |--OtherBranchName (File)
     *      |--HEAD (read current head of branch) (refs/heads/branch?)
     *      |--staging (staged file, real git put staged blobs in objects folder)
     *
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File OBJ_DIR = join(GITLET_DIR, "objects");

    public static final File STAGING_DIR = join(GITLET_DIR, "staging");

    /** gitlet init function */
    public static void init() {
        // git init can be only done once in a repo
        // Buggy: Overwrite for now!
//        if (GITLET_DIR.exists()) {
//            System.out.println("A Gitlet version-control system already exists in the current directory.");
//            System.exit(0);
//        }
        GITLET_DIR.mkdirs();
        STAGING_DIR.mkdirs();
        OBJ_DIR.mkdirs();
        // TODO: Add initial commit
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
        Staging.saveBlob2Staging(blob);
    }
}
