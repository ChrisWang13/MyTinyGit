package gitlet;
import java.io.Serializable;
import java.util.*;

/** TrieIndex for object Commit in obj folder,
 *  add when committing, save to indexFile in obj folder for persistence. */

public class TrieIndex implements Serializable {
    private final TrieNode root = new TrieNode();
    private class TrieNode implements Serializable {
        private boolean isEnd = false;
        private TreeMap<Character, TrieNode> child = new TreeMap<>();
    }

    /** Add commitID to Trie. */
    public void add(String commitID) {
        TrieNode cur = root;
        int len = commitID.length();
        for (int i = 0; i < len; ++i) {
            char ch = commitID.charAt(i);
            if (cur.child.get(ch) == null) {
                // Create new TrieNode
                cur.child.put(ch, new TrieNode());
            }
            if (i == len - 1) {
                // Mark end of string
                cur.isEnd = true;
            }
            // Iterate to next
            cur = cur.child.get(ch);
        }
    }

    /** Check if partCommitID is in Trie, length must be larger than 6.
     *  If matched with prefix, return full commitID (Failed case < 10e-6). else return null.
     */
    public String matchSixDigit(String partCommitID) {
        int len = partCommitID.length();
        assert len >= 6;
        TrieNode cur = root;
        StringBuilder realCommitID = null;
        // check if partCommitID is the prefix of realCommitID
        boolean isPrefix = false;
        for (int i = 0; i < len; ++i) {
            char ch = partCommitID.charAt(i);
            if (cur.child.get(ch) == null) {
                isPrefix = false;
                break;
            }
            if (i == len - 1) {
                isPrefix = true;
            }
            // Concatenate realCommitID with Character
            if (realCommitID == null) {
                realCommitID = new StringBuilder(String.valueOf(ch));
            } else {
                realCommitID.append(ch);
            }
            // Iterate to next
            cur = cur.child.get(ch);
        }

        if (isPrefix) {
            // iterate all the way down
            Set<Character> s = cur.child.keySet();
            // Failed case < 10e-6
            while (cur != null && s.size() == 1) {
                s = cur.child.keySet();
                for (char ch : s) {
                    realCommitID.append(ch);
                    cur = cur.child.get(ch);
                }
            }
            return realCommitID.toString();
        }
        return null;
    }
}
