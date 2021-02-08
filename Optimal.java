package graph_sum;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.lang.MutableString;

import java.util.*;
import com.google.common.collect.Sets; 
import it.unimi.dsi.webgraph.ImmutableGraph;
import java.io.*;

public class Optimal{
	static String outputDirectory = "output/";
    private static String GRAPH_BASENAME;
    private static String UTILITY_OUTPUT_FILE;
    private static String SUPEREDGES_OUTPUT_FILE;
    private static String SUPERNODES_OUTPUT_FILE;
    private static ImmutableGraph G;
    private static final int prime = 31;
    private static TreeMap<Integer,Cluster>  mapClique;
	private static TreeMap<Integer,Cluster>  mapIS;
	private static PrintStream psOutput;



	public static void main(String[] args){
		try{

			long startTime = System.nanoTime();
        	String path = args[0];
  			String[] pathInParts = path.split("\\/");
  			
  			String filename = pathInParts[pathInParts.length-1];
  			GRAPH_BASENAME = path;
        	UTILITY_OUTPUT_FILE=outputDirectory+filename+"-summary.output";
   			SUPEREDGES_OUTPUT_FILE = outputDirectory+filename+"-summary.superedges";
    		SUPERNODES_OUTPUT_FILE = outputDirectory+filename+"-summary.supernodes";

			psOutput = new PrintStream(new File(UTILITY_OUTPUT_FILE));
			G= ImmutableGraph.loadMapped(GRAPH_BASENAME);
			createCandidateClusters();
			System.out.println("created candidate clusters");
			ArrayList<Cluster> clusterList = createClustersFromCandidates();
			System.out.println("clusters created");
			System.out.println(" Number of supernodes: " + clusterList.size());
			System.out.println("Reduction in nodes : "+(G.numNodes()-clusterList.size())*1.0/G.numNodes());
			psOutput.println("Reduction in nodes : "+(G.numNodes()-clusterList.size())*1.0/G.numNodes());

			connectSuperEdges(clusterList);
			System.out.println("superedges connected");
			long endTime   = System.nanoTime();
			long totalTime = endTime - startTime;
			System.out.println("time : "+ totalTime/1000000000.0 +" s" );
			psOutput.println("time : "+ totalTime/1000000000.0 +" s");
			psOutput.flush();
			psOutput.close();
		}
		catch(Exception e){
			System.out.println(e);
		}
	}

	// to generate hash code for neighbor lists (in case of independent set)
	private static int generateHashcodeIS(int u){
		long code = 1;
		for(int v: G.successorArray(u)){
			code = (code*prime + v)%Integer.MAX_VALUE;
		}
		return (int)code;
	}

	// to generate hash code for neighbor lists (in case clique)
	private static int generateHashcodeClique(int u){
		int code = 1;
		boolean uAdded = false;	
		for(int v: G.successorArray(u)){
			if(v>u && !uAdded){
				code = (code*prime + u)%Integer.MAX_VALUE;
				uAdded = true;
			}
			code = (code*prime + v)%Integer.MAX_VALUE;
		}
		if(!uAdded){
			code = (code*prime + u)%Integer.MAX_VALUE;
		}
		return code;
	}


	private static void createCandidateClusters(){
		int hashCodeIS;
		int hashCodeClique;

		mapClique = new TreeMap<Integer,Cluster>();
		mapIS = new TreeMap<Integer,Cluster>();
		
		for(int i=0;i<G.numNodes();i++){
			hashCodeIS = generateHashcodeIS(i);
			hashCodeClique = generateHashcodeClique(i);
			if(mapClique.containsKey(hashCodeClique)){
				mapClique.get(hashCodeClique).add(i);
			}
			else{
				mapClique.put(hashCodeClique,new Cluster(i));
			}
			if(mapIS.containsKey(hashCodeIS)){
				mapIS.get(hashCodeIS).add(i);
			}
			else{
				mapIS.put(hashCodeIS,new Cluster(i));
			}
		}
	}

