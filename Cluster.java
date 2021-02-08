package graph_sum;
import java.util.*;
public class Cluster{
	private LinkedList<Integer> nodes;

	public Cluster(int node){
		nodes = new LinkedList<Integer>();
		nodes.add(node);
	}

	public Cluster(){
		nodes = new LinkedList<Integer>();
	}

	public void add(int node){
		nodes.add(node);
	}

	public void print(){
		System.out.print(" { ");
		for(int i=0;i<nodes.size()-1;i++){
			System.out.print(nodes.get(i)+" , ");
		}
		System.out.print(nodes.get(nodes.size()-1));
		System.out.print(" } ");
	}

	public int size(){
		return nodes.size();
	}

	public int getFirst(){
		return this.nodes.get(0);
	}

	public int getSecond(){
		return this.nodes.get(1);
	}

	public LinkedList<Integer> getNodes(){
		return this.nodes;
	}

}