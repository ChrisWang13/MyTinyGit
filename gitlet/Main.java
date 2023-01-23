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
            case "init" -> Repository.init();
            case "add" -> Repository.add(args[1]);
            case "commit" -> {
                if (args[1].isEmpty()) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                Repository.commit(args[1]);
            }
            case "log" -> Repository.log();
            case "rm" -> Repository.rm(args[1]);
            case "status" -> Repository.status();
            case "branch" -> Repository.branch(args[1]);
            case "checkout"-> {
                if (args.length == 3 && args[1].equals("--")) {
                    Repository.checkoutFileName(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkoutCommitID(args[1],args[3]);
                } else if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
            }
        }
    }
}
