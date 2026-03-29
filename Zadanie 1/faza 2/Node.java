import java.util.ArrayList;
import java.util.Set;

public interface Node {

    // POZNÁMKA: Node je rozhranie a nemá konštruktor.
    // Vaša trieda TrustedNode.java však vyžaduje 4-argumentový
    // konštruktor, ako je definované v Simulation.java
    // Tento konštruktor dáva vášmu uzlu informácie o simulácii
    // vrátane počtu kôl, pre ktoré bude bežať.

    // --- Call order in Simulation ---
    // 1. followeesSet()          — once, before rounds start
    // 2. pendingTransactionSet() — once, before rounds start
    // 3. per round: followersSend() → followeesReceive()
    // 4. followersSend()         — one final time after all rounds; must return consensus set

    /**
     * {@code followees[i]} is true only if this node follows node {@code i}.
     * Called once at startup. Use it to know which peers to trust/count.
     */
    void followeesSet(boolean[] followees);

    /**
     * Called once at startup. Gives this node its initial set of transactions
     * it has "heard about". Use as the starting belief set.
     */
    void pendingTransactionSet(Set<Transaction> pendingTransactions);

    /**
     * Called every round AND once after the final round.
     * During rounds: return your current belief set to broadcast to followers.
     * After final round: return only transactions you believe reached consensus.
     */
    Set<Transaction> followersSend();

    /**
     * Called every round with what your followees broadcast this round.
     * Each Integer[] has exactly 2 elements: [txId, senderNodeIndex].
     * This is where your consensus logic goes — decide which txs to accept.
     */
    void followeesReceive(ArrayList<Integer[]> candidates);
}