package TeamMS.agents;

import TeamMS.utility.Direction;
import TeamMS.utility.PathFinding;
import eis.iilang.*;
import TeamMS.MailService;
import TeamMS.utility.*;
import massim.protocol.data.*;

import java.util.stream.Collectors;
import java.util.*;


public class PercievingAgent extends Agent{

    //     case 0: // no obstacle, not seen before, very valuable
    //     return 2;
    // case 1: // obstacle! has no value
    //     return 0;
    // case -1: // indicates 'seen' but no obstacle, kinda valuable
    //     return 1;

    public static final int WALL=2;
    public static final int OBSTACLE=1;
    public static final int UNKNOWN=0;
    public static final int VISIBLE=-1;

    protected Set<Task> tasks = new HashSet<>();
    protected LinkedList<String> blocks = new LinkedList<String>();
    protected LinkedList<String> attachedIndex = new LinkedList<String>();
    protected LinkedList<String> blockType = new LinkedList<String>();
    protected LinkedList<String> dispenser = new LinkedList<String>(); // type.x.y
    protected LinkedList<String> entity = new LinkedList<String>();
    protected Set<LinkedList<Parameter>> things = new HashSet<>();
    protected Set<LinkedList<Parameter>> obstacles = new HashSet<>();
    protected Set<LinkedList<Parameter>> attached = new HashSet<>();
    protected String disabled;

    protected Set<Thing> agents = new HashSet<>();

    // protected HashMap<String, Position> agentOffsets;
    protected ArrayList<Dot> path = new ArrayList<Dot>();
    protected ArrayList<Message> messages = new ArrayList<Message>();


    protected HashMap<String, Position> agentOffsets;

    protected LinkedList<String> doneDispenser = new LinkedList<String>(); // type.x.y
    protected LinkedList<Action> plans = new LinkedList<Action>();
    protected ArrayList<Action> history;
    protected Position clearing;

    protected int[][] map = new int[100][100];
    protected Position origin = new Position(50, 50);
    protected int x = origin.x;
    protected int y = origin.y;
    protected Position self = new Position(x,y);

    protected Task task = null;
    protected String target = null;
    protected Info info = new Info();
    protected int vision = 5;
    protected ActionDispatch actionDispatch;

    protected Map<Integer, String> hold = new HashMap<Integer, String>() {{
        put(Direction.NORTH, null);
        put(Direction.EAST, null);
        put(Direction.SOUTH, null);
        put(Direction.WEST, null);
    }};

    public PercievingAgent(String name, MailService mailbox) {
        super(name, mailbox);
        history = new ArrayList<Action>();
        actionDispatch = new ActionDispatch();
        disabled = "false";
    }

    protected class Message{
        public final Percept message;
        public final String sender;
        Message(Percept message, String sender){
            this.message = message;
            this.sender = sender;
        }
    }

    @Override
    public void handleMessage(Percept message, String sender) {
        messages.add(new Message(message, sender));
    }

    protected class Dot {
        public int x;
        public int y;

        Dot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        Dot(String st){
            String[] xy = st.split("\\.");
            this.x = new Integer(xy[0]);
            this.y = new Integer(xy[1]);
        }

        Dot(Position p){
            this.x = p.x;
            this.y = p.y;
        }

        public Position toPosition(){
            return new Position(x,y);
        }

        public String toString() {
            return String.format("%d.%d", x, y);
        }

        public int manhattanDistance(Dot other) {
            int x2 = other.x;
            int y2 = other.y;
            int x0 = x - x2;
            int y0 = y - y2;
            return Math.abs(x0) + Math.abs(y0);
        }
    }

    protected class Info {
        protected String lastAction = null;
        protected String lastActionParams = null;
        protected String lastActionResult = null;
        Info() {}

        public String toString(){
            return lastAction + " " + lastActionParams + " " + lastActionResult;
        }
    }

    protected class Task {
        protected String name;
        protected int deadline;
        protected int reward;
        protected Set<LinkedList<Parameter>> taskStructure = new HashSet<>();

        Task(LinkedList<Parameter> parameters) {
            name = parameters.get(0).toString();
            deadline = Integer.parseInt(parameters.get(1).toString());
            reward = Integer.parseInt(parameters.get(2).toString());
            for (Parameter block : ((ParameterList) parameters.get(3))) {
                taskStructure.add(((Function) block).getParameters());
            }
            // TODO: update required blocks
            //for (LinkedList<Parameter> param : taskStructure) {
            //    int x = Integer.parseInt(param.get(0).toString());
            //    int y = Integer.parseInt(param.get(1).toString());
            //    String type = param.get(2).toString();
            //}
        }
    }

