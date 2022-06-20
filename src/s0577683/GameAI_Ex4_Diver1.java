package s0577683;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DivingAction;
import lenz.htw.ai4g.ai.Info;
import lenz.htw.ai4g.ai.PlayerAction;
import lenz.htw.ai4g.ai.ShoppingAction;
import lenz.htw.ai4g.ai.ShoppingItem;

public class GameAI_Ex4_Diver1 extends AI {
	private final int CELL_SIZE = 15;
	
	//Complex Types
	private ArrayList<Node> pathToFollow;
	private ArrayList<ShoppingItem> boughtItems;
	private Path2D[] obstacleArray;
	private Point nearestPearl;
	private Point nearestBottle;
	private Point nextAim;
	private Point shipPosition;
	private Point[] pearlArray;
	private Point[] bottleArray;
	
	//Primitive Types
	private boolean isDiving;
	private boolean isShopping;
	private boolean haveSurfacePearls;
	private int currentScore;
	private int currentMoney;
	private int sceneHeight;
	private int sceneWidth;
	private float playerDirection;
	
	public GameAI_Ex4_Diver1 (Info info) {
		super(info);
		
		enlistForTournament(577683, 577423);
		
		//Get initial values 
		init();

		makeDecision();
	}

	@Override
	public String getName() {
		return "Kinilau-A-Mano";
	}

	@Override
	public Color getPrimaryColor() {
		return Color.GRAY;
	}

	@Override
	public Color getSecondaryColor() {
		return Color.CYAN;
	}

	@Override
	public PlayerAction update() {
		if (info.getMoney() != currentMoney || info.getScore() != currentScore) {
			makeDecision();
		}
		

		if (Math.abs(info.getX() - shipPosition.x) < CELL_SIZE
				&& Math.abs(info.getY() - shipPosition.y) < CELL_SIZE 
				&& currentMoney != 0){
			currentMoney = 0;
			makeDecision();
			isShopping = false;
			return new ShoppingAction(buyItem());
		}
		
		// IF you are following a path, do this
		if (!pathToFollow.isEmpty()) {
			if (nextAim.distance(info.getX(), info.getY()) < CELL_SIZE) {
				Node node = pathToFollow.remove(0);
				nextAim.x = node.getX() * CELL_SIZE + CELL_SIZE/2;
				nextAim.y = node.getY() * CELL_SIZE + CELL_SIZE/2;
				
				playerDirection = calculateDirectionToPoint(nextAim);
			}// Arrived at final node
		
		} else {
			makeDecision();
		}
		
		/*else { 
			// Swim to pearl
			playerDirection = calculateDirectionToPoint(nearestPearl);
			
			if (info.getScore() != currentScore) { // Pearl is collected
				currentScore = info.getScore();
				// If pearl is collected by chance, but path is still not done,
				//remove this pearl instead and continue your way to the next
				removeNearestPoint(pearlArray);
				nearestPearl = findNearestPoint(pearlArray);
				
				// If you still have air, and the next one is really close,
				// Use Pathfinding to get it while you are there
				if (nearestPearl.distance(info.getX(), info.getY()) < 100 && info.getAir() > info.getMaxAir()/2) {
					pathToFollow = calculateDijkstraPath(new Point((int)info.getX(), (int)info.getY()), nearestPearl);
					nextAim.x = pathToFollow.get(0).getX() * CELL_SIZE + CELL_SIZE/2;
					nextAim.y = pathToFollow.get(0).getY() * CELL_SIZE + CELL_SIZE/2;
					playerDirection = calculateDirectionToPoint(nextAim);
				} else {
					// Otherwise play safe and resurface straight up
					isDiving=false;
					nearestPearl = new Point((int) info.getX(), 0);
					
					//Check if there are collisions on the way up
					Point first = calculateCollisionPoint((int) info.getX(), (int) info.getY());
					if (first != null) {
						// avoid the obstacle with "pathfinding"
						pathToFollow = calculateDijkstraPath(first, nearestPearl);
						nextAim.x = first.x;
						nextAim.y = first.y;
						playerDirection = calculateDirectionToPoint(nextAim);
					}
				}
			}
			// We collected a pearl and there's no other nearby,
			// So resurface until you have full air!
			else if (!isDiving && info.getAir() == info.getMaxAir()) {
				// Find your next aim
				nearestPearl = findNearestPoint(pearlArray);
				
				// Check if there is a collision on the way to it
				Point first = calculateCollisionPoint(nearestPearl.x, nearestPearl.y);
				
				// If it's a straight line
				if (first == null) {
					// Set it as a goal
					pathToFollow.add(0, new Node(nearestPearl.x / CELL_SIZE, nearestPearl.y / CELL_SIZE));
					// But before that, swim to the surface point right above it
					nextAim.x = nearestPearl.x;
					nextAim.y = 0;
					// Swim straight down!
					playerDirection = calculateDirectionToPoint(nextAim);
					
				} else {
					// If there are obstacles, use "pathfinding"
					pathToFollow = calculateDijkstraPath(new Point((int)info.getX(), (int)info.getY()), nearestPearl);
					nextAim.x = pathToFollow.get(0).getX() * CELL_SIZE + CELL_SIZE/2;
					nextAim.y = pathToFollow.get(0).getY() * CELL_SIZE + CELL_SIZE/2;
					playerDirection = calculateDirectionToPoint(nextAim);
				}
				// We are done with "surfacing" and want to dive again
				isDiving=true;
			}
		}
		*/
			
		return new DivingAction(info.getMaxAcceleration(), playerDirection);
	}
	
	
	
