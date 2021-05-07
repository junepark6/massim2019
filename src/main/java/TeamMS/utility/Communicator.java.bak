package mof217;
/* 
Author: Morgan Fine-Morris 
Handles encoding and decoding messages between agents.
*/

import pacworld.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.StringJoiner;

import java.util.List;
import java.util.HashSet;

/** contains data extracted from a message */
class Communication{

    public static final String unknownSender = "";

    protected String sender;
    protected String type;
    protected String[] data;
    protected int px, py, ax, ay;
    protected int packid;
    protected double score;
    protected int verbosity;

    public Communication(String type, String[] data){
        this.type = type;
        this.data = data;
        verbosity=-1;
        interpretData(data);
    }

    public Communication(String type, String sender, String[] data){
        this.sender = sender;
        this.type = type;
        this.data = data;
        verbosity=-1;
        interpretData(data);
    }

    /** set fields according to this.type, leaving all other fields null */
    private void interpretData(String[] data){

        String[] types = Communicator.types;
        
        if(type.equals(types[Communicator.POS])){
            ax = Integer.parseInt(data[0]);
            ay = Integer.parseInt(data[1]);

        }else if(type.equals(types[Communicator.DIBS])){
            packid = Integer.parseInt(data[0]);
            if(data.length>1) score = Double.parseDouble(data[1]);

        }else if(type.equals(types[Communicator.PACK])){
            packid = Integer.parseInt(data[0]);
            px = Integer.parseInt(data[1]);
            py = Integer.parseInt(data[2]);

        }else if(type.equals(types[Communicator.VERB])){
            verbosity = Integer.parseInt(data[0]);
        }
    }
    /** getters for various data elements */
    public MyLocation getPackageLocation(){ return new MyLocation(px, py); }
    public MyLocation getAgentLocation(){ return new MyLocation(ax, ay); }
    public String getAgent(){ return sender; }
    public int getPackage(){ return packid; }
    public double getScore(){ return score; }
    public int getVerbosity(){ return verbosity; }

    /** dump all the data in this object to a string and return it */
    public String toString(){
        String msg = "";
        msg = msg + "Msg("+this.type+", aID: "+this.sender + ", aloc: "+new MyLocation(ax, ay)+", ";
        msg = msg + "pID: "+this.packid+", ploc: "+new MyLocation(px, py)+", score: "+this.score+", " ;
        msg = msg + "verbosity: "+ verbosity + ")"+"\n";
        return msg;
    }
}

/** creates messages and decodes recieved messages */
public class Communicator{

    public static final String delimiter = ",";
    public static final String[] types = {"dibs", "pos", "pack", "del", "verb"};
    public static final int DIBS = 0;
    public static final int POS = 1;
    public static final int PACK = 2;
    public static final int DEL = 3;
    public static final int VERB = 4;

    private HashSet<String> inbox;
    private HashSet<String> outbox;

    public Communicator(){
        
    }

    /** Broadcast a message about the agent's current location 
        (if anon is True, don't include agent id)
        Will be interpreted as a request for data about packages near that location? */
    public String broadcastLocation(VisibleAgent agent, Boolean anon){
        String id = null;
        if(!anon){ id = agent.getId(); }
        return broadcastLocation(agent.getX(), agent.getY(), id);
    }

    /** Broadcast a message about the agent's current location 
        (if id is null, don't include agent id)
        Will be interpreted as a request for data about packages near that location? */
    public String broadcastLocation(int x, int y, String id){
        String msg = "pos"+delimiter+x+delimiter+y;
        if (id != null) msg = id+delimiter+msg;
        return msg;
    }

    /** Broadcast a message about the agent's current location 
        (if id is null, don't include agent id)
        Will be interpreted as a request for data about packages near that location? */
    public String broadcastLocation(MyLocation xy, String id){
        String msg = "pos"+delimiter+xy.getX()+delimiter+xy.getY();
        if (id != null) msg = id+delimiter+msg;
        return msg;
    }

    /** broadcast a request to set the verbosity to a new value  */
    public String broadcastVerbosityRequest(String id, int verbosity){
        String msg = id+delimiter+"verb"+delimiter+verbosity;
        return msg;
    }

    private String formatPackageData(VisiblePackage vp){
        String s = vp.getId()+delimiter+vp.getX()+delimiter+vp.getY();
        // if(vp.getDestX())
        return s;
    }

    /** broadcast a message about a package that the current agent wants someone else to get */
    public String broadcastPackage(VisiblePackage vp){
        String msg = "pack"+delimiter+formatPackageData(vp);
        return msg;
    }


