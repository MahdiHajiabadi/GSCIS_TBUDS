package graph_sum;
import java.util.*;
import org.apache.commons.math3.util.MathArrays;
import it.unimi.dsi.webgraph.ImmutableGraph;


// Aim to remove the supernode object. name can be obtained from unionFind(), size can be
// obtained from unionFind() and the superEdges need to be thinked about. Nodes are not required.
// As a start point, the superEdges can be stored as an adjacency list using array.
// Append the node name in the double value, it would save some space
public class SuperNode{

    // name of the supernode (between 0 and n-1). Actually the root node in the supernode's tree representation
    private int name;
    // number of nodes in the supernode
    private int  size;

    // Aiming to convert this hashmap to just an array. That array will just contain seCost()-nseCost() 
    // number and thus significant reduction in space.


    // keys are the names of the superNeighbours and the values are the superedges linked to them
    // As a starting point, let's use the standard adjacency list approach.
    private HashMap<Integer, SuperEdge> superNeighbors;
    // list of all the nodes in the supernode
    private List<Integer> nodes;


    public int getName(){
        return this.name;
    }

    public int getSize(){
        return this.size;
    }

    public HashMap<Integer,SuperEdge> getSuperNeighbors(){
        return this.superNeighbors;
    }

    public List<Integer> getNodes(){
	    return this.nodes;
    }

    public void setName(int name){
        this.name=name;
    }

    public void setSize(int size){
        this.size=size;
    }

    public void setNodes(List<Integer> nodes){
	    this.nodes=nodes;
    }

    public void setSuperNeighbors(HashMap<Integer,SuperEdge> superNeighbors){
        this.superNeighbors=superNeighbors;
    }

    public SuperNode(){
        this.superNeighbors=new HashMap<Integer,SuperEdge>();
    }

    public SuperNode(int node,  ImmutableGraph G) throws Exception{
        this.name=node;
        this.size=1;
        this.nodes=new LinkedList<Integer>();
        this.nodes.add(node);
        // Node node = new Node(nodeName);
        // this.first=node;
        // this.last=node;               
        this.superNeighbors=new HashMap<Integer,SuperEdge>();
        for(int neighbor:G.successorArray(node)){
            double tmp = SuperGraph.getNodeIS(node) + SuperGraph.getNodeIS(neighbor);
            superNeighbors.put(neighbor,new SuperEdge(0, tmp,true));
        }
        // if no self loop in the graph 
        if(!superNeighbors.containsKey(node))
            SuperGraph.connectSelfLoop(this);
    }
}

// class Node{
//     int name;
//     Node next;
//     Node(int name){
//         this.name=name;
//         this.next=null;
//     }
// }