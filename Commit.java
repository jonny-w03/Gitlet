package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/** The essential structure of gitlet commmits. Each commit acts
 * like a node, and all together is a tree of commits. Serializable
 * means each commit will be saved as a file and be accessed later.
 * These files are saved in GITLET_FOLDER in the main class of
 * package gitlet.
 * @author Jonny W. */
public class Commit implements Serializable {

    /** A HashMap of files present in the current commit, in the
     * format Hashmap<name, contents (read the file using reader)>.
     * These files are already committed (should not be changing
     * when current commit files are changing). */
    private HashMap<String, String> files;

    /** A Hashmap of files that will change when files in current
     * commits are changing. */
    private HashMap<String, String> newFiles;

    /** The SHA-1 id for current commit. */
    private String commitId;

    /** Date and time when the current commit is saved (committed). */
    private String timeStamp;

    /** Message when the current commit is saved (committed). */
    private String logMessage;

    /** The previous (parent) commit to the current commit. The
     * actual content of the String is commit ID. */
    private String prev;

    /** The second previous (parent) commit to the current
     * commit. This is null except for commits merged from
     * two branches. */
    private String prev2;

    /** A HashMap accounting files that are staged. Same format
     * as FILES. */
    private HashMap<String, String> staged;

    /** A HashMap containing untracked files. */
    private HashMap<String, String> untracked;

    /** A HashMap containing modified files. */
    private HashMap<String, String> modified;

    /** A HashMap containing removed files. */
    private HashMap<String, String> removed;

    /** Initialization of a commit with a PREV String indicating
     * the commit ID of the commit before. PREV cannot be null
     * except the first commit. */
    public Commit() {
        staged = new HashMap<String, String>();
        untracked = new HashMap<String, String>();
        modified = new HashMap<String, String>();
        removed = new HashMap<String, String>();
    }

    /** Set the previous commit PREVCOMMIT .*/
    public void setPrev(String prevCommit) {
        prev = prevCommit;
    }

    /** Set the second parent PREVCOMMIT for current commit. */
    public void setPrev2(String prevCommit) {
        prev2 = prevCommit;
    }

    /** Set the commit ID ID to the current commit. For each
     * commit, the ID must be set right after initialization. */
    public void setCommitId(String id) {
        commitId = id;
    }

    /** Record the time of the commit. */
    public void setTimeStamp() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "E MMM dd HH:mm:ss yyyy");
        timeStamp = formatter.format(new Date()) + " -0800";
    }

    /** Record the time for the first commit. F is only for
     * differentiating between the regular setTimeStamp method. */
    public void setTimeStamp(int f) {
        timeStamp = "Wed Dec 31 16:00:00 1969 -0800";
    }

    /** Set the log message MESSAGE associated with current commit. */
    public void setLogMessage(String message) {
        logMessage = message;
    }

    /** Save current working commit in the directory COMMITFOLDER. */
    public void saveCommit(File commitFolder) throws IOException {
        File outFile = new File(commitFolder, commitId);
        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(outFile));
        out.writeObject(this);
        out.close();
    }
    /** Add the file FILE with name NAME to the current commit.
     * Files are stored as strings. */
    public void addFiles(File file, String name) {
        String content = Utils.readContentsAsString(file);
        if (newFiles.get(name) != null && newFiles.get(name).equals(content)) {
            return;
        }
        newFiles.put(name, content);
        if (!removed.containsKey(name)) {
            staged.put(name, content);
        }
    }

    /** Add the file with name NAME and content CONTENT to the
     * current commit. */
    public void addFiles(String name, String content) {
        if (newFiles.get(name) != null && newFiles.get(name).equals(content)) {
            return;
        }
        newFiles.put(name, content);
        staged.put(name, content);
    }

    /** Returns the previous commit of current commit. */
    public String getPrev() {
        return prev;
    }

    /** Returns the second parent for current commit. */
    public String getPrev2() {
        return prev2;
    }

    /** Return the HashMap of files of current commit. */
    public HashMap<String, String> getFiles() {
        return files;
    }

    /** Return the HashMap of new files of current commit. */
    public HashMap<String, String> getNewFiles() {
        return newFiles;
    }

    /** return the SHA-1 id for current commmit. */
    public String getCommitId() {
        return commitId;
    }

    /** Return the time stamp of current commit. */
    public String getTimeStamp() {
        return timeStamp;
    }

    /** Return the log message associated with current commit. */
    public String getLogMessage() {
        return logMessage;
    }

    /** Copies the files from PREVCOMMIT to the current files. */
    public void initFiles(HashMap<String, String> prevCommit) {
        if (prev == null) {
            files = new HashMap<String, String>();
        } else {
            files = new HashMap<String, String>(prevCommit);
        }
        newFiles = new HashMap<String, String>(files);
    }

    /** Clear everything. */
    public void clearSetUps() {
        newFiles = new HashMap<String, String>(files);
        staged.clear();
        modified.clear();
        removed.clear();
        untracked.clear();
    }

    /** Return the HashMap staged in the current commit. */
    public HashMap<String, String> getStaged() {
        return staged;
    }

    /** Return the HashMap untracked in the current commit. */
    public HashMap<String, String> getUntracked() {
        return untracked;
    }

    /** Return the HashMap modified in the current commit. */
    public HashMap<String, String> getModified() {
        return modified;
    }

    /** Return the HashMap removed in the current commit. */
    public HashMap<String, String> getRemoved() {
        return removed;
    }

    /** Add the untracked file F to untracked HashMap. This
     * does not stage the file (not the add command). */
    public void addUntracked(File f) {
        String content = Utils.readContentsAsString(f);
        untracked.put(f.getName(), content);
    }

    /** Remove the untracked file named NAME from untracked
     * HashMap. */
    public void removeUntracked(String name) {
        untracked.remove(name);
    }

    /** Add the modified file F to modified HashMap. This
     * does not stage the file (not the add command). */
    public void addModified(File f) {
        String content = Utils.readContentsAsString(f);
        modified.put(f.getName(), content);
    }

    /** Add the file name NAME to modified HashMap. This method
     * is does not add the content of the file named F because
     * it is deleted. This does not stage the file (not the
     * add command). */
    public void addModified(String name) {
        modified.put(name, null);
    }

    /** Add the file named NAME to HashMap removed. Content is
     * always null because this file is going ot be removed.
     * This does not stage the file (not the add command). */
    public void addRemoved(String name) {
        removed.put(name, null);
    }

    /** Remove the modified file named NAME from modified HashMap. */
    public void removeModified(String name) {
        modified.remove(name);
    }

    /** Remove the staged file NAME from HashMap staged and files
     * for current commit. */
    public void removeStaged(String name) {
        staged.remove(name);
        newFiles.remove(name);
    }

    /** Remove the removed file NAME from HashMap removed. */
    public void removeRemoved(String name) {
        removed.remove(name);
    }
}
