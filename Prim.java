package graph_sum;
import java.util.PriorityQueue;
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
import java.util.Comparator;
import java.io.*;
import java.util.*;
import org.apache.commons.math3.util.MathArrays;
import java.util.Scanner;


import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.IncrementalImmutableSequentialGraph;
import java.util.ArrayList;
//args[0] - > Original Graph
//args[1] - > Node PR scores
//args[2] - > output File

public class Prim {
	private boolean [] visited;
	private double[] distanceTo;
	private int[] previous;
	public int n;
	private IndexMinPQ<Double> pq;
		
    public double edgeIS(int u, int v, double[] nodeIS){
        return Math.pow(nodeIS[u],2)+Math.pow(nodeIS[v],2);
    }

    private static void normalizeNodeIS(double[] nodeIS){
        double sumNodeIS=0;
        for(double is:nodeIS){
            sumNodeIS+=is;
        }
        for(int i=0;i<nodeIS.length;i++){
            nodeIS[i]/=sumNodeIS;
        }
    }

    private static  double[] readNodeIS(ImmutableGraph G, String basename) throws Exception{

        double[] nodeIS = new double[G.numNodes()];
        System.out.println(G.numNodes());
	  	Scanner scanner = new Scanner(new File(basename));
	  	System.out.println("starting reading nodebc");
	  	while (scanner.hasNextLine()) {
 			  String[] pair = scanner.nextLine().split("\t");
 			  int node = Integer.parseInt(pair[0]);
 			  double value = Double.parseDouble(pair[1]);
 			  nodeIS[node] = value;
        }
        System.out.println("ended reading nodebc");
        scanner.close();
        normalizeNodeIS(nodeIS);
        // double max = 0; 
        // for (int i = 0 ; i < G.numNodes() ; i++)
        //     if (nodeIS[i] > max) max = nodeIS[i];
        // for (int i = 0 ; i < G.numNodes() ; i++)
        //     nodeIS[i] = max - nodeIS[i];
        return nodeIS;
    }

    public static void main(String[] args){
		try{
			String file = "cnr-2000";
		//	String postfx = "-sym-noself";
			String postfx = "2-hop";
			String centrality = file + "_pageRank_WG.txt";
	//		String centrality = file + "_nodebc.txt";
		//	String centrality =  file + "_eigenCentrality_WG.txt";
		//	String centrality =  file + "_pageRank_WG.txt";
			String dir = "sets/";
		//	String bc= dir + file +"_nodebc.txt";
		//	ImmutableGraph G = ImmutableGraph.loadMapped(dir + file + "2-hop");
			String bc=  dir + centrality;
			ImmutableGraph G = ImmutableGraph.loadMapped(args[0]);
			double[] nodeIS = readNodeIS(G,args[1]);
			Prim prim = new Prim();
			// double[][] mst =prim.mst(G,nodeIS);
			int[][] mst = prim.mst(G,nodeIS);
			System.out.println("Storing into WebGraph Format");
        // String file = ".ungraphEdgeList";
        	String basename = dir + file;
        	//String outfile = "mst_bc/"  + file + "MST-p";
        	String outfile = args[2];
        	PrintStream ps = new PrintStream(new File(outfile+".txt"));
        	for (int i = 0 ; i < prim.n - 1 ; i++)
        		ps.println(mst[i][0] + "\t" + mst[i][1]);
		}catch(Exception e){
			System.out.println(e);
		}		
	}
	
    private void prim(int source,ImmutableGraph G,double[] nodeIS){
    	visited[source]=true;
		previous[source]=-1;
        
        for(int v=0;v<n;v++)
		{
			if(v!=source){
				distanceTo[v]=Double.MAX_VALUE;
			}else{
				distanceTo[v]=0;
			}
			pq.insert(v,distanceTo[v]);
		}

        while(!pq.isEmpty())
		{
			int u = pq.delMin();
			visited[u]=true;
			// HashMap<Integer,Double> twoHop = find_2hop(u,G); //In case of finding the 2-hop graph on the fly, Input: original graph and nodeIS
            // HashMap<Integer,Double> twoHop = find_2hopWJ(u,G,nodeIS); //In case of finding the 2-hop graph based on weighted Jaccard
			// Integer[] nodes = twoHop.keySet().toArray(new Integer[0]); // Extracting all the two-hop away nodes
			// Double[] node_weights = twoHop.values().toArray(new Double[0]); // Extracting the Jasccard similarity weight of each 2-hop away node from the source node (u)
			for(int v:G.successorArray(u)) // In case of 2-hop graph is given as the input, Inputt: 2-hop graph and nodeIS
			// for (int j = 0 ; j < nodes.length ; j++) // In case of the original graph is given.
			{
				// double weight = -1 *(Double)node_weights[j]; In case of creating MST by using either Simple Jaccard or Weighted Jaccard
                //double weight = estimateWeight(u,v,nodeIS);
				//int v = (Integer)nodes[j];
				double weight = edgeIS(u,v,nodeIS); //In case of the two hop Graph, and creating the MST by using PR scores of nodes 
				if(distanceTo[v] > weight && !visited[v]){
					distanceTo[v]=weight;
					previous[v]=u;
					pq.decreaseKey(v,distanceTo[v]);
				}
			}
		}
    }
    // public double estimateWeight(int source , int target ,double[] nodeIS ){
    //     // double weight = 0;
    //     double down = 0; double up = 0;
    //     double[] W  = new double[G_orig.numNodes()];
    //     for (int i:G_orig.successorArray(source)){
    //         down += nodeIS[i];
    //         W[i] = nodeIS[i];
    //     }
    //     for (int j:G_orig.successorArray(target)){
    //         if (W[i]==0)
    //             down = down + nodeIS[i];
    //         else if (nodeIS[i] > W[i])
    //         {
    //             down = down - W[i] + nodeIS[i];
    //             up = up + W[i];
    //         }
    //         else
    //         {
    //             up = up + nodeIS[i];
    //         }
    //     }
    //     return up/down;


