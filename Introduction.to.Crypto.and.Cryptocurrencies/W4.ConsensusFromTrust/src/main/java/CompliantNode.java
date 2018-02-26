import java.util.*;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
//YCM: According to the assignement:
//- the goal of assignment is to get all complaint nodes to output the same set of tx (reaching the consensus) asap
//- the malicious node may behave like
//  - be functionally dead and never actually broadcast any transactions.
//  - constantly broadcasts its own set of transactions and never accept transactions given to it.
//  - change behavior between rounds to avoid detection.
//apparently all these checking is not required. a simple dummy implementation will get us 81% score.
public class CompliantNode implements Node {

    double p_graph, p_malicious, p_txDistribution;
    int numRounds;

    boolean[] followees;

    Set<Candidate> candidates;
    Set<Transaction> txProposedByCandidates;

    Set<Transaction> myProposalOfTransactions;


    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    //called order in Sim: (1)
    //followees[k] is true if this node follows totalNodeGraph[k]
    //or in other words, if boolean flag is true, i follow that node
    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
        this.followees = followees;
    }

    //called order in Sim: (2)
    //this is to set the initial starting state that this node has
    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        //the initial set is always trusted becuase in assignment it reads
        //"Assume that all transactions are valid and that invalid transactions cannot be created."
        this.myProposalOfTransactions = new HashSet<Transaction>(pendingTransactions);
    }

    //called order in Sim: (3)
    //this is basically the consensus that this node concludes (set of tx)
    //so we can also treat/view this method as 'getConsensus' or 'getYourView'
    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS
        return myProposalOfTransactions;
    }

    //this is a total set of tx that broadcast from nodes that i trust/follow
    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS

        //everytime we get the new candidates set,
        this.candidates = candidates;

        //we update candidatesGraph
        Set<Transaction> txProposed = new HashSet<Transaction>();
        for (Candidate c : candidates) {
            Transaction tx = c.tx;
            txProposed.add(tx);
        }
        this.txProposedByCandidates = txProposed;
        this.myProposalOfTransactions.addAll(txProposed);
    }
}
