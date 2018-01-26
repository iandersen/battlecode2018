import bc.MapLocation;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Collections;
import java.util.*;


/// sample statement
/// BattleCodePathfinder x = new BattleCodePathfinder(map);
/// Stack<MapLocation> stack = x.returnPath(unit.location().mapLocation(), enemy.location.mapLocation());
public class BattleCodePathfinder {
  // stacks
  static Stack<Node> stack = new Stack<>();
  static Stack<MapLocation> returnable = new Stack<>();
  // dimensions
  static int height = 5;
  static int width = 5;
  // memory
  static HashMap<Pointxy,Integer> log = new HashMap<>();
  // start, end
  static Piece piece;
  static Piece anton;
  static int[][] map;
  // counter
  static int c = 1;
  
  // initialize map
  BattleCodePathfinder() {
  }
  // most important to the user. resets stacks, HashMap, and counter
  public static Stack<MapLocation> returnPath(int[][] mapg, MapLocation b, MapLocation e) {
	  map = mapg;
	  width = map.length;
	  height = map[0].length;
	  
	  // reinitialize stacks, log and counter
	  stack = new Stack<>();
	  returnable = new Stack<>();
	  log = new HashMap<>();
	  c = 1;
	  
	  piece = new Piece(b.getX(), b.getY());
	  anton = new Piece(e.getX(), e.getY());
	  Node node = new Node(piece.x, piece.y);
	  node.isBeginning = true;
	  stack.push(node);

	  processNodes(node);
	  
	    Node current = stack.peek();
	    while(!current.isBeginning) {
	      returnable.push(new MapLocation(b.getPlanet(), current.x, current.y));
	      current = current.parent;
	    }
	    return returnable;
	  }
  // Auxiliary method
  private static void processNodes(Node current) {
    c++;
    if (c > 499) {
      return;
    }
    current.desvaor();
    ArrayList<Node> options = current.findOptions();
     System.out.println(options);
    if(options.isEmpty()) {
      stack.pop();
      if(current.isBeginning) {
        return;
      }
      else {
        current.parent.good[current.id] = false;
        processNodes(stack.peek());
      }
    }
    else {
      for (Node node : options) {
        stack.push(node);
      }
      processNodes(stack.peek());
    }
  }
  
  // classes
  static class Node {
    // only for isBeginning
    boolean isBeginning = false;
    // common
    Node parent;
    int id = 1000;
    int x = 0;
    int y = 0;
    Node[] nodes = new Node[8];
    boolean[] good = new boolean[]{true, true, true, true, true, true, true, true};
    ArrayList<Node> ordering = new ArrayList<>();
    Node (int x, int y) {
      this.x = x;
      this.y = y;
    }
    // all one fell swoop
    public void desvaor() {
      designateNeighbors();
      validateNeighbors();
      orderNeighbors();
    }

    public double howCloseAmI(double x, double y) {
      double number =Math.sqrt(Math.pow(anton.x-x,2) + Math.pow(anton.y-y,2));
      if((int)x == anton.x && (int)y == anton.y) {
        c = 1000;
      }
      return number;
    }
    // toString
    public String toString() {
      if(x > 19 || x < 0
      || y > 19 || y < 0
      || map[x][y] != 0 ) {
        return "x: " + x + " y: " + y + " " + false;
      }
      return "x: " + x + " y: " + y + " " + true;
    }
    // what neighbors I have
    public void designateNeighbors() {
      nodes[0] = new Node(x, y+1); // north
      nodes[0].id = 0;

      nodes[1] = new Node(x, y-1); // south
      nodes[1].id = 1;

      nodes[2] = new Node(x+1, y); // east
      nodes[2].id = 2;

      nodes[3] = new Node(x-1, y); // west
      nodes[3].id = 3;

      nodes[4] = new Node(x-1, y+1); // northwest
      nodes[4].id = 4;

      nodes[5] = new Node(x+1, y+1); // northeast
      nodes[5].id = 5;

      nodes[6] = new Node(x-1, y-1); // southwest
      nodes[6].id = 6;

      nodes[7] = new Node(x+1, y-1); // southeast
      nodes[7].id = 7;
    }
    // what neighbors are possible and not taken and parents
    public void validateNeighbors() {
      for (int i = 0; i < nodes.length; i++) {
        if (
        nodes[i].x > width - 1 || nodes[i].x < 0
        || nodes[i].y > height - 1 || nodes[i].y < 0
        || map[nodes[i].x][nodes[i].y] != 0
        || log.get(new Pointxy(nodes[i].x, nodes[i].y)) != null) {
          nodes[i] = null;
        }
        else {
          log.put(new Pointxy(nodes[i].x, nodes[i].y), id);
          nodes[i].parent = this;
        }
      }
    }
    // order neighbors farthest first
    public void orderNeighbors() {
      HashMap<Double,Integer> order = new HashMap<>();
      ArrayList<Double> helper = new ArrayList<>();
      double value = 5;
      for (int i = 0; i < nodes.length ; i++ ) {
        if (nodes[i] != null) {
          value = howCloseAmI(nodes[i].x,nodes[i].y);
          value += (Math.random() * 1);
          order.put(value,i);
          helper.add(value);
        }
      }
      // create comparator for reverse order
      Comparator<Double> cmp = Collections.reverseOrder();
      Collections.sort(helper, cmp);
      for(Double number : helper) {
          ordering.add(nodes[order.get(number)]);
      }

    }
    // return this ordering
    public ArrayList<Node> findOptions() {
      return ordering;
    }
  }
  static class Pointxy implements Comparable<Pointxy> {

    int x;
    int y;
    int l;
    Pointxy(int x, int y) {
      l = 0;
      this.x = x;
      this.y = y;
    }


      public int compareTo(Pointxy that) {
          if(that.x == x && that.y == y)
              return 0;

          return 1;
      }


      // must be overridden for the HashSet to know that a Coordinate has been
      // repeated

      @Override
      public boolean equals(Object o) {
          Pointxy second = (Pointxy)o;
          if (second.x == this.x && second.y == this.y) {
              return true;
          }
      return false;
      }
      @Override
      public int hashCode() {
          int result = 17;
          Integer theX = (Integer)x;
          Integer theY = (Integer)y;
          result = 31 * result + theX.hashCode();
          result = 31 * result + theY.hashCode();
          return result;
      }
  }
  static class Piece {
    public int x = 0;
    public int y = 0;
    Piece(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

}
