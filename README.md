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


### `git init`
1. Check if `.gitlet` file exists in `git init`
2. Init all folder with `mkdir`
3. TODO: make initial commit

### `Blob implements Serializable`
#### Fields
1. `byte[] contents`
2. `File file`

### `git add`
1. Staging area is a list of added blobs
2. Place to store is in staging folder by calling `writeObject`
3. Add persistence file to record  staging history







