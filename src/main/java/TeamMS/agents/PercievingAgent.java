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
    public static final int OPPONENT=7;
    public static final int TEAMMATE=6;
    public static final int DISPENSER=5; 
    public static final int AGENT=4; 
    public static final int BLOCK=3;
    public static final int WALL=2;
    public static final int OBSTACLE=1;
    public static final int UNKNOWN=0;
    public static final int VISIBLE=-1;

    private String team = null;
    protected Set<Task> tasks = new HashSet<>();
    protected LinkedList<String> blocks = new LinkedList<String>();
    protected LinkedList<String> attachedIndex = new LinkedList<String>();
    protected LinkedList<String> blockType = new LinkedList<String>();
    protected LinkedList<String> dispenser = new LinkedList<String>(); // type.x.y
    protected LinkedList<String> entity = new LinkedList<String>();
    protected Set<LinkedList<Parameter>> things = new HashSet<>();
    protected Set<LinkedList<Parameter>> obstacles = new HashSet<>();
    protected Set<LinkedList<Parameter>> attached = new HashSet<>();
    protected ArrayList<Position> goalzone = new ArrayList<>();

    protected String disabled;

    protected Set<Thing> agents = new HashSet<>();
    protected int waitFlag = 0; // indicates to hold still for alignment
    protected HashMap<String, Position> agentOffsets = new HashMap<>();

    // protected HashMap<String, Position> agentOffsets;
    protected ArrayList<Dot> path = new ArrayList<Dot>();
    protected ArrayList<Message> inbox = new ArrayList<Message>();
    protected ArrayList<Message> outbox = new ArrayList<Message>();


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
        public Percept message;
        public String sender;
        public String receiver;

        Message(Percept message, String sender){
            this.message = message;
            this.sender = sender;
            this.receiver = null;
        }

        Message(Percept message, String sender, String receiver){
            this(message, sender);
            this.receiver = receiver;
        }

        public String toString(){
           return  "to: "+receiver+" from: "+sender+" "+message.toString();
        }
    }

    @Override
    public void handleMessage(Percept message, String sender) {
        inbox.add(new Message(message, sender));
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
            case "team":
                if (team == null)
                    team = percept.getParameters().get(0).toString();
                break;
            case "thing":
                // say("thing is " + percept.getParameters());
                things.add(percept.getParameters());
                break;
            case "obstacle":
                //say("obstacle is " + percept.getParameters());
                obstacles.add(percept.getParameters());
                break;
            case "goal":
                List<Parameter> params = percept.getParameters();
                int gx = (int)((Numeral)params.get(0)).getValue();
                int gy = (int)((Numeral)params.get(1)).getValue();
                goalzone.add(new Position(gx, gy));
                // say("goal is " + percept.getParameters());
                say("goalzone "+goalzone);
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
        }
    }


    /** did we make any progress over the last 10 actions */
    public void detectNoProgress(){
        Set locations = new HashSet(path.subList(path.size()-10, path.size()-1));
    }

    /* convert thing with map coordinates to coordinates w self as origin */
    public Thing localizedThing(Thing th){
        Position p = new Position(th.x, th.y);
        Position pp = p.toLocal(this.self);
        Thing newTh = new Thing(pp.x, pp.y, th.type, th.details);
        return newTh;
    }

    /** create string describing a set of coordinates in global reference frame, then local to neworigin */
    public String concatLocalAndGlobalCoords(Position globalloc, Position neworigin){
        Position localloc = globalloc.toLocal(neworigin);
        String globalstr = (new Dot(globalloc)).toString();
        String localstr = (new Dot(localloc)).toString();
        String coords = globalstr + ":" + localstr;
        return coords;
    }

    /** process string produced by concatLocalAndGlobalCoords to extract the coords */
    public Position [] extractLocalAndGlobalCoords(String message){
        String [] cs = message.split(":");
        Position global = (new Dot(cs[0])).toPosition();
        Position local = (new Dot(cs[1])).toPosition();
        return new Position[]{global, local};
    }

    public void processSharedCoordinateInformation(Message m){
        if(m.message.getName() != "shareOffset") return;

        String agent = m.message.getParameters().get(0).toString();
        int x = (int)((Numeral)m.message.getParameters().get(1)).getValue();
        int y = (int)((Numeral)m.message.getParameters().get(2)).getValue();
        Position shift = new Position(x,y);
        agentOffsets.put(agent, shift);
    }

    public void shareCoordinateInformationWith(String otherAgent, String targetAgent, Position otherOffset){
        if(targetAgent.equals(otherAgent)) return;

        Position offset = agentOffsets.get(targetAgent);
        offset = offset.translate(otherOffset);
        Percept p = new Percept("shareOffset", new Identifier(targetAgent), 
                                                new Numeral(offset.x), 
                                                new Numeral(offset.y));
        // sendMessage(Percept message, String receiver, String sender)
        sendMessage(p, otherAgent, getName());

    }

    public void shareCoordinateInformationWith(String otherAgent){
        // if we have info about agents in addition to otherAgent
        Position otherOffset = agentOffsets.get(otherAgent);
        for(String agent: agentOffsets.keySet()){
            shareCoordinateInformationWith(otherAgent, agent, otherOffset);
        }
    }

    public void shareCoordinateInformationAbout(String otherAgent){
        // if we have info about agents in addition to otherAgent
        Position otherOffset = agentOffsets.get(otherAgent);
        for(String agent: agentOffsets.keySet()){
            shareCoordinateInformationWith(agent, otherAgent, otherOffset);
        }
    }


    /** broadcast to agents on team that we have seen an agent */
    public void broadcastAgentSightings(){
        // say("Entities"+entity);
        String name = getName();
        Position self = new Position(x,y);

        // for each string in format x.y in entity (entity is in global coords):
        //  make a new Dot object for it
        //  discard it if its the same as the agents current loc
        //  convert to global coords
        //  make a new 'seeAgent' percept
        //  and broadcast the percept
        List<Percept> messages = agents.stream()
                        .filter(ag->ag.details.equals(this.team))
                        .filter(ag->(!(ag.x==this.x && ag.y==this.y)))
                        .map(th->this.concatLocalAndGlobalCoords(new Position(th.x, th.y), self))
                        .map(loc->new Percept("seeAgent", new Identifier(loc.toString())))
                        .collect(Collectors.toList());

        if(messages.isEmpty()) waitFlag=0;
        else waitFlag=messages.size(); // we filtered out the agents not in our team, so this is ok

        say("WaitFlag: "+waitFlag);
        messages.stream().forEach(per->broadcast(per, name));
    }

    public void processAlignmentMessage(Message m, List<Thing> seen){

            // skip message if wrong message type or if sender is self 
            // (probably unnecessary, but just in case)
            if(m.message.getName() != "seeAgent") return;
            
            // get the sent position from the string at index 0
            // (the string should contain both the coords wrt the sender,
            // and the coords wrt the sender's origin)
            String localAndGlobalString = m.message.getParameters().get(0).toString();
            Position [] localAndGlobal = extractLocalAndGlobalCoords(localAndGlobalString);

            // otherPos is where the other agents sees us wrt to themselves
            // globalPos is where the other agent sees us in their own global coordinate system
            
            // we are A, 
            // otherPos is Ab
            // globalPos is Boa
            // -otherPos is Ba
            // self is Aoa


            Position otherPos = localAndGlobal[1]; // the position wrt to the sender (sender as origin)
            Position globalPos = localAndGlobal[0]; // otherPos in sender's global coord sys
                                                    // compare it to our position in global coord sys to
                                                    // determine how to align our global coord systems

            Position otherPosInverted = new Position(-otherPos.x, -otherPos.y);
            say("SENDER:"+m.sender+" "+otherPos + " "+ globalPos);
    
            // compare it to the local positions of the agents we see
            // keeping those where (sent position) == -(seen positions)
            List<Thing> candidates = seen.stream().filter(ag->ag.x==-otherPos.x && ag.y==-otherPos.y)
                                                  .collect(Collectors.toList());
            if(candidates.isEmpty()) return;

            say("CANDIDATES for "+m.sender + ": "+candidates);

            waitFlag--; 
            say("WaitFlag "+waitFlag);
                
            // for each remaining agent, calc its true position in our coordinate system
            // and send it its coordinates, so it can calc where our origin is (in its coordinate sys)
            Thing matching = candidates.get(0);
            Position pos = new Position(matching.x, matching.y);
            pos = self.translate(pos);

            // using our current position and globalPos, determine how sender's coordinate system
            // is offset from our's (I'm pretty sure this is incorrect, feel free to fix)
            Position shift = globalPos.toLocal(self);
            agentOffsets.put(m.sender, shift);

            // say("\tmessage from "+m.sender+" "+otherPos + " "+ pos + " "+shift +" "+globalPos.toLocal(self));
            say("\t otherPos="+otherPos+"  globalPos="+globalPos+"  self="+self+ "  otherPosInv="+otherPosInverted);
            say("\t\t shift="+shift);
            say("\n");
            shareCoordinateInformationWith(m.sender);
            shareCoordinateInformationAbout(m.sender);
    }


    public void processInboxForAgentAlignment(){
        // for all entities, get their position in terms of local coords
        // List<Position> candidates = new ArrayList<Position>();
        // entity.stream().map(loc->new Dot(loc))
        //             .filter(loc->(!(loc.x==this.x && loc.y==this.y)))
        //             .distinct()
        //             .map(loc->loc.toPosition().toLocal(self))
        //             .forEach(loc->candidates.add((Position)loc));
        //         //    .collect(Collectors.toList());
        
        Position self = new Position(x,y);
        // for each agent we percieve, make new instance, with its coords localized
        List<Thing> seen = agents.stream()
                                .filter(ag->ag.details.equals(this.team))
                                .filter(ag->(!(ag.x==this.x && ag.y==this.y)))
                                .map(ag->this.localizedThing(ag))
                                .collect(Collectors.toList());
        int numDiscardedAgents = agents.size() - seen.size();   
        waitFlag -= numDiscardedAgents; 

        say("All Agents: "+agents);
        say("Seen Agents: "+seen + " (self at "+ self + ") w team: "+this.team);
        say("Inbox: "+inbox.toString());
        // find the messages that indicate that this agent and another see each other
        // get the inbox other agents sent, compare the entities they 
        inbox.stream().distinct().forEach(m->processAlignmentMessage(m, seen));
        inbox.stream().distinct().forEach(m->processSharedCoordinateInformation(m));        
        say("agentOffsets: "+agentOffsets);
        
        say("\n\n");
    }

    /** translate pos from the coordinate system of agent agentName to our coordinate system */
    public Position translateFrom(String agentName, Position pos){
        // this math may be incorrect
        Position translator = agentOffsets.get(agentName);
        return pos.toLocal(translator);
    }


    // public void processInboxForAgentAlignment2(){
    //     for(Message m : inbox){
    //         if(m.message.getName() != "truePosition") continue;
    //         String otherLoc = m.message.getParameters().get(0).toString();

            
    //     }
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
        agents.clear();
        // dispenser.clear();
        blocks.clear();
        List<Percept> percepts = getPercepts();
        for (Percept percept: percepts) {
            handlePercept(percept);
        }

        updateThings();
        updateObstacles();
        updateAttached(); 

        broadcastAgentSightings();
        processInboxForAgentAlignment();
        
        inbox.clear();
        

        // if last action was a successful clear(), zero out the clearing Dot
        if(info.lastAction.equals("clear") && info.lastActionResult.equals("success")) clearing=null;
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
            if(entity.contains(dot)) map[pos.x][pos.y] = AGENT;
            else if(dispenser.contains(dot)) map[pos.x][pos.y] = DISPENSER;
            else if(blocks.contains(dot)) map[pos.x][pos.y] = BLOCK;
            else{
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
            // say("parameters "+parameters);

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
        Thing x = agents.stream().filter(ag->(ag.x==i && ag.y==j)).findFirst().orElse(null);
        if(x!=null && x.details.equals(team)) return TEAMMATE;
        if(x!=null && !x.details.equals(team)) return OPPONENT;
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
                else if(symbol==TEAMMATE) strRep.append("AT");
                else if(symbol==OPPONENT) strRep.append("AO");
                else if(symbol==AGENT) strRep.append("A?");

                else{
                    say("Symbol "+symbol);
                    strRep.append("--");
                }

            }
            strRep.append("\n");
        }
        return "\n"+strRep.toString();
    }

}
