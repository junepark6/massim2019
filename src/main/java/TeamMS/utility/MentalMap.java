package TeamMS.utility;
/*
Author: Morgan Fine-Morris
*/

import java.util.ArrayList;
import java.util.List;

import java.lang.Math;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collection;
import java.lang.StringBuilder;

import TeamMS.utility.PathFinding;

/** 
* May eventually be used to represent entities in the MentalMap
*/
class MapElement{
    private int idnum;
    private String type;
    private int timeLastSeen;

    public MapElement(String elementtype, int elementid){
        idnum = elementid;
        type = elementtype;
    }

    public void setLastSighting(int time){
        timeLastSeen = time;
    }
    public int getLastSighting(){
        return timeLastSeen;
    }
}


/** 
* Maintains a map of the space, considers the origin to be agents initial position
* Note that this requires the map to be 2x as large as the true map in both directions,
* So that even if agent starts at furthest corner or edge of the map, there is still enough
* space to either side to fill in the entire map.
*/
public class MentalMap<T>{

    public static final int CW = 0; // RIGHT
    public static final int CCW = 1; // LEFT

	public static final int[] L_ROTATION = new int[4];
	public static final int[] R_ROTATION = new int[4];

	static {
		L_ROTATION[Direction.NORTH] = Direction.WEST;
		L_ROTATION[Direction.WEST] = Direction.SOUTH;
		L_ROTATION[Direction.SOUTH] = Direction.EAST;
		L_ROTATION[Direction.EAST] = Direction.NORTH;

		R_ROTATION[Direction.NORTH] = Direction.EAST;
		R_ROTATION[Direction.EAST] = Direction.SOUTH;
		R_ROTATION[Direction.SOUTH] = Direction.WEST;
		R_ROTATION[Direction.WEST] = Direction.NORTH;
	}

    // public static final String UNKNOWN = "0";
    // public static final String WALL = "W";
    // public static final String EMPTY = " ";
    // public static final String BLOCK = "B"; // a blocks is a string B<N> where <N> is an int indicating type
    // public static final String DISPENSER = "D"; // D<N> indicates dispenser of blocks of type <N>
    
    // Should we use prefix "A" to indicate any agent, or use 
    // different prefixes for agents of unknown team, agents on our team, and agent on the opposite team?
    // For now, let "A" indicate agent of arbitrary team
    // public static final String AGENT = "A"; 

    public static final int [] directions = {Direction.NORTH, 
                                            Direction.EAST, 
                                            Direction.SOUTH, 
                                            Direction.WEST};

    // private Location mycoords; // agent location in xy
    // private Location myinds; // agent location in array indicies
    // private Location origin; // the pair of indexes 

    protected List<ArrayList<T>> map;
    private ArrayList<Location> frontier;
    private ArrayList<Location> visited;
    private int last_frontier_update; // counts since last time the frontier was updated
    private int element_counter;
    private T initial_symbol;
    private ArrayList<T> special_features; // block generators, goal zones, etc


    public MentalMap(int idim, int jdim, T initial_symbol){
        // only init map if dimensions are valid (non-zero)
        System.out.println(idim +" "+ jdim);

        map = null;
        if(idim!=0 && jdim!=0){
            map = new ArrayList<ArrayList<T>>(idim);
            for(int i=0; i<idim; i++){
                ArrayList<T> jth = new ArrayList<T>(jdim);
                for(int j=0; j<jdim; j++){
                    jth.add(initial_symbol);
                }
                map.add(jth);
            }
        }
        frontier = new ArrayList<Location>();
        last_frontier_update = 0;
        element_counter = 0;
        this.initial_symbol = initial_symbol;
    }

    public MentalMap(int dim, T initial_symbol){
        this(dim, dim, initial_symbol);
    }

    public MentalMap(T initial_symbol){
        // init to twice the size of the expected world size
        // (twice the size bc we don't know where the agent starts on the map
        // so there has to be room to represent the entire map above, below, 
        // or to either side of the agents initial position)
        this(80, 80, initial_symbol);
    }

    /** use set of features to make a new map */
    public MentalMap(HashMap<Location, T> features, Location minloc, Location maxloc, T initial_symbol){
        
        // init the map using initial_symbol
        this(maxloc.X()-minloc.X(), maxloc.Y()-minloc.Y(), initial_symbol);
        System.out.println(this.getIDim()+" "+this.getJDim());

        // add the features
        for(Map.Entry<Location, T> entry : features.entrySet()){
            T elem = entry.getValue();
            Location loc = entry.getKey();
            set_location(loc, elem);   
        }
    }