    /** create an action object */
    protected class ActionDispatch{

        public ActionDispatch(){}

        public Action rotateAction(String direc){ // "cw" or "ccw"
            return new Action("rotate", new Identifier(direc));
        }

        public Action moveAction(String direc){
            return new Action("move", new Identifier(direc));
        }

        public Action moveAction(int direc){
            String dir = PercievingAgent.toDirSymbol(direc);
            return new Action("move", new Identifier(dir));
        }

        public Action clearAction(int x, int y){
            return new Action("clear", new Numeral(x), new Numeral(y));
        }
    }

    @Override
    public void handlePercept(Percept percept) {
        switch (percept.getName()) {
            case "thing":
                say("thing is " + percept.getParameters());
                things.add(percept.getParameters());
                break;
            case "obstacle":
                //say("obstacle is " + percept.getParameters());
                obstacles.add(percept.getParameters());
                break;
            case "goal":
                //say("goal is " + percept.getParameters());
                break;
            case "attached":
                attached.add(percept.getParameters());
                break;
            case "name":
                //say("name is " + percept.getParameters().get(0));
                break;
            case "lastAction":
                Parameter lastAction = percept.getParameters().get(0);
                info.lastAction = lastAction.toString();
                //say("lastAction is " + info.lastAction);
                break;
            case "lastActionParams":
                Parameter lastActionParams = percept.getParameters().get(0);
                info.lastActionParams = lastActionParams.toString();
                //say("lastActionParams is " + info.lastActionParams);
                break;
            case "lastActionResult":
                Parameter lastActionResult = percept.getParameters().get(0);
                info.lastActionResult = lastActionResult.toString();
                //say("lastActionResult is " + info.lastActionResult);
                break;
            case "task":
                //say("task is " + percept.getParameters());
                Task task = new Task(percept.getParameters());
                tasks.add(task);
                break;
            case "disabled":
                Parameter status = percept.getParameters().get(0);
                this.disabled = status.toString();
                if(this.disabled.equals("true")) say("\n\nDISABLED\n\n");
                break;
            case "seeAgent":
                Parameter loc = percept.getParameters().get(0);
                Dot dot = new Dot(loc.toString());
                say("handle seeAgent "+dot);
                break;
        }
    }


    /** did we make any progress over the last 10 actions */
    public void detectNoProgress(){
        Set locations = new HashSet(path.subList(path.size()-10, path.size()-1));
    }

    // public void processPercepts2(){
    //     List<Percept> percepts = getPercepts();
    //     Map<String, List<Percept>> categorizedPercepts = percepts.stream()
    //         .collect(Collectors.groupingBy(per -> per.getName()));
        
    // }

    public void processPercepts(){
        updateEmpty();

        if (info.lastAction != null)
            updateCoord();
        path.add(new Dot(x,y));

        things.clear();
        obstacles.clear();
        attached.clear();
        attachedIndex.clear();
        entity.clear();
        // dispenser.clear();
        // blocks.clear();
        List<Percept> percepts = getPercepts();
        for (Percept percept: percepts) {
            handlePercept(percept);
        }

        updateThings();
        updateObstacles();
        updateAttached(); 

        broadcastAgentSightings();

        Position self = new Position(x,y);

        // for all entities, get their in term of local coords
        List<Position> candidates = new ArrayList<Position>();
        entity.stream().map(loc->new Dot(loc))
                    .filter(loc->(!(loc.x==this.x && loc.y==this.y)))
                    .distinct()
                    .map(loc->loc.toPosition().toLocal(self))
                    .forEach(loc->candidates.add((Position)loc));
                //    .collect(Collectors.toList());
        say("Cand"+candidates);

        // get the messages other agents sent
        for(Message m : messages){
            if(m.sender == getName()) continue; //probably unnecessary, but just incase

            // get the sent position (which is local to the sender)
            String otherLoc = m.message.getParameters().get(0).toString();
            Dot d = new Dot(otherLoc);
            Position otherPos = d.toPosition();
            List<Position> positions = candidates.stream()
                                                 .filter(loc->loc.x==-otherPos.x && loc.y==-otherPos.y)
                                                 .collect(Collectors.toList());
            if(positions.isEmpty()) continue;
            Position matching = positions.get(0);
            say("\tmessage "+m.sender+" "+otherPos + " " + positions);
            // need to send another message to the 
            // sendMessage(, , getName()); //Percept message, String receiver, String sender
    
            // //for each element in entity
            // for(String loc: entity){
            //     Dot dd = new Dot(loc);
            //     if(dd.x == x && dd.y == y) continue;
            //     Position pos = dd.toPosition();
            //     pos = pos.toLocal(origin);
            //     dd = new Dot(pos);
            //     if(pos.distanceTo(otherPos) < 5) say("Message from "+m.sender+" loc "+otherPos+"  vs "+ pos);

            // }
 
            // say("Message from "+m.sender+" loc "+loc+"  "+d.toString()+" "+entity.contains(d.toString()));
        }
        say(" ");
        messages.clear();

        // if last action was a successful clear(), zero out the clearing Dot
        if(info.lastAction.equals("clear") && info.lastActionResult.equals("success")) clearing=null;
    }

