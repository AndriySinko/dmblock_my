// Simple data container bundling a transaction with the index of the node that sent it.
// Used optionally in TrustedNode to wrap the raw Integer[] arrays from followeesReceive.
// Note: Simulation.java does NOT use this class — it passes Integer[]{txId, senderIndex} directly.
public class Candidate {
	Transaction tx;   // the transaction being proposed
	int sender;       // index of the node that sent this transaction

	public Candidate(Transaction tx, int sender) {
		this.tx = tx;
		this.sender = sender;
	}
}