	//---------------------Helper Methods-----------------------------
	
	
	
	private ArrayList<Node> calculateDijkstraPath(Point from, Point to) {
		Node[][] nodesMatrix = calculateIntersections(sceneWidth, sceneHeight);
		ArrayList<Node> visitNext = new ArrayList<>();

		Node begin = nodesMatrix[from.x / CELL_SIZE][from.y / CELL_SIZE];
		Node end = nodesMatrix[(to.x / CELL_SIZE)][(to.y / CELL_SIZE)];

		begin.setDistance(0);
		visitNext.add(begin);
		end.setVisited(false);
		
		Node visiting = visitNext.remove(0);
		
		int x = visiting.getX();
		int y = visiting.getY();
		
		while(!end.isVisited()) {
			for (int i = -1; i <= 1; i++) {
				if (x + i < 0 || x + i >= nodesMatrix.length) {
					continue;
				}
				for (int j = -1; j <= 1; j++) {
					if (y + j < 0 || y + j >= nodesMatrix[0].length || (i == 0 && j == 0)) {
						continue;
					}
					if (!nodesMatrix[x + i][y + j].isVisited()) {
						if (!visitNext.contains(nodesMatrix[x + i][y + j])) {
							visitNext.add(visitNext.size(), nodesMatrix[x + i][y + j]);
						}
					}
					if (nodesMatrix[x + i][y + j].getDistance() > visiting.getDistance()+1) {
						nodesMatrix[x + i][y + j].setDistance(visiting.getDistance()+1);
						nodesMatrix[x + i][y + j].setPrevious(visiting);
					}
				}
			}
			nodesMatrix[x][y].setVisited(true);
			// Get the next node to check
			if (!visitNext.isEmpty()) {
				visiting=visitNext.remove(0);
				x = visiting.getX();
				y = visiting.getY();
			}
		}
		
		visitNext.clear();
		ArrayList<Node> temp = new ArrayList<>();
		temp.add(end.getPrevious()); // Skip last, same as pearl, obsolete
		
		while(temp.get(0).getPrevious() != null) {
			temp.add(0, temp.get(0).getPrevious());
		}
		temp.remove(0); // remove first, it's the player position = obsolete
		return temp;
	}
	
	
	
