package graph_sum;
public class SuperNode{
	Node first;
	Node last;

	SuperNode(int i){
		Node node = new Node(i);
		this.first=node;
		this.last=node;
	}
	void merge(SuperNode other){
		this.last.next=other.first;
		this.last=other.last;
		other.first=null;
		other.last=null;
	}
	void print(){
		Node current=first;
		while(current!=null){
			System.out.print(current.name+"\t");
			current=current.next;
		}
		System.out.println();
	}
}

class Node{
	int name;
	Node next;
	Node(int name){
		this.name=name;
		this.next=null;
	}	
}