import java.util.ArrayList;
import java.util.Arrays;

//YCM: =========================================================
//YCM: This is the main (and only) class that need to be implemented
//YCM: =========================================================
public class TxHandler {

    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        pool = new UTXOPool(utxoPool);
    }

    public void printTx(Transaction tx) {
        System.out.println("### Transaction (Hash: "+tx.hashCode()+") :");
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        for (int k=0; k<inputs.size(); k++) {
            Transaction.Input i = inputs.get(k);
            System.out.println("INPUT Hash "+Arrays.toString(i.prevTxHash).substring(0,10)+" Index "+i.outputIndex+" Signature "+Arrays.toString(i.signature));
        }
        for (int k=0; k<outputs.size(); k++) {
            Transaction.Output o = outputs.get(k);
            System.out.println("OUTPUT Value "+o.value+" Addr "+o.address.toString().substring(50,70));
        }
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        //System.out.println("isValidTx()...");
        //printTx(tx);

        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();

        double totalInputValue = 0.0, totalOutputValue = 0.0;

        for (int i=0; i<inputs.size(); i++) {

            Transaction.Input input = inputs.get(i);
            Transaction.Output output = null;

            //(1)
            //we assume the input is unspent, so we try to construct a UTXO obj out of it
            UTXO u = new UTXO(input.prevTxHash, input.outputIndex);
            //we try to look it up in the pool. by right it should be there, because the input should be unspent
            if (!pool.contains(u)) {
                //System.out.println("pool has no utxo, invalid!");
                return false;
            }
            else {
                //if the UTXO is in the pool, we will then get the corresponding TxOutput
                output = pool.getTxOutput(u);
                //as long as it is not null, we pass the test of criteria (1)
                if (output==null) {
                    //System.out.println("pool has no tx output, invalid!");
                    return false;
                }

                //(2)
                //i assume we use data from getRawDataToSign() to sign
                byte[] msgToSign = tx.getRawDataToSign(i);
                boolean verifyResult = Crypto.verifySignature(output.address, msgToSign, input.signature);
                if (!verifyResult) {
                    //System.out.println("sign verify false, invalid!");
                    return false;
                }

                //(3)
                for (int j=i+1;j<inputs.size();j++) {
                    Transaction.Input tempInput = inputs.get(j);
                    if (Arrays.equals(input.prevTxHash,tempInput.prevTxHash) &&
                            input.outputIndex==tempInput.outputIndex) {
                        //then the same utxo (derived from the same input) are claimed more than once in the same tx
                        //System.out.println("following input double spend, invalid!");
                        return false;
                    }
                }

                //(5)
                totalInputValue += output.value;
            }
        }

        for (int i=0; i<outputs.size(); i++) {
            Transaction.Output output = outputs.get(i);
            //(4)
            if (output.value<0) {
                //System.out.println("output negative, invalid!");
                return false;
            }
            //(5)
            totalOutputValue += output.value;
        }
        if (totalInputValue<totalOutputValue) {
            //System.out.println("total input smaller than output, invalid!");
            return false;
        }

        //System.out.println("valid RETURNING TRUE!");
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS

        ArrayList<Transaction> txs = new ArrayList<Transaction>();
        for (int txIndex=0;txIndex<possibleTxs.length;txIndex++) {
            Transaction tx = possibleTxs[txIndex];

            //if the tx is not valid, bypass it
            boolean validFlag = isValidTx(tx);
            //System.out.println("valid flag is "+validFlag);
            if (!validFlag) continue;

            //process the tx
            //step 1: remove previsouly-unspent now-spent utxo from the pool
            ArrayList<Transaction.Input> inputs = tx.getInputs();
            for (int i=0; i<inputs.size(); i++) {
                Transaction.Input input = inputs.get(i);
                UTXO u = new UTXO(input.prevTxHash, input.outputIndex);
                pool.removeUTXO(u);
            }
            //step 2: add new output from the current tx to the new utxopool
            ArrayList<Transaction.Output> outputs = tx.getOutputs();
            for (int i=0; i<outputs.size(); i++) {
                //construct a new UTXO, using the hash of the tx, and index of the output in outputs
                UTXO utxo = new UTXO(tx.getHash(), i);
                pool.addUTXO(utxo, outputs.get(i));
            }

            //add it to the array
            txs.add(tx);
        }

        return Arrays.copyOf(txs.toArray(), txs.size(), Transaction[].class);
    }

}