	/**
	 * Calculates a direction to a certain Point, from player position.
	 * 
	 * @param point	Point to calculate the distance to.
	 * @return
	 */
	private float calculateDirectionToPoint(Point point) {
		
		double distanceX = point.x - info.getX();
		double distanceY = point.y - info.getY();
		
		return (float) -Math.atan2(distanceY, distanceX);
	}
	
	
	
	/**
	 * Calculates an adjacency matrix to use with a pathfinding algorithm
	 * 
	 * @param width		Width of part to check
	 * @aparam height	Height of part to check
	 * @return			A boolean adjacency matrix
	 */
	private Node[][] calculateIntersections(int width, int height) {
		Node[][] tempArray = new Node[width/CELL_SIZE][height/CELL_SIZE];
		Rectangle rect = new Rectangle();
		
		for (int x = 0; x < width/CELL_SIZE; x++) {
			innerLoop: for (int y = 0; y < height/CELL_SIZE; y++) {
				rect.setBounds(x*CELL_SIZE, y*CELL_SIZE, CELL_SIZE, CELL_SIZE);
				
				// Check intersections of Rectangles with sand banks
				for(Path2D obstacle : obstacleArray) {
					if(obstacle.intersects(rect)) {
						
						/* If ANY intersection is found, skip this position
						   as it is not passable keep it false */
						tempArray[x][y] = new Node (null, Integer.MAX_VALUE, true, x, y);
						continue innerLoop;
					}
				}
				
				// Only if it doesn't intersect any, path is free
				tempArray[x][y] = new Node (null, Integer.MAX_VALUE, false, x, y);
			}
		}
		return tempArray;
	}
	
	
	
	/**
	 * Finds the point closest to the player.
	 * 
	 * @param points	The array of points to look over.
	 * @return			A Point object of the closest point
	 */
	private Point findNearestPoint(Point[] points) {
		
		Point closest = null;
		double minimumDistance = Double.MAX_VALUE;
		
		// Check all points
		for (Point point : points) {
			
			//If it is already collected, ignore it and get next
			if (point == null) {
				continue;
			}
			
			double currentPearlDistance = point.distance(info.getX(), info.getY());
			
			// A closer point is found, update all
			if (currentPearlDistance < minimumDistance) {
				minimumDistance = currentPearlDistance;
				closest = point;
			}
		}
		
		// Returns the closest Point
		return closest;
	}
	
	
	
	/**
	 * Finds the point closest to the player and removes it.
	 * 
	 * @param points	The array of points to look over.
	 */
	private void removeNearestPoint(Point[] points) {
		
		double minimumDistance = Double.MAX_VALUE;
		int nearestIndex = 0;
		int index = 0;
		// Check all points
		for (Point point : points) {
			
			//If it is already collected, ignore it and get next
			if (point == null) {
				++index;
				continue;
			}
			
			double currentPearlDistance = point.distance(info.getX(), info.getY());
			
			// A closer point is found, update all
			if (currentPearlDistance < minimumDistance) {
				minimumDistance = currentPearlDistance;
				nearestIndex = index;
			}
			++index;
		}
		
		points[nearestIndex] = null;
	}
	
	
	
	/**
	 * Finds the points closest to the Ocean surface.
	 * 
	 * @param pearls	The array of points to look over.
	 * @return			A Point object of the topmost pearl.
	 */
	private Point findTopmostPoint(Point[] points) {
		
		Point closest = null;
		int minimumDistance = Integer.MAX_VALUE;
		
		// Check all points
		for (Point point : points) {
			//If it is already collected, ignore it and get next
			if (point == null) {
				continue;
			}
			
			// A closer point is found, update all
			if (point.y < minimumDistance) {
				minimumDistance = point.y;
				closest = point;
			}
		}
		
		// Returns the closest Point
		return closest;
	}
	
	
	
	/**
	 * Calculculates the coordinates of a predicted collision point
	 * along a certain virtual ray of variable length.
	 * 
	 * @return		a Point where a collision occurred.
	 */
	private Point calculateCollisionPoint(int x, int y) {
		Point p = new Point();
		p.x = (int) x;
		double sin = Math.sin(Math.PI/2);
		for (int i = 0; i < x; i+=10) {
			p.y = (int) (y - sin*i);
			for (Path2D obstacle : obstacleArray) {
				if (obstacle.contains(p)) {
					return p;
				}
			}
		}
		
		return null;
	}
	
