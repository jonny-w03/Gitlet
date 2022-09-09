# Gitlet Design Document
Author: Jonny W.

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely 
format and style a text file. Organize your design document in a way that 
will make it easy for you or a course-staff member to read.  

## 1. Classes and Data Structures

### Main.java
The main program that executes gitlet. The main structure of gitlet is a tree, each node represents a commit. Once the
node is created with timestamp, log message, and the pointers pointing the correct files that are being committed, it
cannot be changed. All related fields is declared as final.

#### Fields
1. static final File GITLET_FOLDER: A pointer to the directory containing all the blobs (files being committed).
2. private final String timeStamp: A string of the time that the commitment is made.
3. private final String logMessage: A string of the commit message of the commitment.
4. private final ArrayList<File> blobs: An arrayList of files of the commitment.
5. private final Node prev: the pointer to the previous commitment node.
6. private Tree commitTree: The tree containing all the commitments.
7. private Node head: The pointer pointing to the current working commitment in commitTree.

### DumpObj.java
A debugging class whose main program may be invoked as follows: "java gitlet.DumpObj FILE... " where each FILE is a 
file produced by Utils.writeObject (or any file containing a serialized object).  This will simply read FILE, 
deserialize it, and call the dump method on the resulting Object. The object must implement the gitlet.Dumpable 
interface for this to work.

### Diff.java
A comparison of two sequences of strings.  After executing setSequences to initialize the data, methods allow computing 
longest common sequences and differences in the form of edits needed to convert one sequence to the next.

#### Fields
1. private List<String> _lines1, _lines2: The sequences being compared.
2. private int[][] _lls: The memo table for longest common subsequence.
3. private int[] _todo: A stack structure used by lls.
4. private int _todop, _llsState, _llsTop1, _llsTop2: Structure for the work queue used by lls.

## 2. Algorithms

### Main.java
1. main(String[] args): The main operator of the class. Take in gitlet commands from users and run different methods
according to the command passed in. Does not do anything special, just read then string, determine which method to run.
2. init(): Creates the gitlet directory, assuming no gitlet directory already exists (thows an error if this is the 
case). Initializes GITLET_FOLDER. After the initialization, create the first commit, which is also the root of
commitTree. 
3. add(String file): Adds the file appointed by file. First search for the file within the current blobs, and if no
such file found, creates a new File pointing to this file.
4. commit(String message): First checks if anything is changed (anything staged) by going through the files in the 
blobs in the current commit, and also checks message.length > 0. If false, then create a new node of commitTree, while 
saving the last node by copying the all the variables to the final variable. Also puts the message in logMessage.
5. rm(String file): First checks if the file named file is in blobs in the current commit node. If true, just remove it
from blobs.
6. log(): Start from the latest commit, prints the commit sequence, date, and message. Recursion through the previous 
commit nodes using prev pointer. Due to the structure of the tree, there's only one prev (parent) of each node.
7. globalLog(): Iterate through all the files in the .gitlet directory, and prints the log message associated to it.
8. find(String message): Iterate through all commits, while finding the commit that has the message matching message.
9. status(): In the order specified (branches, staged files, removed files, modifications not staged for commit
untracked files), prints out the status of each file. Each kind of file specified above is recorded in corresponding
lists or directories, just need to iterate through them and print the file name in the correct section.
10. checkout(String[] args): First does a check on args.length, and perform different actions. For args.length == 3,
search for the file in current or previous commits, and delete the current version of the file then copy the older
version. for args.length == 4, search the commit using the commit id and does the same thing to the file as above. For
args.length == 2, find the branch with corresponding branch name and removes all current files, replacing them with the
files under that branch.
11. branch(String name): Creates a new pointer to the current commit, also updates head so that it points to the commit
same as the new branch.
12. rmBranch(String name): Find the pointer whose name is name and remove it (By making the pointer points to null).
13. reset(String id): Similar to checkout, finds the commit node corresponding to id and deletes everything in current
commit and replace with the files in that commit.
14. merge(String name): First check the conditions of all the files in the two branches (current branch and the given
branch). If merge-able, then first copy the files from the branch name and add them to current branch, then null points
the name branch.

### Diff.java
1. setSequences(Collection<String> seq1, Collection<String> seq2): Set the sequences currently being compared to the 
contents of SEQ1 and SEQ2 (as delivered by their iterators).
2. setSequences(File file1, File file2): Set the sequences currently being compared to the contents of FILE1 and FILE2.
Null Files set empty lists.
3. sequence1(): Return the first of the current sequences.
4. sequence2(): Return the second of the current sequences.
5. get1(int k): Returns sequence1().get(K).
6. get2(int k): Returns sequence2().get(K).
7. lls(int k1, int k2): Return the length of the longest subsequence of the first K1 and K2 items, respectively, of the 
current data sequences.
8. lls(): Return the length of the longest common subsequence of the current data subsequences.
9. toIntArr(Collection<Integer> list): Return an array containing the int values of the items in LIST.
10. commonSubsequence(): Return largest common subsequence of the sequences being compared as a sequence of 3n values 
s01, s02, L0, s11, s12, L1,..., where si1 is the starting line position of the subsequence in the first file (0-based), 
si2 is the starting position in the second file, and Li is the length of the subsequence.
11. diffs(): Return the edit that converts the first of the sequences being compared to the second.
12. checkData(): Raise an exception if there are no current data sequences for comparison.
13. initStack(): Initialize work stack for lls.
14. push(int i1, int i2, int state): Push an item on the work stack for computing lls(I1, I2).
15. pop(): Pop an item from the work stack.
16. empty(): Return true iff the work stack is empty.

### DumpObj.java
1. main(String... files): Deserialize and apply dump to the contents of each of the files in FILES.

## 3. Persistence

### Blobs (Files)
Store all the files in one directory. One file may have multiple versions, but they are still in the same directory,
differentiated by the id, which is the actual name of the files. And they are pointed by different commitment nodes, so
accessing these files will only be through the nodes in the commit tree.

### Node (Commits)
Each past commit is also stored as a file. They will be serialized and stored within one directory, and each commit is
named by the id. Each commit is also pointed by the prev commit, or its parent, so accessing them will be through the
commit tree.

## 4. Design Diagram

![](/Users/tingmu.chang/Desktop/UCB/Spring2022/CS61B/repo/proj3/gitlet/Untitled-Notebook-2.jpg)
![](/Users/tingmu.chang/Desktop/UCB/Spring2022/CS61B/repo/proj3/gitlet/Untitled-Notebook-3.jpg)
![](/Users/tingmu.chang/Desktop/UCB/Spring2022/CS61B/repo/proj3/gitlet/Untitled-Notebook-4.jpg)