package graph_sum;
import it.unimi.dsi.webgraph.ImmutableGraph;
import java.lang.Exception;
import java.io.File;
import java.util.*;
import java.io.PrintStream;

public class UDS{
    public static void main(String[] args){

        try{
            String basename = args[0];
            String basename_2hop = args[1];
            double  threshold = Double.parseDouble(args[2]);
            String bc_basename = args[3];

        	long startTime = System.nanoTime();
        	SuperNode[] superNodeMap = SuperGraph.UDSummarizer(basename,basename_2hop,threshold,bc_basename);
			long endTime   = System.nanoTime();
			long totalTime = endTime - startTime;
			System.out.println("time : "+ totalTime/1000000000.0 +" s" );

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