    /** broadcast to agents on team that we have seen an agent */
    public void broadcastAgentSightings(){
        // say("Entities"+entity);

        Position self = new Position(x,y);
        String selfStr = new Dot(x,y).toString();
        String name = getName();

        // for each string in format x.y in entity (entity is in global coords):
        //  make a new Dot object for it
        //  discard it if its the same as the agents current loc
        //  convert to global coords
        //  make a new 'seeAgent' percept
        //  and broadcast the percept
        entity.stream().map(loc->new Dot(loc))
                       .filter(loc->(!(loc.x==this.x && loc.y==this.y)))
                       .map(loc->new Dot(loc.toPosition().toLocal(self)))
                       .map(loc->new Percept("seeAgent", new Identifier(loc.toString())))
                       .forEach(per->broadcast(per, name));

        // for(String loc: entity){ // entity is in global coords
        //     Dot d = new Dot(loc);
        //     if(d.x == this.x && d.y == this.y) continue;
        //     Position pos = d.toPosition();
        //     d = new Dot(pos.toLocal(new Position(x,y)));
        //     Percept p = new Percept("seeAgent", new Identifier(d.toString()));
        //     broadcast(p, getName());
        // }
    }

    public int entryValue(int value){
        switch (value) {
                case 0: // no obstacle, not seen before, very valuable
                    return 2;
                case 1: // obstacle! has no value
                    return 0;
                case -1: // indicates 'seen' but no obstacle, kinda valuable
                    return 1;
            }
        return 0;
    }

    public int positionValue(int x, int y){
        return entryValue(map[x][y]);
    }

    // public int valueOfReachable(int xmin, int xmax, int ymin, int ymax, int limit){
    //     int value=0;
    //     int symbol;
    //     for(int i=0; i<limit; i++){
    //         if(i<xmin || i>xmax) continue;
    //         if(symbol>=OBSTACLE) continue;
    //         for(int j=0; j<limit; j++){
    //             symbol = getFromMap(i,j);
    //             if(symbol >= OBSTACLE) continue;
    //             if(j<ymin || j>ymax) continue;
    //             value += positionValue(x+i, x+j);
    //         }
    //     }
    //     return value;
    // }

    /** estimate how valuable the range of locations within the bounds are according to entryValue */
    public int valueOfSection(int xmin, int xmax, int ymin, int ymax){
        int value = 0;
        for(int i=0; i<map.length; i++){
            if(i<xmin || i>xmax) continue;
            for(int j=0; j<map[0].length; j++){
                if(j<ymin || j>ymax) continue;
                value += positionValue(i, j);
            }
        }
        return value;
    }


    public Map<String, Integer> calcDirectionValue(int x, int y, int distance){
        int lefthalf = valueOfSection(x-distance, x, y-distance, y+distance);
        int righthalf = valueOfSection(x, x+distance, y-distance, y+distance);
        int tophalf = valueOfSection(x-distance, x+distance, y, y+distance); // south actually
        int bottomhalf = valueOfSection(x-distance, x+distance, y-distance, y);
        
        Map<String, Integer> value = new HashMap<String, Integer>() {{
            put(toDirSymbol(Direction.NORTH), bottomhalf);
            put(toDirSymbol(Direction.EAST), righthalf);
            put(toDirSymbol(Direction.SOUTH), tophalf);
            put(toDirSymbol(Direction.WEST), lefthalf);
        }};
        return value;
    }

