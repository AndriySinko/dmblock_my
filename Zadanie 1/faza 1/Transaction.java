import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Models a Bitcoin-style transaction.
 * A transaction consumes existing UTXOs (inputs) and creates new outputs.
 * Inputs must be signed to prove the spender owns the coins.
 * The transaction is identified by the SHA-256 hash of its serialized data.
 */
public class Transaction {

    /**
     * Represents one input — a coin being spent.
     * References a specific output of a previous transaction via (prevTxHash, outputIndex).
     */
    public class Input {
        /** Hash of the transaction whose output we are spending */
        public byte[] prevTxHash;
        /** Which output slot in that previous transaction we are spending */
        public int outputIndex;
        /** Cryptographic signature proving the spender owns this coin */
        public byte[] signature;

        /** Constructor: records which previous output this input spends */
        public Input(byte[] prevHash, int index) {
            if (prevHash == null)
                prevTxHash = null;
            else
                prevTxHash = Arrays.copyOf(prevHash, prevHash.length); // defensive copy
            outputIndex = index;
        }

        /** Attaches a signature to this input (proves ownership of the coin) */
        public void addSignature(byte[] sig) {
            if (sig == null)
                signature = null;
            else
                signature = Arrays.copyOf(sig, sig.length); // defensive copy
        }
    }

    /**
     * Represents one output — a coin being created.
     * Specifies how much BTC goes to which recipient address.
     */
    public class Output {
        /** Amount of BTC this output is worth */
        public double value;
        /** Recipient's RSA public key (their "address") */
        public RSAKey address;

        /** Constructor: creates an output sending `v` BTC to `addr` */
        public Output(double v, RSAKey addr) {
            value = v;
            address = addr;
        }
    }

    /** SHA-256 hash of this transaction — its unique ID on the blockchain */
    private byte[] hash;
    private ArrayList<Input> inputs;
    private ArrayList<Output> outputs;

    /** Creates an empty transaction with no inputs or outputs yet */
    public Transaction() {
        inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
    }

    /** Copy constructor: clones an existing transaction (hash + all inputs + all outputs) */
    public Transaction(Transaction tx) {
        hash = tx.hash.clone();                        // copy the hash bytes
        inputs = new ArrayList<Input>(tx.inputs);      // shallow copy of input list
        outputs = new ArrayList<Output>(tx.outputs);   // shallow copy of output list
    }

    /** Adds a new input referencing output #outputIndex of transaction prevTxHash */
    public void addInput(byte[] prevTxHash, int outputIndex) {
        Input in = new Input(prevTxHash, outputIndex); // create the input object
        inputs.add(in);                                // append to the list
    }

    /** Adds a new output sending `value` BTC to `address` */
    public void addOutput(double value, RSAKey address) {
        Output op = new Output(value, address); // create the output object
        outputs.add(op);                        // append to the list
    }

    /** Removes the input at position `index` in the inputs list */
    public void removeInput(int index) {
        inputs.remove(index);
    }

    /**
     * Removes the input that corresponds to the given UTXO.
     * Searches by constructing a UTXO from each input and comparing.
     */
    public void removeInput(UTXO ut) {
        for (int i = 0; i < inputs.size(); i++) {
            Input in = inputs.get(i);
            UTXO u = new UTXO(in.prevTxHash, in.outputIndex); // reconstruct UTXO from input fields
            if (u.equals(ut)) {    // found the matching input
                inputs.remove(i);  // remove it
                return;            // done — only one input can match a given UTXO
            }
        }
    }

    /**
     * Builds the byte array that must be signed for input at position `index`.
     * Includes: the input's prevTxHash + outputIndex, then ALL outputs (value + recipient key).
     * Signing this data proves "I authorize spending this input to produce exactly these outputs."
     */
    public byte[] getDataToSign(int index) {
        ArrayList<Byte> sigData = new ArrayList<Byte>();
        if (index > inputs.size())  // invalid index guard
            return null;
        Input in = inputs.get(index);
        byte[] prevTxHash = in.prevTxHash;

        ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8); // allocate 4 bytes for an int
        b.putInt(in.outputIndex);                              // serialize outputIndex to bytes
        byte[] outputIndex = b.array();

        // append prevTxHash bytes (which transaction's output we're spending)
        if (prevTxHash != null)
            for (int i = 0; i < prevTxHash.length; i++)
                sigData.add(prevTxHash[i]);

        // append outputIndex bytes (which slot in that transaction)
        for (int i = 0; i < outputIndex.length; i++)
            sigData.add(outputIndex[i]);

