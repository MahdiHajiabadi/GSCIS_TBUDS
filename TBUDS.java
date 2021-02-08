package graph_sum_lossy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.lang.MutableString;

import java.util.*;
import com.google.common.collect.Sets; 
import it.unimi.dsi.webgraph.ImmutableGraph;
import java.io.*;

public class TBUDS{
	static String directory = "../../sets/";
	static String sym = "-sym";
	static String filename;
	static String centralityMetric;
	private static String GRAPH_BASENAME;
	public static String NODE_BC_BASENAME;
	public static String TWO_HOP_G_MST;
	public static String UTILITY_OUTPUT_FILE;
	public static final int NUM_CONNECTED_COMPONENTS=1;
	private static ImmutableGraph G;
	private static ImmutableGraph twoHopMST;
	private static double VC2;
	private static double sum;
	private static double threshold;
	private static String parameterType;
	private static double[][] twoHopEdgeList;
	private static TreeMap<Integer,Double> map;
	private static double[] nodeIS;
	private static int[] degree;
	private static UnionFind uf;
	private static PrintStream ps;
	private static PrintStream ps2;
	private static PrintStream ps3;


    public static void readTwoHopGraph() throws Exception{
        int count=0;
        twoHopMST=ImmutableGraph.loadMapped(TWO_HOP_G_MST);
        twoHopEdgeList= new double[(int)twoHopMST.numArcs()][3];
        for (int i = 0 ; i < twoHopMST.numNodes() ; i++){
        	int[] neigh = twoHopMST.successorArray(i);
        	for(int j = 0 ; j < neigh.length ; j++){
    			twoHopEdgeList[count][0] = i;
	    		twoHopEdgeList[count][1]= neigh[j];
	    		twoHopEdgeList[count++][2] = nodeIS[i] + nodeIS[neigh[j]];
        	}
        }
        sortByColumn(twoHopEdgeList, 2);
        System.out.println("Processed two hop edge list");
    }

