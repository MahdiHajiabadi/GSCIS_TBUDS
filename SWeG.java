
package graph_sum;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.javatuples.Pair;
import java.io.FileWriter;
import java.io.File;
import java.util.Comparator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.iterator.TIntIterator;
import java.util.Iterator;
import java.util.LinkedList;

public class SWeG {
	ImmutableGraph Gr;
	int n;
	
	int[] h;
	
	int[] S; //supernodes array; e.g. S[3]==2 means node 3 is in supernode 2.
	int[] I; //the first node array; 
			//e.g. I[3]==5 means the first node in the supernode of node 3 is in index 5.
			//e.g. I[4]==-1 means there is no supernode with id==4. 
	int[] J; //the next node array; 
			//e.g. J[3]==9 means the next node in the supernode of node 3 is in index 9.
	
			//Actually, the index of a node is the same as node id. 
	
	int[] F; //shingle array; 
			 //e.g. F[2]==8 means the shingle of supernode 2 is 8. 
			//e.g. F[2]==-1 means there is no supernode 2. 
	
	Integer[] G; //sorted group array; e.g. F[G[i]] <= F[G[i+1]] 
	int gstart = 0;  //the index in G for which we have a real group (not -1)

    int use_lsh_merge = 0; // gloabl variable to indicate if using lsh merge step
    int print_iteration_offset = 0; // run encode step after how many iterations

    int[] supernode_sizes;
    HashMap<Integer, TIntArrayList> sn_to_n;
    ArrayList<Pair<Integer, Integer>> P;
    TIntArrayList Cp_0, Cp_1;
    TIntArrayList Cm_0, Cm_1;

    double divideAndMergeTime;
    double encodeTime;
    double dropTime;

    double error_bound;
    double[] cv;

    //ArrayList<Integer> merge_group_sizes;
	
	public SWeG (String basename, double error_bound) throws Exception {
		Gr = ImmutableGraph.loadMapped(basename);
		n = Gr.numNodes();
		h = new int[n];
		S = new int[n];
		I = new int[n];
        J = new int[n];	

		for(int i=0; i<n; i++){
			h[i] = i;			
			S[i] = i;
			I[i] = i;
			J[i] = -1;
        }
        
        // global data structures
        supernode_sizes = new int[n];
        sn_to_n = new HashMap<Integer, TIntArrayList>();
        P = new ArrayList<Pair<Integer, Integer>>();
        Cp_0 = new TIntArrayList(); Cp_1 = new TIntArrayList();
        Cm_0 = new TIntArrayList(); Cm_1 = new TIntArrayList();

        cv = new double [n];
        divideAndMergeTime = 0;
        encodeTime = 0;
        dropTime = 0;
        this.error_bound = error_bound;
	}

	void shuffleArray(){
    // If running on Java 6 or older, use `new Random()` on RHS here
	    Random rnd = new Random();
	    rnd = ThreadLocalRandom.current();
	    for (int i = h.length - 1; i > 0; i--)
	    {
	      int index = rnd.nextInt(i + 1);
	      // Simple swap
	      int a = h[index];
	      h[index] = h[i];
	      h[i] = a;
	    }
  }
	
	/*Input: input graph G = (V, E), current supernodes S
	  Output: disjoint groups of supernodes: {S(1), . . ., S(k) }
	1: generate a random bijective hash function h : V to {1, . . ., |V| }
	2: for each supernode A in S do
	3: 		for each node v in A do
	4: 			f(v)=min({h(u) : u in Nv or u = v })  //shingle of node v
	5: 		F(A)=min({f(v) : v in A}) //shingle of supernode A
	6: divide the supernodes in S into {S(1), . . ., S(k) } by their F(.) value
	7: return {S(1), . . ., S(k) }
	 */
	
	
	void Divide() {
		//generate a random bijective hash function h : V to {1, . . ., |V| } 
		// Collections.shuffle(Arrays.asList(h));
		shuffleArray();
		F = new int[n];
		for(int A=0; A<n ; A++) 
			F[A] = -1;
		
		for(int A=0; A<n ; A++) {
			if(I[A]==-1) //A is not supernode; skip
				continue;
			
			F[A] = n; //one more than greatest possible value of n-1
			for(int v=I[A]; ; v=J[v]) {
				int fv = f(v);
				if(F[A] > fv)
					F[A] = fv;
				
				if(J[v]==-1)
					break;
			}
		}
		
		//sort groups
        G = new Integer[n];
        for(int i = 0; i < n; i++) G[i] = i;
        Arrays.sort(G, (o1,o2) -> Integer.compare(F[o1], F[o2]));
        
        //initialize gstart: the index in G for which we have a real group (not -1)
        gstart = 0;
        while(F[G[gstart]] == -1)
        	gstart++;
	}
	
	
	