    public Map<String, Integer> calcDirectionValue(){
        int distance = 10;
        return calcDirectionValue(this.x, this.y, distance);
    }

    /** calc the 'value' for each of the 4 directions up to a distance away */
    public Map<String, Integer> calcDirectionWeight(int x, int y, int distance){
        Map<String, Integer> dirValues = calcDirectionValue(x, y, distance);
        // int total = dirValues.values().stream().reduce(0, Integer::sum);
        // int number = dirValues.values().min();
        int number = Collections.min(dirValues.values());
        if(number >= 0) number = number - 1;
        for(Map.Entry<String,Integer> entry :dirValues.entrySet()){
            dirValues.put(entry.getKey(), entry.getValue().intValue() - number);
        }
        return dirValues;
    }

    public Map<String, Integer> calcDirectionWeight(){
        return calcDirectionWeight(x, y, 10);
    }

    public Position getAdjacentPosition(int x, int y, int dir){
        int newx = x + Direction.DELTA_X[dir];
        int newy = y + Direction.DELTA_Y[dir];
        return new Position(newx, newy);
    }

    public int getAdjacentEntry(int x, int y, int dir){
        int newx = x + Direction.DELTA_X[dir];
        int newy = y + Direction.DELTA_Y[dir];
        return map[newx][newy];
    }

    /** return a set of 4 ints (n, e, s, w), where 1 indicates invalid and 0 indicates valid */
    public int [] calcValidDirections(){
        return new int[]{map[x][y-1], map[x+1][y], map[x][y+1], map[x-1][y]};
    }

    public Map<String, Boolean> calcValidDirections(int x, int y){
        // int [] validDirs = calcValidDirections();
        Map<String, Boolean> valid = new HashMap<String, Boolean>();

        // convert 0 to 1 and 1 to 0
        for(int dir=0; dir<4; dir++){
            String symb = toDirSymbol(dir);
            int adjacent_entry = getAdjacentEntry(x, y, dir);
            int value;
            switch (adjacent_entry) {
                case 1: // obstacle!
                    valid.put(symb, false);
                    break;
                case 0: // no obstacle
                default: // other, (e.g. -1 if we've seen it before)
                    valid.put(symb, true);
                    break;
            }
        }
        return valid;
    }

    /** are we surrounded on all sides? */
    public Boolean trapped(Map<String, Boolean> dirs){
        HashSet<Boolean> valid = new HashSet<Boolean>(dirs.values());
        boolean singleentry = (valid.size() == 1);
        if(singleentry && valid.contains(true)) return false;
        else if(singleentry && valid.contains(false)) return true;
        return false;
    }

    // /** are we surrounded on all sides? */
    // public Boolean trapped(){
    //     // if all of these are 1, we are blocked in and need to clear
    //     Map<String, Boolean> dirs = calcValidDirections(x,y); 
    //     return trapped(dirs);
    // }

    // /**  */
    // public Boolean largeTrapped(){
    // }

    // public getSightBoundary(){
    // }

    /** Either return a useful move action or a useful clear action */
    public Action valuableMoveOrClear(){

        // if we aren't trapped, select a direction to travel in
        Map<String, Boolean> valid = calcValidDirections(x,y);
        boolean amtrapped = trapped(valid);
        if(! amtrapped){
            Action action = valuableMove();
            return action;
        }

        // decide which direct to clear (based on desired travel direction)
        Position clear;
        if(clearing == null){
            Map<String, Integer> weights = calcDirectionWeight();
            String rbd = randomBiasedDirection(weights.get("n"), weights.get("e"), 
                                                weights.get("s"), weights.get("w"));
            int dir = fromDirSymbol(rbd);
            clear = getAdjacentPosition(x, y, dir).toLocal(origin);
            clearing = clear;
        }else clear = clearing;
        return actionDispatch.clearAction(clear.x, clear.y);
    }

    /** determine which direction is most useful to move in according to how
    much unvisited area is within some number of squares */
    public Action valuableMove(){
        Map<String, Boolean> valid = calcValidDirections(x,y);
        if(trapped(valid)) return null;

        Map<String, Integer> weights = calcDirectionWeight(x, y, 10);

        // say(valid.toString());
        // say(weights.toString());
        // if we can't go in that direction (due to obstacle), 
        // overwrite the weights of that direction
        for(String key: weights.keySet()){
            int dir = fromDirSymbol(key);
            if(!valid.get(key)) weights.put(key, 0);
        }
        return randomBiasedMove(weights.get("n"), weights.get("e"), weights.get("s"), weights.get("w"));
    }

