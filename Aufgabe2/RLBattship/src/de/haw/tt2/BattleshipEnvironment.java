package de.haw.tt2;

import java.util.Random;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

public class BattleshipEnvironment implements EnvironmentInterface{

	@Override
	public void env_cleanup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String env_init() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String env_message(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Observation env_start() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	 /**
     * This is a trick we can use to make the agent easily loadable.
     * @param args
     */
    public static void main(String[] args) {
//        EnvironmentLoader theLoader = new EnvironmentLoader(new SampleMinesEnvironment());
//        theLoader.run();
    }
	
}


class World {
	
	private final static int NUM_ROWS = 10;
	private final static int NUM_COLMS = 10;
	
	/*
	 * Size of the different ship types.
	 */
	private final static int SIZE_AIRCRAFT_CARRIER = 5;
	private final static int SIZE_BATTLESHIP = 4;
	private final static int SIZE_SUBMARINE = 3;
	private final static int SIZE_DESTROYER = 3;
	private final static int SIZE_PATROL_BOAT = 2;
	
	/*
	 * Count of available ships per ship type
	 */
	private final static int COUNT_AIRCRAFT_CARRIER = 1;
	private final static int COUNT_BATTLESHIP = 1;
	private final static int COUNT_SUBMARINE = 1;
	private final static int COUNT_DESTROYER = 2;
	private final static int COUNT_PATROL_BOAT = 2;
	
	/*
	 * Marker for identifying the different ship types
	 */
	private final static int MARKER_AIRCRAFT_CARRIER = 1;
	private final static int MARKER_BATTLESHIP = 2;
	private final static int MARKER_SUBMARINE = 3;
	private final static int MARKER_DESTROYER = 4;
	private final static int MARKER_PATROL_BOAT = 5;
	
	/*
	 * Alignment constants
	 */
	private final static int HORIZONTAL = 0;
	private final static int VERTICAL = 1;
	
	private int[][] map;
	private Random rn;
	
	public  World() {
		
		map = new int[NUM_ROWS][NUM_COLMS];
		rn = new Random();
	}
	
	/**
	 * This method places the ships randomly on the battlefield
	 */
	public void placeShips() {
		
		// Choose alignment
		int alignment = rn.nextInt(VERTICAL + 1);
		
		switch (alignment) {
		case HORIZONTAL:
			placeShipsHorizontal();
			break;

		case VERTICAL:
			placeShipsVertical();
			break;
			
		default:
			break;
		}
		
	}
	
	/**
	 * 
	 */
	private void placeShipsHorizontal() {
		
		boolean available =  true;
		int count = 0;
		
		//----------------------------
		// Place aircraft carrier
		//----------------------------
		
		count++;
		
		do {
			
			// Chose row
			int row = rn.nextInt(NUM_ROWS);
			
			// Chose position in row (column)
			int range = NUM_COLMS - SIZE_AIRCRAFT_CARRIER;
			int column = rn.nextInt(range);
			
			// Check if already occupied
			available = checkRangeHorizontal(row, column, SIZE_AIRCRAFT_CARRIER);
			
			// Place aircraft carrier on battlefield
			if(available) {
				placeOnBattlefield(row, column, HORIZONTAL, MARKER_AIRCRAFT_CARRIER, SIZE_AIRCRAFT_CARRIER);
				count++;
			}
			
		} while (available && count < COUNT_AIRCRAFT_CARRIER);
		
	}
	
