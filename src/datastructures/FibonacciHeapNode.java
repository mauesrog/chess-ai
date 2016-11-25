package datastructures;

/**
 * Defines the nodes to be used by PriorityFibonacciHeap. 
 *
 * @author Mauricio Esquivel Rogel
 * @date Fall Term 2016
 */
public class FibonacciHeapNode<T extends KeyableObject> {
/******************************** CONSTANTS ***********************************/
	//
	
/*************************** INSTANCE VARIABLES *******************************/
	// See PriorityFibonacciHeap to see what each variable is for
	
	// PUBLIC
	public FibonacciHeapNode<T> childrenList, left, parent, right;
	public int degree;
	
	// PRIVATE
	private T value;
    private boolean mark;
    private double key;
   
/***************************** INNER CLASSES **********************************/
    /**
	 * If any structure needs to have access to both a FibonacciHeapNode and
	 * its predecessor/parent e.g. in the visited hash map of an aSearch, which
	 * needs to track both the solution path and each visited node to
	 * decrease costs in existing nodes in the frontier 
	 *
	 * @author Mauricio Esquivel Rogel
	 * @date Fall Term 2016
	 */
    public class FibonacciHeapNodeReferenceHandler {
    //----------------------INSTANCE VARIABLES--------------------------------//
    	// PUBLIC
		public FibonacciHeapNode<T> predecessor;
		public FibonacciHeapNode<T> originalElement;
		
		// PRIVATE
			//
		
	//---------------------------CONSTRUCTOR----------------------------------//
		public FibonacciHeapNodeReferenceHandler(FibonacciHeapNode<T> p, 
				FibonacciHeapNode<T> o) {
			predecessor = p;
			originalElement = o;
		}
	}
    
/****************************** CONSTRUCTOR ***********************************/
    public FibonacciHeapNode(T v) {
    	right = left = this;
        value = v;
        key = v.calculateKey();
        childrenList = null;
        parent = null;
    }
    
/******************************* PUBLIC METHODS *******************************/
 //------------------------------ spliceRight() ------------------------------//
    /*
  	 * Inserts a node to the right of this node in the doubly-linked list 
  	 * @param node - node to be inserted 
  	 */
    public void spliceRight(FibonacciHeapNode<T> node) {
    	node.left = this;
    	node.right = this.right;
    	
    	this.right = node;
    	node.right.left = node;
    }

//----------------------------- spliceOut() ---------------------------------//
  	/*
  	 * Extracts this node from the doubly-linked list it belongs to.
  	 */
    public void spliceOut() {
    	this.left.right = this.right;
        this.right.left = this.left;
    }

//-------------------------------- getKey() ----------------------------------//
    public final double getKey() {
        return this.key;
    }

//-------------------------------- setKey() ----------------------------------//
    public void setKey(double k) {
        this.key = k;
    }

//------------------------------ getValue() ----------------------------------//
    public final T getValue() {
        return this.value;
    }
  
//------------------------------- setValue() ---------------------------------//
    public void setValue(T v) {
        this.value = v;
    }
   
//---------------------------- decreaseDegree() ------------------------------//
    public void decreaseDegree() {
    	if (this.degree > 0) { this.degree--; }
    }
    
//---------------------------- increaseDegree() ------------------------------// 
    public void increaseDegree() {
    	this.degree++;
    }

//-------------------------------- unmark() ----------------------------------//    
    public void unmark() {
    	this.mark = false;
    }
    
//------------------------------ isMarked() ----------------------------------//    
    public boolean isMarked() {
    	return this.mark;
    }
    
//-------------------------------- mark() ------------------------------------//    
    public void mark() {
    	this.mark = true;
    }
    
/********************************** OVERRIDES *********************************/
//------------------------------ toString() ----------------------------------//
    @Override
    public String toString() {
        return " " + this.value;
    }

//-------------------------------- equals() ----------------------------------//
    @SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other) {
    	if (other instanceof FibonacciHeapNode) {
    		return ((FibonacciHeapNode<T>) other).getValue()
    				.equals(this.getValue());
    	}
    	
    	return other.equals(this.value);
	}
    
//------------------------------ hashCode() ----------------------------------//   
    @Override
	public int hashCode() {
		return this.value.hashCode();
	}
/**************************** PRIVATE METHODS *********************************/
    //
}