    /** Use map to make a new MentalMap */
    public MentalMap(ArrayList<ArrayList<T>> map, T initial_symbol){
        this(0, 0, initial_symbol); // this will init map to null
        this.map = map;
    }

    /** make copy of Map, replacing entries according to mapping */
    public <E> MentalMap<E> replace(HashMap<T, E> mapping){
        int idim = getIDim();
        int jdim = getJDim();
        ArrayList<ArrayList<E>> newmap = new ArrayList<ArrayList<E>>(idim);
        for(int i=0; i<idim; i++){
            ArrayList<E> jth = new ArrayList<E>(jdim);
            for(int j=0; j<jdim; j++){
                T entry = get(i,j);
                E newentry = mapping.get(entry);
                // if(mapping.contains(entry)) mapping.get()
                jth.add(newentry);
            }
            newmap.add(jth);
        }
        return new MentalMap<E>(newmap, mapping.get(this.initial_symbol));
    }

    /** return the element at indexes (i,j) */
    public T get(int i, int j){
        return map.get(i).get(j);
    }

    /** return the element at indexes (i,j) */
    public T get_mark(int i, int j){
        return get(i, j);
    }

    /** get the corners of the square bounding the locations in locs */
    public Location [] getMinAndMax(Collection<Location> locs){
        int minX, maxX, minY, maxY;
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
        for(Location loc : locs){
            int x = loc.X();
            int y = loc.Y();
            if(x < minX) minX = x;
            if(y < minY) minY = y;
            if(x > maxX) maxX = x;
            if(y > maxY) maxY = y;
        } 
        return new Location[]{ new Location(minX, minY), new Location(maxX, maxY) };
    }

    /** provide corner x,y coordinates */
    public MentalMap<T> submap(Location lowerleftCorner, Location upperrightCorner){
        lowerleftCorner = coords_to_indexes(lowerleftCorner);
        upperrightCorner = coords_to_indexes(upperrightCorner);
        int mini = lowerleftCorner.X();
        int minj = lowerleftCorner.Y();
        int maxi = upperrightCorner.X();
        int maxj = upperrightCorner.Y();
        ArrayList<ArrayList<T>> newmap = new ArrayList<ArrayList<T>>(maxi-mini);
        for(ArrayList<T> line : map.subList(mini, maxi+1)){
            newmap.add(new ArrayList<T>(line.subList(minj, maxj+1)));
        }
        return new MentalMap<T>(newmap, this.initial_symbol);
    }

    public MentalMap<T> submap(Location xy, int radius){
        Location minloc = xy.add(Direction.WEST, radius).add(Direction.SOUTH, radius);
        Location maxloc = xy.add(Direction.NORTH, radius).add(Direction.EAST, radius);
        HashMap<Location, T> features = getSurroundingFeatures(xy, radius);
        return new MentalMap<T>(features, minloc, maxloc, this.initial_symbol);
    }

    public int getIDim(){
        return map.size();
    }

    public int getJDim(){
        if(map.size() < 1) return -1;
        return map.get(0).size();
    }

    public int [] getBounds(){
        int [] b = new int [2];
        b[0] = getIDim();
        b[1] = getJDim();
        return b;
    }

    /** for a given location and radius, return info on distance to each feature within radius  */
    public HashMap<Location, T> getSurroundingFeatures(Location xy, int radius){
        Location ij = coords_to_indexes(xy);
        int i = ij.X();
        int j = ij.Y();

        // for i-radius to i+radius and j-radius to j+radius,
        // look for features of map, and add them to features w/
        // new location giving their offset from xy.
        HashMap<Location, T> features = new HashMap<>();
        for(int ii=i-radius; ii < i+radius; ii++){
            for(int jj=j-radius; jj < j+radius; jj++){
                T m = get_mark(ii, jj);
                Location mloc = new Location(ii, jj);
                features.put(indexes_to_coords(mloc), m);
            }
        }
        return features;
    }

