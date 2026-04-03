// Meno študenta: Andrii Synko
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* TrustedNode označuje uzol, ktorý dodržuje pravidlá (nie je byzantský) */
public class TrustedNode implements Node {

    private boolean[] followees;
    private Set<Transaction> transactionsPool;

    public TrustedNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
        this.transactionsPool = new HashSet<>();
    }

    public void followeesSet(boolean[] followees) {
        this.followees = followees; // get your folowwes
    }

    public void pendingTransactionSet(Set<Transaction> pendingTransactions) {
        transactionsPool = new HashSet<>(pendingTransactions); // init pool
    }

    public Set<Transaction> followersSend() {
        return transactionsPool; // return transactions that node believes in
    }
    public void followeesReceive(ArrayList<Integer[]> candidates) {
        HashMap<Integer, Set<Integer>> txToSenders = new HashMap<>(); // stores senders of the txId
        for (Integer[] candidate : candidates) {
            int txId = candidate[0];
            int sender = candidate[1];
            if(followees[sender]) { // whether we follow sender
                if (!txToSenders.containsKey(txId)) {
                    txToSenders.put(txId, new HashSet<>()); // add tx if absent
                }
                txToSenders.get(txId).add(sender); // put there sender
            }
        }

        int threshold = 1; // one cuz byzantines are silent, otherwise calculate based on followees

        for (int txId : txToSenders.keySet()) {
            if (txToSenders.get(txId).size() >= threshold) { // if transaction received count > threshold, add to pool
                transactionsPool.add(new Transaction(txId));
            }
        }
    }
}