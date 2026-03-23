// Meno študenta: Andrii Synko

import java.util.ArrayList;

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
     * (3) No UTXO is claimed by more than one input (no double-spend within this transaction)
     * (4) Every output value is >= 0 (no negative amounts)
     * (5) Total input value >= total output value (no coins created out of thin air)
     */
    public boolean txIsValid(Transaction tx) {

        ArrayList<Transaction.Input> inputs = tx.getInputs();

        for (int i=0; i<inputs.size(); i++) {
            // (1) Every input references a UTXO that exists in the current pool (no spending phantom coins)
            Transaction.Input input = inputs.get(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex); // reconstruct utxo(transaction hash and index of output) that our input references to

            if (!utxoPool.contains(utxo)) { // check that pool contains, if not return false
                return false;
            }


            // (2) The signature on each input is valid (proves the spender owns the coin)
            Transaction.Output output = utxoPool.getTxOutput(utxo); // get output from input's utxo
            RSAKey publicKey = output.address; // get address of the owner of that output
            byte[] dataToSign = tx.getDataToSign(i); // get the data which will prove ownership

            if (!publicKey.verifySignature(dataToSign, input.signature)){
                return false;
            }

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
        // TODO: implement handler logic
        return false;
    }
}
