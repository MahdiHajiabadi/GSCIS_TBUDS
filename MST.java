//To Compile the Codes on my Machine: javac -cp "lib/*" -d bin src/*.java
// To run it: java -cp "lib/*":"bin/" graph_sum.SuperNodes sets/uk-2007-05@100000-sym (Netowrk's name)
package graph_sum;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.lang.MutableString;
import java.util.ConcurrentModificationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.util.Arrays;
import java.util.Comparator;
import java.io.*;
import java.util.*;

public class MST{
	ImmutableGraph Gr;
	ImmutableGraph GrHop;
	int n;	
	long m;
	float[] score;
	public MST(String basename, String hop , String bc ) throws Exception {
	  	Gr = ImmutableGraph.loadMapped(basename);
	  	GrHop = ImmutableGraph.loadMapped(hop);
	  	n = Gr.numNodes();
	  	m = Gr.numArcs();
	  	score = new float[n];
	  	Scanner scanner = new Scanner(new File(bc));
	  	while (scanner.hasNextLine()) {
 			  String line = scanner.nextLine();
 			  String[] sp = line.split("\t");
 			  int node = Integer.parseInt(sp[0]);
 			  float value = Float.parseFloat(sp[1]);
 			  score[node] = value;
		}		
		// ApplyMST();
	}
	public int[][] ApplyMST() throws Exception{
		long edgeHop = GrHop.numArcs();
		double[][] weights = new double[(int)GrHop.numArcs()][3];
		int count = 0;
		for (int i = 0 ; i < n ; i++){
			int[] neighbors = GrHop.successorArray(i);
			for (int j = 0 ; j < neighbors.length ; j++){
				weights[count][0] = i;
				weights[count][1]= neighbors[j];
				weights[count++][2] = score[i] + score[neighbors[j]]; 
			}
		}
		//sort matrix based on this column
		int col  = 2;
		sortbyColumn(weights,col);
		Kruskal kruskal = new Kruskal();
		int [][] ordering = kruskal.mst(weights,n);
		return ordering;
		// ============================== Applying MST on 2-hop network, first of all for each node considers 
		//Assigning each node to its own forrest,
		// two nodes are connected when two different forrests becomes connected to each other.
		// int[] parents = new int[n];
		// for (int i = 0 ; i < n ; i++) parents[i] = i;
		// int num_edges = 0;
		// int[][] ordering = new int[n-1][2];
		// int counter = 0; int orderingCount = 0;
		// while(num_edges < n - 1 ){
		// 	int src = (int)weights[counter][0];
		// 	int trg = (int)weights[counter++][1];
		// 	// System.out.println("Calling for source nodes: " + src + " And Target node: "+ trg);
		// 	if (chekMerge(src,trg,parents)==true){
		// 		num_edges++;
		// 		//System.out.println("They should be merged: " + src + " , " + trg);
		// 		int swap = parents[src];
		// 		parents[src] = parents[trg];
		// 		parents[trg] = swap;
		// 		ordering[orderingCount][0] = src;
		// 		ordering[orderingCount++][1] = trg;
		// 	}

		// }
		// return ordering;
	}
	public boolean chekMerge(int a, int b, int[] p){
		int idx = a;
		while(p[a]!=b){
			if (p[a]==idx) return true;
			a = p[a];
		}
		return false;
	}
 	public static void sortbyColumn(double arr[][], int col){ 
        // Using built-in sort function Arrays.sort 
        Arrays.sort(arr, new Comparator<double[]>() { 
            
          @Override              
          // Compare values according to columns 
          public int compare(final double[] entry1,  
                             final double[] entry2) { 
  
            // To sort in ascending order revert  
            // the '<' Operator 
            if (entry1[col] < entry2[col]) 
                return -1; 
            else
                return 1; 
          } 
        });  // End of function call sort(). 
    } 



	public static void main(String[] args)throws Exception{
		// System.out.println("Please enter graph's basename.");
    	// Scanner sc = new Scanner(System.in);
    	// String basename = sc.next();
    	// System.out.println("Please enter the 2-hop graph's basename");
    	// String hop = sc.next();
    	// System.out.println("Please enter the Betweenness Centrality's basename");
    	// String bc = sc.next();
		String basename="Sets/wordassociation-2011";
		String hop = "Sets/2-hop";
		String bc="Sets/BC.txt";

		long start = System.currentTimeMillis();
		MST mst = new MST(basename,hop,bc);   
		long end = System.currentTimeMillis();
		double time = (end - start) / 1000F;
		System.out.println("execution time : " + time + " s");
	}

}
