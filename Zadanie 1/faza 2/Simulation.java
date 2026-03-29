// Príklad simulácie. Tento test spúšťa uzly na náhodnom grafe.
// Na konci vypíše ID transakcií, na ktorých bol podľa uzlov
// dosiahnutý konsenzus. Túto simuláciu môžete použiť na
// otestovanie svojich uzlov. Budete chcieť vyskúšať vytvoriť nejaké podvodné uzly a
// zmiešať ich v sieti na úplné otestovanie.

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;

public class Simulation {

   public static void main(String[] args) {

      // Run with 4 command-line args: p_graph p_byzantine p_txDistribution numRounds
      // Recommended test matrix — all 54 combinations of:
      //   p_graph:          0.1, 0.2, 0.3
      //   p_byzantine:      0.15, 0.30, 0.45
      //   p_txDistribution: 0.01, 0.05, 0.10
      //   numRounds:        10, 20

      int numNodes = 100;
      double p_graph = Double.parseDouble(args[0]);          // probability an edge exists between any two nodes
      double p_byzantine = Double.parseDouble(args[1]);      // probability a node is Byzantine (malicious/silent)
      double p_txDistribution = Double.parseDouble(args[2]); // probability each node initially knows each transaction
      int numRounds = Integer.parseInt(args[3]);             // how many gossip rounds to simulate

      // --- STEP 1: Create nodes ---
      // Each node is either Byzantine (malicious) or TrustedNode (honest).
      Node[] nodes = new Node[numNodes];
      for (int i = 0; i < numNodes; i++) {
         if (Math.random() < p_byzantine)
            nodes[i] = new ByzantineNode(p_graph, p_byzantine, p_txDistribution, numRounds);
         else
            nodes[i] = new TrustedNode(p_graph, p_byzantine, p_txDistribution, numRounds);
      }

      // --- STEP 2: Build random directed follow graph ---
      // followees[i][j] = true means node i follows node j (i receives broadcasts from j).
      boolean[][] followees = new boolean[numNodes][numNodes];
      for (int i = 0; i < numNodes; i++) {
         for (int j = 0; j < numNodes; j++) {
            if (i == j)
               continue; // a node does not follow itself
            if (Math.random() < p_graph) {
               followees[i][j] = true;
            }
         }
      }

      // Tell each node who it follows (its row in the matrix).
      for (int i = 0; i < numNodes; i++)
         nodes[i].followeesSet(followees[i]);

      // --- STEP 3: Distribute initial transactions ---
      // 500 transactions with random ids are created.
      int numTx = 500;
      HashSet<Integer> validTxIds = new HashSet<Integer>();
      Random random = new Random();
      for (int i = 0; i < numTx; i++) {
         int r = random.nextInt();
         validTxIds.add(r); // ids are random ints; duplicates possible but unlikely
      }

      // Each node gets a random subset of the 500 transactions as its starting knowledge.
      for (int i = 0; i < numNodes; i++) {
         HashSet<Transaction> pendingTransactions = new HashSet<Transaction>();
         for (Integer txID : validTxIds) {
            if (Math.random() < p_txDistribution)
               pendingTransactions.add(new Transaction(txID));
         }
         nodes[i].pendingTransactionSet(pendingTransactions);
      }

      // --- STEP 4: Run gossip rounds ---
      for (int round = 0; round < numRounds; round++) {

         // Collect all broadcasts for this round into a map: receiverIndex -> list of [txId, senderIndex]
         HashMap<Integer, ArrayList<Integer[]>> allProposals = new HashMap<>();

         for (int i = 0; i < numNodes; i++) {
            Set<Transaction> proposals = nodes[i].followersSend(); // node i broadcasts its current belief set
            for (Transaction tx : proposals) {
               if (!validTxIds.contains(tx.id))
                  continue; // discard any tx that isn't globally valid (Byzantine nodes could send garbage)

               // Deliver this tx from node i to every node j that follows i
               for (int j = 0; j < numNodes; j++) {
                  if (!followees[j][i])
                     continue; // j does not follow i, skip

                  // candidate[0] = txId, candidate[1] = sender index (i)
                  if (allProposals.containsKey(j)) {
                     Integer[] candidate = new Integer[2];
                     candidate[0] = tx.id;
                     candidate[1] = i;
                     allProposals.get(j).add(candidate);
                  } else {
                     ArrayList<Integer[]> candidates = new ArrayList<Integer[]>();
                     Integer[] candidate = new Integer[2];
                     candidate[0] = tx.id;
                     candidate[1] = i;
                     candidates.add(candidate);
                     allProposals.put(j, candidates);
                  }
               }
            }
         }

         // Deliver collected proposals to each receiving node
         for (int i = 0; i < numNodes; i++) {
            if (allProposals.containsKey(i))
               nodes[i].followeesReceive(allProposals.get(i));
            // if no proposals for node i this round, followeesReceive is NOT called
         }
      }

      // --- STEP 5: Print final consensus ---
      // followersSend() is called one final time — each node should return its consensus set.
      for (int i = 0; i < numNodes; i++) {
         Set<Transaction> transactions = nodes[i].followersSend();
         System.out.println("Transaction ids that Node " + i + " believes consensus on:");
         for (Transaction tx : transactions)
            System.out.println(tx.id);
         System.out.println();
         System.out.println();
      }

   }

}