        // append all outputs — signing locks in exactly where the money goes
        for (Output op : outputs) {
            ByteBuffer bo = ByteBuffer.allocate(Double.SIZE / 8); // allocate 8 bytes for a double
            bo.putDouble(op.value);
            byte[] value = bo.array();                            // serialize the BTC amount

            byte[] addressExponent = op.address.getExponent().toByteArray(); // RSA public exponent
            byte[] addressModulus  = op.address.getModulus().toByteArray();  // RSA modulus

            for (int i = 0; i < value.length; i++)
                sigData.add(value[i]);           // append output value
            for (int i = 0; i < addressExponent.length; i++)
                sigData.add(addressExponent[i]); // append recipient's exponent
            for (int i = 0; i < addressModulus.length; i++)
                sigData.add(addressModulus[i]);  // append recipient's modulus
        }

        // convert ArrayList<Byte> to primitive byte[]
        byte[] sigD = new byte[sigData.size()];
        int i = 0;
        for (Byte sb : sigData)
            sigD[i++] = sb;
        return sigD;
    }

    /** Attaches a signature to the input at position `index` */
    public void addSignature(byte[] signature, int index) {
        inputs.get(index).addSignature(signature);
    }

    /**
     * Serializes the entire transaction to a byte array.
     * Used by finalize() to compute the transaction hash.
     * Format: [for each input: prevTxHash + outputIndex + signature] [for each output: value + exponent + modulus]
     */
    public byte[] getTx() {
        ArrayList<Byte> Tx = new ArrayList<Byte>();

        for (Input in : inputs) {
            byte[] prevTxHash = in.prevTxHash;

            ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8); // 4 bytes for int
            b.putInt(in.outputIndex);
            byte[] outputIndex = b.array();
            byte[] signature = in.signature;

            if (prevTxHash != null)                                    // append prevTxHash if present
                for (int i = 0; i < prevTxHash.length; i++)
                    Tx.add(prevTxHash[i]);
            for (int i = 0; i < outputIndex.length; i++)              // append serialized index
                Tx.add(outputIndex[i]);
            if (signature != null)                                     // append signature if present
                for (int i = 0; i < signature.length; i++)
                    Tx.add(signature[i]);
        }

        for (Output op : outputs) {
            ByteBuffer b = ByteBuffer.allocate(Double.SIZE / 8); // 8 bytes for double
            b.putDouble(op.value);
            byte[] value = b.array();
            byte[] addressExponent = op.address.getExponent().toByteArray();
            byte[] addressModulus  = op.address.getModulus().toByteArray();

            for (int i = 0; i < value.length; i++)
                Tx.add(value[i]);            // append output amount
            for (int i = 0; i < addressExponent.length; i++)
                Tx.add(addressExponent[i]);  // append recipient public key exponent
            for (int i = 0; i < addressModulus.length; i++)
                Tx.add(addressModulus[i]);   // append recipient public key modulus
        }

        // convert ArrayList<Byte> to primitive byte[]
        byte[] tx = new byte[Tx.size()];
        int i = 0;
        for (Byte b : Tx)
            tx[i++] = b;
        return tx;
    }

    /**
     * Finalizes the transaction by computing and storing its SHA-256 hash.
     * Must be called after all inputs/outputs are set. The hash is the transaction's ID.
     */

    public void finalize() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256"); // get SHA-256 hasher
            md.update(getTx());   // feed the serialized transaction bytes
            hash = md.digest();   // compute and store the 32-byte hash
        } catch (NoSuchAlgorithmException x) {
            x.printStackTrace(System.err); // SHA-256 is always available in Java — this won't happen
        }
    }

    /** Manually sets the hash (used when loading a transaction from external data) */
    public void setHash(byte[] h) {
        hash = h;
    }

    /** Returns the transaction's hash (its unique ID) */
    public byte[] getHash() {
        return hash;
    }

    /** Returns all inputs */
    public ArrayList<Input> getInputs() {
        return inputs;
    }

    /** Returns all outputs */
    public ArrayList<Output> getOutputs() {
        return outputs;
    }

    /** Returns the input at position `index`, or null if out of bounds */
    public Input getInput(int index) {
        if (index < inputs.size()) {
            return inputs.get(index);
        }
        return null;
    }

    /** Returns the output at position `index`, or null if out of bounds */
    public Output getOutput(int index) {
        if (index < outputs.size()) {
            return outputs.get(index);
        }
        return null;
    }

    /** Returns the number of inputs */
    public int numInputs() {
        return inputs.size();
    }

    /** Returns the number of outputs */
    public int numOutputs() {
        return outputs.size();
    }
}
