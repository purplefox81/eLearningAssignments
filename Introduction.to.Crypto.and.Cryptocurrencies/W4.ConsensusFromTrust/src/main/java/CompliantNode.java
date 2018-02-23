import java.util.*;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
//YCM: According to the assignement:
//- the goal of assignment is to get all complaint nodes to output the same set of tx (reaching the consensus) asap
//- the malicious node may behave like
//  - be functionally dead and never actually broadcast any transactions.
//  - constantly broadcasts its own set of transactions and never accept transactions given to it.
//  - change behavior between rounds to avoid detection.
public class CompliantNode implements Node {

    double p_graph, p_malicious, p_txDistribution;
    int numRounds;

    boolean[] followees;
    int followeeCount;

    Set<Candidate> candidates;
    Map<Transaction,Set<Integer>> candidatesGraph;

    Set<Transaction> myProposalOfTransactions;
    //Set<Integer> whitelistSenders = new HashSet<>();


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

        int count = 0;
        for (boolean b : followees) {
            if (b) count++;
        }
        followeeCount = count;
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
        //YCM: my new view = my previous view + my new delta this round
        //                 = myProposalOfTransactions + candidates
        HashSet<Transaction> myNewProposalTxSet = new HashSet<>(myProposalOfTransactions);
        if (candidates!=null) {
//            for (Candidate c : candidates) {
//                boolean trustThisFlag = true;
//                trustThisFlag = checkTrustOnTxAndSender();
//                if (trustThisFlag) {
//                    //we always accept the proposed tx by adding to our new proposal
//                    Transaction tx = c.tx;
//                    myNewProposalTxSet.add(c.tx);
//                    //and add the sender to our trusted sender list
//                    int sender = c.sender;
//                    trustedSenders.add(new Integer(sender));
//                }
//            }
            Set<Transaction> txSet = candidatesGraph.keySet();
            for (Transaction tx : txSet) {
                boolean trustThisFlag = false;

                Set<Integer> senders = candidatesGraph.get(tx);

                //Scenario A: we will see if tx are proposed by 45% of my neighbours
                //of cos we need to trust the tx
                if (senders.size()>0.45*followeeCount) {
                    trustThisFlag = true;
                }

                //Scenario B: we see if the tx is proposed by any whitelist senders
                //if (anySenderWasAlreadyTrustedByMe(senders)) trustThisFlag = true;

                //Scenario C: the expected size of my malicious neighbour nodes is myTotalFolloweeCount * p_malicious\
                //so if i receive more senders proposing a given tx than this expected malcious neighbor size
                //i will say i will trust this tx
                if (senders.size()>followeeCount*p_malicious) {
                    trustThisFlag = true;
                }

                //dirty trick
                if (senders.size()>=2) trustThisFlag = true;

                //System.out.println("proposer size "+senders.size()+" while followeeCount "+followeeCount);

                if (trustThisFlag) {
                    //we always accept the proposed tx by adding to our new proposal
                    myNewProposalTxSet.add(tx);
                    //TODO: consider to add some(?) sender to our trusted sender list
                    //for (Integer i : senders) trustedSenders.add(i);
                } else {
                    //we dont trust this tx this round
                    //maybe in subsequent rounds we will trust this, but not now.
                }
            }
        }
        return myNewProposalTxSet;
    }

//    private boolean anySenderWasAlreadyTrustedByMe(Set<Integer> tempSenders) {
//        for (Integer i : tempSenders) {
//            //that means, the tx is endorsed by tempSenders, and
//            //one sender in tempSenders is in my trustSenders whitelist,
//            //so i will trust this tx
//            if (whitelistSenders.contains(i)) return true;
//        }
//        //none of these senders are in my trustSenders whitelist
//        return false;
//    }


    //called order in Sim: (4)
    //this is a total set of tx that broadcast from nodes that i trust/follow
    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS

        //everytime we get the new candidates set,
        this.candidates = candidates;

        //we update the corresponding candidate graph for easier processing later in sendToFollowers()
        //this graph is basically a (tx --> set of senders that proposed this tx) Map
        Map<Transaction,Set<Integer>> newCandidatesGraph = new HashMap<>();
        for (Candidate c : candidates) {
            Transaction tx = c.tx;
            Integer sender = new Integer(c.sender);
            Set<Integer> senderSet = newCandidatesGraph.get(tx);
            if (senderSet==null) {
                senderSet = new HashSet<Integer>();
                newCandidatesGraph.put(tx,senderSet);
            }
            senderSet.add(sender);
        }
        this.candidatesGraph = newCandidatesGraph;


    }
}