	//shingle of a node
	int f(int v) {
		int fv = h[v]; // since we consider the shingle value of node and its neighbors, 
		// we initialize fv as the shingle value of node v. 
		int v_deg = Gr.outdegree(v);
		int[] v_succ = Gr.successorArray(v);
		for(int j=0; j<v_deg; j++) {
			int u = v_succ[j];
			if(fv > h[u])
				fv = h[u];
		}
		return fv;
	}
	//==============
			/*
			Input: input graph G = (V, E), current supernodes S,
		current iteration t, disjoint groups of supernodes {S(1), ..., S(k)}
		Output: updated supernodes S
		1: for each group S^(i) ∈ {S(1), ..., S^(k)} do
		2: Q <- S(i)
		3: while | Q | > 1 do
		4: pick and remove a random supernode A from Q
		5: B <- arg maxC∈Q SuperJaccard(A, C) ▷ Eq. (4) SuperJaccard(A, B) =sum v ∈N_A ∪ N_B min(w(A,v),w(B,v)) / sum v ∈N_A ∪ N_B max(w(A,v),w(B,v)),
		6: if Saving(A, B, S) ≥ θ (t) then ▷ Eq. (3) and Eq. (5)
		7: S <- (S − {A, B }) ∪ {A ∪ B } ▷ merge A and B
		8: S(i) <- (S(i) − {A, B }) ∪ {A ∪ B }
		9: Q <- (Q − {B }) ∪ {A ∪ B } ▷ replace B with A ∪ B
		10: return 
			*/
	void merge(int number_groups, int tt, int[][] group_prop){
		//tt -> current iteration, it is useful for defining the threshoold
		// group_prop: enables us to recover the supernodes inside group with O(1) time
		//number_groups: number of groups that generated by the dividing step.
		int merges_number = 0; int idx = 0; //Responsilbe for number of times merging between a pair of supernodes happen.
		int[] temp = new int[n]; // temp <- F[G]
		double Threshold = 1/((tt + 1)*1.0);
		int group_size = 0; double[] jac_sim;
		for (int i = 0 ; i < n ; i++) temp[i] = F[G[i]];
		//for (int i = 0; i<200;i++) System.out.print(temp[i] + " ");

		int total_merge_successes = 0;
		int total_merge_fails = 0;
		// for each group of supernodes in the dividing step
		for (int i = 0 ; i < number_groups ; i++){

			int st_position = group_prop[i][1];
			group_size = groups_length(temp,group_prop[i][0],st_position)-1;
			if (group_size<2) continue;

			// this is just for printing
			/*if(i%100 == 1){
				System.out.println("Key is: " + temp[group_prop[i][1]] + " Idx is: " + st_position + " Temp Value is: "  + group_prop[i][0] + " Number of Hits: " + group_size + " Currnet Super Group is: " + i);
				System.out.println("Number of Merging Happens so far: " + merges_number);
			}*/
			
			// Q is the current group of supernodes that we're merging (from dividing step)
			int[] Q = new int[group_size];
			int counter = 0;
			// create the group Q
			for (int j= st_position ; j < (st_position + group_size) ; j++){
				Q[counter++] = G[j];
			}

			int merging_success = 0;
			int merging_fails = 0;
			HashMap<Integer, HashMap<Integer,Integer>> hm = create_W(Q,group_size);

			int initial_size = hm.size();
			while(hm.size()>1){
					Random rand = new Random();
					int A = rand.nextInt(initial_size);
					if(hm.get(A) == null)
						continue;
					double max = 0;
					idx = -1;
					// boolean condition = false; 
					for(int j = 0 ; j < initial_size ; j++){
						if(hm.get(j) == null)
							continue;
						if (j==A) continue;
						jac_sim = JacSim_SavCost(hm.get(A),hm.get(j));
						if (jac_sim[0] > max)
						{
							// condition = true;
							max = jac_sim[0];
							idx = j;
						}
					}
					
					if (idx==-1){
						// System.out.println("IDX = -1");
						hm.remove(A);
						continue;
					}
					double savings = costSaving(hm.get(A) , hm.get(idx) , Q[A] , Q[idx]);
					if (savings >=Threshold){
						HashMap<Integer,Integer> w_update = update_W(hm.get(A),hm.get(idx));
						hm.replace(A, w_update);
						hm.remove(idx);
				 		Update_S(Q[A] , Q[idx]);
				 		merging_success++;
						merges_number++;
					}
					else
					{
						merging_fails++;
						hm.remove(A);
					}
			}

			total_merge_successes += merging_success;
			total_merge_fails += merging_fails;
		}

		//System.out.println();
		//System.out.println("Merging Successes: " + total_merge_successes);
		//System.out.println("Merging Fails:     " + total_merge_fails);

	}

    
    void encode_old() {
        //System.out.println("----------------------------------- ENCODE ----------------------------------------");
        System.out.println("Encoding NOT LSH...");        

        int supernode_count = 0;
        int[] S_copy = Arrays.copyOf(S, S.length);

        for (int i = 0; i < n; i++) {
            // if i is a supernode
            if (I[i] != -1) {
                int[] nodes_inside = Recover_S(i);
                TIntArrayList nodes_inside_list = new TIntArrayList();
                supernode_sizes[supernode_count] = nodes_inside.length;

                for (int j = 0; j < nodes_inside.length; j++) {
                    nodes_inside_list.add(nodes_inside[j]);
                    S_copy[nodes_inside[j]] = supernode_count;
                }

                sn_to_n.put(supernode_count, nodes_inside_list);
                supernode_count++;
            }
        }

        System.out.println("Supernodes: " + supernode_count);

        for (int A = 0; A < supernode_count; A++) {
            TIntArrayList in_A = sn_to_n.get(A);
            int[] edges_count = new int[supernode_count]; // neighbours_count[B] = how many edges between A and B
            HashSet<?>[] edges_list = new HashSet<?>[supernode_count]; // edges_list[B] = set of edges between A and B
            TIntHashSet has_edge_with_A = new TIntHashSet();
            

            for (int a = 0; a < in_A.size(); a++) {
                int node = in_A.get(a);
                int[] neighbours = Gr.successorArray(node);

                for (int i = 0; i < neighbours.length; i++) {
                    edges_count[S_copy[neighbours[i]]]++;

                    if (S_copy[neighbours[i]] >= A) { // if this B has not already been processed
                        has_edge_with_A.add(S_copy[neighbours[i]]);
                    }

                    if (edges_list[S_copy[neighbours[i]]] == null) {
                        edges_list[S_copy[neighbours[i]]] = new HashSet<Pair<Integer, Integer>>();
                    }
                    ((HashSet<Pair<Integer, Integer>>) edges_list[S_copy[neighbours[i]]]).add(new Pair(node, neighbours[i]));
                } // for i
            } // for A

            //for (int B = A; B < supernode_count; B++) {
            TIntIterator iter = has_edge_with_A.iterator();            
            while (iter.hasNext()) {
                int B = (Integer)iter.next();
            
                double edge_compare_cond = 0;
                if (A == B) { edge_compare_cond = supernode_sizes[A] * (supernode_sizes[A] - 1) / 4; }
                else        { edge_compare_cond = (supernode_sizes[A] * supernode_sizes[B]) / 2;     }

                if (edges_count[B] <= edge_compare_cond) {
                    // Add edges between A and B to C+
                    for (Pair<Integer, Integer> edge : ((HashSet<Pair<Integer, Integer>>)edges_list[B])) {
                        //Cp.add(edge);
                        Cp_0.add(edge.getValue0());
                        Cp_1.add(edge.getValue1());
                    }
                } else {
                    // Add an edge between A and B to P and add the difference to C-
                    P.add(new Pair(A, B));

                    TIntArrayList in_B = sn_to_n.get(B);

                    for (int a = 0; a < in_A.size(); a++) {
                        for (int b = 0; b < in_B.size(); b++) {
                            Pair<Integer, Integer> edge = new Pair(in_A.get(a), in_B.get(b));

                            if (!((HashSet<Pair<Integer, Integer>>)edges_list[B]).contains(edge)) {
                                //Cm.add(new Pair(in_A.get(a), in_B.get(b)));
                                Cm_0.add(in_A.get(a));
                                Cm_1.add(in_B.get(b));
                            }
                        } // for b
                    } // for a
                    
                } // else

            } // for B
        } // for A

        System.out.print("P: " + P.size() + " edges | ");
        System.out.print("Cp: " + Cp_0.size() + " edges | ");
        System.out.print("Cm: " + Cm_0.size() + " edges | ");
        System.out.println("Orig Edges: " + Gr.numArcs() + " edges");
        System.out.println("No Drop Compression: " + (1 - (P.size() + Cp_0.size() + Cm_0.size() * 1.0)/(Gr.numArcs() / 2 * 1.0)));

    }


