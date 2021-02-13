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
3. cnr-2000-sym-noself-summary.superedges (showing the superedges among supernodes) 

# T-BUDS (A Lossy Grpah Summarization)

T-BUDS is a utility-based lossy graph summarization. The utility is computed based on the capability of reconstructed graph in not missing actual edges as well as not introducing any spurious edges. Each actual edge <u,v> has a weight which is defined based on the centrality score of its endpoint nodes and the weight of each spurious edge is a constant small number. We used the PageRank centrality score in assining weight to edges.   

The input graph can be either the original graph or the G-SCIS lossless summary graph. If your input graph is the G-SCIS lossless please make sure that you convert the G-SCIS summary into the webgraph format. The procedures are put in Remakrs section. 


# SWeG (Lossless/Lossy Graph Summarization)

# UDS (Utility-Driven Graph Summarization)
