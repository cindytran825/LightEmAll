import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;


// comparator that checks the difference between two given edges
class CompareEdge implements Comparator<Edge> {
  public int compare(Edge o1, Edge o2) {
    return o1.weight - o2.weight;
  }
}

class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e. represents board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station
  // as well as its effective radius
  int powerRow;
  int powerCol;
  Random rand;
  boolean win;
  int counter;
  int numClicks;

  // int radius; // optional for extra credit
  LightEmAll(int width, int height, Random rand) {
    this.width = width; // cols
    this.height = height; // rows
    this.rand = rand;
    this.nodes = new ArrayList<GamePiece>();
    this.mst = new ArrayList<Edge>();
    this.board = this.makeBoard();
    this.generateEdge();
    this.kruskal();
    this.boardK();
    this.randomize();
    this.counter = 0;
    this.win = false;
  }

  LightEmAll(int width, int height) {
    this.width = width; // cols
    this.height = height; // rows
    this.rand = new Random(20);
    this.nodes = new ArrayList<GamePiece>();
    this.mst = new ArrayList<Edge>();
    this.board = this.makeBoard();
    this.generateEdge();
    this.win = false;
  }

  // displays the board on the screen
  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(500, 500);
    for (int c = 0; c < this.width; c++) {
      for (int r = 0; r < this.height; r++) {
        GamePiece tile = this.board.get(c).get(r);
        if (tile.powerStation) {
          ws.placeImageXY(tile.tileImage(60, 5, Color.YELLOW, tile.powerStation), c * 60 + 30,
              r * 60 + 30);
        }
        else if (tile.powered) {
          ws.placeImageXY(tile.tileImage(60, 5, Color.YELLOW, tile.powerStation), c * 60 + 30,
              r * 60 + 30);
        }
        else {
          ws.placeImageXY(tile.tileImage(60, 5, Color.LIGHT_GRAY, tile.powerStation), c * 60 + 30,
              r * 60 + 30);
        }
      }
    }
    if (this.win) {
      ws.placeImageXY(new TextImage("You Win ♡⸜(˶˃ ᵕ ˂˶)⸝♡", 30, Color.white), this.width * 6 + 150,
          this.height * 6 + 150);
      ws.placeImageXY(new TextImage("Click a to restart!", 30, Color.white), this.width * 6 + 150,
          this.height * 6 + 180);
    }
    ws.placeImageXY(new TextImage("Click Count: " + this.numClicks, 25, Color.black),
        width * 20 / 2 + 20, height * 20 + 310);
    ws.placeImageXY(new TextImage("⏱︎", 30, Color.red), width * 20 + 170, height * 20 + 310);
    ws.placeImageXY(
        new OverlayImage(new TextImage(this.counter + "", 25, Color.red),
            new RectangleImage(60, 40, OutlineMode.SOLID, Color.BLACK)),
        width * 20 + 230, height * 20 + 310);
    this.propagateLight();
    return ws;
  }

  // on Tick for the clock
  public void onTick() {
    if (!this.win) {
      this.counter += 1;
    }
  }

  // creates the board
  public ArrayList<ArrayList<GamePiece>> makeBoard() {
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> nodes = new ArrayList<GamePiece>();
    for (int c = 0; c < this.width; c++) {
      ArrayList<GamePiece> row = new ArrayList<GamePiece>();
      for (int r = 0; r < this.height; r++) {
        GamePiece gc = new GamePiece(c, r);
        row.add(gc);
        if (c == 0 && r == 0) { // origin point
          gc.powerStation = true;
        }
        nodes.add(gc);
      }
      board.add(row);
    }
    this.nodes = nodes;
    return board;
  }

  // when the user clicks on a tile, the wire rotates in a clockwise direction
  // also, the light propagates through the wires if the connection is valid
  public void onMouseClicked(Posn pos) {
    int tileDim = 60;
    int c = Math.floorDiv(pos.x, tileDim);
    int r = Math.floorDiv(pos.y, tileDim);
    // if the user clicks a location outside the board constraints
    if (c > this.width - 1 || r > this.height - 1) {
      return;
    }
    GamePiece gcPressed = this.board.get(c).get(r);
    gcPressed.rotate();
    this.numClicks += 1;
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        GamePiece g = this.board.get(i).get(j);
        if (!g.powerStation) {
          g.powered = false;
        }
      }
    }
    this.propagateLight();
  }

  // when the user presses on the arrow keys, the power station moves in that
  // direction
  // as long as it is valid
  public void onKeyEvent(String key) {
    GamePiece tile = this.board.get(this.powerCol).get(this.powerRow);
    if (key.equals("a")) {
      this.nodes = new ArrayList<GamePiece>();
      this.mst = new ArrayList<Edge>();
      this.board = this.makeBoard();
      this.generateEdge();
      this.kruskal();
      this.boardK();
      this.randomize();
      this.counter = 0;
      this.numClicks = 0;
      this.win = false;
    }
    if (key.equals("left") && this.powerCol > 0) {
      GamePiece gc = this.board.get(this.powerCol - 1).get(this.powerRow);
      if (gc.right && tile.left) {
        tile.powerStation = false;
        this.powerCol -= 1;
        gc.powerStation = true;
        gc.tileImage(60, 5, Color.YELLOW, gc.powerStation);
      }
    }
    else if (key.equals("right") && this.powerCol < this.width - 1) {
      GamePiece gc = this.board.get(this.powerCol + 1).get(this.powerRow);
      if (gc.left && tile.right) {
        tile.powerStation = false;
        this.powerCol += 1;
        gc.powerStation = true;
        gc.tileImage(60, 5, Color.YELLOW, gc.powerStation);
      }
    }
    else if (key.equals("up") && this.powerRow > 0) {
      GamePiece gc = this.board.get(this.powerCol).get(this.powerRow - 1);
      if (gc.bottom && tile.top) {
        tile.powerStation = false;
        this.powerRow -= 1;
        gc.powerStation = true;
        gc.tileImage(60, 5, Color.YELLOW, gc.powerStation);
      }
    }
    else if (key.equals("down") && this.powerRow < this.height - 1) {
      GamePiece gc = this.board.get(this.powerCol).get(this.powerRow + 1);
      if (gc.top && tile.bottom) {
        tile.powerStation = false;
        this.powerRow += 1;
        gc.powerStation = true;
        gc.tileImage(60, 5, Color.YELLOW, gc.powerStation);
      }
    }
    else {
      return;
    }
  }

  // kruskal algorithm
  public ArrayList<Edge> kruskal() {
    HashMap<GamePiece, GamePiece> a = new HashMap<GamePiece, GamePiece>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = new ArrayList<Edge>(); // all edges in graph, sorted by edge weights
    for (GamePiece gc : this.nodes) {
      a.put(gc, gc);
    }
    worklist.addAll(mst);
    // find
    while (worklist.size() > 0) {
      Edge cur = worklist.remove(0);
      GamePiece g1 = find(a, cur.fromNode);
      GamePiece g2 = find(a, cur.toNode);
      if (g1.checkEquality(g2)) {
      }
      // union
      else {
        edgesInTree.add(cur);
        union(a, find(a, cur.fromNode), find(a, cur.toNode));
      }
    }
    this.mst = edgesInTree;
    return mst;
  }

  // each time the user interacts with the board, bfs is run to check if the
  // light can propagate into other tiles on the board
  public void propagateLight() {
    int numTiles = this.width * this.height;
    ArrayList<GamePiece> worklist = new ArrayList<GamePiece>();
    ArrayList<GamePiece> seen = new ArrayList<GamePiece>();
    GamePiece s = this.board.get(this.powerCol).get(this.powerRow);
    worklist.add(s);
    while (!worklist.isEmpty()) {
      GamePiece next = worklist.remove(0);
      if (next.powered || next.powerStation) {
        next.tileImage(60, 5, Color.YELLOW, next.powerStation);
      }
      if (seen.contains(next)) {
        // do nothing
      }
      else {
        if (next.left && next.col > 0) {
          GamePiece neighbor = this.board.get(next.col - 1).get(next.row);
          if (neighbor.right) {
            neighbor.powered = true;
            worklist.add(neighbor);
          }
        }
        if (next.right && next.col < this.width - 1) {
          GamePiece neighbor = this.board.get(next.col + 1).get(next.row);
          if (neighbor.left) {
            neighbor.powered = true;
            worklist.add(neighbor);
          }
        }
        if (next.top && next.row > 0) {
          GamePiece neighbor = this.board.get(next.col).get(next.row - 1);
          if (neighbor.bottom) {
            neighbor.powered = true;
            worklist.add(neighbor);
          }
        }
        if (next.bottom && next.row < this.height - 1) {
          GamePiece neighbor = this.board.get(next.col).get(next.row + 1);
          if (neighbor.top) {
            neighbor.powered = true;
            worklist.add(neighbor);
          }
        }
        seen.add(next);
      }
    }
    if (seen.size() == numTiles) {
      this.win = true;
    }
  }

  // to randomly rotate tiles at the start of the game
  public void randomize() {
    int random = this.rand.nextInt(4);
    for (int w = 0; w < width; w++) {
      for (int h = 0; h < height; h++) {
        for (int r = 0; r < random; r++) {
          this.board.get(w).get(h).rotate();
        }
        random = this.rand.nextInt(4);
      }
    }
  }

  // generates edges to represent each tile and generates a random edge weight
  // value
  // finally, sorts all of the edge weights from lowest to greatest value
  void generateEdge() {
    int val = height * width;
    for (int w = 0; w < this.width; w++) {
      for (int h = 0; h < this.height; h++) {
        GamePiece tile = board.get(w).get(h);
        if (h < height - 1) {
          Edge e = new Edge(tile, board.get(w).get(h + 1), this.rand.nextInt(val));
          mst.add(e);
        }
        if (w < width - 1) {
          Edge e = new Edge(tile, board.get(w + 1).get(h), this.rand.nextInt(val));
          mst.add(e);
        }
      }
    }
    mst.sort(new CompareEdge());
  }

  // for all the edges in the mst, set the gamepiece wire values depending on
  // which ones are connected
  public void boardK() {
    // loops through every row
    for (Edge e : this.mst) {
      GamePiece side = e.fromNode;
      GamePiece side2 = e.toNode;
      if (side.row < side2.row && side.col == side2.col) {
        side.bottom = true;
        side2.top = true;
      }
      if (side.row > side2.row && side.col == side2.col) {
        side.top = true;
        side2.bottom = true;
      }
      if (side.row == side2.row && side.col < side2.col) {
        side.right = true;
        side2.left = true;
      }
      if (side.row == side2.row && side.col > side2.col) {
        side.left = true;
        side2.right = true;
      }
    }
  }

  // finds the given GamePiece in the hashmap and returns the value associated
  // with it
  public GamePiece find(HashMap<GamePiece, GamePiece> map, GamePiece from) {
    GamePiece fromEdge = map.get(from);
    if (from == (fromEdge)) {
      return fromEdge;
    }
    else {
      return find(map, fromEdge);
    }
  }

  // combines the given GamePiece edges into one tree in the HashMap
  void union(HashMap<GamePiece, GamePiece> map, GamePiece fromEdge, GamePiece toEdge) {
    map.put(find(map, fromEdge), find(map, toEdge));
  }
}

