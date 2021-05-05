package TeamMS.utility;
import java.lang.Math;
import java.util.*;

public class PathFinding {

    private class Dot {
        public int x;
        public int y;

        Dot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return String.format("%d.%d", x, y);
        }
    }

    private class Weight {
        public int score;
        public Dot xy;

        Weight(int score, Dot xy) {
            this.score = score;
            this.xy = xy;
        }
    }

    private Queue<Weight> pq = new PriorityQueue<>(new Comparator<Weight>() {
        @Override
        public int compare(Weight o1, Weight o2) {
            return Integer.compare(o1.score, o2.score);
        }
    });

    private Vector<String> ans = new Vector<String>();
    private HashSet<String> closed = new HashSet<String>();
    private int maxL = 100;
    private int[][] scoreF = new int[maxL][maxL];
    private int[][] scoreG = new int[maxL][maxL];
    private int[][] scoreH = new int[maxL][maxL];

    public PathFinding(int[][] map, int x1, int y1, int x2, int y2) {
        // start and end position
        Dot a = new Dot(x1, y1);
        Dot b = new Dot(x2, y2);
        int map_x = map.length;
        int map_y = map[0].length;
        System.out.println("map size: " + map_x + "," + map_y);
        aStar(map, map_x, map_y, a, b);
    }

    private void h(Dot end, Dot next) { // manhattan distance
        int x = Math.abs(end.x - next.x);
        int y = Math.abs(end.y - next.y);
        scoreH[next.x][next.y] = (x + y) * 10;
    }

    private void g(Dot now, Dot next, int plus) {
        scoreG[next.x][next.y] = scoreF[now.x][now.y] + plus;
    }

    private void f(Dot next) {
        scoreF[next.x][next.y] = scoreG[next.x][next.y] + scoreH[next.x][next.y];
    }

    public void aStar(int[][] map, int map_x, int map_y, Dot a, Dot b) {
        for (int[] row:scoreF) {
            Arrays.fill(row, 0);
        }
        for (int[] row:scoreG) {
            Arrays.fill(row, 0);
        }
        for (int[] row:scoreH) {
            Arrays.fill(row, 0);
        }

        // start node
        pq.add(new Weight(0, a));
        ans.add(a.toString());

        // add obstacle into closed
        for (int i=0; i < map_x; i++) {
            for (int j=0; j < map_y; j++) {
                String xy = String.format("%d.%d", i, j);
                if (map[i][j] == 1) closed.add(xy);
            }
        }

        Dot now;

        //int[] dx = new int[]{0, 1, 1,  1,  0, -1, -1, -1};
        //int[] dy = new int[]{1, 1, 0, -1, -1, -1,  0,  1};
        int[] dx = new int[]{0, 1,  0, -1};
        int[] dy = new int[]{1, 0, -1,  0};
        int step = 0;
        int plus = 10;

        while (!ans.lastElement().equals(b.toString())) {
            System.out.println("#STEP: " + step);
            now = pq.poll().xy;
            String xy = now.toString();
            System.out.println("CURRENT XY: " + xy);
            pq.clear();
            closed.add(xy);
            if (step > 0) ans.add(xy);

            for (int i=0; i < 4; i++) {
                int x = now.x + dx[i];
                int y = now.y + dy[i];
                if (x < 0 || y < 0 || x > (map_x-1) || y > (map_y-1)) continue;
                xy = String.format("%d.%d", x, y);
                if (closed.contains(xy)) continue;;
                Dot curr = new Dot(x, y);
                System.out.println("adjacent X,Y: " + curr.toString());
                h(b, curr);
                //int plus = 20;
                //if (dx[i] == 0 || dy[i] == 0) plus = 10;
                g(now, curr, plus);
                f(curr);
                Weight pushed = new Weight(scoreF[x][y], curr);
                pq.add(pushed);
            }
            step++;
        }
        System.out.println(ans.toString());
    }

    public static void main(String[] args) {
        // map size
        int map_x = 8;
        int map_y = 4;
        int[][] map = new int[map_x][map_y];

        /*
        $ is abstacle, S is start, E is end...
        $00$000E
        0$000$00
        00000$00
        S0000$00
         */
        for (int[] row:map) {
            Arrays.fill(row, 0);
        }

        // set obstacle as 1
        map[0][0] = 1;
        map[1][1] = 1;
        map[3][0] = 1;
        map[5][1] = 1;
        map[5][2] = 1;
        map[5][3] = 1;

        PathFinding pf = new PathFinding(map, 0, 3, 7, 0);
    }
}