    /**
     * do features surrounding myloc possibly overlap with features in theirloc?
     * @param myloc an x,y coordinate
     * @param theirloc an x,y coordinate
     * @param theirfeatures a set of features centered at their loc
     */
    public void align(Location myloc, Location theirloc, HashMap<Location, T> theirfeatures){

        Location [] minmaxlocs = getMinAndMax(theirfeatures.keySet());
        Location minloc = minmaxlocs[0];
        Location maxloc = minmaxlocs[1];
        int radius = (int)(maxloc.X() - minloc.X())/2;

        // given a set of features and their center
        // do they see an agent where 
        HashMap<Location, T> myfeatures = getSurroundingFeatures(myloc, radius);
        for(Map.Entry<Location, T> entry : theirfeatures.entrySet()){
            Location key = entry.getKey();
            T item = entry.getValue();
        }
    }

    /**  convert to a 2D array. All items that should be considered obstacles */
    // public int [][] toArray(){
    //     for(int i=0; i<getIDim(); i++){
    //         map.toArray();
    //     }
    //     return ;
    // }

    // public ArrayList<Location> findPath(Location loc1, Location loc2){
    //     // int[][] map, int x1, int y1, int x2, int y2
    //     int x1, x2, y1, y2;
    //     x1 = loc1.X();
    //     x2 = loc2.X();
    //     y1 = loc1.Y();
    //     y2 = loc2.Y();
    //     PathFinding pf = PathFinding.aStar(map, x1, y1, x2, y2);
    //     return pf.getPath();
    // }


    /** convert x,y coordinates to array index coordinates i,j */
    public Location coords_to_indexes(int x, int y){
        // int dim = (getIDim()/2)-1;
        int i = ((getIDim()/2)-1) + x;
        int j = ((getJDim()/2)-1) + y;
        return new Location(i, j);
    }

    /** convert x,y coordinates to array index coordinates i,j */
    public Location coords_to_indexes(Location c){
        return coords_to_indexes(c.X(), c.Y());
    }

    /** convert array index coordinates i,j to x,y coordinates */
    public Location indexes_to_coords(int i, int j){
        int dim = (getIDim()/2)-1;
        int x = i - dim;
        int y = j - dim;
        return new Location(x, y);
    }
    
    /** convert array index coordinates to x,y coordinates */
    public Location indexes_to_coords(Location c){
        return indexes_to_coords(c.X(), c.Y());
    }


    /** set the element at indexes (i,j) to mark */
    public void set_mark(int i, int j, T mark){
        // if i,j is currently unknown, and mark is not unknown
        // add i,j to the frontier
        if (get(i,j).equals(this.initial_symbol)){
            frontier.add(new Location(i,j));
            last_frontier_update = 0;
        }
        map.get(i).set(j, mark);
    }

    /** return the entry at pair */
    public T get_mark(Location xy){
        Location ij = coords_to_indexes(xy);
        return get_mark(ij.X(), ij.Y());
    }

    /** convert xy into indexes, and set entry at indexes to mark */
    public void set_location(Location xy, T mark){
        Location ij = coords_to_indexes(xy);
        set_mark(ij.X(), ij.Y(), mark);
    }

    /** convert each location into indexes, and set entries at indexes to corresponding marks */
    public void set_locations(HashMap<Location, T> marks){
        for(Location xy : marks.keySet()){
            set_location(xy, marks.get(xy));
        }
    }

    /** convert each location into indexes, and set entries at indexes to mark */
    public void set_locations(List<Location> locs, T mark){
        for(Location xy : locs){
            set_location(xy, mark);
        }
    }

    public boolean valid_coords(Location xy){
        int [] b = getBounds();
        int x = xy.X();
        int y = xy.Y();
        if (x >= 0 && x < b[0] && y >= 0 && y < b[1]) return true;
        return false;
    }

    // /* mark square at (i,j) with mark */
    // public void mark_map (int i, int j, int mark){

    //     map[i][j] = mark;
    //     if(mark == VISITED) {
    //         Location cp = new Location(i,j);
    //         visited.add(cp);
    //         if(frontier.contains(cp)) frontier.remove(cp);
    //     }else if(mark == OBSTACLE) return;

    //     // add squares adjacent to (i,j) to the frontier
    //     ArrayList<Location> adj = adjacent(i, j);
    //     for(Location p : adj){
    //         // if (!frontier.contains(p) ){

    //         if (!frontier.contains(p) && !visited.contains(p)){
    //             frontier.add(p);
    //             last_frontier_update = 0;
    //         }
    //     }
    // }

    // /* mark square at pair with mark */
    // public void mark_map(Location pair, int mark){
    //     mark_map(pair.X(), pair.Y(), mark);
    // }

