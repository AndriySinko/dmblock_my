import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;


public class MaxFeeHandleTxs extends HandleTxs {

    public MaxFeeHandleTxs(UTXOPool utxoPool) {
        super(utxoPool);
    }

    public static class TransactionFee { // mapping
        private Transaction tx;
        private double fee;

        public TransactionFee(Transaction tx, double fee) {
            this.tx = tx;
            this.fee = fee;
        }

        public Transaction getTx() {
            return tx;
        }

        public double getFee() {
            return fee;
        }
    }

    /* Accepts the set of valid transactions that maximizes total fees.
     * The fee of a transaction is sum of inputs - sum of outputs.
     * It tries to pick the combination that gives the highest total fee.**/

    public Transaction[] handler(Transaction[] possibleTxs) {
        ArrayList<TransactionFee> fees = new ArrayList<>();
        UTXOPool utxoPool = UTXOPoolGet(); // get pool wiht utxo of inputs

        for (Transaction transaction : possibleTxs) {
            ArrayList<Transaction.Input> inputs = transaction.getInputs();
            double sumOfInputs = 0;
            for (Transaction.Input input : inputs) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex); // recreate utxo of the input
                Transaction.Output txOutput = utxoPool.getTxOutput(utxo); // get utxo from the pool
                if (txOutput != null) { // transaction depends on another transaction if null
                    sumOfInputs += txOutput.value;
                }
            }

            // get outputs of transaction and calculate feex
            ArrayList<Transaction.Output> outputs = transaction.getOutputs();
            double sumOfOutputs = 0;
            for (Transaction.Output output : outputs) {
                sumOfOutputs += output.value;
            }

            fees.add(new TransactionFee(transaction, sumOfInputs - sumOfOutputs));
        }

        /** AI GENERATED SORT (im lazy to write it on my own) **/
        fees.sort(Comparator.comparingDouble(TransactionFee::getFee).reversed());
        Transaction[] sortedTxs = fees.stream()
                .map(TransactionFee::getTx)
                .toArray(Transaction[]::new);
        /** End of ai genrrated code**/

        return super.handler(sortedTxs); // call handler to choose the best list
    }
}
