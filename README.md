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
2. Create a new "bin" directory
3. Compile GSCIS.java and Cluster.java (the required jar files are in the lib directory)
  javac -cp "lib/*" -d bin/ GSCIS.java Cluster.java
4. Run the GSCIS.class like the following:
  java -cp "lib/*":"bin/" graph_sum.GSCIS cnr-2000-sym-noself



  





# T-BUDS (A Lossy Grpah Summarization)

# SWeG (Lossless/Lossy Graph Summarization)

# UDS (Utility-Driven Graph Summarization)
