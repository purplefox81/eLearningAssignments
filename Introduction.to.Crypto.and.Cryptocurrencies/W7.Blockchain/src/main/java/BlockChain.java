// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

public class BlockChain {

    public static final int CUT_OFF_AGE = 10;

    TreeBlock lastBlock;
    TreeBlock firstBlock;

    //when there is no entry, then we must be working on the mapping for genesis block
    Map<Block,UTXOPool> block2utxoMapping = new HashMap<Block,UTXOPool>();

    TransactionPool txPool = new TransactionPool();

    public class TreeBlock {

        Block block;
        TreeBlock parent;
        List<TreeBlock> children;

        int height;

        public TreeBlock(Block block) {
            this.block = block;
            this.children = new LinkedList<TreeBlock>();
        }

        public TreeBlock addChild(Block child) {
            TreeBlock childNode = new TreeBlock(child);
            childNode.parent = this;
            childNode.height = this.height+1;
            this.children.add(childNode);
            return childNode;
        }

        public TreeBlock getMaxHeightTreeBlock() {
            //if we reach the end of the data structure (aka the chain)
            //we return the last node (aka this node)
            if (children==null || children.size()==0) return this;
            //or we continue to go down the tree
            return children.get(0).getMaxHeightTreeBlock();
        }
        public Block getMaxHeightBlock() {
            return getMaxHeightTreeBlock().block;
        }

        public List<TreeBlock> getChildren() { return this.children; }
    }

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        //create a new chain and its utxopool
        firstBlock = new TreeBlock(genesisBlock);
        createFirstUTXOPool(genesisBlock);
        //also point the lastBlock to the genesis block
        lastBlock = firstBlock;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return getMaxHeightTreeBlock().block;
    }
    private TreeBlock getMaxHeightTreeBlock() {
        // IMPLEMENT THIS
        return lastBlock.getMaxHeightTreeBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return block2utxoMapping.get(lastBlock.getMaxHeightBlock());
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return this.txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS

        //Validation: claim to be a genesis block or fail to provide a prevBlk hash
        if (block.getPrevBlockHash()==null) return false;

        //check validity for each tx
        List<Transaction> txs = block.getTransactions();
        for (Transaction tx : txs) {
            //TxHandler.
        }

        //TODO: YCM
        //1. validate the tx
        //2. add coinbase tx to the block? when?
        //3. coinbase tx can be spent in the block, i.e. they r in the utxopool
        //4. update utxo and utxopool etc when create a new block, when recv a new block, when a side branch becomes main branch
        //5. when a side branch becomes the main branch, those tx presented in the side branch and relected in the utxopool are Dropped
        //   if these tx are not in the side branch, it is fine to drop too. what if tx are in both side and main branch
        //6. do we use txHandler to manage utxo+pool? or


        //now assume block is valid, we add the block to the chain according to its prevHash
        int heightLimit = getHeightLimit();
        boolean processingFlag = addNewBlockByHash(block,heightLimit>=0?heightLimit:0);

        return processingFlag;
    }

    private int getHeightLimit() {
        return getMaxHeightTreeBlock().height-CUT_OFF_AGE;
    }

    private boolean addNewBlockByHash(Block newBlock, int heightLimit) {

        String targetH = getHashString(newBlock.getPrevBlockHash());
        //we search starting from the lastblock upwards
        TreeBlock targetParentBlock = lastBlock;
        while (true) {
            //System.out.println("trying block "+ targetBlock+" height "+targetBlock.height);
            String parentH = getHashString(targetParentBlock.block.getHash());
            if (targetH.equals(parentH)) {
                //we now test the height
                int newHeight = targetParentBlock.height+1;

                //if we try to insert to a block which is already cut off, we invalidate this block (return false)
                //System.out.println("trying to insert at newheight "+newHeight+" with limit "+heightLimit);
                if (newHeight<=heightLimit) return false;

                //otherwise we try to process this insertion (and return true)

                targetParentBlock.addChild(newBlock);
                //if this new block is the first child of the parent block
                if (targetParentBlock.children.size()==1) {
                    lastBlock = targetParentBlock.getChildren().get(0);
                }

                //optionally cut the head of chain
                int newHeightLimit = getHeightLimit();
                forwardFirstBlock(newHeightLimit);

                return true;

            } else {
                if (targetParentBlock.parent==null) return false;
                else targetParentBlock = targetParentBlock.parent;
            }
        }
    }

    private void forwardFirstBlock(int newHeight) {
        //
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        this.txPool.addTransaction(tx);
    }

    private void createFirstUTXOPool(Block genesisBlock) {
        UTXOPool utxoPool = new UTXOPool();
        //use the tx info in block data structure to derive
        List<Transaction> txs = genesisBlock.getTransactions(); //no tx
        Transaction tx = genesisBlock.getCoinbase();
        List<Transaction.Output> outs = tx.getOutputs();
        for (Transaction.Output o : outs) {
            UTXO utxo = new UTXO(tx.getHash(),0);
            utxoPool.addUTXO(utxo, o);
        }
        block2utxoMapping.put(genesisBlock,utxoPool);
    }

    //for debugging purpose
    public void printBlockChain() {
        printTreeBlock(this.firstBlock);
    }
    private void printTreeBlock(TreeBlock targetBlock) {

        //print myself first
        //System.out.println(getIndentationString(targetBlock.height)+ getBlockString(targetBlock));
        //print decendants
        List<TreeBlock> cs = targetBlock.getChildren();
        for (TreeBlock t : cs) {
            printTreeBlock(t);
        }
    }
    private String getBlockString(TreeBlock b) {
        return "Block height "+b.height+" hash "+ getHashString(b.block.getHash())+" prevHash "+ getHashString(b.block.getPrevBlockHash());
    }
    private String getHashString(byte[] bs) {
        if (bs==null || bs.length==0) return "";
        return bs.toString().substring(3,11);
    }
    private String getIndentationString(int x) {
        String s = "";
        for (int i=0;i<x;i++) {
            s += "  ";
        }
        return s;
    }
}