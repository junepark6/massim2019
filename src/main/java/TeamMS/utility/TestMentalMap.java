package TeamMS.utility;

// compile with "javac -cp .:vacworld/:mof217/ mof217/TestMentalMap.java -verbose"
// run with "java -cp . mof217.TestMentalMap"

public class TestMentalMap{
    public static void testOne(int x, int y){
        System.out.println("Test 1");
        MentalMap m = new MentalMap<String>(20, "U");

        System.out.println("original (x,y): "+ x + ", "+y);

        Location ij = m.coords_to_indexes(x,y);
        int i = ij.X();
        int j = ij.Y();
        System.out.println("(i,j): "+ i + ", "+j);
        Location xy = m.indexes_to_coords(i,j);
        x = xy.X();
        y = xy.Y();
        System.out.println("(x,y): "+ x + ", "+y);

    }

    public static void testTwo(int i, int j){
        System.out.println("Test 2");
        MentalMap m = new MentalMap<String>(20, "U");

        System.out.println("original (i,i): "+ i + ", "+j);
        Location xy = m.indexes_to_coords(i,j);
        int x = xy.X();
        int y = xy.Y();
        System.out.println("(x,y): "+ x + ", "+y);

        Location ij = m.coords_to_indexes(x, y);
        i = ij.X();
        j = ij.Y();
        System.out.println("(i,j): "+ i + ", "+j);
        
    }

    // public static void testSet(int i, int j, String s){

    // }

    // public static void test__min_num_turns(){
    //     MentalMap m = new MentalMap(20);
    //     for(int dir : MentalMap.directions){
    //         for(int tar : MentalMap.directions){
    //             int turns = m.min_num_turns(dir, tar);
    //             System.out.println("dir, tar: "+ Direction.toString(dir) + ", "+ Direction.toString(tar) + ": "+ turns);
    //         }
    //     }
    // }

    // public static void test__minimal_turns(){
    //     MentalMap m = new MentalMap(20);
    //     for(int dir : MentalMap.directions){
    //         for(int tar : MentalMap.directions){
    //             int [] turns = m.minimal_turns(dir, tar);
    //             System.out.println("dir, tar: "+ Direction.toString(dir) + ", "+ Direction.toString(tar));
    //             for(int turn : turns){
    //                 System.out.print(turn + ", ");
    //             }
    //             System.out.print("\n\n");
    //         }
    //     }
    // }
 

    public static void main(String[] args) {

        MentalMap<String> m = new MentalMap<String>(10,10,"U");
        m.set_mark(0,0,"0");
        m.set_location(new Location(0,0),"1");
        m.set_location(new Location(1,1),null);

        System.out.println(m.toString());
        
        // testOne(0,0);
        // testOne(0,1);
        // testOne(0,-1);
        // testTwo(0,0);
        // testTwo(0,1);
        // testTwo(0,-1);

        // test__min_num_turns();
        // test__minimal_turns();
        
    }

}