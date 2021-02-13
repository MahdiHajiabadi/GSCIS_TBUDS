# GSCIS_TBUDS

Requirements:
webgraph-3.4.3
dsiutils-2.2.3
fastutil-6.6.3
jsap-2.1
sux4j-3.2.2
trove-3.1a1-src
javatuples-1.2


There are four implementaion in this repository

# G-SCIS (Lossless Grpah Summarization)

This is one of the lossless proposed method discussed in the paper. 
## How to run it?
1. Navigate to the directory Java files exist.
2. Create a new directory named "bin" (mkdir bin)
3. Create a new directory named "output" (mkdir output)
3. Compile GSCIS.java and Cluster.java (the required jar files are in the lib directory)

  javac -cp "lib/*" -d bin/ GSCIS.java Cluster.java
  
4. Run the GSCIS.class like the following:

java -cp "lib/*":"bin/" graph_sum.GSCIS input-graph

java -cp "lib/*":"bin/" graph_sum.GSCIS cnr-2000-sym-noself

3 files are saved in output directory: 

1. cnr-2000-sym-noself-summary.output (showing the reduction in nodes and running time)
2. cnr-2000-sym-noself-summary.supernodes (representing the set of nodes per each supernode)

# Remarks
All the input graphs should be in the webgraph format. In order to convert the edgelist into the webgraph format you should do the following steps:

3. cnr-2000-sym-noself-summary.superedges (showing the superedges among supernodes) 

# T-BUDS (A Lossy Grpah Summarization)

T-BUDS is a utility-based lossy graph summarization. The utility is computed based on the capability of reconstructed graph in not missing actual edges as well as not introducing any spurious edges. Each actual edge <u,v> has a weight which is defined based on the centrality score of its endpoint nodes and the weight of each spurious edge is a constant small number. We used the PageRank centrality score in assining weight to edges.   

The input graph can be either the original graph or the G-SCIS lossless summary graph. If your input graph is the G-SCIS lossless please make sure that you convert the G-SCIS summary into the webgraph format. The procedures are put in Remakrs section.

## How to run it?
1. Navigate to the directory Java files exist.
2. Create a new directory named "bin" (mkdir bin)
3. Create a new directory named "output" (mkdir output)
4. Compile TBUDS.java and SuperNode.java and UnionFind.java (the required jar files are in the lib directory)

javac -cp "lib/*" -d bin/ UnionFind.java TBUDS.java SuperNode.java

5. Run the TBUDS.class like the following:

java -cp "lib/*":"bin/" graph_sum.TBUDS input_graph utility_threshold twohopMST_graph node_centrality

utility_threshold: a float number between [0,1]

twohopMST_graph: the maximum spanning tree of V nodes and V-1 edges which each edge is connected nodes with distance 2

node_centrality: the centrality score of each node (the first column is the node id and the second column is the centrality score of node. a txt file with tab delimeter)





# SWeG (Lossless/Lossy Graph Summarization)

SWeG is one of the state-of-the-art algorithms based on correction-set framework. You can download the paper [here](https://dl.acm.org/doi/10.1145/3308558.3313402)
## How to run it? 
1. Navigate to the directory Java files exist.
2. Create a new directory named "bin" (mkdir bin)
3. Create a new directory named "output" (mkdir output)
4. Compile SWeG.java in the following way: (the required jar files are in the lib directory)

javac -cp "lib/*" -d bin/ SWeG.java 

5. Run SWeG.class with the following input parameters:

java -cp "lib/*":"bin/" graph_sum.SWeG input_graph number_of_iteration print_iteration_offset dropping_ratio





# UDS (Utility-Driven Graph Summarization)

The state-of-the-art (lossless/lossy) utility-based graph summarization. You can read the paper [here](http://www.vldb.org/pvldb/vol12/p335-kumar.pdf) for more information. 
## How to run it?
1. Navigate to the directory Java files exist.
2. Create a new directory named "bin" (mkdir bin)
3. Create a new directory named "output" (mkdir output)
4. Compile UDS.java and SuperNode.java SuperGraph.java SuperEdge.java and UnionFind.java (the required jar files are in the lib directory)

javac -cp "lib/*" -d bin/ UnionFind.java UDS.java Super\*.java

5. Run the UDS.class like the following:

java -cp "lib/*":"bin/" graph_sum.UDS input_graph twohop_graph utility_threshold node_centrality


# Remarks 
All the input graphs should be in the [webgraph](https://www.ics.uci.edu/~djp3/classes/2008_01_01_INF141/Materials/p595-boldi.pdf) format because of its wonderful performance in compressing graphs. Available datasets in this format can be found in: <http://law.di.unimi.it/datasets.php>


### *Converting Edgelist Format to WebGraph Format*

This section is for the case when your graph is given a text file of edges (known as edgelist).

It is very easy to convert an edgelist file into WebGraph format. I am making the folloiwng assumptions:


Now, to convert the edgelist (TAB seprated) file to WebGraph format execute the following steps:

Sort the file, then remove any duplicate edges:

```
sort -nk 1 edgelistfile | uniq > edgelistsortedfile
```

(If you are on Windows, download *sort.exe* and *uniq.exe* from <http://gnuwin32.sourceforge.net/packages/coreutils.htm>)

Run:

```
java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -g ArcListASCIIGraph edgelistsortedfile  dummyBasename
```

For example:

```
java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -g ArcListASCIIGraph cnr-2000.txt  cnr-2000
```


