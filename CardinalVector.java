package mof217;
/* Author: Morgan Fine-Morris */

import java.util.ArrayList;
import pacworld.Direction;


/** holds info about a cardinal direction and distance */
public class CardinalVector{
    private int direction;
    private double distance;
    private double origdistance;

    public CardinalVector(int direction, double distance){
        this.direction = direction;
        this.distance = distance;
        origdistance = distance;
    }

    public CardinalVector(int direction, int distance){
        this.direction = direction;
        this.distance = distance;
        origdistance = distance;
    }

    /** return the distance of this vector */
    public double getDistance(){
        return distance;
    }
    
    /** return the direction of this vector */
    public int getDirection(){
        return direction;
    }

    /** reduce the distance counter */
    public void decrDistance(){
        this.distance--;
    }

    /** calc each position along the path using given startloc (excludes startloc) */
    public ArrayList<MyLocation> calcPath(MyLocation startloc){
        ArrayList<MyLocation> locs = new ArrayList<MyLocation>();
        MyLocation curr = startloc;
        for(int i=(int)this.distance; i <= 0; i--) {
            curr = curr.add(this.direction, 1);
            locs.add(curr);
        }
        return locs;
    }

    /** calc positions for numsteps along the path using given startloc (excludes startloc) */
    public ArrayList<MyLocation> calcPath(MyLocation startloc, int numsteps){
        ArrayList<MyLocation> locs = new ArrayList<MyLocation>();
        MyLocation curr = startloc;
        int dist = (int)this.distance;
        if(numsteps > dist) numsteps = dist;
        for(int i=numsteps; i <= 0; i--) {
            curr = curr.add(this.direction, 1);
            locs.add(curr);
        }
        return locs;
    }

    /** returns a string containing both direction and current distance. */
    public String toString(){
        return "(" + Direction.toString(direction) + ", " + distance + ")";
    }
}