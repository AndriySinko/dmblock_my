// Meno študenta: Andrii Synko

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Processes batches of transactions against the current UTXO pool.
 * Validates each transaction and updates the pool state for accepted ones.
 */
public class HandleTxs {

    // The working UTXO pool. Modified in-place as transactions are accepted:
    // spent outputs are removed, new outputs are added.
    private UTXOPool utxoPool;

    /**
     * Vytvorí verejný ledger (účtovnú knihu), ktorého aktuálny UTXOPool (zbierka nevyčerpaných
     * transakčných výstupov) je {@code utxoPool}. Malo by to vytvoriť bezpečnú kópiu
     * utxoPool pomocou konštruktora UTXOPool (UTXOPool uPool).
     */
    // Makes a defensive copy so changes during handler() don't affect the caller's pool.
    public HandleTxs(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool); // copy, not reference - protects caller's state
    }

    // Returns the current pool state after all accepted transactions have been applied.
    // Called by Blockchain.blockAdd() (step 6) to get the updated pool to store in the new BlockNode.
    public UTXOPool UTXOPoolGet() {
        if (utxoPool == null) {
            utxoPool = new UTXOPool(); // defensive: return empty pool if somehow null
        }
        return utxoPool;
    }

    /**
     * Validates a single transaction. Returns true only if ALL of these hold:
     */
    public boolean txIsValid(Transaction tx) {

        ArrayList<Transaction.Input> inputs = tx.getInputs();
        HashSet<UTXO> claimedUtxo = new HashSet<>(); // tracks UTXOs claimed in this tx to detect double-spend
        double totalInputValue = 0;

        for (int i=0; i<inputs.size(); i++) {
            // (1) Every input references a UTXO that exists in the current pool (no spending phantom coins)
            Transaction.Input input = inputs.get(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex); // reconstruct the UTXO this input points to

            if (!utxoPool.contains(utxo)) { // if the referenced output isn't spendable -> invalid
                return false;
            }

            // (2) The signature on each input is valid (proves the spender owns the coin)
            Transaction.Output txOutput = utxoPool.getTxOutput(utxo); // look up the output being spent
            RSAKey publicKey = txOutput.address;                       // owner's public key
            byte[] dataToSign = tx.getDataToSign(i);                   // serialized data that was signed

            if (!publicKey.verifySignature(dataToSign, input.signature)){ // signature must match owner's key
                return false;
            }

            // (3) No UTXO is claimed by more than one input (no double-spend within this transaction)
            if (!claimedUtxo.add(utxo)) { // add() returns false if already present -> double-spend detected
                return false;
            }

            totalInputValue += txOutput.value; // accumulate total coins being spent
        }

        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        double totalOutputValue = 0;

        for (int i=0; i<outputs.size(); i++) {
            Transaction.Output output = outputs.get(i);

            // (4) Every output value is >= 0 (no negative amounts)
            if (output.value<0) {
                return false;
            }

            totalOutputValue += output.value; // accumulate total coins being created
        }

        // (5) Total input value >= total output value (no coins created out of thin air; difference is the fee)
        if (totalInputValue < totalOutputValue) {
            return false;
        }

        return true; // all checks passed
    }

    /**
     * Processes one epoch (round) of proposed transactions.
     * Accepts all mutually valid transactions from the unordered list,
     * updates the UTXO pool (removes spent outputs, adds new ones),
     * and returns the array of accepted transactions.
     */
    // Uses a greedy fixed-point loop: repeatedly scan remaining txs until no more can be accepted.
    // This handles dependency chains: tx B can become valid only after tx A is accepted in the same round.
    public Transaction[] handler(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTransactions = new ArrayList<>();
        boolean found = true; // loop until a full pass finds nothing new

        ArrayList<Transaction> transactions = new ArrayList<>(Arrays.asList(possibleTxs)); // mutable working list
        ArrayList<Transaction> removeList = new ArrayList<>(); // txs accepted this pass, to be removed after iteration

        while (found) {
            found = false;       // assume nothing new until proven otherwise this pass
            removeList.clear();  // reset accepted-this-pass list

            for (Transaction transaction : transactions) {
                if (txIsValid(transaction)) {          // check against current pool state
                    acceptedTransactions.add(transaction); // mark as accepted for output

                    for (Transaction.Input input : transaction.getInputs()) {
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex); // UTXO being consumed
                        utxoPool.removeUTXO(utxo); // remove spent output so it can't be double-spent later
                    }

                    ArrayList<Transaction.Output> outputs = transaction.getOutputs();
                    for (int j = 0; j < outputs.size(); j++) {
                        UTXO utxo = new UTXO(transaction.getHash(), j); // new UTXO created by this tx
                        utxoPool.addUTXO(utxo, outputs.get(j)); // add to pool so downstream txs can spend it
                    }

                    found = true;              // at least one tx accepted this pass -> loop again
                    removeList.add(transaction); // schedule for removal from working list
                }
            }
            transactions.removeAll(removeList); // remove accepted txs so they aren't checked again
        }

        return acceptedTransactions.toArray(new Transaction[0]); // return accepted txs as array
    }
}
