
// Meno študenta: Andrii Synko
// Blockchain by mal na naplnenie funkcií udržiavať iba obmedzené množstvo uzlov
// Nemali by ste mať všetky bloky pridané do blockchainu v pamäti  
// pretože by to mohlo spôsobiť pretečenie pamäte.
import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {
    public static final int CUT_OFF_AGE = 12;

    /**Data structure suggested by AI (claude) for faster lookup than list O(1) vs O(n)**/
    private HashMap<ByteArrayWrapper, BlockNode> hashToBlock;

    private BlockNode lastBlock;
    private TransactionPool memoryPool;
    private HandleTxs txHandler;

    // všetky potrebné informácie na spracovanie bloku v reťazi blokov
    private class BlockNode {
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        // utxo pool na vytvorenie nového bloku na vrchu tohto bloku
        private UTXOPool uPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<BlockNode>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
        }
    }

    /**
     * vytvor prázdny blockchain iba s prvým (Genesis) blokom. Predpokladajme, že
     * {@code genesisBlock} je platný blok
     */
    public Blockchain(Block genesisBlock) {
        // create utxopool for genesis block and update it with new coinbase utxo
        UTXOPool uPool = new UTXOPool();
        Transaction coinbase = genesisBlock.getCoinbase();
        ArrayList<Transaction.Output> coinbaseOutputs =  coinbase.getOutputs();

        for (int i = 0; i <coinbaseOutputs.size(); i++) {
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            uPool.addUTXO(utxo, coinbaseOutputs.get(i));
        }

        //create blockchain
        hashToBlock = new HashMap<>(); // init blockhain
        BlockNode root = new BlockNode(genesisBlock, null, uPool); // create genesis blocknode
        hashToBlock.put(new ByteArrayWrapper(genesisBlock.getHash()), root); // add genesis block

        lastBlock = root; // update highest block

        memoryPool = new TransactionPool();
    }

    /** Získaj najvyšší (maximum height) blok */
    public Block getBlockAtMaxHeight() {
        return lastBlock.b;
    }

    /**
     * Získaj UTXOPool na ťaženie nového bloku na vrchu najvyššieho (max height)
     * bloku
     */
    public UTXOPool getUTXOPoolAtMaxHeight() {
        return lastBlock.getUTXOPoolCopy(); // copy, we dont want to change existing block upool
    }

    /** Získaj pool transakcií na vyťaženie nového bloku */
    public TransactionPool getTransactionPool() {
        return memoryPool;
    }

    /**
     * Pridaj {@code block} do blockchainu, ak je platný. Kvôli platnosti by mali
     * byť všetky transakcie platné a blok by mal byť na
     * {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * Môžete napríklad vyskúšať vytvoriť nový blok nad blokom Genesis (výška bloku
     * 2), ak height blockchainu je {@code <=
     * CUT_OFF_AGE + 1}. Len čo {@code height > CUT_OFF_AGE + 1}, nemôžete vytvoriť
     * nový blok vo výške 2.
     *
     * @return true, ak je blok úspešne pridaný
     */
    public boolean blockAdd(Block block) {
        if (block == null) return false;
        if (block.getPrevBlockHash() == null) return false; // genesis
        if (hashToBlock.containsKey(new ByteArrayWrapper(block.getHash()))) return false; // alredy in blockchain

        // get parent
        ByteArrayWrapper parentHash = new ByteArrayWrapper(block.getPrevBlockHash());
        BlockNode parentNode = hashToBlock.get(parentHash);
        if (parentNode == null) return false; // info from parent is lost

        if (parentNode.height + 1 <= lastBlock.height - CUT_OFF_AGE) return false; // too old


        // check transactions
        UTXOPool parentPool =  parentNode.getUTXOPoolCopy();
        txHandler = new HandleTxs(parentPool);

        ArrayList<Transaction> transactions = block.getTransactions();
        Transaction[] acceptedTxs = txHandler.handler(transactions.toArray(new Transaction[0]));
        if (acceptedTxs.length != transactions.size()) return false; // transactions arent valid


        // update utxopool with coinbase
        UTXOPool newUTXOPool = txHandler.UTXOPoolGet();
        Transaction coinbase = block.getCoinbase();
        ArrayList<Transaction.Output> coinbaseOutputs =  coinbase.getOutputs();
        for (int i = 0; i <coinbaseOutputs.size(); i++) {
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            newUTXOPool.addUTXO(utxo, coinbaseOutputs.get(i));
        }

        // update mempool
        for (Transaction tx : acceptedTxs) {
            memoryPool.removeTransaction(tx.getHash());
        }

        // create blockand append blockchain
        BlockNode newNode = new BlockNode(block, parentNode, newUTXOPool);
        hashToBlock.put(new ByteArrayWrapper(block.getHash()), newNode);


        // update tree
        if (newNode.height > lastBlock.height) {
            lastBlock = newNode; // update the latest block

           int threshold = lastBlock.height - CUT_OFF_AGE;
           ArrayList<ByteArrayWrapper> hashesToRemove = new ArrayList<>();

           // find nodes to delete
           for (ByteArrayWrapper hash : hashToBlock.keySet()) {
               if (hashToBlock.get(hash).height < threshold) {
                   hashesToRemove.add(hash);
               }
           }

           // delete nodes
           for (ByteArrayWrapper hash : hashesToRemove) {
               BlockNode toRemove = hashToBlock.get(hash);
               for (BlockNode child : toRemove.children) {
                   child.parent = null;
               }
               toRemove.children.clear();

               hashToBlock.remove(hash);
           }
        }

        return true;
    }

    /** Pridaj transakciu do transakčného poolu */
    public void transactionAdd(Transaction tx) {
        memoryPool.addTransaction(tx);
    }
}