package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Jonny W.
 */
public class Main {

    /** Current working directory. */
    private static final File CWD = new File(".");

    /** A gitlet folder containing all the files. */
    private static final File GITLET_FOLDER = new File(".gitlet");

    /** A commits folder containing all commmits. */
    private static final File COMMIT_FOLDER = new File("."
            + File.separator + ".gitlet" + File.separator + "commits");

    /** A temps folder containing all temporary files (should only
     * have headNode, branches, and branchUpdate). */
    private static final File TEMP_FOLDER = new File("."
            + File.separator + ".gitlet" + File.separator + "temps");

    /** A pointer to the current commit in the commit tree. */
    private static Commit headNode;

    /** Which branch to update when current commit will be committed. */
    private static String branchUpdate;

    /** A hashmap that stores all the branches. Each is a pointer
     * to the head of each branch, in the format
     * Hashmap<Branch, Commit ID>*/
    private static HashMap<String, String> branches;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    @SuppressWarnings("unchecked")
    public static void main(String... args) throws
            IOException, ClassNotFoundException {
        if (args.length == 0) {
            handleError("Please enter a command.");
        }
        if (!args[0].equals("init") && GITLET_FOLDER.exists()) {
            File inFile = new File(TEMP_FOLDER, "working-headNode");
            File inFile2 = new File(TEMP_FOLDER, "working-branches");
            File inFile3 = new File(TEMP_FOLDER, "working-branchUpdate");
            ObjectInputStream inp = new ObjectInputStream(
                    new FileInputStream(inFile));
            ObjectInputStream inp2 = new ObjectInputStream(
                    new FileInputStream(inFile2));
            ObjectInputStream inp3 = new ObjectInputStream(
                    new FileInputStream(inFile3));
            headNode = (Commit) inp.readObject();
            branches = (HashMap<String, String>) inp2.readObject();
            branchUpdate = (String) inp3.readObject();
            inp.close();
            inp2.close();
            inp3.close();
            updateUntracked();
            updateModified();
        } else if (!args[0].equals("init") && !GITLET_FOLDER.exists()) {
            handleError("Not in an initialized Gitlet directory.");
        }
        divert(args);
        headNode.saveCommit(COMMIT_FOLDER);
        saveCurVar(headNode, "headNode", TEMP_FOLDER);
        saveCurVar(branches, "branches", TEMP_FOLDER);
        saveCurVar(branchUpdate, "branchUpdate", TEMP_FOLDER);
    }

    /** A diversion class that determines which method to run from
     * ARGS, user input commands. */
    private static void divert(String[] args) throws
            IOException, ClassNotFoundException {
        switch (args[0]) {
        case "init" -> {
            checkFormat(args[0], args);
            init();
        }
        case "add" -> {
            checkFormat(args[0], args);
            add(args[1]);
        }
        case "commit" -> {
            commit(args);
        }
        case "rm" -> {
            checkFormat(args[0], args);
            rm(args[1]);
        }
        case "log" -> {
            checkFormat(args[0], args);
            log();
        }
        case "global-log" -> {
            checkFormat(args[0], args);
            globalLog();
        }
        case "find" -> {
            checkFormat(args[0], args);
            find(args[1]);
        }
        case "status" -> {
            checkFormat(args[0], args);
            status();
        }
        case "checkout" -> {
            checkout(args);
        }
        case "branch" -> {
            checkFormat(args[0], args);
            branch(args[1]);
        }
        case "rm-branch" -> {
            checkFormat(args[0], args);
            rmBranch(args[1]);
        }
        case "reset" -> {
            checkFormat(args[0], args);
            reset(args[1]);
        }
        case "merge" -> {
            checkFormat(args[0], args);
            merge(args[1]);
        }
        default -> handleError("No command with that name exists.");
        }
    }