	private static ArrayList<Cluster> createClustersFromCandidates(){
		boolean [] visited = new boolean[G.numNodes()];
		TreeSet<Integer> V = new TreeSet<Integer>();
		ArrayList<Cluster> clusterList = new ArrayList<Cluster>();

		int count=0;
		for (Map.Entry<Integer, Cluster> entry : mapClique.entrySet()) {
			Cluster clique = entry.getValue();
			// filter clique
			if(clique.size()>1){
				for(int node:clique.getNodes()){
					V.add(node);
				}
				while(V.size()>0){
					Iterator<Integer> iterator = V.iterator();
					int u = iterator.next();

					Cluster Cu = new Cluster(u);
					iterator.remove();
					while (iterator.hasNext()) {
					    Integer v = iterator.next();
					    if (compareNeighbors(u,v)) {
					    	if(!visited[v]){
			    		Cu.add(v);
			    		visited[v]=true;
					    	}
				        iterator.remove();
					    }
					}
					if(Cu.size()>1){
						visited[u]=true;
						clusterList.add(Cu);
						count+=Cu.size();
					}
				}
			}
		}
		V.clear();

		for (Map.Entry<Integer, Cluster> entry : mapIS.entrySet()) {
			Cluster is = entry.getValue();
			// filter IS
			if(is.size()>1){
				for(int node:is.getNodes()){
					// visited[node]=true;
					V.add(node);
				}
				while(V.size()>0){
					Iterator<Integer> iterator = V.iterator();
					int u = iterator.next();
					Cluster Iu = new Cluster(u);
					iterator.remove();
					while (iterator.hasNext()) {
					    Integer v = iterator.next();
					    if (compareNeighbors(u,v)) {
					    	if(!visited[v]){
					    		Iu.add(v);
					    		visited[v]=true;
					    	}
					        iterator.remove();
					    }
					}
					if(Iu.size()>1){
						visited[u]=true;
						clusterList.add(Iu);
						count+=Iu.size();
					}
				}
			}
		}		
		for(int i=0;i<G.numNodes();i++){
			if(!visited[i]){
				clusterList.add(new Cluster(i));
				count++;
			}
		}
		System.out.println(count);

		mapIS=null;
		mapClique=null;
		V=null;

		return clusterList;
	}

	private static void connectSuperEdges(ArrayList<Cluster> clusterList) throws Exception{

		long numSuperEdges=0;
		long numEdges =0;
		PrintStream psSuperNodes = new PrintStream(new File(SUPERNODES_OUTPUT_FILE));
		boolean[] visited = new boolean[G.numNodes()];

		// preprocessing for quick find
		int [] quickFind = new int[G.numNodes()];
		for(int i=0;i<clusterList.size();i++){
			psSuperNodes.print(i);
			for(int node:clusterList.get(i).getNodes()){
				// if (visited[node]) continue;
				psSuperNodes.print("\t" + node);
				visited[node] = true;
				quickFind[node]=i;
			}
			psSuperNodes.println();
		}
		psSuperNodes.flush();
		psSuperNodes.close();
		
		TreeSet<Integer> superNeigbhors = new TreeSet<Integer>();
		PrintStream psSuperEdges= new PrintStream(new File(SUPEREDGES_OUTPUT_FILE));
		for(int i=0;i<clusterList.size();i++){
			Cluster cluster = clusterList.get(i);
			for(int u:cluster.getNodes()){
				for(int v:G.successorArray(u)){
					numEdges++;
					superNeigbhors.add(quickFind[v]);
				}
			}	
			numSuperEdges+=superNeigbhors.size();
			for(int Sw: superNeigbhors){
				psSuperEdges.println(i+"\t"+Sw);
			}
			superNeigbhors.clear();
		}
		psSuperEdges.flush();
		psSuperEdges.close();
		System.out.println("Reduction in edges : " +(numEdges- numSuperEdges)*1.0/numEdges);
		psOutput.println("Reduction in edges : " +(numEdges- numSuperEdges)*1.0/numEdges);	
	}


	// returns true if val is contained in arr; false otherwise

	// returns true if u and v are a part of the same Cluster
	private static boolean compareNeighbors(int u,int v){
		// return true;

		//neighbors are sorted and this approach should be fine
		int [] uNeighbors = G.successorArray(u);
		int [] vNeighbors = G.successorArray(v);

		int i=0;
        int j=0;
        while(i<uNeighbors.length && j<vNeighbors.length){
                if(uNeighbors[i]==vNeighbors[j]){
                        i++;
                        j++;
                }
                else if(uNeighbors[i]==v){
                        i++;
                }
                else if(vNeighbors[j]==u){
                        j++;
                }
                else{
                	break;
                }
        }

        if(i<uNeighbors.length && uNeighbors[i]==v){
                i++;
        }

        if(j<vNeighbors.length && vNeighbors[j]==u){
                j++;
        }

        if(i==j && i==uNeighbors.length && j==vNeighbors.length){
                return true;
        }
        return false;
	}
}
