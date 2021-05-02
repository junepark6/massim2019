package teamms;
/* Author: Morgan Fine-Morris */

import agent.*;
import teamms.CardinalVector;

import java.io.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.lang.Math;
import java.util.Arrays;

/** Represents an agent or package location. 
    Has several utility functions for determining relationships between locations. */
public class Location{
    
    private int x, y;
    public static final int [] directions = {Direction.NORTH, Direction.EAST, 
                                             Direction.SOUTH, Direction.WEST};

    public Location(int x, int y){
        this.x = x;
        this.y = y;
    }

    /** make a copy of loc */
    public Location(Location loc){
        super(loc.getX(), loc.getY());
    }

    /** covert a len-2 array into a location */
    public Location(int [] loc){
        super(loc[0], loc[1]);
    }

    /** make a location (x,y) */
    public Location(int x, int y){
        super(x, y);
    }

    public int getX(){ return this.x; }
    public int getY(){ return this.y; }

    /** if o is a location, do they have the same x and y value? */
    @Override
    public boolean equals(Object o){
        if(!(o instanceof Location) && !(o instanceof Location)) return false;
        Location oo = (Location)o;
        return (getX() == oo.getX()) && (getY() == oo.getY());
    }

    /** is x==this.x, and is y==this.y? */
    public boolean equals(int x, int y){
        if(x == this.getX() && y == this.getY()) return true;
        return false;
    }
    
    /** hash based on the string */
    @Override
    public int hashCode(){
        int hash = this.toString().hashCode();
        return hash;
    }

    /** return string describing coords */
    public String toString(){
        return "(" + getX() + "," + getY() + ")";
    }

    /** calc euclidean dist between self and loc */
    public double euclidean(Location loc){
        double diffX = loc.getX()-getX();
        double diffY = loc.getY()-getY();
        double dist = Math.hypot(diffX, diffY);
        return dist;
    }

    /** calc manhattan distance between this and loc */
    public double manhattan(Location loc){
        double diffX = loc.getX()-getX();
        double diffY = loc.getY()-getY();
        return Math.abs(diffX) + Math.abs(diffY);
    }

    /** calc dist between self and loc */
    public double euclidean(Location loc){
        return euclidean(new Location(loc));
    }

    /** calc dist between self and loc */
    public double manhattan(Location loc){
        return manhattan(new Location(loc));
    }

    /** calc dist between self and loc */
    public double manhattan(int x, int y){
        return manhattan(new Location(x, y));
    }

    /** calc dist between self and loc */
    public double euclidean(int x, int y){
        return euclidean(new Location(x, y));
    }

    /* Find which direction loc is from this location. 
     * Returns an array with either the two directions, 
     * or one direction and a -1. */
    public int[] direction(Location loc){
        // calc distance between xy2 and points adjacent to xy1
        // to determine which direction of travel brings us closer to xy2
        double curr_dist = euclidean(loc);
        double [] dists = new double[4];
        int [] directionality = {-1, -1}; // init to invalid entries
        // find which direction brings us closer to loc 
        // (by calc'ing which of the 4 adjacent squares is closer to loc)
        // there should be at most 2 squares that do so
        int i = 0;
        for(int dir : directions){
            Location adjLoc = calcAdjacent(dir);
            dists[dir] = loc.euclidean(adjLoc);
            if (dists[dir] < curr_dist) directionality[i++] = dir;
        }
        return directionality;
    }

    /** is location loc in the direction dir from this location? */
    public boolean inDirection(Location loc, int dir){
        double curr_dist = euclidean(loc);
        Location adjLoc = calcAdjacent(dir);
        if( loc.euclidean(adjLoc) < curr_dist ){ return true; }
        return false;
    }

    /** is location loc in the both directions dir1 and dir2 from this location? */
    public boolean inDirection(Location loc, int dir1, int dir2){
        if( inDirection(loc, dir1) && inDirection(loc, dir2) ){ return true; }
        return false;
    }

