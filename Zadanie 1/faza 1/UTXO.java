/**
 * UTXO.java
 *
 * Represents an Unspent Transaction Output (UTXO) — a pointer to a specific
 * output of a past transaction that has not been spent yet.
 * A UTXO is uniquely identified by (txHash, index): which transaction + which output slot.
 */
import java.util.Arrays;

public class UTXO implements Comparable<UTXO> {

    /** The hash of the transaction this output belongs to (acts as its ID) */
    private byte[] txHash;

    /** The position/slot of this output within that transaction */
    private int index;

    /**
     * Constructor: creates a UTXO pointing to output #index of transaction with hash txHash.
     */
    public UTXO(byte[] txHash, int index) {
        this.txHash = Arrays.copyOf(txHash, txHash.length); // defensive copy so external changes don't affect this object
        this.index = index;
    }

    /** Returns the transaction hash this UTXO belongs to */
    public byte[] getTxHash() {
        return txHash;
    }

    /** Returns the output index within the transaction */
    public int getIndex() {
        return index;
    }

    /**
     * Checks if this UTXO is equal to another object.
     * Two UTXOs are equal if they have the same txHash bytes AND the same index.
     */
    public boolean equals(Object other) {
        if (other == null) {                      // null is never equal
            return false;
        }
        if (getClass() != other.getClass()) {     // must be the same class (UTXO)
            return false;
        }

        UTXO utxo = (UTXO) other;                // safe cast now that class is confirmed
        byte[] hash = utxo.txHash;
        int in = utxo.index;
        if (hash.length != txHash.length || index != in) // quick check: different length or different index → not equal
            return false;
        for (int i = 0; i < hash.length; i++) {  // byte-by-byte comparison of the hash arrays
            if (hash[i] != txHash[i])
                return false;
        }
        return true; // all bytes matched and index matched → equal
    }

    /**
     * Computes a hash code consistent with equals().
     * Required so that equal UTXOs produce the same hash code when used in HashMap/HashSet.
     */
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + index;                   // mix in the index using prime multiplier 17
        hash = hash * 31 + Arrays.hashCode(txHash); // mix in the txHash array using prime multiplier 31
        return hash;
    }

    /**
     * Compares this UTXO with another for ordering (used for sorting).
     * Returns -1 (this < other), 0 (equal), or 1 (this > other).
     * Order: first by index, then by txHash length, then byte-by-byte.
     */
    public int compareTo(UTXO utxo) {
        byte[] hash = utxo.txHash;
        int in = utxo.index;
        if (in > index)       // other has larger index → this comes first
            return -1;
        else if (in < index)  // other has smaller index → this comes second
            return 1;
        else {                // same index → break tie by comparing txHash
            int len1 = txHash.length;
            int len2 = hash.length;
            if (len2 > len1)  // other's hash is longer → this comes first
                return -1;
            else if (len2 < len1) // other's hash is shorter → this comes second
                return 1;
            else {            // same length → compare byte by byte
                for (int i = 0; i < len1; i++) {
                    if (hash[i] > txHash[i])  // other's byte is larger → this comes first
                        return -1;
                    else if (hash[i] < txHash[i]) // other's byte is smaller → this comes second
                        return 1;
                }
                return 0; // every byte matched → UTXOs are identical
            }
        }
    }
}