    /** construct message for announcing multiple package locations */
    public String broadcastMultiplePackages(VisiblePackage [] vps){
        String msg = "";
        for(VisiblePackage vp: vps){
            if(msg == "") msg = broadcastPackage(vp);
            else msg = msg + ";" + formatPackageData(vp);
        }
        return msg;
    }

    /** broadcast message to indicate dibs on a package */
    public String broadcastDibs(int id){
        String msg = "dibs"+delimiter+id;
        return msg;
    }

    /** broadcast message to indicate dibs on a package */
    public String broadcastDibs(VisiblePackage vp){
        return broadcastDibs(vp.getId());
    }

    /** broadcast message to indicate dibs on a package (with score) */
    public String broadcastDibs(int id, double dist){
        String msg = "dibs"+delimiter+id+delimiter+dist;
        return msg;
    }

    /** broadcast message to indicate dibs on a package */
    public String broadcastDibs(VisiblePackage vp, double dist){
        return broadcastDibs(vp.getId(), dist);
    }

    /** broadcast that a package has been delivered */
    public String broadcastDelivered(VisiblePackage vp){
        String msg = "del"+delimiter+vp.getId();
        return msg;
    }

    /** combine multiple strings into a single string delimited by ';;' */
    public static String concateMultipleMessages(ArrayList<String> mess){
        StringJoiner sj = new StringJoiner(";;");
        for(String s: mess){ sj.add(s); }
        return sj.toString();
    }
    
    /** Attempts to extract message type and sender from front of msg
        return a len-2 array of type and message tail if no sender
        otherwise returns a len-3 array of type, sender, and message tail */
    public String [] separateTypeSenderData(String msg){
        String[] parts = msg.split(delimiter, 2);
        List<String> typeslist = Arrays.asList(this.types);
        // System.out.println("separateTypeSenderData"+msg);

        if(typeslist.contains(parts[0])){ // starts with a type: this is an anon message
            // System.out.println(parts[0] +" "+ parts[1]);
            return new String[] {parts[0], parts[1]}; // type, data
        }else{
            String sender = parts[0];
            parts = parts[1].split(delimiter, 2);
            String type = parts[0];
            // System.out.println(">>"+type +" "+sender+" "+ parts[1]);
            return new String[] {type, sender, parts[1]}; // type, sender, data
        }
    }

    /** extract components of messages and create and return Communication structure */
    public Communication decodeMessage(String msg){
        String[] parts = separateTypeSenderData(msg);
        // System.out.println("decodeMessage "+ parts[0]+" "+ parts[1]);
        if(parts.length == 2){
            // System.out.println("decodeMessage len 2");
            return decodeMessage(parts[1], parts[0], null);
        }else{ 
            // System.out.println("decodeMessage len 3");
            return decodeMessage(parts[2], parts[0], parts[1]); 
        }
    }

    /** return message content as data objects */
    public Communication decodeMessage(String msgdata, String type, String sender){
        // sender msgtype data
        String[] parts = msgdata.split(delimiter);
        if(sender == null){ 
            return new Communication(type, parts);
        }else{ // this is a signed message
            return new Communication(type, sender, parts);
        }
    }

    /** return true if the message has multiple components */
    public boolean isMultiMessage(String msg){ return msg.contains(";"); }

    /** return multi-message content as data objects */
    public ArrayList<Communication> decodeMultiMessage(String msg){

        ArrayList<Communication> cs = new ArrayList<Communication>();

        // multiple of diff type of message in string
        if(msg.contains(";;")){
            String [] msgs = msg.split(";;");
            for(String s : msgs){
                cs.addAll(decodeMultiMessage(s));
            }
            return cs;
        }

        // multiple of same type of message in string
        if(msg.contains(";")){
            String [] msgs = msg.split(";");
            Communication c = decodeMessage(msgs[0]);
            cs.add(c);
            String type = c.type;
            String sender = c.sender;
            for(String s : msgs){
                if(s == msgs[0]) continue;
                c = decodeMessage(s, type, sender);
                cs.add(c);
            }
            return cs;            
        }
        // its not a multimessage, so just populate cs with the single message
        cs.add(decodeMessage(msg));
        return cs;
    }

    /** return message content as data objects */
    public Communication [] decodeMessages(String [] msgs){
        ArrayList<Communication> msgData = new ArrayList<Communication>();
        // Communication [] msgData = new Communication[msgs.length];
        for(int i=0; i<msgs.length; i++) {
            if(isMultiMessage(msgs[i])) { msgData.addAll(decodeMultiMessage(msgs[i])); }
            else msgData.add(decodeMessage(msgs[i])); //msgData[i] = decodeMessage(msgs[i]);
        }
        return msgData.toArray(new Communication [msgData.size()]);
    }
}