    public static void sortByColumn(double arr[][], int col){ 
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
            else if(entry1[col] > entry2[col])
                return 1;
            else
                return 0; 
          } 
        });  // End of function call sort(). 
    }

    private static void normalizeNodeIS(){
        double sumNodeIS=0;
        for(double is:nodeIS){
            sumNodeIS+=is;
        }
	System.out.println(sumNodeIS);
       for(int i=0;i<nodeIS.length;i++){
           nodeIS[i]/=sumNodeIS;
       }
    }

    private static void readNodeIS() throws Exception{
        nodeIS = new double[G.numNodes()];
        Scanner scanner = new Scanner(new File(NODE_BC_BASENAME));
        while (scanner.hasNextLine()) {
              String[] pair = scanner.nextLine().split("\t");
              int node = Integer.parseInt(pair[0]);
              double value = Double.parseDouble(pair[1]);
              nodeIS[node] = value;
        }
        scanner.close();
       normalizeNodeIS();   
        System.out.println("completed reading nodeIS");
        for (int i = 0 ; i < G.numNodes() ; i++)
        	if (nodeIS[i] < 0 ) System.out.println(" Negative Centrality");
    }

    public static void saveNodeDegree(){
    	degree = new int[G.numNodes()];
    	for(int i=0;i<G.numNodes();i++){
    		if (G.outdegree(i)==0)
    			degree[i] = 1;
    		else
    			degree[i] = G.outdegree(i);
    	}
    	System.out.println("saved degree of all nodes");
    }

    public static double getNodeIS(int node) {
    	if(degree[node]==0)
    		return 0;
//	return nodeIS[node]/degree[node];
         return nodeIS[node];
    }

	public static void main(String[] args){

		try{
		
			filename = args[0];
//			parameterType = "rn";
			parameterType = "u";
			//parameterType = args[1];
			//if(!(parameterType.equals("rn")||parameterType.equals("u")))
			//	throw new Exception("wrong parameterType:please enter u(for utility threhold) or rn(reduction in nodes)");

			threshold = Double.parseDouble(args[1]);
			//centralityMetric = args[3];
			
		/*	if(centralityMetric.equals("ec"))
				NODE_BC_BASENAME= directory + filename +"_eigenCentrality_WG.txt";
			else if(centralityMetric.equals("bc"))
				NODE_BC_BASENAME= directory + filename +"_nodebc.txt";
			else if(centralityMetric.equals("pc"))
				NODE_BC_BASENAME= directory + filename +"_pageRank_WG.txt";
			else if(centralityMetric.equals("dc"))
				NODE_BC_BASENAME= directory + filename +sym +"_degreec.txt";
			else
				throw new Exception("wrong cetnrality scores :please enter ec, bc, dc or pc");
	*/
			NODE_BC_BASENAME = args[3];
			TWO_HOP_G_MST = args[2];

			GRAPH_BASENAME=directory + filename + sym ;
   			//TWO_HOP_G_MST= directory + filename+sym+"-"+centralityMetric;
			G= ImmutableGraph.loadMapped(args[0]);
	                VC2 =G.numNodes()*1.0*(G.numNodes()-1)/2;
	       		readNodeIS();
			readTwoHopGraph();
			saveNodeDegree();
			uf=new UnionFind(G.numNodes());
			System.out.println("initialized union find");
			
			// extra code
			sum=0;
			for(int i=0;i<G.numNodes();i++){
				for(int j:G.successorArray(i)){
					if(i<j)
					//	sum += Math.pow(getNodeIS(i),2)+Math.pow(getNodeIS(j),2);
						sum += getNodeIS(i)+getNodeIS(j);
				}
			}
			System.out.println("sum "+sum);


			long startTime, endTime, time;
			long t1 = System.currentTimeMillis();
			int left,right,mid;
			double utility = 1;
			
			if(parameterType.equals("rn")){
				left = (int)(threshold * G.numNodes());
				utility=Summarize(left);
			}
			else{
				right=twoHopEdgeList.length;	
				left=0;
				mid=(left+right)/2;
				while(left<right){
					mid=(left+right)/2;
					utility=Summarize(mid);
					if(utility<threshold)
						right=mid-1;
					else
						left=mid+1;
				}
			}
			
			System.out.print("utility : "+utility); 
			System.out.print("middle : "+ left); 
			System.out.println("rn : " + left*1.0/G.numNodes());
			System.out.println("Time elapsed for summarization is: " + (System.currentTimeMillis() - t1)/1000);
			System.out.println("Saving into file: Be Patient Please:");
//			SaveinMem(left);

		}catch(Exception e){
			System.out.println(e);
		}
	}


	private static double Summarize(int index){
		uf.reset();
		int nseCount = 0 , seCount = 0;
		double sum = 0;
		int n = G.numNodes();
		for (int i = 0 ; i < G.numNodes() ; i++){
			for (int j : G.successorArray(i))
				if (i < j)
					sum += getNodeIS(i) + getNodeIS(j);
		}
		// for (int i = 0 ; i < G.numNodes() ; i++){

		// 	for (int j : G.successorArray(i))
		// 		sum += getNodeIS(i)/(n - G.outdegree(i)) + getNodeIS(j)/(n-G.outdegree(j));
		// // 		if (i < j)
		// // 			sum += getNodeIS(i)/G.outdegree(i) + getNodeIS(j)/G.outdegree(j);
		// // }
		// }
		System.out.println("The value of sp sum is: " + sum);
		for(int i=0;i<index;i++){
			int u=(int)twoHopEdgeList[i][0];
			int v=(int)twoHopEdgeList[i][1];
			uf.union(u,v);
		}
		uf.renameSuperNodes();
		int numSuperNodes=G.numNodes()-index;
		SuperNode[] superNode = uf.getSuperNodes();
		map = new TreeMap<Integer,Double>();
		int u,SuName,SvName,SuTempName,SvTempName,Auv;
		double utility=1;
		double value;
		double seCost,nseCost;

		// Adding by Mahdi 
		double[] sp_total = new double[n];
		for(SuTempName=0;SuTempName<numSuperNodes;SuTempName++){
			SuName=uf.getOriginalName(SuTempName);
			sum = 0;
			Node current=superNode[SuName].first;
		/*	while(current!=null){
				u = current.name;
				sum = sum + getNodeIS(u)/(n - G.outdegree(u));
				current = current.next;
			}
			// System.out.println("Sum is: " + sum);
			sp_total[SuName] = sum; */
		}

		HashMap<Integer,Double> sp_count = new HashMap<Integer,Double>();
		for(SuTempName=0;SuTempName<numSuperNodes;SuTempName++){
			SuName=uf.getOriginalName(SuTempName);
			Node current=superNode[SuName].first;
			while(current!=null){
				u=current.name;
				for(int v:G.successorArray(u)){
					// if(u==v)	continue;
					SvName = uf.find(v);
					if(map.containsKey(SvName)){
                        map.put(SvName,map.get(SvName)+(getNodeIS(u)/degree[u] + getNodeIS(v)/degree[v])+ 1);
                        // map.put(SvName,map.get(SvName)+(getNodeIS(u) + getNodeIS(v))+ 1);
                        // map.put(SvName,map.get(SvName)+(getNodeIS(u)/sum +getNodeIS(v)/sum)+ 1);
                       // map.put(SvName,map.get(SvName)+1.0/G.numArcs()+1);
                      // sp_count.put(SvName,sp_count.get(SvName) + getNodeIS(u)/(n - degree[u] - 1) + getNodeIS(v)/(n - degree[v] - 1));
                        }
                    else{
                    	map.put(SvName,getNodeIS(u)/degree[u] + getNodeIS(v)/degree[v]+ 1);
                    	// map.put(SvName,getNodeIS(u) + getNodeIS(v)+ 1);
                        // map.put(SvName,(getNodeIS(u)/sum + getNodeIS(v)/sum) + 1);
						// map.put(SvName,1.0/G.numArcs()+1);
					//	sp_count.put(SvName , getNodeIS(u)/(n - degree[u] - 1) + getNodeIS(v)/(n - degree[v] - 1));
                        }
				}
				current=current.next;
			}

			for(Map.Entry<Integer,Double> entry : map.entrySet()) {
				SvName = entry.getKey();
				value = entry.getValue();
				Auv=(int)value;
				nseCost=value-Auv;
				// System.out.println(" getSize(SuName) is: " + uf.getSize(SuName) + " getSize(SvName) is: " + uf.getSize(SvName) +  " Auv is: " + Auv + " VC2 is: " + VC2);
 				if(SuName==SvName){
					nseCost/=2;
					seCost=  Math.abs(((1.0*uf.getSize(SuName)*(uf.getSize(SuName)-1)/2) - Auv/2)/( VC2-G.numArcs()/2));
					// seCost = (uf.getSize(SvName) * sp_total[SuName] + uf.getSize(SuTempName) * sp_total[SvName] - sp_count.get(SvName));
					// System.out.println(" SeCost is: "+ seCost + " nseCost is: "+ nseCost);
					 if(seCost<=nseCost){
                	                        utility-=seCost;
                	                        seCount++;
		                         }else if (seCost>=nseCost){
                 	                       utility-=nseCost;
                 	                       nseCount++;
                        	         }

				}
				else{
					seCost=  Math.abs((1.0*uf.getSize(SuName)*uf.getSize(SvName)-Auv)/(VC2-G.numArcs()/2));
					// seCost = (uf.getSize(SvName) * sp_total[SuName] + uf.getSize(SuTempName) * sp_total[SvName] - sp_count.get(SvName)) ;
					if(seCost<nseCost){
						seCount++;
						utility-=seCost;
					}else{
						utility-=nseCost;
						nseCount++;
					}
				}
				// System.out.println(" SeCost is: " + seCost + " nseCost is: " + nseCost + " SuName is: " + SuName + " SvName is: " + SvName) ;
			}
			map.clear();
		}
		//ystem.out.println("Used Memory: "+ (instance.totalMemory() - instance.freeMemory()) / mb);
		System.out.println(index+"  "+utility);
		System.out.println(" seCount: " + seCount + " nseCount: " + nseCount);
		return utility;
	}
	// Saving in Memory
	private static void SaveinMem(int index ) throws Exception{	


	ps=new PrintStream(new File("output_RN/"  + filename + "_supernodes-"+parameterType+"-"+threshold));
	ps2 = new PrintStream(new File("output_RN/"  + filename + "_superneighbors-"+ parameterType+"-"+threshold));
	PrintStream ps2_t = new PrintStream(new File("output_RN/"  + filename + "_superneighbors-"+ parameterType+"-"+threshold + "-t"));	 
	ps3 = new PrintStream(new File("output_RN/"  + filename + "_result-"+parameterType+"-"+threshold));
	uf.reset();
	int n = G.numNodes();
	for(int i=0;i<index;i++){
		int u=(int)twoHopEdgeList[i][0];
		int v=(int)twoHopEdgeList[i][1];
		uf.union(u,v);
	}
	uf.renameSuperNodes();
	int numSuperNodes=G.numNodes()-index;
	SuperNode[] superNode = uf.getSuperNodes();
	map = new TreeMap<Integer,Double>();
	int u,SuName,SvName,SuTempName,SvTempName,Auv;
	double utility=1;
	double value;
	double sum = 0;
	double seCost,nseCost;
	double[] sp_total = new double[n];
	HashMap<Integer,Integer> newName = new HashMap<Integer,Integer>();
	int count = 0;
	for(SuTempName=0;SuTempName<numSuperNodes;SuTempName++){
		SuName=uf.getOriginalName(SuTempName);
		newName.put(SuName,count);
		count++;
		sum = 0;
		Node current=superNode[SuName].first;
		while(current!=null){
			u = current.name;
			sum = sum + getNodeIS(u)/(n - G.outdegree(u));
			current = current.next;
		}
		// System.out.println("Sum is: " + sum);
		sp_total[SuName] = sum;
	}
	HashMap<Integer,Double> sp_count = new HashMap<Integer,Double>();
	for(SuTempName=0;SuTempName<numSuperNodes;SuTempName++){
		SuName=uf.getOriginalName(SuTempName);
		Node current=superNode[SuName].first;
		ps.print(newName.get(SuName));
		while(current!=null){
			u=current.name;
			ps.print( "\t" + u);
			for(int v:G.successorArray(u)){
				// if(u==v)
				// 	continue;
				SvName = uf.find(v);
				if(map.containsKey(SvName)){
					map.put(SvName,map.get(SvName)+(getNodeIS(u)/G.outdegree(u) + getNodeIS(v)/G.outdegree(v))+1);
					sp_count.put(SvName,sp_count.get(SvName) + getNodeIS(u)/(n - degree[u] - 1) + getNodeIS(v)/(n - degree[v] - 1));

			//		map.put(SvName,1.0/G.numArcs()+1);
				}
				else{
					map.put(SvName,getNodeIS(u)/G.outdegree(u) + getNodeIS(v)/G.outdegree(v) + 1);
				//	map.put(SvName,1.0/G.numArcs()+1);
					sp_count.put(SvName , getNodeIS(u)/(n - degree[u] - 1) + getNodeIS(v)/(n - degree[v] - 1));	

				}
			}
			current=current.next;
		}
		ps.println();

		for(Map.Entry<Integer,Double> entry : map.entrySet()) {
			SvName = entry.getKey();
			value = entry.getValue();
			Auv=(int)value;
			nseCost=value-Auv;
				if(SuName==SvName){
				nseCost/=2;
				// seCost = (uf.getSize(SvName) * sp_total[SuName] + uf.getSize(SuTempName) * sp_total[SvName] - sp_count.get(SvName));
				seCost=  Math.abs(((1.0*uf.getSize(SuName)*(uf.getSize(SuName)-1)/2)-Auv/2)/(VC2-G.numArcs()/2));
				 if(seCost<nseCost){
	                        utility-=seCost;
	                        if (newName.get(SuName)<= newName.get(SvName))
	                        	ps2.println(newName.get(SuName) + "\t" + newName.get(SvName));
	                        else if (newName.get(SuName)>= newName.get(SvName))
	                        	ps2_t.println(newName.get(SuName) + "\t" + newName.get(SvName));
                 }else{
 	                       utility-=nseCost;
        	         }

			}
			else{
				seCost= (1.0*uf.getSize(SuName)*uf.getSize(SvName)-Auv)/(VC2-G.numArcs()/2);
				// seCost = (uf.getSize(SvName) * sp_total[SuName] + uf.getSize(SuTempName) * sp_total[SvName] - sp_count.get(SvName));
				if(seCost<nseCost){
					utility-=seCost;
					if (newName.get(SuName)<= newName.get(SvName))
						ps2.println(newName.get(SuName) + "\t" + newName.get(SvName));
					else if (newName.get(SuName)>= newName.get(SvName))
						ps2_t.println(newName.get(SuName) + "\t" + newName.get(SvName));

				}else{
					utility-=nseCost;
				}
			}
		}
		map.clear();
	}
	ps3.println("rn : " + index*1.0/G.numNodes());
	ps3.println("utility :" + utility);

	ps.flush();
	ps2.flush();
	ps2_t.flush();
	ps3.flush();
	ps.close();
	ps2.close();
	ps2_t.close();
	ps3.close();
}

}