    void drop() {
        //System.out.println("----------------------------------- DROP ----------------------------------------");
        System.out.println("Dropping...");

        for (int i = 0; i < n; i++) {
            cv[i] = error_bound * Gr.outdegree(i);
        }

        //ArrayList<Pair<Integer, Integer>> updated_Cp = new ArrayList<Pair<Integer, Integer>>();
        TIntArrayList updated_Cp_0 = new TIntArrayList();
        TIntArrayList updated_Cp_1 = new TIntArrayList();        
        //for (Pair<Integer, Integer> edge : Cp) {
        for (int i = 0; i < Cp_0.size(); i++) {
            //int edge_u = edge.getValue0();
            //int edge_v = edge.getValue1();
            int edge_u = Cp_0.get(i);
            int edge_v = Cp_1.get(i);

            if (cv[edge_u] >= 1 && cv[edge_v] >= 1) {
                cv[edge_u] = cv[edge_u] - 1;
                cv[edge_v] = cv[edge_v] - 1;
            } else {
                //updated_Cp.add(edge);
                updated_Cp_0.add(edge_u);
                updated_Cp_1.add(edge_v);
            }
            /* Cp_0.removeAt(i);
            Cp_1.removeAt(i); */
        }
        Cp_0 = updated_Cp_0;
        Cp_1 = updated_Cp_1;
        
        //ArrayList<Pair<Integer, Integer>> updated_Cm = new ArrayList<Pair<Integer, Integer>>();
        TIntArrayList updated_Cm_0 = new TIntArrayList();
        TIntArrayList updated_Cm_1 = new TIntArrayList();   
        //for (Pair<Integer, Integer> edge : Cm) {
        for (int i = 0; i < Cm_0.size(); i++) {
            //int edge_u = edge.getValue0();
            //int edge_v = edge.getValue1();
            int edge_u = Cm_0.get(i);
            int edge_v = Cm_1.get(i);

            if (cv[edge_u] >= 1 && cv[edge_v] >= 1) {
                cv[edge_u] = cv[edge_u] - 1;
                cv[edge_v] = cv[edge_v] - 1;
            } else {
                //updated_Cm.add(edge);
                updated_Cm_0.add(edge_u);
                updated_Cm_1.add(edge_v);
            }
            /* Cm_0.removeAt(i);
            Cm_1.removeAt(i); */
        }
        Cm_0 = updated_Cm_0;
        Cm_1 = updated_Cm_1;

        Collections.sort(P, new EdgeCompare(supernode_sizes));
        ArrayList<Pair<Integer, Integer>> updated_P = new ArrayList<Pair<Integer, Integer>>();
        for (Pair<Integer, Integer> edge : P) {
            int A = edge.getValue0();
            int B = edge.getValue1();

            if (A == B) { 
                updated_P.add(edge);
                continue; 
            }

            int size_B = supernode_sizes[B];
            boolean cond_A = true;
            TIntArrayList in_A = sn_to_n.get(A);

            for (int i = 0; i < in_A.size(); i++) {
                if (cv[in_A.get(i)] < size_B) {
                    cond_A = false;
                    break;
                }
            }
            if (!cond_A) { 
                updated_P.add(edge);
                continue; 
            }

            int size_A = supernode_sizes[A];
            boolean cond_B = true;
            TIntArrayList in_B = sn_to_n.get(B);

            for (int i = 0; i < in_B.size(); i++) {
                if (cv[in_B.get(i)] < size_A) {
                    cond_B = false;
                    break;
                }
            }
            if (!cond_B) { 
                updated_P.add(edge);
                continue; 
            }

            // if conditions are all true, ie (A != B && all v in A && all v in B)
            for (int i = 0; i < in_A.size(); i++) {
                cv[in_A.get(i)] = cv[in_A.get(i)] - size_B;
            }    
            for (int i = 0; i < in_B.size(); i++) {
                cv[in_B.get(i)] = cv[in_B.get(i)] - size_A;
            } 
            
        } 
        P = updated_P;   
        System.out.println("Drop Compression: " + (1 - (P.size() + Cp_0.size() + Cm_0.size() * 1.0)/(Gr.numArcs()/2 * 1.0)));
    }