    public boolean is(int i, int j, T mark){
        return get(i,j).equals(mark);
    }

    public boolean is(Location pair, T mark){
        return is(pair.X(), pair.Y(), mark);
    }

    /** count the number of times mark appears in map */
    public int count_marked(T mark){
        // UNKNOWN, OBSTACLE, VISITED
        int count = 0;
        for(int i=0; i<getIDim(); i++){
            for(int j=0; j<getJDim(); j++){
                T m = get_mark(i, j);
                if (m.equals(mark)) count++;
            }
        }
        return count;
    }

    // /** increment the frontier counter */
    // public void increment_frontier_counter(){
    //     last_frontier_update ++;
    // }

    // /** return value of frontier counter */
    // public int frontier_counter(){
    //     return last_frontier_update;
    // }

    // /** print a list of the frontier coords and their marks 
    //  * prior to the most recent action update */
    // public void print_frontier_marks(){
    //     for(Location cp : frontier){
    //         String m = get_mark(cp);
    //         System.out.println(cp + ": " + m);
    //     }
    // }

    // /** count the number of squares marked with UNKNOWN in the frontier */
    // public int frontier_unknowns(){
    //     // if all coords in the frontier are 1 or 2, 
    //     // then no UNKNOWN squares are reachable
    //     int count = 0;
    //     for(Location cp : frontier){
    //         int m = get_mark(cp);
    //         if(m == UNKNOWN){ count ++;}
    //     }
    //     return count;
    // }


    /** draw the grid using a multiline string */
    public String toString(){
        StringBuilder strRep = new StringBuilder(); // or StringBuilder
        String empty_str = "               "; // sequence of blank chars to substring for arb-sized blank
        int sqr_width = 2;
        for(int j=0; j<getJDim(); j++){
        

            strRep.append("|");

            // T[] seq = map.get(i);
            for(int i=0; i<getIDim(); i++){
                T s = get(i,j);
                String symbol = s.toString();
                String next_symbol;

                if(s == null){
                    next_symbol = empty_str.substring(sqr_width);

                }else if(symbol.length() >= sqr_width){
                    next_symbol = symbol;

                }else{
                    // pad the front and back of a short string
                    // with substrings of the empty_str
                    int leftover = sqr_width - symbol.length();
                    int front, back;
                    if((leftover % 2) == 0) front = back = leftover/2;
                    else{ 
                        front = (int)leftover/2;
                        back = front + 1;
                    }
                    next_symbol = empty_str.substring(0,front) + symbol + empty_str.substring(0,back);
                }
                strRep.append(next_symbol);
            }
            strRep.append("|\n");
        }
        return strRep.toString();
    }

    /* quick-and-dirty printer for the nested array, map. */
    public void print_array(){
        System.out.println("MentalMap:");
        System.out.println(toString());

        // System.out.print("   ");
        // // use % 10 to ensure that the label digit is always has 1 place
        // // so the labels align
        // for(int j=0; j<map[0].length; j++){
        //     String s = Integer(j).toString();
        //     if(s.length() >= 2) System.out.print(s);
        //     else System.out.print(" "+s)
        // }
        // System.out.print("\n");
        // System.out.println("----------------------");

        // for(int i=0; i<getIDim(); i++){
        //     // use % 10 to ensure that the label digit is always has 1 place
        //     // so the labels align
        //     // System.out.print((i%10) + "|");
        //     System.out.print("|");

        //     String[] seq = map[i];
        //     for(int j=0; j<seq.length; j++){
        //         // String s = Stringacter.toString(map[j][i]);//.toString();
        //         String s = map[j][i];
        //         if(s == null){
        //             System.out.print("  ");
        //         }else if(s.length() >= 2){
        //             System.out.print(s);
        //         }else System.out.print(" "+s);
        //     }
        //     System.out.print("|");
        //     System.out.println();
        // }
    }

    // /* find the coordinates closes to cx, cy that is marked the same as mark*/
    // public Location closest(int cx, int cy, int mark){
    //     Location c = new Location(cx, cy);
    //     ArrayList<Location> next = new ArrayList<Location>();
    //     ArrayList<Location> seen = new ArrayList<Location>();

    //     next.add(c);
    //     seen.add(c);

