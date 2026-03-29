/**
 * Tento Byzantský uzol by sa dal považovať za vypnutý.
 * Nikdy nevysiela žiadne transakcie ani neodpovedá
 * na komunikáciu s inými uzlami.
 *
 * Toto je len jeden príklad (najjednoduchší) takéhoto
 * byzantského (škodlivého) uzla.
 *
 * This is the "silent attacker" variant of a Byzantine node.
 * It participates in the network structure but contributes nothing.
 * TrustedNode must still reach consensus even when some peers behave like this.
 */

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class ByzantineNode implements Node {

    // Accepts the same constructor arguments as TrustedNode but ignores all of them.
    public ByzantineNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
    }

    // Does nothing — Byzantine node ignores its followee list.
    public void followeesSet(boolean[] followees) {
        return;
    }

    // Does nothing — Byzantine node ignores its initial transactions.
    public void pendingTransactionSet(Set<Transaction> pendingTransactions) {
        return;
    }

    // Always returns empty set — never broadcasts anything to followers.
    // From TrustedNode's perspective: this peer is always silent.
    public Set<Transaction> followersSend() {
        return new HashSet<Transaction>();
    }

    // Does nothing — Byzantine node ignores everything it receives.
    public void followeesReceive(ArrayList<Integer[]> candidates) {
        return;
    }
}
