# MyTinyGit
MyTinyGit is a version control system which mimics some basic features of `Git`, as well as some additional features.

## Gernal Design Idea

### Storing Approach 1: Store Multiple Copies of Everything
Each commit is stored in a subdirectory with copies of every file.
1. Commit simply creates a new subdirectory then copies all added files to the subdirectory.

2. Checkout simply deletes everything in the current folder and copies all files from the requested subdirectory in their place.

3. Take too much space.

### Storing Approach 2a: Store Only Files That Change
1. To figure out which files to copy, we had to walk through the entire commit history starting from commit 1.
2. Name different folder(v1, v2, v3...), `Hashmap <Commit, Name>`

### Storing Approach 2b: Store Only Files That Change With Assistance of Cache(linked list)

Look for previous commit and modify the file with this commit.
```
V1 Commit (Hello.java): Hello.java → v1
V2 Commit (Hello.java, Friend.java): Hello.java → v2, Friend.java → v2 
V3 Commit (Friend.java, Egg.java): Hello.java → v2, Friend.java → v3, Egg.java → v3
V4 Commit (Friend.java): Hello.java → v2, Friend.java → v3, Egg.java → v3
V5 Commit (Hello.java): Hello.java → v5, Friend.java → v3, Egg.java → v3
```

### Storing Approach 2c: `git hash-object .java` as an inspiration

1. Two people commit at the same time, this previous commit is `N`. Who is going to be commit `N + 1` ? No center server! Buggy!
2. Use hashing to solve the name of commit (Extremely unlikely to have collision)


## Classes and Data Structures


### `gitlet init`
1. Check if `.gitlet` file exists in `gitlet init`
2. Init all folder with `mkdir`
3. make empty Staging area and initial commit

### `Blob implements Serializable`
#### Fields
1. `byte[] contents`
2. `File file`

### `gitlet add`
1. Staging area is a list of added blobs
2. Place to store is in staging folder by calling `writeObject`
3. Add persistence file to record staging history
#### Fields
1. ` private Map<String, String> addBlobs`
2. ` private Set<String> rmBlobs`

### `gitlet commit`
#### Fields
1. ` private Map<String, String> addBlobs`

### `gitlet branch and checkout`
1. `HEAD` is reference of branch file.Store that SHA-1 value under a simple name
2. HEAD (ref: refs/heads/branch?)(File Contents should name of branch File). Use `join` to refer to File in heads folder. 
```shell
# Read HEAD file
$ cat .git/HEAD
ref: refs/heads/master
# Crude way of updating master 
$ echo 1a410efbd13591db07496601ebc7a059dd55cfe9 > .git/refs/heads/master
# Safe way to update master
$ git update-ref refs/heads/master 1a410efbd13591db07496601ebc7a059dd55cfe9
```

### `gitlet merge` 
1. Difference between a tree and a graph: Tree has root, therefore transverse from root to
get to any node; Graph has paths. Start from a random node and dfs or bfs all the way with 
distance v.
2. Find LCA (lowest(Regard to commit tree root: Initial commit) / latest common ancestor) for any two commits object.


### Refractor (2023.1.23 Update)
1. In `Staging` class, refractor storeBlobs from `List` that store `Blob` object to HashMap
that map between <Blob pathName, SHA1-hash of Blob>. Easy to delete!
2. Still need Blob to quick read byte for diff or quick view file contents
```java
/** This is buggy! Since these Blob objects are diffent!! */
/** Shared list to store blobs, add or remove file to storeBlobs in Staging area. */
    public List<Blob> storeBlobs = new ArrayList<>();
/** Remove file in storeBlob list. */
    public void rmFileInStaging(File fileName) {
        Blob blob = new Blob(fileName);
        storeBlobs.remove(blob);
    }
```
3. When Writing staged file to staging folder (BlobID diff from previous commit and current Staging).
    Use `Utils.sha1(blob.getFilePath())` as staging file entry name(String), easy to overwrite if file is already staged.
4. Be careful with object referencing! Return copied new object.
5. Use `TrieIndex` to speed up its search for abbreviated `commitID`
## Debugging
1. IntelliJ provides a feature called “remote JVM debugging” that will allow you to add breakpoints that trigger during integration tests.
2. The script that will connect to the IntelliJ JVM is `runner.py`
```shell
# Setup for dubgging
# nano ~/.bashrc
# export MY_TINY_GIT=/home/chris/MyTinyGit
# Activate
# source ~/.bashrc
# echo $MY_TINY_GIT
```

```shell
make
cd /home/chris/Desktop/MyTinyGit/testing
python3 runner.py --debug samples/test00-lca.in
```
## Testing
1. Print my output in testing folder
```shell
cd /home/chris/Desktop/MyTinyGit
make
make check TESTER_FLAGS="--verbose"
```
2. Test specific test
```shell
make 
cd /home/chris/Desktop/MyTinyGit/testing
python3 tester.py --verbose samples/test00-lca.in
```

## Count total work
```shell
cd /home/chris/Desktop/MyTinyGit
find . -name "*java" | xargs cat | wc
```