    /** Saves the current working variable SAVE, given the variable
     * name NAME, in the directory PATH. All names are saved as
     * "working-[variable name]*/
    private static void saveCurVar(Object save, String name,
                                   File path) throws IOException {
        File outFile = new File(path, "working-" + name);
        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(outFile));
        out.writeObject(save);
        out.close();
    }

    /** Initializes a new gitlet directory, if none exists. */
    private static void init() {
        if (!GITLET_FOLDER.exists()) {
            GITLET_FOLDER.mkdir();
            COMMIT_FOLDER.mkdir();
            TEMP_FOLDER.mkdir();
            Commit newCommit = new Commit();
            newCommit.setCommitId(Utils.sha1(Utils.serialize(newCommit)));
            headNode = newCommit;
            headNode.initFiles(null);
            headNode.setTimeStamp(1);
            headNode.setLogMessage("initial commit");
            branches = new HashMap<String, String>();
            branches.put("master", newCommit.getCommitId());
            branchUpdate = "master";
        } else {
            handleError("A Gitlet version-control system already "
                    + "exists in the current directory.");
        }
    }

    /** Add the file FILE to the current commit. */
    private static void add(String file) {
        File f = new File(file);
        if (f.exists()) {
            headNode.addFiles(f, file);
            headNode.removeUntracked(file);
            headNode.removeModified(file);
            headNode.removeRemoved(file);
        } else {
            handleError("File does not exist.");
        }
    }

    /** Commit the current node with ARGS in the format as it
     * was passed in, and creates a new node. Any variable in
     * the committed commit cannot be changed. */
    private static void commit(String[] args) throws IOException {
        if (args.length > 2) {
            handleError("Incorrect operands.");
        } else if (args.length == 1 || args[1].length() == 0) {
            handleError("Please enter a commit message.");
        }
        if (headNode.getStaged().size() == 0
                && headNode.getRemoved().size() == 0) {
            handleError("No changes added to the commit.");
        }
        Commit newCommit = new Commit();
        newCommit.setPrev(headNode.getCommitId());
        newCommit.initFiles(headNode.getNewFiles());
        newCommit.setLogMessage(args[1]);
        newCommit.setTimeStamp();
        newCommit.setCommitId(Utils.sha1(Utils.serialize(newCommit)));
        File outFile = new File(COMMIT_FOLDER, headNode.getCommitId());
        outFile.createNewFile();
        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(outFile));
        out.writeObject(headNode);
        out.close();
        headNode = newCommit;
        branches.put(branchUpdate, headNode.getCommitId());
    }

    /** Remove the file FILE from current commit. If staged,
     * unstage; if modified, un-modify and remove the file. */
    private static void rm(String file) {
        if (headNode.getStaged().containsKey(file)) {
            headNode.removeStaged(file);
        } else if (headNode.getNewFiles().containsKey(file)) {
            headNode.addRemoved(file);
            headNode.removeStaged(file);
            headNode.removeModified(file);
            File inFile = new File(file);
            if (inFile.exists()) {
                inFile.delete();
            }
        } else {
            handleError("No reason to remove the file.");
        }
    }

    /** Prints out the log for every commit, starting with the
     * current commit and tracing back to the first commit. */
    private static void log() throws IOException, ClassNotFoundException {
        Commit temp = headNode;
        while (temp != null) {
            logOutput(temp);
            temp = getCommit(temp.getPrev());
        }
    }

    /** Prints out the log for every commit, including commits in
     * other branch. */
    private static void globalLog() throws
            IOException, ClassNotFoundException {
        File[] filesList = COMMIT_FOLDER.listFiles();
        if (filesList != null) {
            for (File f : filesList) {
                if (f.isFile()) {
                    Commit c = getCommit(f.getName());
                    logOutput(c);
                }
            }
        }
    }

    /** Iterate through all the commit files in the COMMIT_FOLDER,
     * and prints out the commit with log message MESSAGE. */
    private static void find(String message) throws
            IOException, ClassNotFoundException {
        File[] filesList = COMMIT_FOLDER.listFiles();
        if (filesList != null) {
            boolean found = false;
            for (File f : filesList) {
                if (f.isFile()) {
                    Commit c = getCommit(f.getName());
                    if (c.getLogMessage().equals(message)) {
                        System.out.println(c.getCommitId());
                        found = true;
                    }
                }
            }
            if (!found) {
                handleError("Found no commit with that message.");
            }
        }
    }

    /** Prints out all the current status of gitlet. Including
     * branches, staged file, removed file, modifications not
     * staged for commit, and untracked files. */
    private static void status() {
        System.out.println("=== Branches ===");
        System.out.println("*" + branchUpdate);
        printHashmap(branches, branchUpdate);
        System.out.println();
        System.out.println("=== Staged Files ===");
        printHashmap(headNode.getStaged(), null);
        System.out.println();
        System.out.println("=== Removed Files ===");
        printHashmap(headNode.getRemoved(), null);
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        TreeMap<String, String> sorted = new TreeMap<String, String>(
                headNode.getModified());
        for (var each: sorted.entrySet()) {
            File check = new File(each.getKey());
            if (check.exists()) {
                System.out.println(each.getKey() + " (modified)");
            } else {
                System.out.println(each.getKey() + " (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        printHashmap(headNode.getUntracked(), null);
        System.out.println();
    }

    /** A checkout method that deals with three different
     * checkout commands, determined by the length of ARGS. The
     * essential function of this method is to overwrite the
     * current commit from a past commit. The overwritten files
     * depend on the checkout command. */
    private static void checkout(String[] args) throws
            IOException, ClassNotFoundException {
        if (args.length == 3) {
            checkFormatSpc(args[1], "--");
            if (headNode.getFiles().containsKey(args[2])) {
                headNode.getNewFiles().put(args[2],
                        headNode.getFiles().get(args[2]));
                writeActualFile(args[2],
                        headNode.getNewFiles().get(args[2]));
            } else {
                handleError("File does not exist in that commit");
            }
        } else if (args.length == 4) {
            checkFormatSpc(args[2], "--");
            Commit c = getCommit(args[1]);
            if (c == null) {
                c = checkShortId(args[1]);
            }
            if (c != null) {
                if (c.getFiles().containsKey(args[3])) {
                    headNode.getNewFiles().put(
                            args[3], c.getFiles().get(args[3]));
                    writeActualFile(args[3],
                            headNode.getNewFiles().get(args[3]));
                } else {
                    handleError("File does not exist in that commit");
                }
            } else {
                handleError("No commit with that id exists.");
            }
        } else if (args.length == 2) {
            if (args[1].equals(branchUpdate)) {
                handleError("No need to checkout the current branch.");
            }
            String id = branches.get(args[1]);
            if (id != null) {
                Commit c = getCommit(id);
                for (var each: headNode.getUntracked().entrySet()) {
                    if (c.getFiles().containsKey(each.getKey())) {
                        handleError("There is an untracked file in "
                                + "the way; delete it, or add and commit "
                                + "it first.");
                    }
                }
                headNode = c;
                for (var each: headNode.getFiles().entrySet()) {
                    writeActualFile(each.getKey(), each.getValue());
                }
                deleteFiles();
                headNode.clearSetUps();
                branchUpdate = args[1];
            } else {
                handleError("No such branch exists.");
            }
        } else {
            handleError("Incorrect operands.");
        }
    }

    /** Checks if SHORTENED belongs to any commit. Return null if
     * still no commit found. */
    private static Commit checkShortId(String shortened) throws
            IOException, ClassNotFoundException {
        File[] filesList = COMMIT_FOLDER.listFiles();
        if (filesList != null) {
            for (File f : filesList) {
                if (f.isFile()) {
                    if (f.getName().contains(shortened)) {
                        return getCommit(f.getName());
                    }
                }
            }
        }
        return null;
    }

    /** Created a new branch of commits named NAME. Does not
     * change the headNode pointer, still pointing to the
     * current branch.*/
    private static void branch(String name) {
        if (branches.containsKey(name)) {
            handleError("A branch with that name already exists.");
        }
        branches.put(name, headNode.getCommitId());
    }

    /** Removes the branch pointer named NAME. Does not delete
     * the actual commits in that branch. */
    private static void rmBranch(String name) {
        if (branches.containsKey(name)) {
            if (branchUpdate.equals(name)) {
                handleError("Cannot remove the current branch");
            } else {
                branches.remove(name);
            }
        } else {
            handleError("A branch with that name does not exist.");
        }
    }

    /** Checkout the commit appointed by ID. Also changes the
     *  head pointer to that commit.*/
    private static void reset(String id) throws
            IOException, ClassNotFoundException {
        Commit c = getCommit(id);
        if (c != null) {
            for (var each: headNode.getUntracked().entrySet()) {
                if (c.getFiles().containsKey(each.getKey())) {
                    handleError("There is an untracked file in "
                            + "the way; delete it, or add and "
                            + "commit it first.");
                }
            }
            headNode = c;
            for (var each: headNode.getFiles().entrySet()) {
                writeActualFile(each.getKey(), each.getValue());
            }
            deleteFiles();
            headNode.clearSetUps();
            branches.put(branchUpdate, headNode.getCommitId());
        } else {
            handleError("No commit with that id exists.");
        }
    }

    /** Merge the branch BRANCH to the current branch.*/
    private static void merge(String branch) throws
            IOException, ClassNotFoundException {
        if (headNode.getStaged().isEmpty() && headNode.getRemoved().isEmpty()) {
            if (!branch.equals(branchUpdate)) {
                String id = branches.get(branch);
                if (id != null) {
                    if (!headNode.getUntracked().isEmpty()) {
                        handleError("There is an untracked "
                                + "file in the way; delete it, or "
                                + "add and commit it first.");
                    }
                    boolean conflict = false;
                    String spId = getSplitPoint(branch);
                    if (spId.equals(id)) {
                        handleError("Given branch is an ancestor "
                                + "of the current branch.");
                    }
                    Commit splitPoint = getCommit(spId);
                    Commit mergeBranch = getCommit(id);
                    HashMap<String, String> hnFiles = headNode.getFiles();
                    HashMap<String, String> spFiles = splitPoint.getFiles();
                    HashMap<String, String> mbFiles = mergeBranch.getFiles();
                    conflict = mergeHelper(hnFiles, spFiles, mbFiles);
                    for (var each: mbFiles.entrySet()) {
                        if (spFiles.containsKey(each.getKey())) {
                            if (!spFiles.get(each.getKey()).equals(
                                    each.getValue())) {
                                if (!headNode.getStaged().containsKey(
                                        each.getKey())) {
                                    writeConflict(each.getKey(), "",
                                            mbFiles.get(each.getKey()));
                                    conflict = true;
                                }
                            }
                        } else {
                            if (!headNode.getStaged().containsKey(
                                    each.getKey())) {
                                headNode.addFiles(each.getKey(),
                                        mbFiles.get(each.getKey()));
                                writeActualFile(each.getKey(),
                                        mbFiles.get(each.getKey()));
                            }
                        }
                    }
                    if (spId.equals(headNode.getCommitId())) {
                        System.out.println("Current branch fast-forwarded");
                    }
                    mergeCommit(branch);
                    if (conflict) {
                        System.out.println("Encountered a merge conflict.");
                    }
                } else {
                    handleError("A branch with that name does not exist.");
                }
            } else {
                handleError("Cannot merge a branch with itself.");
            }
        } else {
            handleError("You have uncommitted changes.");
        }
    }

    /** A method just for making merge method shorter in order to
     * pass the style check. HNFILES SPFILES MBFILES. Return conflict. */
    private static boolean mergeHelper(HashMap<String, String> hnFiles,
                                    HashMap<String, String> spFiles,
                                    HashMap<String, String> mbFiles) throws
            IOException {
        boolean conflict = false;
        for (var each: hnFiles.entrySet()) {
            if (spFiles.containsKey(each.getKey())) {
                if (spFiles.get(each.getKey()).equals(
                        each.getValue())) {
                    if (mbFiles.containsKey(each.getKey())) {
                        if (!mbFiles.get(each.getKey()).equals(
                                each.getValue())) {
                            headNode.addFiles(each.getKey(),
                                    mbFiles.get(each.getKey()));
                            writeActualFile(each.getKey(), mbFiles.get(
                                    each.getKey()));
                        }
                    } else {
                        headNode.removeStaged(each.getKey());
                        File inFile = new File(each.getKey());
                        if (inFile.exists()) {
                            inFile.delete();
                        }
                    }
                } else {
                    if (mbFiles.containsKey(each.getKey())) {
                        if (!mbFiles.get(each.getKey()).equals(
                                each.getValue())) {
                            if (!mbFiles.get(each.getKey()).equals(
                                    spFiles.get(each.getKey()))) {
                                writeConflict(each.getKey(),
                                        hnFiles.get(each.getKey()),
                                        mbFiles.get(each.getKey()));
                                conflict = true;
                            }
                        }
                    } else {
                        writeConflict(each.getKey(),
                                hnFiles.get(each.getKey()),
                                "");
                        conflict = true;
                    }
                }
            } else {
                if (mbFiles.containsKey(each.getKey())) {
                    if (!mbFiles.get(each.getKey()).equals(
                            each.getValue())) {
                        writeConflict(each.getKey(),
                                hnFiles.get(each.getKey()),
                                mbFiles.get(each.getKey()));
                        conflict = true;
                    }
                }
            }
        }
        return conflict;
    }

    /** Write the conflict file NAME with CURCONTENT and CONFCONTENT. */
    private static void writeConflict(String name, String curContent,
                                      String confContent) throws IOException {
        writeActualFile(name, "<<<<<<< HEAD\n"
                + curContent + "=======\n"
                + confContent + ">>>>>>>\n");
        headNode.addFiles(name, "<<<<<<< HEAD\n"
                + curContent + "=======\n"
                + confContent + ">>>>>>>\n");
    }

    /** The commit for merging BRANCH into current branch. */
    private static void mergeCommit(String branch) throws IOException {
        Commit newCommit = new Commit();
        newCommit.setPrev(headNode.getCommitId());
        newCommit.setPrev2(branches.get(branch));
        newCommit.initFiles(headNode.getNewFiles());
        newCommit.setLogMessage("Merged " + branch + " into "
                + branchUpdate + ".");
        newCommit.setTimeStamp();
        newCommit.setCommitId(Utils.sha1(Utils.serialize(newCommit)));
        File outFile = new File(COMMIT_FOLDER, headNode.getCommitId());
        outFile.createNewFile();
        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(outFile));
        out.writeObject(headNode);
        out.close();
        headNode = newCommit;
        branches.put(branchUpdate, headNode.getCommitId());
    }

    /** Return the commid ID for the split point between current
     * and given branch MERGE. */
    private static String getSplitPoint(String merge) throws
            IOException, ClassNotFoundException {
        String splitPoint;
        Commit temp = headNode;
        ArrayList<String> headHistory = new ArrayList<String>();
        while (true) {
            if (temp != null) {
                headHistory.add(temp.getCommitId());
                if (temp.getPrev2() != null) {
                    temp = getCommit(temp.getPrev2());
                } else {
                    temp = getCommit(temp.getPrev());
                }
            } else {
                break;
            }
        }
        temp = getCommit(branches.get(merge));
        while (true) {
            if (headHistory.contains(temp.getCommitId())) {
                splitPoint = temp.getCommitId();
                break;
            } else if (temp.getPrev2() != null) {
                temp = getCommit(temp.getPrev2());
            } else {
                temp = getCommit(temp.getPrev());
            }
        }
        return splitPoint;
    }

    /** Checks if the command entered is correctly formatted,
     * given the COMMAND and the ARGS.*/
    private static void checkFormat(String command, String[] args) {
        if (command.equals("init") || command.equals("log")
                || command.equals("global-log")
                || command.equals("status")) {
            if (args.length != 1) {
                handleError("Incorrect operands.");
            }
        } else if (command.equals("add") || command.equals("rm")
                || command.equals("find") || command.equals("branch")
                || command.equals("rm-branch") || command.equals("reset")
                || command.equals("merge")) {
            if (args.length != 2) {
                handleError("Incorrect operands.");
            }
        }
    }

    /** Check if INPUT matches EXPECTED. */
    private static void checkFormatSpc(String input, String expected) {
        if (!input.equals(expected)) {
            handleError("Incorrect Operands");
        }
    }

    /** Return the commit appointed by ID. Return null if such
     * commit does not exist. */
    private static Commit getCommit(String id) throws
            IOException, ClassNotFoundException {
        if (id != null) {
            Commit c;
            File inFile = new File(COMMIT_FOLDER, id);
            if (inFile.exists()) {
                ObjectInputStream inp = new ObjectInputStream(
                        new FileInputStream(inFile));
                c = (Commit) inp.readObject();
                inp.close();
                return c;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /** Update any untracked file in the directory. */
    private static void updateUntracked() {
        File[] filesList = CWD.listFiles();
        if (filesList != null) {
            for (File f : filesList) {
                if (f.isFile()) {
                    if (!headNode.getNewFiles().containsKey(f.getName())) {
                        headNode.addUntracked(f);
                        if (headNode.getModified().containsKey(f.getName())) {
                            headNode.removeModified(f.getName());
                        }
                    }
                }
            }
        }
    }

    /** Update any files that are modified. Modified files must
     * already be present in headNode.getFile(). */
    private static void updateModified() {
        if (headNode != null) {
            for (var each : headNode.getNewFiles().entrySet()) {
                File curFile = new File(each.getKey());
                if (curFile.exists()) {
                    String content = Utils.readContentsAsString(curFile);
                    if (!content.equals(
                            headNode.getNewFiles().get(each.getKey()))) {
                        headNode.addModified(curFile);
                    }
                } else {
                    headNode.addModified(each.getKey());
                }
            }
        }
    }

    /** Exit with printing the error message ERR. */
    private static void handleError(String err) {
        System.out.println(err);
        System.exit(0);
    }

    /** Write/overwrite the actual file NAME with content CONTENT. */
    private static void writeActualFile(String name,
                                        String content) throws IOException {
        File outFile = new File(name);
        if (outFile.exists()) {
            Utils.writeContents(outFile, content);
        } else {
            outFile.createNewFile();
            Utils.writeContents(outFile, content);
        }
    }

    /** Prints out the log for commit C. */
    private static void logOutput(Commit c) {
        System.out.println("===");
        System.out.println("commit " + c.getCommitId());
        System.out.println("Date: " + c.getTimeStamp());
        System.out.println(c.getLogMessage());
        System.out.println();
    }

    /** Print the keys in HashMap PRINTING in lexicographic order,
     * skips the key that matches EXCEPT if EXCEPT is not null. */
    private static void printHashmap(HashMap<String, String> printing,
                                     String except) {
        TreeMap<String, String> sorted = new TreeMap<String, String>(printing);
        for (var each: sorted.entrySet()) {
            if (!each.getKey().equals(except)) {
                System.out.println(each.getKey());
            }
        }
    }

    /** Delete the files in the current working directory that
     * are not in the current commit. */
    private static void deleteFiles() {
        File[] filesList = CWD.listFiles();
        if (filesList != null) {
            for (File f : filesList) {
                if (f.isFile()) {
                    if (!headNode.getFiles().containsKey(f.getName())) {
                        f.delete();
                    }
                }
            }
        }
    }
}
