// Example of a Simulation. This test runs the nodes on a random graph.
// At the end, it will print out the Transaction ids which each node
// believes consensus has been reached upon. You can use this simulation to
// test your nodes. You will want to try creating some deviant nodes and
// mixing them in the network to fully test.

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;

public class Simulation {

    public static void main(String[] args) {

        // There are four required command line arguments: p_graph (.1, .2, .3),
        // p_malicious (.15, .30, .45), p_txDistribution (.01, .05, .10),
        // and numRounds (10, 20). You should try to test your CompliantNode
        // code for all 3x3x3x2 = 54 combinations.

        int numNodes = 100;
        double p_graph = Double.parseDouble(args[0]); // parameter for random graph: prob. that an edge will exist
        double p_malicious = Double.parseDouble(args[1]); // prob. that a node will be set to be malicious
        double p_txDistribution = Double.parseDouble(args[2]); // probability of assigning an initial transaction to each node
        int numRounds = Integer.parseInt(args[3]); // number of simulation rounds your nodes will run for

        int ycmTrustedTxCountMax = 0;
        boolean ycmGlobalConsensusReachedFlag = false;


        // pick which nodes are malicious and which are compliant
        Node[] nodes = new Node[numNodes];
        for (int i = 0; i < numNodes; i++) {
            if (Math.random() < p_malicious)
                // When you are ready to try testing with malicious nodes, replace the
                // instantiation below with an instantiation of a MaliciousNode
                //YCM:
                //nodes[i] = new MalDoNothing(p_graph, p_malicious, p_txDistribution, numRounds);
                nodes[i] = new MaliciousNode(p_graph, p_malicious, p_txDistribution, numRounds);
            else
                nodes[i] = new CompliantNode(p_graph, p_malicious, p_txDistribution, numRounds);
        }


        // initialize random follow graph
        boolean[][] followees = new boolean[numNodes][numNodes]; // followees[i][j] is true if i follows j
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i == j) continue;
                if (Math.random() < p_graph) { // p_graph is .1, .2, or .3
                    followees[i][j] = true;
                }
            }
        }

        // notify all nodes of their followees
        for (int i = 0; i < numNodes; i++)
            nodes[i].setFollowees(followees[i]);

        // initialize a set of 500 valid Transactions with random ids
        int numTx = 500;    //TODO: original 500
        HashSet<Integer> validTxIds = new HashSet<Integer>();
        Random random = new Random();
        for (int i = 0; i < numTx; i++) {
            int r = random.nextInt();
            validTxIds.add(r);
        }

        // distribute the 500 Transactions throughout the nodes, to initialize
        // the starting state of Transactions each node has heard. The distribution
        // is random with probability p_txDistribution for each Transaction-Node pair.
        for (int i = 0; i < numNodes; i++) {
            HashSet<Transaction> pendingTransactions = new HashSet<Transaction>();
            for (Integer txID : validTxIds) {
                if (Math.random() < p_txDistribution) // p_txDistribution is .01, .05, or .10.
                    pendingTransactions.add(new Transaction(txID));
            }
            nodes[i].setPendingTransaction(pendingTransactions);
        }


        // Simulate for numRounds times
        for (int round = 0; round < numRounds; round++) { // numRounds is either 10 or 20

            if (ycmGlobalConsensusReachedFlag) break;

            System.out.println("STARTED ROUND "+round);
            boolean ycmCurrentRoundConsensusReachedFlag = true;

            // gather all the proposals into a map. The key is the index of the node receiving
            // proposals. The value is an ArrayList containing 1x2 Integer arrays. The first
            // element of each array is the id of the transaction being proposed and the second
            // element is the index # of the node proposing the transaction.
            //YCM: The given comment is wrong. it is no longer an arraylist, it is now a hashmap
            HashMap<Integer, Set<Candidate>> allProposals = new HashMap<>();

            //YCM: summary of this nested for-loop
            //YCM: for a given node node-i, it reached its own consensus and therefore will broadcast this set to all its followers
            //YCM:   all tx that node-i trust will be put into node-j's proposal
            //YCM:   this proposal is a set of candidate, a candidate is a tx (also tag who proposed it, i)
            //YCM: at the end of the loops, allProposals[k] contains all the candidates proposed by the nodes that node-k follows.
            //YCM: if a tx is proposed by multiple senders say 5 senders, we will have 5 candidate instances which all points to the same tx
            for (int i = 0; i < numNodes; i++) {
                Set<Transaction> proposals = nodes[i].sendToFollowers();
                for (Transaction tx : proposals) {
                    if (!validTxIds.contains(tx.id))
                        continue; // ensure that each tx is actually valid

                    for (int j = 0; j < numNodes; j++) {
                        if (!followees[j][i]) continue; // tx only matters if j follows i

                        //YCM: upon reaching to this point, for this particular i-j pair, we have:
                        //YCM: node j follow node i, and node i has a proposal to send to j

                        if (!allProposals.containsKey(j)) {
                            Set<Candidate> candidates = new HashSet<>();
                            allProposals.put(j, candidates);
                        }

                        Candidate candidate = new Candidate(tx, i);
                        allProposals.get(j).add(candidate);
                    }   //YCM: at the end of this for-loop, all node j that follow i, will have
                        //YCM: a tx candidate (taken and loop from j's consensus) pushed to j (from i)
                        //YCM: all tx candidates (broadcasted from nodes i trust) is retrievable via allProposals
                }

                //YCM: this is a short method (part 1) to output if all nodes that output some tx (tx size>0),
                //YCM: their tx size are all equal
                if (proposals.size()>0) {
                    if (proposals.size()==ycmTrustedTxCountMax) {
                        //do nothing
                    } else {
                        ycmCurrentRoundConsensusReachedFlag = false;
                        //System.out.println("Node "+i+" has set current consensus flag to false because its node size is "+proposals.size()+" and current tx max is "+ycmTrustedTxCountMax);
                        if (proposals.size()>ycmTrustedTxCountMax) {
                            ycmTrustedTxCountMax = proposals.size();
                            //System.out.println("Node "+i+ " propose node size "+ycmTrustedTxCountMax+" and is now the local maximum");
                        }
                    }
                } else {
                    //System.out.println("Bypassed. We purposely ignore this node because this node's proposals size is 0");
                }
            }

            // Distribute the Proposals to their intended recipients as Candidates
            for (int i = 0; i < numNodes; i++) {
                if (allProposals.containsKey(i))
                    nodes[i].receiveFromFollowees(allProposals.get(i));
            }

            //YCM: this is a short method (part 2) to output if all nodes that output some tx (tx size>0),
            //YCM: their tx size are all equal
            if (ycmCurrentRoundConsensusReachedFlag) {
                ycmGlobalConsensusReachedFlag = true;
                System.out.println("REACHED CONSENSUS at ROUND "+round);
            }

        }

        // print results
        /*
        for (int i = 0; i < numNodes; i++) {
            Set<Transaction> transactions = nodes[i].sendToFollowers();
            System.out.println("Transaction ids that Node " + i + " believes consensus on: [size] "+transactions.size());
            //for (Transaction tx : transactions) System.out.println(tx.id);
            System.out.println();
            System.out.println();
        }
        */
    }
}

