// Immutable data class — represents a single transaction in the network.
// "final" means the class cannot be subclassed.
final public class Transaction {
    final int id; // unique identifier for this transaction; also immutable (final)

    public Transaction(int id) {
        this.id = id;
    }

    @Override
    /**  @return true ak táto transakcia má rovnaké id ako {@code obj} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) { // must be the same class, not just any object
            return false;
        }
        final Transaction other = (Transaction) obj; // safe cast after class check
        if (this.id != other.id) { // two transactions are equal iff their ids match
            return false;
        }
        return true;
    }

    @Override
    // hashCode must be consistent with equals — since equality is based on id,
    // we use id directly. This makes Transaction work correctly in HashSet/HashMap.
    public int hashCode() {
        return id;
    }
}