    /** get the coordinates of position adjacent to self in direction dir. */
    public Location calcAdjacent(int dir){
        int newx = getX() + Direction.DELTA_X[dir];
        int newy = getY() + Direction.DELTA_Y[dir];
        return new Location(newx, newy);
    }

    /** is loc 1 unit away on a diagonal? */
    public boolean isDiagonallyAdjacent(Location loc){
        double dist = euclidean(loc);
        if(!isAdjacent(loc) && dist == 1.0) return true;
        return false;
    }

    /** is loc 1 unit away to the north, south, east, or west? */
    public boolean isAdjacent(Location loc){
        double dist = manhattan(loc);
        if (dist <= 1.0 && dist > .5) return true;
        return false;
    }

    /** calc (signed) x and y offset from loc */ 
    public double[] signed_offset(Location loc){
        double [] offset = new double[2];
        // int[] dirs = direction(loc);
        // x locations
        Location thislocX = new Location(getX(), 0);
        Location locX = new Location(loc.getX(), 0);
        offset[0] = thislocX.manhattan(locX);

        Location thislocY = new Location(0, getY());
        Location locY = new Location(0, loc.getY());
        offset[1] = thislocY.manhattan(locY);
        return offset;
    }

    /** calc (unsigned, i.e. abs value) x and y offset from loc */ 
    public double[] unsigned_offset(Location loc){
        double [] off = signed_offset(loc);
        off[0] = Math.abs(off[0]);
        off[1] = Math.abs(off[1]);
        return off;
    }

    /** return info about direction and distance */
    public CardinalVector [] get_offset(Location loc){
        CardinalVector [] vecs = new CardinalVector[2];

        int[] dirs = direction(loc); // 2 ints between -1 and 3 
        double[] off = unsigned_offset(loc);
        double xoffset = off[0];
        double yoffset = off[1];

        // if direction 0 is north or south, pair it with the y offset,
        // otherwise, pair it with the x offset
        if(Direction.NORTH == dirs[0] || Direction.SOUTH == dirs[0]){
            vecs[0] = new CardinalVector(dirs[0], yoffset);
        }else if(Direction.EAST == dirs[0] || Direction.WEST == dirs[0]){
            vecs[0] = new CardinalVector(dirs[0], xoffset);
        }
        if (dirs[1] == -1){  return vecs; }

        // if direction 1 is north or south, pair it with the y offset,
        // otherwise, pair it with the x offset
        if(Direction.NORTH == dirs[1] || Direction.SOUTH == dirs[1]){
            vecs[1] = new CardinalVector(dirs[1], yoffset);
        }else if(Direction.EAST == dirs[1] || Direction.WEST == dirs[1]){
            vecs[1] = new CardinalVector(dirs[1], xoffset);
        }
        return vecs;
    }

    /** is the specified direction valid? */
    public static boolean validDirection(int dir){ return dir >= 0 && dir <= 3;}

    /** return which of the cardinal directions is opposite cardinal direction dir */
    public static int opposite_direction(int dir){
        return (dir + 2) % 4;
    }

    /** return the 2 cardinal directions that are perpendicular to cardinal direction dir */
    public static int[] perpendicular_directions(int dir){
        int [] dirs = new int [2];
        // find one of the cardinal directions perpendicular to dir
        // then find the opposite for the second one
        dirs[0] = (dir + 1) % 4;
        dirs[1] = opposite_direction(dirs[0]);
        return dirs;
    }

    /** return a new instance of Location with x and y updated 
        according to distance and direction of v */
    public Location add(CardinalVector v){
        return add(v.getDirection(), (int) v.getDistance());
    }

    /** return a new instance of Location with x and y updated 
        according to distance and direction of v */
    public Location add(int direction, int distance){
        int x = getX() + Direction.DELTA_X[direction]*distance;
        int y = getY() + Direction.DELTA_Y[direction]*distance;
        return new Location(x,y);

    }
}