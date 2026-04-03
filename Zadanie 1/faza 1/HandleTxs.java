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

    private UTXOPool utxoPool;

    /**
     * Vytvorí verejný ledger (účtovnú knihu), ktorého aktuálny UTXOPool (zbierka nevyčerpaných
     * transakčných výstupov) je {@code utxoPool}. Malo by to vytvoriť bezpečnú kópiu
     * utxoPool pomocou konštruktora UTXOPool (UTXOPool uPool).
     */

    public HandleTxs(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public UTXOPool UTXOPoolGet() {
        if (utxoPool == null) {
            utxoPool = new UTXOPool();
        }

        return utxoPool;
    }

    /**
     * Validates a single transaction. Returns true only if ALL of these hold:
     */
    public boolean txIsValid(Transaction tx) {

        ArrayList<Transaction.Input> inputs = tx.getInputs();
        HashSet<UTXO> claimedUtxo = new HashSet<>();
        double totalInputValue = 0;

        for (int i=0; i<inputs.size(); i++) {
            // (1) Every input references a UTXO that exists in the current pool (no spending phantom coins)
            Transaction.Input input = inputs.get(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex); // reconstruct utxo(transaction hash and index of output) that our input references to

            if (!utxoPool.contains(utxo)) { // check that pool contains, if not return false
                return false;
            }

            // (2) The signature on each input is valid (proves the spender owns the coin)
            Transaction.Output txOutput = utxoPool.getTxOutput(utxo); // get output from input's utxo
            RSAKey publicKey = txOutput.address; // get address of the owner of that output
            byte[] dataToSign = tx.getDataToSign(i); // get the data which will prove ownership

            if (!publicKey.verifySignature(dataToSign, input.signature)){
                return false;
            }

            // (3) No UTXO is claimed by more than one input (no double-spend within this transaction)
            if (!claimedUtxo.add(utxo)) { // add element if its not present, false if it is
                return false;
            }


            totalInputValue += txOutput.value;
        }

        ArrayList<Transaction.Output> outputs =tx.getOutputs();
        double totalOutputValue = 0;

        for (int i=0; i<outputs.size(); i++) {
            Transaction.Output output = outputs.get(i);

            // (4) Every output value is >= 0 (no negative amounts)
            if (output.value<0) {
                return false;
            }

            // (5) Total input value >= total output value (no coins created out of thin air)
            totalOutputValue += output.value;
        }

        if (totalInputValue < totalOutputValue) {
            return false;
        }

        return true;
    }

    /**
     * Processes one epoch (round) of proposed transactions.
     * Accepts all mutually valid transactions from the unordered list,
     * updates the UTXO pool (removes spent outputs, adds new ones),
     * and returns the array of accepted transactions.
     */
    public Transaction[] handler(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTransactions = new ArrayList<>();
        boolean found = true;

        ArrayList<Transaction> transactions = new ArrayList<>(Arrays.asList(possibleTxs));

        ArrayList<Transaction> removeList = new ArrayList<>(); /** Data structure and removing method suggested by AI (Claude)**/

        while (found) {
            found = false; // check by rounds, until transaction isnt found anymore
            removeList.clear();

            for (Transaction transaction : transactions) {
                if (txIsValid(transaction)) {
                    acceptedTransactions.add(transaction); // add tx to pool

                    // get all input utxos and remove from the pool
                    for (Transaction.Input input : transaction.getInputs()) {
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                        utxoPool.removeUTXO(utxo);
                    }

                    ArrayList<Transaction.Output> outputs = transaction.getOutputs(); // get outputs
                    for (int j = 0; j < outputs.size(); j++) {
                        UTXO utxo = new UTXO(transaction.getHash(), j); // create new utxo
                        utxoPool.addUTXO(utxo, outputs.get(j)); // add them to the pool, cuz they might unlock new transaction
                    }

                    found = true; // at least one is found
                    removeList.add(transaction);
                }
            }
            transactions.removeAll(removeList); // remove all accepted txs
        }


        return acceptedTransactions.toArray(new Transaction[0]);
    }
}