    /** return a random, but valid move action */
    public Action validMove(){
        Map<String, Boolean> valid = calcValidDirections(x,y);
        if(trapped(valid)) return null;

        int [] dirs = new int [4];
        // convert 0 to 1 and 1 to 0
        for(int i=0; i<4; i++){
            String s = toDirSymbol(i);
            boolean b = valid.get(s);
            if(b) dirs[i] = 1;
            else dirs[i] = 0;
        }
        return randomBiasedMove(dirs[0], dirs[1], dirs[2], dirs[3]);
    }

    @Override
    public Action step() {
        processPercepts();
        return validMove();
    }

    protected void updateCoord() {
        if (info.lastAction.equals("move") && info.lastActionResult.equals("success")) {

            switch (info.lastActionParams) {
                case "[n]":
                    this.y -= 1;
                    break;
                case "[e]":
                    this.x += 1;
                    break;
                case "[s]":
                    this.y += 1;
                    break;
                case "[w]":
                    this.x -= 1;
                    break;
            }
        }else if(info.lastAction.equals("move") && !info.lastActionResult.equals("success")){
            // if move action failed, "failed_path", "failed_forbidden"
            // there is a wall there (but how do I know which direction it runs?)
            
            int dir = fromDirSymbol(info.lastActionParams);
            Position pos = getAdjacentPosition(this.x,this.y, dir);
            String dot = new Dot(pos).toString();
            if(!entity.contains(dot) && !dispenser.contains(dot) && !blocks.contains(dot)){
                map[pos.x][pos.y] = WALL;
                // say("Marked Wall at "+pos);
            }  
        }
        // say("x and y is " + x + "," + y);
    }

    protected void updateEmpty() {
        for (int i=-vision; i < vision+1; i++) {
            for (int j=-vision; j < vision+1; j++) {
                if (Math.abs(i) + Math.abs(j) > vision) continue;
                if (map[x+i][y+j]==WALL) continue; //if its a wall, don't overwrite
                map[x+i][y+j] = -1;
            }
        }
    }

    protected void updateThings() {
        for (LinkedList<Parameter> parameters : things) {
            int dx;
            int dy;
            int tx;
            int ty;
            String type;

            dx = Integer.parseInt(parameters.get(0).toString());
            dy = Integer.parseInt(parameters.get(1).toString());
            type = parameters.get(2).toString();
            tx = x + dx;
            ty = y + dy;
            say("parameters "+parameters);

            switch (type) {
                case "dispenser":
                    // say("DISPENSER " + dx + "." + dy);
                    dispenser.add(String.format("%s.dispenser.%d.%d", type, tx, ty));
                    // say("dispenser " + String.format("%s.%d.%d", type, tx, ty));
                    break;
                case "block":
                    say("BLOCK" + dx + "." + dy);
                    blocks.add(String.format("%s.block.%d.%d", type, tx, ty));
                    // say("block" + String.format("%s.%d.%d", type, tx, ty));
                    break;
                case "entity":
                   entity.add(String.format("%d.%d", x+dx, y+dy));
                   Thing t = new Thing(tx, ty, type, parameters.get(3).toString());
                   agents.add(t);
                   say("Entity"+t);
                //    need to tell agent we've seen it
                   break;
            }
        }
    }

    protected void updateObstacles() {
        for (LinkedList<Parameter> parameters : obstacles) {
            int dx = Integer.parseInt(parameters.get(0).toString());
            int dy = Integer.parseInt(parameters.get(1).toString());
            map[x+dx][y+dy] = 1;
        }
    }

    protected void updateAttached() {
        for (LinkedList<Parameter> parameters : attached) {
            int dx = Integer.parseInt(parameters.get(0).toString());
            int dy = Integer.parseInt(parameters.get(1).toString());
            int tx = x + dx;
            int ty = y + dy;
            attachedIndex.add(String.format("%d.%d", tx, ty));
        }
    }

