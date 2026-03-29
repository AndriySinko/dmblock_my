// Meno študenta: Andrii Synko
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/* TrustedNode označuje uzol, ktorý dodržuje pravidlá (nie je byzantský) */
public class TrustedNode implements Node {

    private double p_graph;
    private double p_byzantine;
    private double p_txDistribution;
    private int numRounds;

    private boolean[] followees;
    private Set<Transaction> transactionsPool;

    public TrustedNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_byzantine = p_byzantine;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;

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
    }
}