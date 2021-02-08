package graph_sum;

public class UnionFind {

	// id[i] contains the parent of the ith node
	private int[] id;
	// size[i] contains the size of the subtree rooted at ith node
	private int[] size;

	// newName given to each supernode
	private int[] tempName;

	private int[] originalName;

	private int numSuperNodes;


	private SuperNode[] superNode;
	// nodes are numbered from 0 to n-1
	// each supernode is a tree and the root of that tree is the name of the
	// supernode

	public SuperNode[] getSuperNodes(){
		return superNode;
	}

	public UnionFind(int n) {
		id = new int[n];
		size = new int[n];
		tempName=new int[n];
		originalName=new int[n];
		superNode=new SuperNode[n];
		for (int i = 0; i < n; i++) {
			id[i] = i;
			size[i] = 1;
			superNode[i] = new SuperNode(i);
		}
	}

	//find(i) will return the supernode containing node i
	public int find(int i) {
		while (i != id[i]) {
			// path compression
			id[i] = id[id[i]];
			i = id[i];
		}
		return i;
	}

	public void reset(){
		for (int i = 0; i < id.length; i++) {
			id[i] = i;
			size[i] = 1;
			superNode[i] = new SuperNode(i);
		}
	}

	public void renameSuperNodes(){
		int count=0;
		for(int i=0;i<id.length;i++){
			if(id[i]==i){
				//i is the name of the supernode
				tempName[i]=count;
				originalName[count]=i;
				count++;
			}
		}
	}

	public int getOriginalName(int i){
		return originalName[i];
	}

	public int getTempName(int i){
		return tempName[i];
	}
	
	public int getSize(int i){
		return size[i];
	}


	public boolean connected(int u,int v){
		if(find(u)==find(v))
			return true;
		return false;
	}

	// union (p,q) would merge the supernode containing node p and the supernode
	// containing node q
	public void union(int p, int q) {
		// i is the supernode containing node p
		int i = find(p);
		// j is the supernode containing node q
		int j = find(q);

		// if p and q are in the same supernode then don't do anything
		if (i == j)
			return;

		numSuperNodes--;

		if (size[i] < size[j]) {
			id[i] = j;
			size[j] += size[i];
			superNode[j].merge(superNode[i]);
			superNode[i]=null;
		} else {
			id[j] = i;
			size[i] += size[j];
			superNode[i].merge(superNode[j]);
			superNode[j]=null;
		}
	}
}