	/**
	 * 
	 */
	private void placeShipsVertical() {
		
		boolean available =  true;
		int count = 0;
		
		//----------------------------
		// Place aircraft carrier
		//----------------------------
		
		count = 0;
		
		do {
			
			// Chose row
			int range = NUM_ROWS - SIZE_AIRCRAFT_CARRIER;
			int row = rn.nextInt(range);
			
			
			// Chose position in row (column)
			int column = rn.nextInt(NUM_COLMS);
			
			// Check if already occupied
			available = checkRangeVertical(row, column, SIZE_AIRCRAFT_CARRIER);
			
			// Place aircraft carrier on battlefield
			if(available) {
				placeOnBattlefield(row, column, VERTICAL, MARKER_AIRCRAFT_CARRIER, SIZE_AIRCRAFT_CARRIER);
				count++;
			}
			
		} while (available && count < COUNT_AIRCRAFT_CARRIER);
		
		//----------------------------
		// Place battleship
		//----------------------------
		
		count = 0;
		
		do {
			
			// Chose row
			int range = NUM_ROWS - SIZE_BATTLESHIP;
			int row = rn.nextInt(range);
			
			
			// Chose position in row (column)
			int column = rn.nextInt(NUM_COLMS);
			
			// Check if already occupied
			available = checkRangeVertical(row, column, SIZE_BATTLESHIP);
			
			// Place aircraft carrier on battlefield
			if(available) {
				placeOnBattlefield(row, column, VERTICAL, MARKER_BATTLESHIP, SIZE_BATTLESHIP);
				count++;
			}
			
		} while (available && count < COUNT_BATTLESHIP);
		
		//----------------------------
		// Place submarine
		//----------------------------
		
		count = 0;
		
		do {
			
			// Chose row
			int range = NUM_ROWS - SIZE_SUBMARINE;
			int row = rn.nextInt(range);
			
			
			// Chose position in row (column)
			int column = rn.nextInt(NUM_COLMS);
			
			// Check if already occupied
			available = checkRangeVertical(row, column, SIZE_SUBMARINE);
			
			// Place aircraft carrier on battlefield
			if(available){
				placeOnBattlefield(row, column, VERTICAL, MARKER_SUBMARINE, SIZE_SUBMARINE);
				count++;
			}				
			
		} while (available && count < COUNT_SUBMARINE);
		
		//----------------------------
		// Place destroyer
		//----------------------------
		
		count = 0;
		
		do {
			
			// Chose row
			int range = NUM_ROWS - SIZE_DESTROYER;
			int row = rn.nextInt(range);
			
			
			// Chose position in row (column)
			int column = rn.nextInt(NUM_COLMS);
			
			// Check if already occupied
			available = checkRangeVertical(row, column, SIZE_DESTROYER);
			
			// Place aircraft carrier on battlefield
			if(available) {
				placeOnBattlefield(row, column, VERTICAL, MARKER_DESTROYER, SIZE_DESTROYER);
				count++;
			}
				
			
		} while (available && count < COUNT_DESTROYER);
		
		//----------------------------
		// Place patrol boat
		//----------------------------
		
		count = 0;
		
		do {
			
			// Chose row
			int range = NUM_ROWS - SIZE_PATROL_BOAT;
			int row = rn.nextInt(range);
			
			
			// Chose position in row (column)
			int column = rn.nextInt(NUM_COLMS);
			
			// Check if already occupied
			available = checkRangeVertical(row, column, SIZE_PATROL_BOAT);
			
			// Place aircraft carrier on battlefield
			if(available) {
				placeOnBattlefield(row, column, VERTICAL, MARKER_PATROL_BOAT, SIZE_PATROL_BOAT);
				count++;
			}
				
			
		} while (available && count < COUNT_PATROL_BOAT);
	}
	
	/**
	 * 
	 * @param row
	 * @param column
	 * @param size
	 * @return
	 */
	private boolean checkRangeHorizontal(int row, int column, int size) {
		
		boolean available = true;
		int place = 0;
		
		for (int i = 0; i < size && available; i++) {
			
			place = map[row][column+i];
			if(place > 0)
				available = false;
			
		}
		
		return available;
		
	}
	
	/**
	 * 
	 * @param row
	 * @param column
	 * @param size
	 * @return
	 */
	private boolean checkRangeVertical(int row, int column, int size) {
		
		boolean available = true;
		int place = 0;
		
		for (int i = 0; i < size && available; i++) {
			
			place = map[row+i][column];
			if(place > 0)
				available = false;
			
		}
		
		return available;
		
	}
	
	/**
	 * Places given type of ship with the given alignment on the battlefield.
	 * @param row		Y position of the first field the ship occupies
	 * @param column	X position of the first filed the ship occupies
	 * @param alignment If the ship should be placed horizontal or vertical
	 * @param type		The ship's type or marker
	 * @param size		How much fields a ship occupies
	 */
	private void placeOnBattlefield(int row, int column, int alignment, int type, int size) {
		
		switch (alignment) {
		case HORIZONTAL:
			for (int i = 0; i < size; i++) {
				
				map[row][column+i] = type;
				
			}
			break;

		case VERTICAL:
			for (int i = 0; i < size; i++) {
				
				map[row+i][column] = type;
				
			}
			break;
			
		default:
			break;
		}
		
	}
	
}