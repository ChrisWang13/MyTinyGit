package gitlet;

/** Driver class for MyTinyGit, a subset of the Git version-control system.
 *
 *  @author ChrisWang13
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */

    public static void main(String[] args) {
        int cmdLen = args.length;
        if (cmdLen == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            // java gitlet.Main init
            case "init":
                Repository.init();
                break;
            case "add":
                // java gitlet.Main add a.file
                Repository.add(args[1]);
                break;
            case "commit":
                if (args[1] == null) {
                    System.out.println("Please enter a commit message.");
                }
                Repository.commit(args[1]);
                break;
            case "log":
                Repository.log();
                break;
        }
    }
}
