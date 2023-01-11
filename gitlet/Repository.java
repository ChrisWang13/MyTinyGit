package gitlet;

import java.io.File;
import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *  @author ChrisWang13
 */
public class Repository {
    /** TODO: add instance variables here. */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /* TODO: fill in the rest of this class. */
}
