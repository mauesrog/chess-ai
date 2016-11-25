package chess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;

import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.pgn.PGNReader;
import chesspresso.pgn.PGNSyntaxError;
import chesspresso.game.Game;
import chesspresso.position.Position;
import datastructures.PriorityFibonacciHeap;
import datastructures.FibonacciHeapNode;
import datastructures.KeyableObject;

/**
 * An implementation of iterative deepening minimax search with alpha-beta 
 * pruning, a transposition table, an opening book, move reordering, null-move
 * and forward pruning, and a delta-pruning quiescence search. 
 *
 * @author Mauricio Esquivel Rogel;
 * @date Fall Term 2016
 */
public class ChessMinimaxAI implements ChessAI {	
/******************************** CONSTANTS ***********************************/
	//
	
/*************************** INSTANCE VARIABLES *******************************/
	// PUBLIC
		//
	
	// PRIVATE
	private static int MAX_DEPTH = 4;
	private int computerId = -1;
	private HashMap<Integer, Double> evalValues;
	private static int RATIO = 5;
	private static int PENALTY = 1;
	private Game[] openingBook = new Game[120];
	private int GAME_INDEX = (int)(Math.random() * openingBook.length);
	private Game opening = null;
	private short nextMove = Move.NO_MOVE;
	private boolean invalidMove = false; 
	private int nodesExplored = 0;
	private EvalMoveComparator comparator;
	
/***************************** INNER CLASSES **********************************/
	/**
	 * The moves to be sorted by the Fibonacci Heap
	 *
	 * @author Mauricio Esquivel Rogel
	 * @date Fall Term 2016
	 */
	private class EvalMove implements KeyableObject {
	//----------------------INSTANCE VARIABLES--------------------------------//
		// PUBLIC
		public Double eval = 0.0;
		public short move = Move.NO_MOVE;
		
		// PRIVATE
			//
		
	//---------------------------CONSTRUCTOR----------------------------------//
		private EvalMove(Short m, Double e) {
			eval = e;
			move = m;
		}
		
		private short getMove() {
			return move;
		}
		
		@Override
		public Double calculateKey() {
			return eval;
		}
	}
	
	/**
	 * Compares moves for the Fibonacci Heap
	 *
	 * @author Mauricio Esquivel Rogel
	 * @date Fall Term 2016
	 */
	private class EvalMoveComparator implements Comparator<EvalMove> {
	//----------------------INSTANCE VARIABLES--------------------------------//
		// PUBLIC
		public int type;
		
		// PRIVATE
			//
		
	//---------------------------CONSTRUCTOR----------------------------------//
		public EvalMoveComparator(int t) {
			type = t;
		}
		