    //     while(true){
    //         ArrayList<Location> tmpnext = (ArrayList<Location>) next.clone();
    //         for(Location n : tmpnext){
    //             next.remove(n);
    //             ArrayList<Location> adj = adjacent(n.X(), n.Y());
    //             adj.removeAll(seen);
    //             if(adj.isEmpty())
    //                 // if no more adjacent squares are unseen,
    //                 // then this area is unreachable
    //                 return null;
    //             seen.addAll(adj);
    //             for(Location pair : adj){
    //                 int m = get_mark(pair.X(), pair.Y());
    //                 if (m == mark){
    //                     return pair;
    //                 }else if(m != OBSTACLE){
    //                     next.add(pair);
    //                 }
    //             }
    //         }
    //     }
    // }

    // /** find adjacent squares with the specified mark */
    // public ArrayList<Location> adjacent(int i, int j, int mark){
    //     ArrayList<Location> adj = adjacent(i, j);
    //     ArrayList<Location> marked_adj = new ArrayList<Location>();
    //     for(Location sq : adj){
    //         try{
    //             int sqmark = get_mark(sq.X(), sq.Y());
    //             if (sqmark == mark) marked_adj.add(sq);
    //         }catch(ArrayIndexOutOfBoundsException e){} // do nothing
    //     }
    //     return marked_adj;
    // }

    /** Methods below are utility methods that don't require info about map */

    // /** Calc the smallest number of turns to point in the target direction from dir. */
    // public int min_num_turns(int dir, int target){ return Math.abs((dir - target) % 2); }

    // /** return the shortest sequence of turns that will result in 
    // pointing in direction target if initially pointing in direction dir. */
    // public int[] minimal_turns(int dir, int target){
    //     if(dir == target) return new int[0];

    //     int diff = dir - target;
    //     int abs = Math.abs(diff);
    //     int mod = min_num_turns(dir, target);
    //     int turn_dir;

    //     if (abs == mod) turn_dir = CW;  // turn CW (i.e. RIGHT) mod # times!
    //     else turn_dir = CCW; // turn CCW (i.e. LEFT) mod # times!
    
    //     int [] turns = new int[abs];
    //     for(int i=0; i<abs; i++){
    //         turns[i] = turn_dir;
    //     }
    //     return turns;
    // }

    // /** return projected coordinates after moving once in the specified direction from x,y */
    // public int[] calculate_position_on_moveforward(int x, int y, int direction){
    //     int newx = x + Direction.DELTA_X[direction];
    //     int newy = y + Direction.DELTA_Y[direction];
    //     int[] position = {newx, newy};
    //     return position;
    // }

    // /** return the expected new direction after turn */
    // public int calculate_heading_on_turn(int direction, int turn){
    //     // i should handle possibility that turn is
    //     // and invalid option (i.e. not CW or CCW). Same for direction.
    //     int[] lookup = R_ROTATION; 
    //     if (turn == CCW) lookup = L_ROTATION;
    //     return lookup[direction];
    // }

    // /** calculate all coordinate pairs adjacent to (x,y) */
    // public ArrayList<Location> adjacent(int x, int y){
    //     ArrayList<Location> adj = new ArrayList<Location>();
    //     for(int d : directions){
    //         int[] pos = calculate_position_on_moveforward(x, y, d);
    //         Location pair = new Location(pos);
    //         adj.add(pair);
    //     }
    //     return adj;
    // }

    // /** return the distance between two pairs of coordinates. */
    // public double distance_between(Location xy1, Location xy2){
    //     double dx = (xy1.X() - xy2.X());
    //     double dy = (xy1.Y() - xy2.Y());
    //     return Math.hypot(dx, dy);
    // }

    // /* Find which direction xy2 is from xy1. 
    //  * Returns an array with either the two directions, 
    //  * or one direction and a -1. */
    // public int[] direction(Location xy1, Location xy2){
    //     // calc distance between xy2 and points adjacent to xy1
    //     // to determine which direction of travel brings us closer to xy2
    //     double curr_dist = distance_between(xy1, xy2);
    //     double [] dists = new double[4];

    //     int [] directionality = {-1, -1}; // init to invalid entries
    //     int i = 0;
    //     for(int dir : directions){
    //         int[] xy3 = calculate_position_on_moveforward(xy1.X(), xy1.Y(), dir);
    //         dists[dir] = distance_between(xy2, new Location(xy3));

    //         if (dists[dir] < curr_dist){
    //             directionality[i++] = dir;
    //         }
    //     }
    //     return directionality;
    // }


}