    // }


    public HashMap<Integer,Double> find_2hop(int source, ImmutableGraph G){
        boolean[] neigh_2 = new boolean[G.numNodes()];
        neigh_2[source] = true;
        HashMap<Integer,Double> hm = new HashMap<Integer,Double>();
        int degSource = G.outdegree(source);
        int[] appearance = new int[G.numNodes()];
        ArrayList<Integer> temp = new ArrayList<Integer>();
        int[] neigh = G.successorArray(source);
        Set<Integer> sourceNeigh = new HashSet<Integer>();
        for (int v:neigh)
        	sourceNeigh.add(v);
        // sourceNeigh.addAll();
        for (int j : G.successorArray(source)){
        	if (j == source) continue; // Don't want to consider the super loop
            for (int k : G.successorArray(j)){
            	if (k == source) continue; // Don' want to get back to the source node
                appearance[k] +=1;
                if (neigh_2[k]== false){
                	neigh_2[k] = true;
                	temp.add(k); // Set of unique two-hop away nodes from node source
                }
            }
        }
        // int size = new int[temp.size()];
        for (int j = 0 ; j < temp.size() ; j++){
        	int node = temp.get(j);
        	int[] neigh_node = G.successorArray(node);
        	Set<Integer> nodeNeigh = new HashSet<Integer>();
        	for (int k:neigh_node)
        		nodeNeigh.add(k);
        	nodeNeigh.addAll(sourceNeigh);
        	double weight = (appearance[node] * 1.0)/(nodeNeigh.size() * 1.0);
        	hm.put(node,weight);
        }
        return hm;
    }

    // For finding the Weighted Jaccard Similarity
    public HashMap<Integer,Double> find_2hopWJ(int source, ImmutableGraph G,double[] nodeIS){
        boolean[] neigh_2 = new boolean[G.numNodes()];
        neigh_2[source] = true;
        HashMap<Integer,Double> hm = new HashMap<Integer,Double>();
        int degSource = G.outdegree(source);
        double[] up = new double[G.numNodes()];
        double[] down = new double[G.numNodes()];
        int[] appearance = new int[G.numNodes()];
        ArrayList<Integer> temp = new ArrayList<Integer>();
        int[] neigh = G.successorArray(source);
        Set<Integer> sourceNeigh = new HashSet<Integer>();
        for (int v:neigh)
            sourceNeigh.add(v);
        // sourceNeigh.addAll();
        for (int j : G.successorArray(source)){
            if (j == source) continue; // Don't want to consider the super loop
            for (int k : G.successorArray(j)){
                if (k == source) continue; // Don' want to get back to the source node
                if (nodeIS[k] <=nodeIS[source]){
                    up[k] = up[k] + nodeIS[k] + nodeIS[j];
                    down[k] = down[k] + nodeIS[source] + nodeIS[j];
                }
                else{
                    up[k] = up[k] + nodeIS[source] + nodeIS[j];
                    down[k] = down[k] + nodeIS[k] + nodeIS[j];

                }
                appearance[k] +=1;
                if (neigh_2[k]== false){
                    neigh_2[k] = true;
                    temp.add(k); // Set of unique two-hop away nodes from node source
                }
            }
        }
        // int size = new int[temp.size()];
        for (int j = 0 ; j < temp.size() ; j++){
            int node = temp.get(j);
            for (int k:G.successorArray(node)){
                if (sourceNeigh.contains(k))
                    sourceNeigh.remove(k);
                else
                    down[node] = down[node] + nodeIS[node] + nodeIS[k];
            }
            for (Integer k : sourceNeigh)
                down[node] = down[node] + nodeIS[source] + nodeIS[k];

            double weight = up[node]/down[node];
            hm.put(node,weight);
        }
        return hm;


    }

	public int[][] mst(ImmutableGraph G, double[] nodeIS) {
        
        n=G.numNodes();
		//System.out.print(n);
		//PriorityQueue<Edge> pq = preprocessEdges(graph,nodeIS);
        visited = new boolean [n];
        distanceTo = new double [n];
        previous = new int[n];
        pq = new IndexMinPQ<Double>(n);
        int numConnectedComponents=0;
       // prim(0,G,nodeIS);
         for(int i=0;i<n;i++){
         	if(!visited[i]){
         		prim(i,G,nodeIS);
         		numConnectedComponents++;
         	}
         }

        // System.out.println("numConnectedComponents : "+numConnectedComponents);
		int[][] mst =new int [n-1][2];
		int[] edge = new int[n - 1];
		for(int i=0;i<G.numNodes();i++){
			if(visited[i] && previous[i]!=-1){
				mst[i-1][0]=i;
				mst[i-1][1]=previous[i];
			}	
		}
		return mst;
	}
}