		@Override
		public int compare(EvalMove o1, EvalMove o2) {
			return Double.compare(o1.eval, o2.eval) * type;
		}
		
	}

/****************************** CONSTRUCTOR ***********************************/
	public ChessMinimaxAI() {
		evalValues = new HashMap<Integer, Double>(); 	// transposition table
		URL url = this.getClass().getResource("book.pgn");
		File f;
		FileInputStream fis;
		PGNReader pgnReader;
		
		// load all the openings
		try {
			f = new File(url.toURI());
			fis = new FileInputStream(f);
			pgnReader = new PGNReader(fis, "book.pgn");
			
			for (int i = 0; i < 120; i++) {
				openingBook[i] = pgnReader.parseGame();
			}
			
			opening = openingBook[GAME_INDEX];
			opening.gotoStart();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (PGNSyntaxError e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

/******************************* PUBLIC METHODS *******************************/
 //------------------------------ getMove() ------------------------------//
	/*
	 * Will get called each turn to calculate the move 
	 * @param position - current position which will serve as the only instance
	 * 					of Position 
	 */
	public short getMove(Position position) {
		try {
			// Get the first 10 moves from the opening book but stop if at
			// any point the move is invalid or not quiescent, then start using
			// AI
			if (position.getPlyNumber() < 10 && !invalidMove) {
				opening.goForward();
				short move = opening.getNextMove().getShortMoveDesc();
				boolean empty = position.isSquareEmpty(Move.getToSqi(move)),
						sacrifice = false, capture = Move.isCapturing(move),
						recapture;
				
				position.doMove(move);
				opening.goForward();

				if (!position.isLegal() || !empty) {
					position.undoMove();
					invalidMove = true;
					return IDMinimaxSerach(position);
				}
				
				Double v = eval(position, 0);
				
				recapture = position.getAllCapturingMoves().length > 0;
				sacrifice = recapture ? !capture : quiescenceSearch(position, 
						Double.NEGATIVE_INFINITY, 
							Double.POSITIVE_INFINITY, 0) <= v;
				
				// check if the horizon effect makes this a bad move
				if (sacrifice) {
					position.undoMove();
					invalidMove = true;
					return IDMinimaxSerach(position);
				}
				
				position.undoMove();
				return move;
			}
			
			// get the next move using the regular AI
			return  IDMinimaxSerach(position);
		} catch (IllegalMoveException e) {
			e.printStackTrace();
			return emergencyMove(position);
		}
	} 

/**************************** PRIVATE METHODS *********************************/
 //--------------------- IDMinimaxSerach() --------------------------//
	/*
	 * The search using all the heuristics and implementations  
	 * @param position - current position which will serve as the only instance
	 * 					of Position 
	 */
	private short IDMinimaxSerach(Position position)
			throws IllegalMoveException {
		int i = 1; // current depth
		Double eval = Double.NEGATIVE_INFINITY;    
		short move = emergencyMove(position);  // in case something goes wrong  
		boolean undo = false;  // whether there's a move to undo
		
		while (!(position.isTerminal() && position.isMate()) && i < MAX_DEPTH) {
			if (undo) {
				position.undoMove();
			}
			
			nodesExplored = 0;
			eval = MinimaxAlphaBetaSerach(position, i++);
			move = nextMove; 
			
			// Log the current best move evaluation
			System.out.println("Search i = " + (i-1) + " eval: " + eval + 
					" nodes explored: "+ nodesExplored);
			
			if (move != Move.NO_MOVE) {
				position.doMove(nextMove);
				undo = true;
			} else {
				undo = false;
			}
		}
		
		if (undo) {
			position.undoMove();
		}
		
		return move != Move.NO_MOVE ? move : emergencyMove(position);
	}
	
 //--------------------- MinimaxAlphaBetaSerach() --------------------------//
	/*
	 * Calls the initial recursion of the set  
	 * @param position - current position which will serve as the only instance
	 * 					of Position 
	 * @param maxDepth - maximum number of plys ahead
	 */
	private Double MinimaxAlphaBetaSerach(Position position, int maxDepth)
			throws IllegalMoveException {
		// track the AI's player id
		if (computerId != position.getToPlay())
			computerId = position.getToPlay();
		
		return maxValue(position, 0, maxDepth,
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

 //-------------------------------- maxValue() -------------------------------//
	/*
	 * Tries to get the max min value possible  
	 * @param position - current position which will serve as the only instance
	 * 				     of Position 
	 * @param d		   - current depth
	 * @param maxDepth - maximum number of plys ahead
	 * @param a		   - current max min value
	 * @param b		   - current min max value
	 */
	private Double maxValue(Position position, int d, 
			int maxDepth, Double a, Double b) throws IllegalMoveException {
		nodesExplored++;
		
		// CUTOFF test with quiescence search
		if (cutoffTest(position, d, maxDepth)) {
			Double value = eval(position, d), q;
			
			if (position.isStaleMate()) {
				value = 0.0;
			}  else if (position.isTerminal() && position.isMate()) {
				value = position.getToPlay() == computerId ? 
						Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			} else if 
				(Move.isCapturing(position.getLastMove().getShortMoveDesc()) &&
					(q = quiescenceSearch(position, Double.NEGATIVE_INFINITY, 
						Double.POSITIVE_INFINITY, d)) < value) {
				value = q; 
			}
			
			return value;
		}
		
		short moves[] = position.getAllMoves(),move;
		boolean canOrder = false;
		comparator = new EvalMoveComparator(-1);
		PriorityFibonacciHeap<EvalMove> orderedMovesHeap = 
				new PriorityFibonacciHeap<EvalMove>(comparator);
		int j = 1; 
		Double v, childV;
		EvalMove x;
		
		
		// MOVE ORDERING
		move = moves[0];
		position.doMove(move);
		
		while (evalValues.containsKey(position) && j < moves.length) {
			orderedMovesHeap.insert(new 
					FibonacciHeapNode<EvalMove>(new EvalMove(move, 
							evalValues.get(position))));
			
			position.undoMove();
			
			if (j < moves.length) {
				move = moves[j++];
				position.doMove(move);
			} else {
				j++;
			}
		}
		
		if (j > 1) {
			if (orderedMovesHeap.size() == moves.length) {
				canOrder = true;
			}
		}
		
		position.undoMove();
		
		// NULL-MOVE PRUNING	
		if (position.isCheck() && position.getToPlay() == computerId) {
			v = Double.NEGATIVE_INFINITY;
		} else {
			position.setToPlay(0);
			v = minValue(position, d+1, maxDepth, a, b);
			position.setToPlay(computerId);
		}
		
		if (v > Double.NEGATIVE_INFINITY) {
			if (v >= b) {
				return v;
			} else {
				v = Double.NEGATIVE_INFINITY;
			}
		}
		
		// check min values to get the max
		for (int i = 0; i < moves.length; i++) {
			x = canOrder ? orderedMovesHeap.poll().getValue() : null;
			
			move = canOrder ? x.getMove() : moves[i];
			
			position.doMove(move);
			childV = minValue(position, d+1, maxDepth, a, b);
	
			if (childV > v) {	
				if (d == 0) {
					nextMove = move;
				}
				
				v = childV;
			}

			position.undoMove();
			
			// BETA PRUNING
			if (v >= b) {	
				return v;
			}
			
			a = Math.max(a, v);
		}
		
		return v;
	}

 //-------------------------------- minValue() -------------------------------//
	/*
	 * Tries to get the min max value possible  
	 * @param position - current position which will serve as the only instance
	 * 				     of Position 
	 * @param d		   - current depth
	 * @param maxDepth - maximum number of plys ahead
	 * @param a		   - current max min value
	 * @param b		   - current min max value
	 */
	private Double minValue(Position position, int d, int maxDepth, 
			Double a, Double b) throws IllegalMoveException {
		nodesExplored++;
		
		// CUTOFF test with quiescence search
		if (cutoffTest(position, d, maxDepth)) {
			Double value = eval(position, d), q;		
			
			if (position.isStaleMate()) {
				value = 0.0;
			}  else if (position.isTerminal() && position.isMate()) {
				value = position.getToPlay() == computerId ? 
						Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			} else if 
				(Move.isCapturing(position.getLastMove().getShortMoveDesc()) &&
					(q = quiescenceSearch(position, Double.NEGATIVE_INFINITY, 
						Double.POSITIVE_INFINITY, d)) < value) {
				value = q; 
			}
			
			return value;
		}
		
		short moves[] = position.getAllMoves(), move;
		Double v, childV;
		boolean canOrder = false;
		comparator = new EvalMoveComparator(1);
		PriorityFibonacciHeap<EvalMove> orderedMovesHeap = 
				new PriorityFibonacciHeap<EvalMove>(comparator);
		int j = 1; 
		EvalMove x;
		

		// MOVE ORDERING
		move = moves[0];
		position.doMove(move);
		
		while (evalValues.containsKey(position) && j < moves.length) {
			orderedMovesHeap.insert(new 
					FibonacciHeapNode<EvalMove>(new 
							 EvalMove(move, evalValues.get(position))));
			
			position.undoMove();
			
			if (j < moves.length) {
				move = moves[j++];
				position.doMove(move);
			} else {
				j++;
			}
		}
		
		if (j > 1) {
			if (orderedMovesHeap.size() == moves.length) {
				canOrder = true;
			}
		}
		
		position.undoMove();
		
		// NULL-MOVE PRUNING	
		if (position.isCheck() && position.getToPlay() != computerId) {
			v = Double.POSITIVE_INFINITY;
		} else {
			position.setToPlay(computerId);
			v = maxValue(position, d+1, maxDepth, a, b);
			position.setToPlay(0);
		}
		
		if (v > Double.POSITIVE_INFINITY) {
			if (v <= a) {
				return v;
			} else {
				v = Double.POSITIVE_INFINITY;
			}
		}
		
		// check max values to get the min
		for (int i = 0; i < moves.length; i++) {
			x = canOrder ? orderedMovesHeap.poll().getValue() : null;
			
			move = canOrder ? x.getMove() : moves[i];
			
			if (canOrder) {
				System.out.println(x.eval);
			}
			
			position.doMove(move);			
			childV = maxValue(position, d+1, maxDepth, a, b);
	
			if (childV < v) {	
				v = childV;
			}

			position.undoMove();
			
			// ALPHA PRUNING
			if (v <= a) {	
				return v;
			}
			
			b = Math.min(b, v);
		}
		
		return v;
	}

 //--------------------------- quiescenceSearch() ----------------------------//
	/*
	 * Given a non-quiescent move, it runs a reduced minimax search with alpha-
	 * beta and delta pruning until a quiescente move is found. At this point, 
	 * we know we have the real evaluation of the capture.   
	 * @param position - current position which will serve as the only instance
	 * 				     of Position 
	 * @param a		   - current max min value
	 * @param b		   - current min max value
	 * @param d		   - current depth
	 */
	private Double quiescenceSearch(Position position, Double a, 
			Double b, int d) throws IllegalMoveException {
		
		// the evaluation of the current "hand"
		Double standPat = eval(position, d), counter, v = standPat, bigDelta;
		short capturingMoves[] = position.getAllCapturingMoves(), move;
		
		// go through all the capturing moves until a quiescent node has been
		// found
		if (capturingMoves.length > 0) {	
			
			// max
			if (position.getToPlay() == computerId) { 
				// beta cutoff
				if (standPat >= b) {
					return standPat;
				}
				
				for (int i = 0; i < capturingMoves.length; i++) {
					move = capturingMoves[i];
					
					position.doMove(move);
					counter = quiescenceSearch(position, a, b, d);
					
					v = Math.max(counter, v);
					
					position.undoMove();
					
					if (v >= b) {
						return v;
					}
					
					bigDelta = 975.0; // queen eval
					if (Move.isPromotion(move)) {
						bigDelta += 775.0;
					}
					
					// delta cutoff
					// if not near the end of the game
					if (v < 2000.0 && v < a - bigDelta) {
					   return a;
					}
						
					a = Math.max(a, v);
				}
				
			// min
			} else {
				// beta cutoff
				if (standPat <= b) {
					return standPat;
				}
				
				for (int i = 0; i < capturingMoves.length; i++) {
					move = capturingMoves[i];
					
					position.doMove(move);
					counter = quiescenceSearch(position, a, b, d);
					
					v = Math.min(counter, v);
					
					position.undoMove();
					
					if (v <= a) {
						return v;
					}
					
					bigDelta = -975.0; // queen eval
					if (Move.isPromotion(move)) {
						bigDelta -= 775.0;
					}
					
					// delta cutoff
					// if not near the end of the game
					if (v > -2000 && v > b - bigDelta) {
					   return b;
					}
						
					b = Math.min(b, v);
				}
			}
			
			return v;
		} else {
			return b;
		}
	}

 //-------------------------------- eval() -----------------------------------//
	/*
	 * Calculates the evaluation of a position based on its material and its
	 * domination (with more weight on the material)
	 * @param position - current position which will serve as the only instance
	 * 				     of Position
	 * @param d		   - current depth
	 */
	private Double eval(Position position, int d) throws IllegalMoveException {
		Double material;
			
		// TRANSPOSITION TABLE
		if (evalValues.containsKey(position.hashCode())) {
			// repeated positions are less valuable
			material = evalValues.get(position.hashCode()) - PENALTY;
		} else {
			// more weight on the material
			material = position.getMaterial() + position.getDomination()
				/ RATIO;
			evalValues.put(position.hashCode(), material);
		}
		
		if (position.getToPlay() == computerId) {
			return material;
		}
		
		return material * -1;
	}

 //---------------------------- cutoffTest() ---------------------------------//
	/*
	 * Check if any condition terminates the search
	 * @param position - current position which will serve as the only instance
	 * 				     of Position
	 * @param d		   - current depth
	 * @param maxDepth - maximum number of plys ahead
	 */
	private boolean cutoffTest(Position position, int d, int maxDepth) {
		return (position.isMate() && position.isTerminal())
				|| position.isStaleMate() || d == maxDepth;
	}

 //---------------------------- emergencyMove() ------------------------------//
	/*
	 * If AI can't figure out a move, return an emergency random move
	 * @param position - current position which will serve as the only instance
	 * 				     of Position
	 */
	private short emergencyMove(Position position) {
		short moves[] = position.getAllMoves(); 
		return moves[(int) Math.floor(Math.random() * (moves.length - 1))];
	}
}
