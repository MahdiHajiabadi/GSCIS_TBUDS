package graph_sum;

// purpose is to reduce the superEdge just to a single number
public class SuperEdge{
    
    private double seCost;
    private double nseCost;
    private boolean permanent;

    public SuperEdge(double seCost, double nseCost, boolean permanent){
        this.seCost=seCost;
        this.nseCost=nseCost;
        this.permanent=permanent;
    }

    public double getSeCost(){
        return this.seCost;
    }

    public double getNseCost(){
        return this.nseCost;
    }

    public boolean isPermanent(){
        return this.permanent;
    }
}