// represents an Edge
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  // Edge constructor
  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }
}

// represents a GamePiece
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;

  // GamePiece constructor
  GamePiece(int row, int col) {
    this.row = col; // modified to match rest of code
    this.col = row;
    this.left = false;
    this.right = false;
    this.top = false;
    this.bottom = false;
    this.powerStation = false;
    this.powered = false;
  }

  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation, boolean powered) {
    this.row = col; // modified to match rest of code
    this.col = row;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
  }

  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the
  // power station
  //
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that
    // can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);
    if (this.top)
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    if (this.right)
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    if (this.bottom)
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    if (this.left)
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    return image;
  }

  // when called, rotates the tile in a clockwise direction
  public void rotate() {
    boolean var = this.left;
    this.left = this.bottom;
    this.bottom = this.right;
    this.right = this.top;
    this.top = var;
  }

  // compares two gamepieces to see if they are the same object
  public boolean checkEquality(GamePiece that) {
    return this.row == that.row && this.col == that.col && this.left == that.left
        && this.right == that.right && this.top == that.top && this.bottom == that.bottom
        && this.powerStation == that.powerStation && this.powered == that.powered;
  }
}

class ExamplesGame {
  // mini2 board (2 x 2)
  LightEmAll mini2;
  ArrayList<ArrayList<GamePiece>> mini2Board;
  ArrayList<ArrayList<GamePiece>> mini2SBoard;
  ArrayList<ArrayList<GamePiece>> mini2MBoard;
  ArrayList<GamePiece> mini2R1;
  ArrayList<GamePiece> mini2R2;
  GamePiece m2r1gp1;
  GamePiece m2r1gp2;
  GamePiece m2r2gp1;
  GamePiece m2r2gp2;
  // kruskal ver
  ArrayList<ArrayList<GamePiece>> kmini2Board;
  ArrayList<GamePiece> kmini2R1;
  ArrayList<GamePiece> kmini2R2;
  GamePiece km2r1gp1;
  GamePiece km2r1gp2;
  GamePiece km2r2gp1;
  GamePiece km2r2gp2;
  // mini3 board (3 x 3)
  LightEmAll mini3;
  ArrayList<ArrayList<GamePiece>> mini3Board;
  ArrayList<GamePiece> mini3R1;
  ArrayList<GamePiece> mini3R2;
  ArrayList<GamePiece> mini3R3;
  GamePiece m3r1gp1;
  GamePiece m3r1gp2;
  GamePiece m3r1gp3;
  GamePiece m3r2gp1;
  GamePiece m3r2gp2;
  GamePiece m3r2gp3;
  GamePiece m3r3gp1;
  GamePiece m3r3gp2;
  GamePiece m3r3gp3;
  ArrayList<ArrayList<GamePiece>> kmini3Board;
  ArrayList<GamePiece> kmini3R1;
  ArrayList<GamePiece> kmini3R2;
  ArrayList<GamePiece> kmini3R3;
  GamePiece km3r1gp1;
  GamePiece km3r1gp2;
  GamePiece km3r1gp3;
  GamePiece km3r2gp1;
  GamePiece km3r2gp2;
  GamePiece km3r2gp3;
  GamePiece km3r3gp1;
  GamePiece km3r3gp2;
  GamePiece km3r3gp3;
  // mini32 board (3 x 2)
  LightEmAll mini32;
  ArrayList<ArrayList<GamePiece>> mini32Board;
  ArrayList<GamePiece> mini32R1;
  ArrayList<GamePiece> mini32R2;
  ArrayList<GamePiece> mini32R3;
  GamePiece m32r1gp1;
  GamePiece m32r1gp2;
  GamePiece m32r2gp1;
  GamePiece m32r2gp2;
  GamePiece m32r3gp1;
  GamePiece m32r3gp2;
  // mini32 board (3 x 2)2
  LightEmAll mini321;
  ArrayList<ArrayList<GamePiece>> mini32Board1;
  ArrayList<GamePiece> mini32R11;
  ArrayList<GamePiece> mini32R21;
  ArrayList<GamePiece> mini32R31;
  GamePiece m32r1gp11;
  GamePiece m32r1gp21;
  GamePiece m32r2gp11;
  GamePiece m32r2gp21;
  GamePiece m32r3gp11;
  GamePiece m32r3gp21;
  ArrayList<ArrayList<GamePiece>> kmini32Board;
  ArrayList<GamePiece> kmini32R1;
  ArrayList<GamePiece> kmini32R2;
  ArrayList<GamePiece> kmini32R3;
  GamePiece km32r1gp1;
  GamePiece km32r1gp2;
  GamePiece km32r2gp1;
  GamePiece km32r2gp2;
  GamePiece km32r3gp1;
  GamePiece km32r3gp2;
  ArrayList<ArrayList<GamePiece>> a3;
  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;
  Edge e5;
  Edge e6;
  Edge e7;
  Edge e8;
  Edge e9;
  Edge e10;
  Edge e11;
  Edge e12;
  GamePiece g1;
  GamePiece g2;
  GamePiece g3;
  GamePiece g4;
  ArrayList<Edge> a1;
  ArrayList<Edge> a11;
  CompareEdge compareEdge;