    /** return a direction string, randomly selected according to bias */
    public String randomBiasedDirection(int n, int e, int s, int w){
        int total = n + s + e + w;
        String direc = null;

        int r = getRandomNumberUsingNextInt(0,total);
        if(r<n){ // r is [0, n)
            direc = "n";
        }else if( r>=n && r<(n+s) ){ // r is [n, n+s)
            direc = "s";
        }else if( r>=(n+s) && r<(n+s+w) ){ // r is [n+s, n+s+w)
            direc = "w";
        }else if( r>=(n+s+w) ){ // r is [n+s+w, total]
            direc = "e";
        }
        return direc;
    }

    /** 
     * return a Move action, with direction biased according to values of n, s, e, or w.
     */
    public Action randomBiasedMove(int n, int e, int s, int w){
        String direc = randomBiasedDirection(n, e, s, w);
        return new Action("move", new Identifier(direc));
    }

    public static int fromDirSymbol(String sym){
        int direc=-1;
        switch (sym) {
            case "n":
            case "[n]":
                direc = 0;
                break;
            case "e":
            case "[e]":
                direc = 1;
                break;
            case "s":
            case "[s]":
                direc = 2;
                break;
            case "w":
            case "[w]":
                direc = 3;
                break;
        }
        return direc;
    }

    public static String toDirSymbol(int r){
        String direc = "";
        switch (r) {
            case 0:
                direc = "n";
                break;
            case 1:
                direc = "e";
                break;
            case 2:
                direc = "s";
                break;
            case 3:
                direc = "w";
                break;
        }
        return direc;
    }

    /** return a completely random direction */
    public String randomDirection(){
        int r = getRandomNumberUsingNextInt(0,4);
        String direc = null;

        switch (r) {
            case 0:
                direc = "n";
                break;
            case 1:
                direc = "s";
                break;
            case 2:
                direc = "e";
                break;
            case 3:
                direc = "w";
                break;
        }
        return direc;
    }

    /** return a Move with a completely random direction */
    public Action randomMove(){
        String direc = randomDirection();
        return new Action("move", new Identifier(direc));
    }

    public int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    public int getManhattanDistance(int x1, int y1, int x2, int y2) {
        int x0 = x1 - x2;
        int y0 = y1 - y2;
        return Math.abs(x0) + Math.abs(y0);
    }

    public int getFromMap(int i, int j){
        return map[i][j];
    }

    /** draw the grid using a multiline string */
    public String mapToString(){
        StringBuilder strRep = new StringBuilder(); // or StringBuilder
        String empty_str = "               "; // sequence of blank chars to substring for arb-sized blank
        int sqr_width = 2;
        for(int i=0; i<map.length; i++){
            strRep.append("|");
            for(int j=0; j<map[i].length; j++){
                int symbol = getFromMap(i,j);
                if(x==i && y==j) strRep.append("5");
                else if(symbol==-1) strRep.append(".");
                else strRep.append(symbol);
            }
            strRep.append("|\n");
        }
        return "\n"+strRep.toString();
    }

    public String submapString(int xmin, int xmax, int ymin, int ymax){
        StringBuilder strRep = new StringBuilder(); // or StringBuilder

        boolean xmin_is_even = ((xmin % 2) == 0);
        strRep.append("   ");
        for(int i=xmin; i<=xmax; i++){
            String symb = String.valueOf(i);
            // if xmin is even, 
            if((xmin_is_even && i%2!=0) || (!xmin_is_even && i%2==0)){
                strRep.append("  ");
                continue;
            }
            if(symb.length() < 2) symb = " " + symb;

            strRep.append(symb);
        }
        strRep.append("\n");
        // for(int i=xmin; i<xmax+3; i++) strRep.append("--");
        // strRep.append("\n");
        for(int j=0; j<map[0].length; j++){
            if(j<ymin || j>ymax) continue;
            strRep.append(j+"|");
            for(int i=0; i<map.length; i++){
                if(i<xmin || i>xmax) continue;
                

                // add symbol for (i,j) to string
                int symbol = getFromMap(i,j);
                if(x==i && y==j) strRep.append("AG");
                else if(symbol==-1) strRep.append("  ");
                else if(symbol==1) strRep.append("<>"); // obstacle
                else if(symbol==WALL) strRep.append("WW"); // obstacle
                else if(symbol==UNKNOWN) strRep.append("00");
                else strRep.append("--");

            }
            strRep.append("\n");
        }
        return "\n"+strRep.toString();
    }

}
