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

public class SuperGraph{
    private static double[] nodeIS;
    private static int[] degree;
    private static int[] accumulatedDeg;
    private static SuperNode[] superNodeMap;
    private static ImmutableGraph G;
    private static ImmutableGraph twoHopG;
    private static double VC2;
    private static UnionFind uf;

    public static void readNodeIS(String basename) throws Exception{

        nodeIS = new double[G.numNodes()];
        Scanner scanner = new Scanner(new File(basename));
        while (scanner.hasNextLine()) {
              String[] pair = scanner.nextLine().split("\t");
              int node = Integer.parseInt(pair[0]);
              double value = Double.parseDouble(pair[1]);
              nodeIS[node] = value;
        }
        scanner.close();
        normalizeNodeIS();
        saveDegree();
    }

    private static void saveDegree(){
        degree=new int[G.numNodes()];
        for(int i=0;i<G.numNodes();i++){
            for(int j:G.successorArray(i)){
                degree[i]++;
                degree[j]++;
            }
        }
    }

    private static void normalizeNodeIS(){
        double sumNodeIS=0;
        for(double is:nodeIS){
            sumNodeIS+=is;
        }
        for(int i=0;i<nodeIS.length;i++){
            nodeIS[i]/=sumNodeIS;
        }
    }

    public static double getNodeIS(int node) throws Exception{
        return nodeIS[node];
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


    public static double seCost(SuperNode Su, SuperNode Sv){

        if(Su.getSuperNeighbors().containsKey(Sv.getName())){
            SuperEdge superEdge = Su.getSuperNeighbors().get(Sv.getName());
            return superEdge.getSeCost();
        }
        if(Su.getName()==Sv.getName()){
            // return (uf.getSize(Su.getName())*uf.getSize(Sv.getName())-1)
            return (Su.getSize()*(Sv.getSize()-1)/2)/(VC2-G.numArcs()/2);
        }
        return (Su.getSize()*Sv.getSize()*1.0)/(VC2-G.numArcs()/2);
    }

    public static double nseCost(SuperNode Su, SuperNode Sv){
    
        if(Su.getSuperNeighbors().containsKey(Sv.getName())){
            SuperEdge superEdge = Su.getSuperNeighbors().get(Sv.getName());
            // if(superEdge.isPermanent()){
                // return superEdge.getNseCost()-superEdge.getSeCost();
            // }
            return superEdge.getNseCost();
        }
        return 0;
    }

    // will help in the initialization of the super nodes. Adding a loop of length 0
    public static void connectSelfLoop(SuperNode Su){
        Su.getSuperNeighbors().put(Su.getName(), new SuperEdge(0,0,false));
    }

    private static double connectSuperEdge(SuperNode Su, SuperNode Sv, SuperNode Suv, SuperNode Sw){
        
        double seCost,nseCost;
        double penalty = 0;
        //for connecting a self loop

        if(Sw.getName()==Suv.getName()){
            seCost=seCost(Su, Sv) + seCost(Su,Su) + seCost(Sv,Sv);
            nseCost=nseCost(Su, Sv) + nseCost(Su,Su) + nseCost(Sv,Sv);
            if(seCost<=nseCost){
                if(Su.getSuperNeighbors().containsKey(Su.getName())){
                    SuperEdge superEdge = Su.getSuperNeighbors().get(Su.getName());
                    if(superEdge.isPermanent()){
                        // do nothing
                    }
                    else{
                        penalty+=seCost(Su,Su)-nseCost(Su,Su);
                    }
                }
                else{
                    penalty+=seCost(Su,Su);
                }


                if(Sv.getSuperNeighbors().containsKey(Sv.getName())){
                    SuperEdge superEdge = Sv.getSuperNeighbors().get(Sv.getName());
                    if(superEdge.isPermanent()){
                        // do nothing
                    }
                    else{
                        penalty+=seCost(Sv,Sv)-nseCost(Sv,Sv);
                    }
                }
                else{
                    penalty+=seCost(Sv,Sv);
                }


                if(Su.getSuperNeighbors().containsKey(Sv.getName())){
                    SuperEdge superEdge = Su.getSuperNeighbors().get(Sv.getName());
                    if(superEdge.isPermanent()){
                    }
                    else{
                        penalty+=seCost(Su,Sv)-nseCost(Su,Sv);
                    }
                }
                else{
                    penalty+=seCost(Su,Sv);
                }
            }

            else{
                if(Su.getSuperNeighbors().containsKey(Su.getName())){
                    SuperEdge superEdge = Su.getSuperNeighbors().get(Su.getName());
                    // if superedge between Su and Sw
                    if(superEdge.isPermanent()){
                        penalty+=nseCost(Su,Su)-seCost(Su,Su);
                    }
                    else{
                        // do nothing
                    }
                }
                else{
                    // do nothing
                }

                if(Sv.getSuperNeighbors().containsKey(Sv.getName())){
                    SuperEdge superEdge = Sv.getSuperNeighbors().get(Sv.getName());
                    // if superedge between Su and Sw
                    if(superEdge.isPermanent()){
                        penalty+=nseCost(Sv,Sv)-seCost(Sv,Sv);
                    }
                    else{
                        // do nothing
                    }
                }
                else{
                    // do nothing
                }

                if(Su.getSuperNeighbors().containsKey(Sv.getName())){
                    SuperEdge superEdge = Su.getSuperNeighbors().get(Sv.getName());
                    // if superedge between Su and Sw
                    if(superEdge.isPermanent()){
                        penalty+=nseCost(Su,Sv)-seCost(Su,Sv);
                    }
                    else{
                        // do nothing
                    }
                }
                else{
                    // do nothing
                }
            }
        }

        // for connecting two separate supernodes
        else{
            seCost = seCost(Su, Sw) + seCost(Sv, Sw);
            nseCost = nseCost(Su, Sw) + nseCost(Sv, Sw);

            // System.out.println("seCost "+seCost+" nseCost "+nseCost);
            // superEdge between Suv and Sw
            if(seCost<=nseCost){
                if(Su.getSuperNeighbors().containsKey(Sw.getName())){
                    SuperEdge superEdge = Su.getSuperNeighbors().get(Sw.getName());
                    // is superedge between Su and Sw
                    if(superEdge.getSeCost()<superEdge.getNseCost()){
                        // do nothing
                    
                    }
                    else{
                        penalty += seCost(Su,Sw)-nseCost(Su,Sw);
                    }
                }
                else{
                    penalty += seCost(Su,Sw);
                }

                if(Sv.getSuperNeighbors().containsKey(Sw.getName())){
                    SuperEdge superEdge = Sv.getSuperNeighbors().get(Sw.getName());
                    // is superedge between Su and Sw
                    if(superEdge.isPermanent()){
                        // do nothing
                    }
                    else{
                        penalty += seCost(Sv,Sw)-nseCost(Sv,Sw);
                    }
                }
                else{
                    penalty += seCost(Sv,Sw);
                }
            }



            // no superEdge between Suv and Sw
            else{
                if(Su.getSuperNeighbors().containsKey(Sw.getName())){
                    SuperEdge superEdge = Su.getSuperNeighbors().get(Sw.getName());
                    // if superedge between Su and Sw
                    if(superEdge.isPermanent()){
                        penalty+=nseCost(Su,Sw)-seCost(Su,Sw);
                    }
                    else{
                        // do nothing
                    }
                }
                else{
                    // do nothing
                }

                if(Sv.getSuperNeighbors().containsKey(Sw.getName())){
                    SuperEdge superEdge = Sv.getSuperNeighbors().get(Sw.getName());
                    // if superedge between Su and Sw
                    if(superEdge.isPermanent()){
                        penalty+=nseCost(Sv,Sw)-seCost(Sv,Sw);
                    }
                    else{
                        // do nothing
                    }
                }
                else{
                    // do nothing
                }
            }

            // removing Su and Sv from Sw's neighbor list and adding Suv 
            Sw.getSuperNeighbors().remove(Su.getName());
            Sw.getSuperNeighbors().remove(Sv.getName());  

        }
        
        boolean isPermanent = (seCost<=nseCost)?true:false;
        SuperEdge SwSuv = new SuperEdge(seCost,nseCost,isPermanent);
        
        if(Sw.getName()!=Suv.getName()){
            Sw.getSuperNeighbors().put(Suv.getName(),SwSuv);
        }
        // a node can be its own superNeighbor (if there is a loop)
        Suv.getSuperNeighbors().put(Sw.getName(), SwSuv);

        
        return penalty;
    }

    // code to generate 2-hop graph, nodeIS and edgeIS shsould be run separately
    public static SuperNode[] UDSummarizer(String base1, String base2, double threhold,String base3) throws Exception{

       G = ImmutableGraph.loadMapped(base1);
       twoHopG = ImmutableGraph.loadMapped(base2);
        VC2 =G.numNodes()*1.0*(G.numNodes()-1)/2;

        double utility = 1;
        double mergeThreshold =threhold;

        // no need of superNodeMap
        superNodeMap=new SuperNode[G.numNodes()];
        // need to initialize differently        
        for (int i = 0 ; i < G.numNodes(); i++){
            superNodeMap[i]=new SuperNode(i,G);
        }
        readNodeIS(base3);

//        System.out.println("Supergraph initialized");
        PrintStream ps3 = new PrintStream(new File("output/utility-drop"));

        int count=0;
        double[][] twoHopEdgeList= new double[(int)twoHopG.numArcs()][3];

        for (int i = 0 ; i < twoHopG.numNodes(); i++){
            int[] neighbors = twoHopG.successorArray(i);
            for (int j = 0 ; j < neighbors.length ; j++){
                twoHopEdgeList[count][0] = i;
                twoHopEdgeList[count][1]= neighbors[j];
                twoHopEdgeList[count++][2] = getNodeIS(i)+ getNodeIS(neighbors[j]);       
            }
        }
        
        sortByColumn(twoHopEdgeList, 2);
    
        System.out.println("Starting summarization");

        count=0;
        uf = new UnionFind(G.numNodes());
        int numMerges=0;

        while(utility>mergeThreshold && count<twoHopEdgeList.length){
        // while(count<=3){ 	    
            //System.out.println(count);

            int u=(int)twoHopEdgeList[count][0];
            int v=(int)twoHopEdgeList[count++][1];
            // System.out.println("u : "+u+"   v:  "+v);
            // System.out.println(u+"\t"+v+"\t"+twoHopEdgeList[count-1][2]);


            // code for merging two supernodes 
            int SuName=uf.find(u);
            int SvName=uf.find(v);
            if(SuName==SvName){
                continue;
            }
            SuperNode Su = superNodeMap[SuName];
            SuperNode Sv = superNodeMap[SvName];


            uf.union(u, v); numMerges++;
            SuperNode Suv = new SuperNode();
            int SuvName=uf.find(v);
            Suv.setName(SuvName);
            Suv.setSize(Su.getSize()+Sv.getSize());

            // Suv.nodes = Su.nodes union Sv.nodes 
            List<Integer> nodes = new LinkedList<Integer>();
            nodes.addAll(Su.getNodes());
            nodes.addAll(Sv.getNodes());
            Suv.setNodes(nodes);

            superNodeMap[SuName]=null;
            superNodeMap[SvName]=null;
            superNodeMap[SuvName]=Suv;

            // getting the names of the new neighbors for Suv
            Set<Integer> superNeighborNames = new HashSet<Integer>();
            superNeighborNames.addAll(Su.getSuperNeighbors().keySet());
            superNeighborNames.addAll(Sv.getSuperNeighbors().keySet());
            superNeighborNames.remove(SuName);
            superNeighborNames.remove(SvName);

            // creating superedges from Suv to potential neighbors
            double penalty;
            for(int SwName:superNeighborNames){       
                SuperNode Sw=superNodeMap[uf.find(SwName)];
                penalty = connectSuperEdge(Su, Sv, Suv, Sw);
                // System.out.println(SuvName+"\t"+SwName+"\t"+penalty+"\t other");
                utility -= penalty;
            }
            penalty= connectSuperEdge(Su, Sv, Suv, Suv);
            // System.out.println(SuvName+"\t"+SuvName+"\t"+penalty+"\t self");

            // ps3.println(count+"\t"+utility);
            utility -= penalty;
            // System.out.println(utility);
//            System.out.println("\n"+count+"\t"+utility+"\n");
        }
        // System.out.println(count);
      //  Util.printSuperGraph(superNodeMap,count);
        return superNodeMap;
    }
}