    public void output_graph() {

        try {
            File directory = new File("compressed");
            if (!directory.exists()) {
                directory.mkdir();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        // Write graph supernodes
        try {
            FileWriter fileWriterG = new FileWriter("compressed/G.txt");

            int supernode_count = sn_to_n.size();
            for (int supernode = 0; supernode < supernode_count; supernode++) {
                fileWriterG.write(supernode + "\t"); 

                TIntArrayList in_supernode = sn_to_n.get(supernode);
                for (int node = 0; node < in_supernode.size(); node++) {
                    fileWriterG.write("" + in_supernode.get(node));

                    if (node != in_supernode.size() - 1) {
                        fileWriterG.write("\t");
                    }
                }  

                fileWriterG.write("\n");
            }

            fileWriterG.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        // Write Edge List P and Correct Sets
        try {
            FileWriter fileWriterP = new FileWriter("compressed/P.txt");
            for (Pair<Integer, Integer> edge : P) {
                fileWriterP.write(edge.getValue0() + "\t" + edge.getValue1() + "\n");
            }
            fileWriterP.close();

            FileWriter fileWriterCp = new FileWriter("compressed/Cp.txt");
            //for (Pair<Integer, Integer> edge : Cp) {
            for (int i = 0; i < Cp_0.size(); i++) {
                //fileWriterCp.write(edge.getValue0() + "\t" + edge.getValue1() + "\n");
                fileWriterCp.write(Cp_0.get(i) + "\t" + Cp_1.get(i) + "\n");
            }
            fileWriterCp.close();

            FileWriter fileWriterCm = new FileWriter("compressed/Cm.txt");
            //for (Pair<Integer, Integer> edge : Cm) {
            for (int i = 0; i < Cm_0.size(); i++) {
                //fileWriterCm.write(edge.getValue0() + "\t" + edge.getValue1() + "\n");
                fileWriterCm.write(Cm_0.get(i) + "\t" + Cm_1.get(i) + "\n");
            }
            fileWriterCm.close();
        } catch (Exception e) {
            System.out.println(e);
        } 
    }

	//==================== Creating w_a for each suprenodes inside group Q
	HashMap<Integer, HashMap<Integer, Integer>> create_W(int[] Q, int group_size){

		HashMap<Integer, HashMap<Integer, Integer>> w_All = new HashMap<Integer, HashMap<Integer, Integer>>();

		for (int i = 0 ; i < group_size ; i++){
			HashMap<Integer, Integer> w_Single = new HashMap<Integer, Integer>();
			int[] Nodes = Recover_S(Q[i]);

			for (int j = 0 ; j < Nodes.length ; j++){
				int[] Neigh = Gr.successorArray(Nodes[j]);

				for (int k = 0 ; k < Neigh.length ; k++){
					if (w_Single.containsKey(Neigh[k]))
						w_Single.put(Neigh[k], w_Single.get(Neigh[k])+1);
					else
						w_Single.put(Neigh[k], 1);
				}
			}
			w_All.put(i,w_Single);
		}

		return w_All;
    }
    
    // spA, spB are W_A, W_B respectively
	double costSaving(HashMap<Integer,Integer> spA , HashMap<Integer,Integer> spB , int spA_id , int spB_id){
		int[] nodesA = Recover_S(spA_id);
		int[] nodesB = Recover_S(spB_id);
		double costA = 0,costB = 0 ,costAunionB = 0;
		HashMap<Integer,Integer> candidateSize  = new HashMap<Integer,Integer>(); // supernode id S to the number of nodes inside S
		HashMap<Integer,Integer> candidate_spA  = new HashMap<Integer,Integer>(); // supdnode id S to number of edges from A to all ndoes in S
        HashMap<Integer,Integer> candidate_spB  = new HashMap<Integer,Integer>(); // supdnode id S to number of edges from B to all ndoes in S
        
		for (Integer key : spA.keySet()){
			if (!candidateSize.containsKey(S[key])) {
				int[] nodes = Recover_S(S[key]);
				candidateSize.put(S[key] , nodes.length);
				candidate_spA.put(S[key] , spA.get(key));
			} else {
                candidate_spA.put(S[key] , candidate_spA.get(S[key]) + spA.get(key));
            }
        }
		for (Integer key : spB.keySet()){
			if (!candidateSize.containsKey(S[key])) {
				int[] nodes = Recover_S(S[key]);
				candidateSize.put(S[key] , nodes.length);
				candidate_spB.put(S[key] , spB.get(key));
			} else if(candidate_spB.containsKey(S[key])) {
				candidate_spB.put(S[key] , candidate_spB.get(S[key]) + spB.get(key));
            } else {
				candidate_spB.put(S[key] , spB.get(key));
            }
		}
		
		//Start Calculating costA,costB & costAunionB
		for (Integer key : candidate_spA.keySet()){
			if (key == spA_id) { // in case of superloop
				if (candidate_spA.get(key) >= (nodesA.length * 1.0 * nodesA.length)/4.0)
					costA += (nodesA.length * nodesA.length) - candidate_spA.get(key) ;
				else
					costA += candidate_spA.get(key);
				continue;
			}
			if (candidate_spA.get(key) >= (nodesA.length * 1.0 * candidateSize.get(key))/2.0)
				costA += (candidateSize.get(key) * nodesA.length) - candidate_spA.get(key) + 1;
			else 
				 costA += candidate_spA.get(key);
			
			if (key==spB_id)
				continue;

			if (candidate_spB.containsKey(key)){
				if ((candidate_spA.get(key) + candidate_spB.get(key)) >= ((nodesA.length + nodesB.length) * 1.0 * candidateSize.get(key))/2.0)
					costAunionB += (candidateSize.get(key) * (nodesA.length + nodesB.length)) - candidate_spA.get(key) - candidate_spB.get(key) + 1;
			    else 
				    costAunionB += candidate_spA.get(key) + candidate_spB.get(key);
			} else {
				if ((candidate_spA.get(key) >= ((nodesA.length + nodesB.length)* 1.0 *candidateSize.get(key))/2.0))
					costAunionB += (candidateSize.get(key)  *(nodesA.length + nodesB.length)) - candidate_spA.get(key) + 1;
				else 
					costAunionB += candidate_spA.get(key);
			}
		}
		for (Integer key:candidate_spB.keySet()){

			if (key== spB_id){ // in case of superloop
				if (candidate_spB.get(key) >= (nodesB.length * 1.0 * nodesB.length)/4.0)
					costB += (nodesB.length * nodesB.length) - candidate_spB.get(key) ;
				else
					costB += candidate_spB.get(key);
				continue;
			}

			if (candidate_spB.get(key) >= (nodesB.length * 1.0 * candidateSize.get(key))/2.0)
				costB += (candidateSize.get(key) * nodesB.length) - candidate_spB.get(key) + 1;
			else 
                costB += candidate_spB.get(key);
                
			if (candidate_spA.containsKey(key) || key == spA_id){  
				continue;
			} else {
				if ((candidate_spB.get(key) >= ((nodesA.length + nodesB.length)* 1.0 * candidateSize.get(key))/2))
					costAunionB += (candidateSize.get(key) * (nodesA.length + nodesB.length)) - candidate_spB.get(key) + 1;
				else 
					costAunionB += candidate_spB.get(key);
			}
        }
        
		int aUnionBEdges = 0;
		if (candidate_spA.containsKey(spA_id))   //Superloop between spA and spA
			aUnionBEdges += candidate_spA.get(spA_id);
		if (candidate_spA.containsKey(spB_id)) // Superloop between spA and spB
			aUnionBEdges += candidate_spA.get(spB_id);
		if (candidate_spB.containsKey(spB_id)) // Superloop between spB and spB
			aUnionBEdges += candidate_spB.get(spB_id);
		if (aUnionBEdges > 0){
			if (aUnionBEdges >= ((nodesA.length + nodesB.length) * 1.0 * (nodesA.length + nodesB.length))/4.0)
				costAunionB += (nodesA.length + nodesB.length) * (nodesA.length + nodesB.length) - aUnionBEdges;
			else
				costAunionB += aUnionBEdges;
        }
        
		return 1 - (costAunionB )/(costA + costB);
	}

	//======================= HashMap<Integer,Integer> update_w after merging
	HashMap<Integer,Integer> update_W(HashMap<Integer,Integer> w_A , HashMap<Integer,Integer> w_B)
	{
		HashMap<Integer,Integer> result = new HashMap<Integer,Integer>();
		for(Integer key: w_A.keySet()){
			if (w_B.containsKey(key))
				result.put(key, w_A.get(key) + w_B.get(key));
			else
				result.put(key, w_A.get(key));
		}
		for(Integer key: w_B.keySet()){
			if (w_A.containsKey(key))
				continue;
			result.put(key, w_B.get(key));
		}
		return result;
	}
	//==================== Jaccard Similarity (Eq.4) and Savings Eq 3 and Eq. 5
	// Updated version of Jaccard Sim with using just pair of W's
//====================================================================
	double[] JacSim_SavCost(HashMap<Integer, Integer> w_A , HashMap<Integer,Integer> w_B){
		int  down =0; int up = 0; int savings_up = 0; int savings_down = 0;
		savings_down = w_A.size() + w_B.size();
		double[] res = new double[2];
		for (Integer key : w_A.keySet()) {
			savings_up++;
    		if (w_B.containsKey(key)) {
    			if (w_A.get(key)<=w_B.get(key))
    			{
    				up = up + w_A.get(key);	
    				down = down + w_B.get(key);	
    			}
    			else
    			{
    				down = down + w_A.get(key);	
    				up = up + w_B.get(key);	
    			}
    		}
    		else
    			down = down + w_A.get(key);	
    	}
    	for (Integer key: w_B.keySet()){
    		if (!(w_A.containsKey(key))){
    			savings_up++;
    			down = down + w_B.get(key);	
    		}
    	}
    	res[0] = (up*1.0)/(down*1.0);
		res[1] = 1 - (savings_up * 1.0)/(savings_down * 1.0) ;
		return res;
	}
//=============== Realize the number of duplicate value (key) from the start postion (st_pos) in arr
	int groups_length(int[] arr,int key , int st_pos){
		int count = 1;
		while(arr[st_pos++]==key && st_pos<arr.length - 1) count++;
		return count;
	}

//====================== Finds the nodes in each supernode with index key
	int[] Recover_S(int key){
		//Extracting the nodes belong to supernode key and return it (Arr)
		int length1 = supernode_length(key);
		int[] Arr = new int[length1];
		int counter = 0;
		int kk1 = I[key];
		while (kk1 !=-1){
			Arr[counter++] = kk1;
			kk1 = J[kk1];
		}
		return Arr;
	}
	//=================================================================== Find the length of supernodes with index SP_A
	int supernode_length(int SP_A){
		int counter = 0;
		int kk = I[SP_A];
		while(kk!=-1){
			counter++;
			kk = J[kk];
		}
		return counter;
	}
	//========================================================== Update Merging Supernodes
	void Update_S(int A, int B){
		int[] A_Nodes = Recover_S(A);
		int[] B_Nodes = Recover_S(B);
		J[A_Nodes[A_Nodes.length-1]] = I[B];
		I[B] = -1;
		for (int i = 0 ; i < A_Nodes.length ; i++) S[A_Nodes[i]] = I[A];
			for (int i = 0 ; i < B_Nodes.length ; i++) S[B_Nodes[i]] = I[A];
	}
	void test(int Iter) {
    	System.out.println("-----------------------------------  Merging ----------------------------------------");
        long divideStartTime = System.currentTimeMillis();
        long encodeStartTime = 0;
        long dropStartTime = 0;

		for (int iter = 1 ; iter <=Iter ; iter++){
            System.out.print(iter + " ");
			Divide();
			//System.out.println("gstart = " + gstart);
			int cnt_groups = 0;
			int g = -1; //current group
			for(int i=gstart; i<n; i++) {
				//System.out.println(F[G[i]]);
				if(F[G[i]] != g) {
					cnt_groups++;
					g = F[G[i]];
				}
			}
            int[][] group_prop = new int[cnt_groups][2]; // when F[G[i]]>F[G[i-1]] -> Another new group. 
			//In this case: F[G[i]]: id of the new detected group  -> group_prop[counter][0] <- F[G[i]]
			// The first hit of the group in F[G[]]  is i -> group_prop[counter][1] <- i
			g = -1;
			int counter = 0;
			for(int i=gstart; i<n; i++) {
				if(F[G[i]] != g) {
					group_prop[counter][0] = F[G[i]];
					group_prop[counter][1] = i;
					counter++;
					// cnt_groups++;
					g = F[G[i]];
				}
			}
			merge(cnt_groups, iter, group_prop); 
            if (iter % print_iteration_offset == 0 && iter != Iter) {
                System.out.println("\n------------------------- ITERATION " + iter);
                divideAndMergeTime += (System.currentTimeMillis() - divideStartTime) / 1000.0;
                System.out.println("Divide and Merge Time: " + divideAndMergeTime + " seconds");

                supernode_sizes = new int[n];
                sn_to_n = new HashMap<Integer, TIntArrayList>();
                P = new ArrayList<Pair<Integer, Integer>>();
                Cp_0 = new TIntArrayList(); Cp_1 = new TIntArrayList();
                Cm_0 = new TIntArrayList(); Cm_1 = new TIntArrayList();

                encodeStartTime = System.currentTimeMillis();
                encode_old();
                encodeTime = (System.currentTimeMillis() - encodeStartTime) / 1000.0;
                System.out.println("Encode Time: " + encodeTime + " seconds");

                dropStartTime = System.currentTimeMillis();
                if (error_bound > 0) {
                    drop();
                }
                dropTime = (System.currentTimeMillis() - dropStartTime) / 1000.0;
                System.out.println("Drop Time: " + dropTime + " seconds\n");

                divideStartTime = System.currentTimeMillis();
            }

        } // for	

        System.out.println("\n------------------------- FINAL (Iteration " + Iter + ")");
        divideAndMergeTime += (System.currentTimeMillis() - divideStartTime) / 1000.0;
        System.out.println("Divide and Merge Time: " + divideAndMergeTime + " seconds");

        supernode_sizes = new int[n];
        sn_to_n = new HashMap<Integer, TIntArrayList>();
        P = new ArrayList<Pair<Integer, Integer>>();
        Cp_0 = new TIntArrayList(); Cp_1 = new TIntArrayList();
        Cm_0 = new TIntArrayList(); Cm_1 = new TIntArrayList();

        encodeStartTime = System.currentTimeMillis();
		encode_old();         
        encodeTime = (System.currentTimeMillis() - encodeStartTime) / 1000.0;
        System.out.println("Encode Time: " + encodeTime + " seconds");

        dropStartTime = System.currentTimeMillis();
        if (error_bound > 0) {
            drop();
        }
        dropTime = (System.currentTimeMillis() - dropStartTime) / 1000.0;
        System.out.println("Drop Time: " + dropTime + " seconds");

	}

	void evaluateCompression() {
        int sp_num = 0;
        for (int i = 0; i < n; i++) {
            if(I[i] != -1) { sp_num++; }
        }
        System.out.println("Number of Supernodes: " + sp_num);
        System.out.println("Number of Edges Compressed: " + (P.size() + Cp_0.size() + Cm_0.size()));
        System.out.println("P edges: " + P.size());
        System.out.println("Cp edges: " + Cp_0.size());
        System.out.println("Cm edges: " + Cm_0.size());
        System.out.println("Number of Edges Original:   " + Gr.numArcs() / 2);
        System.out.println("Compression: " + (1 - (P.size() + Cp_0.size() + Cm_0.size() * 1.0)/(Gr.numArcs() / 2 * 1.0)));
	}

	public static void main(String[] args) throws Exception {
        int iteration = Integer.parseInt(args[1]);
        double error_bound = Double.parseDouble(args[3]);
		String basename = args[0];
		String[] postfx = args[0].split("/");
		long startTime = System.currentTimeMillis();
		SWeG t = new SWeG(basename, error_bound);
        t.print_iteration_offset = Integer.parseInt(args[2]);
        t.test(iteration);
        System.out.println();
        System.out.println("----------------------------------- EVALUATION ----------------------------------------");
        t.evaluateCompression();
        
        System.out.println();
        System.out.println("----------------------------------- OUTPUT ----------------------------------------");
        t.output_graph();
	}
}

class EdgeEncodeCompare implements Comparator<String>
{
    public int compare(String s1, String s2) {
        String[] s1a = s1.split(" ");
        String[] s2a = s2.split(" ");

        if (Integer.parseInt(s1a[0]) < Integer.parseInt(s2a[0]) || (Integer.parseInt(s1a[0]) == Integer.parseInt(s2a[0]) && Integer.parseInt(s1a[1]) < Integer.parseInt(s2a[1]))) {
            return -1;
        } else if (Integer.parseInt(s1a[0]) > Integer.parseInt(s2a[0]) || (Integer.parseInt(s1a[0]) == Integer.parseInt(s2a[0]) && Integer.parseInt(s1a[1]) > Integer.parseInt(s2a[1]))) {
            return 1;
        } else {
            return 0;
        }

        //return Integer.parseInt(s2a[0]) - Integer.parseInt(s1a[0]);
    }
}


class EdgeCompare implements Comparator<Pair<Integer, Integer>>
{   
    int[] supernode_sizes;

    public EdgeCompare(int[] supernode_sizes) {
        this.supernode_sizes = supernode_sizes;
    }

    public int compare(Pair<Integer, Integer> P1, Pair<Integer, Integer> P2) {
        int P1_val = supernode_sizes[P1.getValue0()] * supernode_sizes[P1.getValue1()];
        int P2_val = supernode_sizes[P2.getValue0()] * supernode_sizes[P2.getValue1()];

        if (P1_val < P2_val) { return -1; }
        else if (P1_val > P2_val) { return 1; }
        else { return 0; }
    }
}

class FourTuple implements Comparable<FourTuple> {
    int A, B, u, v;

    public FourTuple(int A, int B, int u, int v) {
        this.A = A;
        this.B = B;
        this.u = u;
        this.v = v;
    }

    public int compareTo(FourTuple other) {
        if (this.A < other.A || (this.A == other.A && this.B < other.B)) {
            return -1;
        } else if (this.A > other.A || (this.A == other.A && this.B > other.B)) {
            return 1;
        } else {
            return 0;
        }
    }
}