	private Point calculateCollision(Point to) {
		Point p = new Point();
		int n = (int) Math.abs(to.x - info.getX());
		double sin = Math.sin(playerDirection);
		double cos = Math.cos(playerDirection);
		for (int i = 0; i < n; i+=10) {
			p.y = (int) (to.y + sin*i);
			p.x = (int) (to.x - cos*i);
			for (Path2D obstacle : obstacleArray) {
				if (obstacle.contains(p)) {
					return p;
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * Initialize starting variables
	 */
	private void init() {
		currentScore = 0;
		currentMoney = 0;
		pathToFollow = new ArrayList<>();
		boughtItems = new ArrayList<>();
		nextAim = new Point();
		obstacleArray = info.getScene().getObstacles();
		pearlArray = info.getScene().getPearl();
		bottleArray = info.getScene().getRecyclingProducts();
		sceneHeight = info.getScene().getHeight();
		sceneWidth = info.getScene().getWidth();
		shipPosition = new Point(info.getScene().getShopPosition(), 0);
		

		//Get calculated values
//		nearestPearl = findTopmostPearl(pearlArray); // Get top to bottom
//		nearestPearl = findNearestPearl(pearlArray); // closest always
//		nearestPearl = findNearestPoint(pearlArray);
	}
	
	private void makeDecision() {
		if (info.getMoney() != currentMoney && !isShopping) {
			currentMoney = info.getMoney();
			removeNearestPoint(bottleArray);
		}
		
		if (currentMoney < 2 && boughtItems.size() < 4) {
			nextAim = findNearestPoint(bottleArray);
		}
		
//		if (!isDiving && isShopping) {
//		} else if (isDiving && !isShopping) {
//			nextAim = findNearestPoint(pearlArray);
//		} else if (!isDiving && !isShopping){
//			nextAim = new Point((int) info.getX(), 0);
//		}
		
		if (currentMoney >= 2 && boughtItems.size() < 4) {
			isShopping = true;
			nextAim = shipPosition;
		}
		
		directSwimOrPath(nextAim);
	}
	
	/**
	 * Decide if you swim to an aim directly or with pathfinding
	 * @param to	Point to swim to
	 */
	private void directSwimOrPath(Point to) {
		playerDirection = calculateDirectionToPoint(nextAim);
//		Point first = calculateCollisionPoint(to.x, to.y);
		Point first = calculateCollision(to);
		// No collisions, dive down at aim position, swim there first at surface
		if (first != null) {
			pathToFollow = calculateDijkstraPath(new Point((int)info.getX(),(int)info.getY()), first);
			Node node = pathToFollow.remove(0);
			nextAim.x = node.getX() * CELL_SIZE + CELL_SIZE/2;
			nextAim.y = node.getY() * CELL_SIZE + CELL_SIZE/2;
		}
	}
	
	/**
	 * Decide which item to buy, based on some parameters
	 * @return	The ShoppingItem to buy
	 */
	private ShoppingItem buyItem() {
		ShoppingItem Item;
		
		if (!boughtItems.contains(ShoppingItem.MOTORIZED_FLIPPERS)) {
			Item = ShoppingItem.MOTORIZED_FLIPPERS;
		} else if (!haveSurfacePearls && !boughtItems.contains(ShoppingItem.BALLOON_SET)) {
			Item = ShoppingItem.BALLOON_SET;
		} else {
			Item = ShoppingItem.STREAMLINED_WIG;
		}
		// Add the Item to list of bought items
		boughtItems.add(Item);
		
		return Item;
	}
	
	@Override
	public void drawDebugStuff(Graphics2D gfx) {
		gfx.setColor(Color.MAGENTA);
		gfx.drawLine((int) info.getX(),(int) info.getY(), nextAim.x, nextAim.y);
	}
	
}