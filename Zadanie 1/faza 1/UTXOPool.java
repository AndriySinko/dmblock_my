
/**
 * UTXOPool.java
 *
 * Represents the set of all currently unspent transaction outputs (the "ledger state").
 * Maps each UTXO (pointer to an output) to the actual Transaction.Output object (value + recipient).
 * When a coin is created it's added here; when it's spent it's removed.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class UTXOPool {

    /**
     * The internal map: UTXO (txHash + index) → Transaction.Output (value + address).
     * Looking up a UTXO tells you how much it's worth and who owns it.
     */
    private HashMap<UTXO, Transaction.Output> H;

    /** Creates a new empty pool — no unspent outputs yet */
    public UTXOPool() {
        H = new HashMap<UTXO, Transaction.Output>();
    }

    /**
     * Copy constructor: creates an independent copy of uPool.
     * Changes to this new pool won't affect the original.
     */
    public UTXOPool(UTXOPool uPool) {
        H = new HashMap<UTXO, Transaction.Output>(uPool.H); // shallow copy: same entries, new map object
    }

    /**
     * Registers a new unspent output in the pool.
     * Called when a transaction output is created (new coin exists).
     */
    public void addUTXO(UTXO utxo, Transaction.Output txOut) {
        H.put(utxo, txOut); // key = UTXO pointer, value = the actual output data
    }

    /**
     * Marks a UTXO as spent by removing it from the pool.
     * Called when a transaction input consumes this output.
     */
    public void removeUTXO(UTXO utxo) {
        H.remove(utxo); // removes the entry; the coin is now "spent"
    }

    /**
     * Returns the Transaction.Output associated with the given UTXO.
     * Returns null if the UTXO is not in the pool (already spent or never existed).
     */
    public Transaction.Output getTxOutput(UTXO ut) {
        return H.get(ut); // HashMap lookup by UTXO key
    }

    /** Returns true if the UTXO is in the pool (i.e., still unspent) */
    public boolean contains(UTXO utxo) {
        return H.containsKey(utxo); // checks if the key exists in the map
    }

    /** Returns all UTXOs currently in the pool as a list */
    public ArrayList<UTXO> getAllUTXO() {
        Set<UTXO> setUTXO = H.keySet();               // get all keys from the map as a Set
        ArrayList<UTXO> allUTXO = new ArrayList<UTXO>();
        for (UTXO ut : setUTXO) {
            allUTXO.add(ut);                           // copy each UTXO into the ArrayList
        }
        return allUTXO;
    }
}