  // TESTS
  void testBigBang(Tester t) {
    LightEmAll world = new LightEmAll(7, 7, new Random());
    int worldWidth = 60 * 7;
    int worldHeight = 60 * 7 + 60;
    double tickRate = 1;
    world.bigBang(worldWidth, worldHeight, tickRate);
  }

  void initConditions() {
    compareEdge = new CompareEdge();
    // Mini 2 x 2 board
    // +---+---+
    // | | | | |
    // +---+---+
    // |-P-|-| |
    // +---+---+
    mini2 = new LightEmAll(2, 2, new Random(20));
    m2r1gp1 = new GamePiece(0, 0, false, false, false, true, false, false);
    m2r1gp2 = new GamePiece(0, 1, true, true, true, true, true, false);
    mini2R1 = new ArrayList<GamePiece>(Arrays.asList(m2r1gp1, m2r1gp2));
    m2r2gp1 = new GamePiece(1, 0, false, false, false, true, false, false);
    m2r2gp2 = new GamePiece(1, 1, true, false, true, true, false, false);
    mini2R2 = new ArrayList<GamePiece>(Arrays.asList(m2r2gp1, m2r2gp2));
    mini2Board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(mini2R1, mini2R2));
    // mini 2 x 2 kruskal ver
    km2r1gp1 = new GamePiece(0, 0, false, false, false, false, true, false);
    km2r1gp2 = new GamePiece(0, 1, false, false, false, false, false, false);
    kmini2R1 = new ArrayList<GamePiece>(Arrays.asList(km2r1gp1, km2r1gp2));
    km2r2gp1 = new GamePiece(1, 0, false, false, false, false, false, false);
    km2r2gp2 = new GamePiece(1, 1, false, false, false, false, false, false);
    kmini2R2 = new ArrayList<GamePiece>(Arrays.asList(km2r2gp1, km2r2gp2));
    kmini2Board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(kmini2R1, kmini2R2));
    g1 = new GamePiece(1, 0, true, false, false, true, false, false);
    g2 = new GamePiece(1, 1, true, false, false, false, false, false);
    g3 = new GamePiece(0, 0, false, true, false, true, true, false);
    g4 = new GamePiece(0, 1, false, true, true, false, false, false);
    e1 = new Edge(g1, g2, 0);
    e2 = new Edge(g1, g2, 0);
    e3 = new Edge(g2, g1, 1);
    e4 = new Edge(g3, g1, 2);
    e5 = new Edge(g1, g3, 2);
    e6 = new Edge(g2, g1, 2);
    e7 = new Edge(g3, g1, 2);
    e8 = new Edge(g1, g3, 2);
    e9 = new Edge(g4, g3, 2);
    e10 = new Edge(g2, g4, 2);
    e11 = new Edge(g4, g3, 3);
    e12 = new Edge(g2, g4, 3);
    a1 = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11));
    // Mini 3 x 3 board
    // +---+---+---+
    // | | | | | | |
    // +---+---+---+
    // | |-|-P-|-| |
    // +---+---+---+
    // | | | | | | |
    // +---+---+---+
    mini3 = new LightEmAll(3, 3, new Random(20));
    m3r1gp1 = new GamePiece(0, 0, false, false, false, true, false, false);
    m3r1gp2 = new GamePiece(0, 1, false, true, true, true, false, false);
    m3r1gp3 = new GamePiece(0, 2, false, false, true, false, false, false);
    mini3R1 = new ArrayList<GamePiece>(Arrays.asList(m3r1gp1, m3r1gp2, m3r1gp3));
    m3r2gp1 = new GamePiece(1, 0, false, false, false, true, false, false);
    m3r2gp2 = new GamePiece(1, 1, true, true, true, true, true, false);
    m3r2gp3 = new GamePiece(1, 2, false, false, true, false, false, false);
    mini3R2 = new ArrayList<GamePiece>(Arrays.asList(m3r2gp1, m3r2gp2, m3r2gp3));
    m3r3gp1 = new GamePiece(2, 0, false, false, false, true, false, false);
    m3r3gp2 = new GamePiece(2, 1, true, false, true, true, false, false);
    m3r3gp3 = new GamePiece(2, 2, false, false, true, false, false, false);
    mini3R3 = new ArrayList<GamePiece>(Arrays.asList(m3r3gp1, m3r3gp2, m3r3gp3));
    mini3Board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(mini3R1, mini3R2, mini3R3));
    // kruskal ver
    km3r1gp1 = new GamePiece(0, 0, false, false, false, false, true, false);
    km3r1gp2 = new GamePiece(0, 1, false, false, false, false, false, false);
    km3r1gp3 = new GamePiece(0, 2, false, false, false, false, false, false);
    kmini3R1 = new ArrayList<GamePiece>(Arrays.asList(km3r1gp1, km3r1gp2, km3r1gp3));
    km3r2gp1 = new GamePiece(1, 0, false, false, false, false, false, false);
    km3r2gp2 = new GamePiece(1, 1, false, false, false, false, false, false);
    km3r2gp3 = new GamePiece(1, 2, false, false, false, false, false, false);
    kmini3R2 = new ArrayList<GamePiece>(Arrays.asList(km3r2gp1, km3r2gp2, km3r2gp3));
    km3r3gp1 = new GamePiece(2, 0, false, false, false, false, false, false);
    km3r3gp2 = new GamePiece(2, 1, false, false, false, false, false, false);
    km3r3gp3 = new GamePiece(2, 2, false, false, false, false, false, false);
    kmini3R3 = new ArrayList<GamePiece>(Arrays.asList(km3r3gp1, km3r3gp2, km3r3gp3));
    kmini3Board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(kmini3R1, kmini3R2, kmini3R3));
    // 2 x 3 board
    // +---+---+---+
    // | | | | | | |
    // +---+---+---+
    // | |-|-|-|-| |
    // +---+---+---+
    mini32 = new LightEmAll(3, 2, new Random(20));
    m32r1gp1 = new GamePiece(0, 0, false, false, false, true, false, false);
    m32r1gp2 = new GamePiece(0, 1, false, true, true, true, false, false);
    mini32R1 = new ArrayList<GamePiece>(Arrays.asList(m32r1gp1, m32r1gp2));
    m32r2gp1 = new GamePiece(1, 0, false, false, false, true, false, false);
    m32r2gp2 = new GamePiece(1, 1, true, true, true, true, true, false);
    mini32R2 = new ArrayList<GamePiece>(Arrays.asList(m32r2gp1, m32r2gp2));
    m32r3gp1 = new GamePiece(2, 0, false, false, false, true, false, false);
    m32r3gp2 = new GamePiece(2, 1, true, false, true, true, false, false);
    mini32R3 = new ArrayList<GamePiece>(Arrays.asList(m32r3gp1, m32r3gp2));
    mini32Board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(mini32R1, mini32R2, mini32R3));
    km32r1gp1 = new GamePiece(0, 0, false, false, false, false, true, false);
    km32r1gp2 = new GamePiece(0, 1, false, false, false, false, false, false);
    kmini32R1 = new ArrayList<GamePiece>(Arrays.asList(km32r1gp1, km32r1gp2));
    km32r2gp1 = new GamePiece(1, 0, false, false, false, false, false, false);
    km32r2gp2 = new GamePiece(1, 1, false, false, false, false, false, false);
    kmini32R2 = new ArrayList<GamePiece>(Arrays.asList(km32r2gp1, km32r2gp2));
    km32r3gp1 = new GamePiece(2, 0, false, false, false, false, false, false);
    km32r3gp2 = new GamePiece(2, 1, false, false, false, false, false, false);
    kmini32R3 = new ArrayList<GamePiece>(Arrays.asList(km32r3gp1, km32r3gp2));
    kmini32Board = new ArrayList<ArrayList<GamePiece>>(
        Arrays.asList(kmini32R1, kmini32R2, kmini32R3));
    mini32 = new LightEmAll(3, 2, new Random(20));
    m32r1gp1 = new GamePiece(0, 0, false, false, false, true, false, false);
    m32r1gp2 = new GamePiece(0, 1, false, true, true, true, false, false);
    mini32R1 = new ArrayList<GamePiece>(Arrays.asList(m32r1gp1, m32r1gp2));
    m32r2gp1 = new GamePiece(1, 0, false, false, false, true, false, false);
    m32r2gp2 = new GamePiece(1, 1, true, true, true, true, true, false);
    mini32R2 = new ArrayList<GamePiece>(Arrays.asList(m32r2gp1, m32r2gp2));
    m32r3gp1 = new GamePiece(2, 0, false, false, false, true, false, false);
    m32r3gp2 = new GamePiece(2, 1, true, false, true, true, false, false);
    mini32R3 = new ArrayList<GamePiece>(Arrays.asList(m32r3gp1, m32r3gp2));
    mini32Board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(mini32R1, mini32R2, mini32R3));
    // 2x3 2
    mini321 = new LightEmAll(3, 2, new Random(20));
    m32r1gp11 = new GamePiece(0, 0, false, false, false, true, false, false);
    m32r1gp21 = new GamePiece(0, 1, false, true, true, true, false, false);
    mini32R11 = new ArrayList<GamePiece>(Arrays.asList(m32r1gp11, m32r1gp21));
    m32r2gp11 = new GamePiece(1, 0, false, false, false, true, false, false);
    m32r2gp21 = new GamePiece(1, 1, true, true, true, true, true, true);
    mini32R21 = new ArrayList<GamePiece>(Arrays.asList(m32r2gp11, m32r2gp21));
    m32r3gp11 = new GamePiece(2, 0, false, false, false, true, false, true);
    m32r3gp21 = new GamePiece(2, 1, true, false, true, true, false, true);
    mini32R31 = new ArrayList<GamePiece>(Arrays.asList(m32r3gp11, m32r3gp21));
    mini32Board1 = new ArrayList<ArrayList<GamePiece>>(
        Arrays.asList(mini32R11, mini32R21, mini32R31));
  }

  // LIGHTEMALL TESTS
  // tests that the board is drawn - powerStation placed at center
  void testMakeScene(Tester t) {
    this.initConditions();
    WorldScene ws = new WorldScene(500, 500);
    // randomize the sample board before making the original scene
    this.mini32.randomize();
    ws.placeImageXY(this.mini32.board.get(0).get(0).tileImage(60, 5, Color.YELLOW, true), 30, 30);
    ws.placeImageXY(this.mini32.board.get(0).get(1).tileImage(60, 5, Color.LIGHT_GRAY, false), 30,
        90);
    ws.placeImageXY(this.mini32.board.get(1).get(0).tileImage(60, 5, Color.LIGHT_GRAY, false), 90,
        30);
    ws.placeImageXY(this.mini32.board.get(1).get(1).tileImage(60, 5, Color.LIGHT_GRAY, false), 90,
        90);
    ws.placeImageXY(this.mini32.board.get(2).get(0).tileImage(60, 5, Color.LIGHT_GRAY, false), 150,
        30);
    ws.placeImageXY(this.mini32.board.get(2).get(1).tileImage(60, 5, Color.LIGHT_GRAY, false), 150,
        90);
    ws.placeImageXY(new TextImage("Click Count: 0", 25, Color.black), 3 * 20 / 2 + 20,
        2 * 20 + 310);
    ws.placeImageXY(new TextImage("⏱︎", 30, Color.red), 3 * 20 + 170, 2 * 20 + 310);
    ws.placeImageXY(
        new OverlayImage(new TextImage("0", 25, Color.red),
            new RectangleImage(60, 40, OutlineMode.SOLID, Color.BLACK)),
        3 * 20 + 230, 2 * 20 + 310);
    t.checkExpect(this.mini32.makeScene(), ws);
  }

  // constructs a base level-board for the game
  void testMakeBoard(Tester t) {
    this.initConditions();
    t.checkExpect(this.mini2.makeBoard(), this.kmini2Board);
    t.checkExpect(this.mini3.makeBoard(), this.kmini3Board);
    t.checkExpect(this.mini32.makeBoard(), this.kmini32Board);
  }

  void testGenerateEdge(Tester t) {
    this.initConditions();
    this.mini2.generateEdge();
    GamePiece c1 = this.mini2.board.get(0).get(0); // 6
    GamePiece c2 = this.mini2.board.get(1).get(0); // 7
    GamePiece c3 = this.mini2.board.get(0).get(1); // 3
    GamePiece c4 = this.mini2.board.get(1).get(1); // 4
    Edge e13 = new Edge(c3, c4, 0);
    Edge e14 = new Edge(c1, c2, 1);
    Edge e15 = new Edge(c3, c4, 1);
    Edge e16 = new Edge(c2, c4, 1);
    Edge e17 = new Edge(c1, c3, 2);
    Edge e18 = new Edge(c1, c2, 2);
    Edge e19 = new Edge(c1, c3, 2);
    a11 = new ArrayList<Edge>(Arrays.asList(e13, e14, e15, e16, e17, e18, e19));
    // c3 = gamepiece;3
    // c4 = gp;4
    // c1 = gp;8
    // c2 = gp;14
    t.checkExpect(this.mini2.mst, a11);
  }

  // tests that the tile that is clicked on is rotated
  void testOnMouseClicked(Tester t) {
    this.initConditions();
    // UNI-DIRECTIONAL WIRE ROTATION
    // +---+---+
    // | | | | |
    // +---+---+
    // |-P-|-| |
    // +---+---+
    // initial orientation
    t.checkExpect(this.mini2.board.get(0).get(0).left, false);
    t.checkExpect(this.mini2.board.get(0).get(0).bottom, false);
    t.checkExpect(this.mini2.board.get(0).get(1).right, false);
    this.mini2.onMouseClicked(new Posn(30, 30));
    // new orientation following mouse click
    t.checkExpect(this.mini2.board.get(0).get(0).left, false);
    t.checkExpect(this.mini2.board.get(0).get(0).bottom, true);
    // new orientation following mouse click
    this.mini2.onMouseClicked(new Posn(30, 30));
    t.checkExpect(this.mini2.board.get(0).get(0).left, true);
    t.checkExpect(this.mini2.board.get(0).get(0).top, false);
    t.checkExpect(this.mini2.board.get(1).get(1).left, false);
    t.checkExpect(this.mini2.board.get(1).get(1).right, true);
    // TRI-DIRECTIONAL WIRE ROTATION
    // +---+---+---+
    // | | | | | | |
    // +---+---+---+
    // | |-|-P-|-| |
    // +---+---+---+
    // | | | | | | |
    // +---+---+---+
    // initial orientation
    t.checkExpect(this.mini3.board.get(2).get(2).left, false);
    t.checkExpect(this.mini3.board.get(2).get(2).right, false);
    t.checkExpect(this.mini3.board.get(2).get(2).top, false);
    t.checkExpect(this.mini3.board.get(2).get(2).bottom, true);
    this.mini3.onMouseClicked(new Posn(90, 90));
    // new orientation following mouse click
    t.checkExpect(this.mini3.board.get(2).get(2).left, false);
    t.checkExpect(this.mini3.board.get(2).get(2).right, false);
    t.checkExpect(this.mini3.board.get(2).get(2).top, false);
    t.checkExpect(this.mini3.board.get(2).get(2).bottom, true);
  }

  // tests that the powerStation is moved in the direction of the
  // key press as long as there is a valid wire adjacent to it
  // in the given direction
  void testOnKeyEvent(Tester t) {
    this.initConditions();
    t.checkExpect(mini2.powerRow, 0);
    t.checkExpect(mini2.powerCol, 0);
    mini2.onKeyEvent("right");
    // moves to the right
    t.checkExpect(mini2.powerCol, 1);
    mini2.onKeyEvent("left");
    // no left
    t.checkExpect(mini2.powerCol, 0);
    mini2.onKeyEvent("up");
    // no up
    t.checkExpect(mini2.powerRow, 0);
    t.checkExpect(mini3.powerRow, 0);
    t.checkExpect(mini3.powerCol, 0);
    mini3.onKeyEvent("bottom");
    t.checkExpect(mini3.powerCol, 0);
    
    LightEmAll mini2n = new LightEmAll(2, 2, new Random(20)); 
    GamePiece c1 = mini2n.board.get(0).get(0); // 2
    GamePiece c2 = mini2n.board.get(0).get(1); // 4
    GamePiece c3 = mini2n.board.get(1).get(0); // 6
    GamePiece c4 = mini2n.board.get(1).get(1); // 7

    ArrayList<GamePiece> a1 = new ArrayList<GamePiece>(Arrays.asList(c1, c2));
    ArrayList<GamePiece> a2 = new ArrayList<GamePiece>(Arrays.asList(c3, c4));
    ArrayList<ArrayList<GamePiece>> a3 = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(a1, a2));
    mini2n.onKeyEvent("a");
    t.checkExpect(mini2n.board, a3);
    
  }

  // checks that a given board and all its tiles are given random orientations
  // tests for the method rotate
  void testRandomize(Tester t) {
    this.initConditions();
    // 2x2
    // before (randomizing a single tile)
    t.checkExpect(this.mini2.board.get(0).get(0).left, false);
    t.checkExpect(this.mini2.board.get(0).get(0).bottom, false);
    t.checkExpect(this.mini2.board.get(0).get(0).top, true);
    t.checkExpect(this.mini2.board.get(0).get(0).right, true);
    this.mini2.randomize();
    // after randomizing
    t.checkExpect(this.mini2.makeBoard().get(0).get(0).left, false);
    t.checkExpect(this.mini2.makeBoard().get(0).get(0).bottom, false);
    t.checkExpect(this.mini2.makeBoard().get(0).get(0).top, false);
    t.checkExpect(this.mini2.makeBoard().get(0).get(0).right, false);
    // 3x3 (randomizing a single tile)
    // before calling
    t.checkExpect(this.mini3.board.get(1).get(0).left, true);
    t.checkExpect(this.mini3.board.get(1).get(0).bottom, false);
    t.checkExpect(this.mini3.board.get(1).get(0).top, false);
    t.checkExpect(this.mini3.board.get(1).get(0).right, true);
    this.mini3.randomize();
    // after randomizing
    t.checkExpect(this.mini3.makeBoard().get(1).get(0).left, false);
    t.checkExpect(this.mini3.makeBoard().get(1).get(0).bottom, false);
    t.checkExpect(this.mini3.makeBoard().get(1).get(0).top, false);
    t.checkExpect(this.mini3.makeBoard().get(1).get(0).right, false);
  }

  // checks that breadth-first search properly identifies wires that are connected
  // to the powerStation and spreads the light to those areas
  void testPropagateLight(Tester t) {
    this.initConditions();
    // 3 x 3 board
    // +---+---+---+
    // | | | -| | |
    // +---+---+---+
    // |-| |-P-| |-|
    // +---+---+---+
    // | | | | | | |
    // +---+---+---+
    // calls the bfs on the initial board where the powerStation is not connected to
    // anything
    this.mini3.propagateLight();
    // checks that light is not spread to invalid neighbors
    t.checkExpect(this.mini3.board.get(1).get(1).powerStation, false);
    t.checkExpect(this.mini3.board.get(0).get(1).powered, false);
    t.checkExpect(this.mini3.board.get(1).get(0).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(2).powered, false);
    t.checkExpect(this.mini3.board.get(1).get(2).powered, false);
    // clicks on the tile on the right so that the light can spread to the right
    // column
    this.mini3.onMouseClicked(new Posn(140, 90));
    this.mini3.onMouseClicked(new Posn(140, 90));
    this.mini3.propagateLight();
    t.checkExpect(this.mini3.board.get(2).get(0).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(1).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(2).powered, false);
    // clicks on the tile on the left so that light can spread to the middle and
    // bottom tiles
    // in the left column
    this.mini3.onMouseClicked(new Posn(30, 90));
    this.mini3.onMouseClicked(new Posn(30, 90));
    this.mini3.propagateLight();
    t.checkExpect(this.mini3.board.get(0).get(1).powered, true);
    t.checkExpect(this.mini3.board.get(0).get(2).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(0).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(1).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(2).powered, false);
    // moves the power station to the bottom left corner of the board
    this.mini3.onKeyEvent("left");
    this.mini3.onKeyEvent("down");
    t.checkExpect(this.mini3.board.get(0).get(2).powerStation, false);
    // clicks on the tile above it (middle-tile of left column) into new orientation
    // where
    // it does not connect with center tile
    this.mini3.onMouseClicked(new Posn(30, 90));
    this.mini3.onMouseClicked(new Posn(30, 90));
    t.checkExpect(this.mini3.board.get(0).get(1).powered, true);
    t.checkExpect(this.mini3.board.get(1).get(1).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(0).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(1).powered, false);
    t.checkExpect(this.mini3.board.get(2).get(2).powered, false);
  }

  // GAMEPIECE TESTS
  // checks that the tile rotates in a clockwise direction when
  // method is called
  void testRotate(Tester t) {
    this.initConditions();
    // half top wire
    // wires before
    t.checkExpect(m2r1gp1.left, false);
    t.checkExpect(m2r1gp1.bottom, true);
    t.checkExpect(m2r1gp1.right, false);
    t.checkExpect(m2r1gp1.top, false);
    m2r1gp1.rotate();
    // wires after
    t.checkExpect(m2r1gp1.left, true);
    t.checkExpect(m2r1gp1.bottom, false);
    t.checkExpect(m2r1gp1.right, false);
    t.checkExpect(m2r1gp1.top, false);
    // half bottom wire
    // wires before
    t.checkExpect(m2r2gp1.left, false);
    t.checkExpect(m2r2gp1.bottom, true);
    t.checkExpect(m2r2gp1.right, false);
    t.checkExpect(m2r2gp1.top, false);
    m2r2gp1.rotate();
    // wires after
    t.checkExpect(m2r2gp1.left, true);
    t.checkExpect(m2r2gp1.bottom, false);
    t.checkExpect(m2r2gp1.right, false);
    t.checkExpect(m2r2gp1.top, false);
    // 3 wire
    // wires before
    t.checkExpect(m2r2gp2.left, true);
    t.checkExpect(m2r2gp2.bottom, true);
    t.checkExpect(m2r2gp2.right, false);
    t.checkExpect(m2r2gp2.top, true);
    m2r2gp2.rotate();
    // wires after
    t.checkExpect(m2r2gp2.left, true);
    t.checkExpect(m2r2gp2.bottom, false);
    t.checkExpect(m2r2gp2.right, true);
    t.checkExpect(m2r2gp2.top, true);
  }

  // tests on click for the clock
  void testOnTick(Tester t) {
    this.initConditions();
    this.mini2.counter = 10;
    t.checkExpect(this.mini2.counter, 10);
    mini2.onTick();
    t.checkExpect(this.mini2.counter, 11);
  }

  // test kruskal algorithm
  void testKruskal(Tester t) {
    this.initConditions();
    LightEmAll mini2n = new LightEmAll(2, 2); // without running kruskal
    t.checkExpect(mini2n.mst.size(), 4);
    GamePiece c1 = mini2n.board.get(0).get(0); // 6
    GamePiece c2 = mini2n.board.get(1).get(0); // 3
    GamePiece c3 = mini2n.board.get(0).get(1); // 8
    GamePiece c4 = mini2n.board.get(1).get(1); // 4
    Edge e13 = new Edge(c3, c4, 0);
    Edge e14 = new Edge(c1, c3, 2);
    Edge e15 = new Edge(c1, c2, 2);
    Edge e16 = new Edge(c2, c4, 3);
    ArrayList<Edge> a2 = new ArrayList<Edge>(Arrays.asList(e13, e14, e15, e16));
    t.checkExpect(mini2n.mst, a2);
    mini2n.kruskal();
    ArrayList<Edge> a3 = new ArrayList<Edge>(Arrays.asList(e13, e14, e15));
    // after running kruskal on the mst
    t.checkExpect(mini2n.mst.size(), 3);
    t.checkExpect(mini2n.mst, a3);
  }

  // tests boardK
  void testBoardk(Tester t) {
    initConditions();
    this.mini2.boardK();
    GamePiece c3 = this.mini2.board.get(0).get(0);
    GamePiece c6 = this.mini2.board.get(1).get(0);
    GamePiece c4 = this.mini2.board.get(0).get(1);
    GamePiece c7 = this.mini2.board.get(1).get(1);
    ArrayList<GamePiece> f1 = new ArrayList<GamePiece>(Arrays.asList(c3, c4));
    ArrayList<GamePiece> f2 = new ArrayList<GamePiece>(Arrays.asList(c6, c7));
    ArrayList<ArrayList<GamePiece>> f3 = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(f1, f2));
    t.checkExpect(this.mini2.board, f3);
  }

  // test find and union
  void testUnionFind(Tester t) {
    this.initConditions();
    LightEmAll w1 = new LightEmAll(2, 2, new Random());
    HashMap<GamePiece, GamePiece> hash = new HashMap<GamePiece, GamePiece>();
    GamePiece g1 = new GamePiece(0, 0);
    GamePiece g2 = new GamePiece(0, 1);
    GamePiece g3 = new GamePiece(1, 1);
    GamePiece g4 = new GamePiece(3, 3);
    hash.put(g2, g2);
    hash.put(g1, g1);
    t.checkExpect(w1.find(hash, g1), g1);
    hash.put(g4, g4);
    t.checkExpect(w1.find(hash, g4), g4);
    hash.put(g3, g2);
    t.checkExpect(w1.find(hash, g3), g2);
    w1.union(hash, g2, g1);
    t.checkExpect(w1.find(hash, g2), g1);
    t.checkExpect(w1.find(hash, g3), g1);
    w1.union(hash, g4, g3);
    t.checkExpect(w1.find(hash, g4), g1);
  }

  // tests comparator
  void testComparator(Tester t) {
    this.initConditions();
    t.checkExpect(this.compareEdge.compare(this.e1, this.e3), -1);
    t.checkExpect(this.compareEdge.compare(this.e5, this.e8), 0);
    t.checkExpect(this.compareEdge.compare(this.e10, this.e3), 1);
  }

  // tests gamepiece equality check
  void testEqually(Tester t) {
    initConditions();
    t.checkExpect(this.m2r1gp1.checkEquality(g1), false);
    t.checkExpect(this.km32r1gp2.checkEquality(g1), false);
    t.checkExpect(this.km32r1gp2.checkEquality(km32r1gp2), true);
  }
}
