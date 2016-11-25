package datastructures;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * A Fibonacci heap ordered by priority according to a Comparator<E> object.
 * All nodes in the same level share a doubly-linked list, and all of them 
 * are connected to a parent and a children list that comply with the minHeap
 * property. Before a min has been extracted, the Fibonacci heap is simply one
 * big doubly-linked list. During extraction, the heap rearranges itself
 * under the constraint that no two nodes in the same level can have the same
 * degree i.e. the number of children in the doubly-linked list directly below
 * each node. This ensure constant running-time for all its operations except
 * for poll() and delete (), those ones run in O(lg n) time. 
 *
 * @author Implementation: Mauricio Esquivel Rogel; Pseudo Code: Professor 
 * 		   Cormen's "Introduction to Algorithms";
 * @date Fall Term 2016
 */
public class PriorityFibonacciHeap<E extends KeyableObject>{
/******************************** CONSTANTS ***********************************/
	//
	
/*************************** INSTANCE VARIABLES *******************************/
	// PUBLIC
		//
	
	// PRIVATE
	private int numberOfNodes;
	private Comparator<E> comparator;
	private FibonacciHeapNode<E> min;

/***************************** INNER CLASSES **********************************/
	//
	
/****************************** CONSTRUCTOR ***********************************/	
	public PriorityFibonacciHeap(Comparator<E> c){
		numberOfNodes = 0;
		comparator = c;
		min = null;
	}
	
/******************************* PUBLIC METHODS *******************************/
//-------------------------------- insert() ----------------------------------//
	/*
	 * Adds a new node to the heap.
	 * @param newNode - node to be added 
	 */
	public void insert(FibonacciHeapNode<E> newNode) {
		// if heap is empty, make this new node be the only element in a new
		// root list
		if (this.min == null) { this.min = newNode; }
		
		// otherwise, splice it into the root list and update min if necessary 
		else {
			this.min.spliceRight(newNode);
			
			if (this.comparator.compare(newNode.getValue(),
					this.min.getValue()) == -1) {
                this.min = newNode;
            }
		}
		
		this.numberOfNodes++; 
	}

//---------------------------------- peek() ----------------------------------//
	/*
	 * Returns the min element without removing it from the heap.
	 * @return min element 
	 */
	public FibonacciHeapNode<E> peek() {
		return this.min;
	}
//---------------------------------- poll() ----------------------------------//	
	/*
	 * Returns and removes the heap's min element while "consolidating" the heap
	 * i.e. rearranging the heap to satisfy the constraint. 
	 * @return min element
	 */
	public FibonacciHeapNode<E> poll() {
		FibonacciHeapNode<E> min = this.min, child = null, temp = null;
		
		// if the heap is not empty, extract the min element from the heap,
		// add any of its children to the root list, and consolidate if
		// necessary
		if (min != null) {
			child = min.childrenList;
			
			// splice min element's children into the root list
			while (min.degree > 0) {
				temp = child.right;
				
				child.spliceOut();
				this.min.spliceRight(child);
				
				child.parent = null;
				
				min.decreaseDegree();
				child = temp;
			}
			
			// extract min element
			min.spliceOut();
			
			// if min element was the only element, then there is no new min
			if (min == min.right) { this.min = null; }
			
			// otherwise consolidate heap starting with the element to the right
			// of the min element as the new min element
			else {
				this.min = min.right;
				consolidate();
			}
			
			this.numberOfNodes--;
		}
		
		return min;
	}

//------------------------------ decreaseKey() -------------------------------//
	/*
	 * Returns and removes the heap's min element while "consolidating" the heap
	 * i.e. rearranging the heap to satisfy the constraint. 
	 * @param currNode - node already in heap whose key will be decreased
	 * @param newKey - currNode's new key
	 */
	public void decreaseKey(FibonacciHeapNode<E> currNode, double newKey) {
		FibonacciHeapNode<E> y = null;
		
		// if the newKey is bigger than the old one, so decreasing is
		// impossible
		if (newKey > currNode.getKey()) { return; }
		
		// update old key to new key
		currNode.setKey(newKey);
		
		// get the node's parent and check if there are changes to be made
		y = currNode.parent;
		
		// if changes are in order, rearrange the heap back into structure
	    if (y != null &&
	    		this.comparator.compare(currNode.getValue(),
	    				y.getValue()) == -1) {
	    	cut(currNode, y);
	        cascadingCut(y);
	    }
	
	    // update min if necessary
	    if (this.comparator.compare(currNode.getValue(), 
	    		this.min.getValue()) == -1) {
	    	this.min = currNode;
	    }
	}
	
//-------------------------------- delete() ----------------------------------//
	/*
	 * Forces an element to be polled out of the heap. 
	 * @param node - node to be deleted
	 */
	public void delete(FibonacciHeapNode<E> node) {
		// make node new min and extract it as usual
        decreaseKey(node, Double.NEGATIVE_INFINITY);
        poll();
    }

//-------------------------------- size() ------------------------------------//
	public int size() {
		return this.numberOfNodes;
	}

//-------------------------------- isEmpty() ---------------------------------//
	public boolean isEmpty() {
		return this.min == null;  
	}

//--------------------------------- clear() ----------------------------------//
	public void clear() {
		this.min = null;
		this.numberOfNodes = 0;
	}

/********************************** OVERRIDES *********************************/
	//
	
/**************************** PRIVATE METHODS *********************************/
//--------------------------- consolidate() ----------------------------------//
	/*
	 * Rearranges the heap back into structure after a node has been extracted, 
	 * satisfying the Fibonacci heap constraints 
	 */
	private void consolidate() {
		// Create an auxiliary array with enough capacity to hold the maximum
		// number of root nodes possible i.e. the Fibonacci determined upper 
		// bound
		int upperBound = getUpperBound(), currentDegree = 0,
				numberOfRootNodes = 0;
		FibonacciHeapNode<E> currentNode = null, otherNode = null, temp = null,
				tempRight = null;
		ArrayList<FibonacciHeapNode<E>> auxiliaryArray = 
				new ArrayList<FibonacciHeapNode<E>>(upperBound);
		
		// temporarily initialize array to null values to hold the root nodes
		for (int i = 0; i < upperBound; i++) { auxiliaryArray.add(i, null); }
		
		// count the current number of root nodes
		if ((currentNode = this.min) != null) {
			numberOfRootNodes++;
            currentNode = currentNode.right;

            while (currentNode != this.min) {
            	numberOfRootNodes++;
                currentNode = currentNode.right;
            }
		}
		
		// navigate through the current root list, merging nodes to satisfy
		// the constraint
		while (numberOfRootNodes > 0) {	
			currentDegree = currentNode.degree;
			tempRight = currentNode.right;
			
			// fix all conflicting degrees by making the node with the bigger
			// key a child of the node with the smaller key
			while ((otherNode = auxiliaryArray.get(currentDegree)) != null) {
				// if the node to the left is bigger than the node to the right,
				// swap the nodes with each other
				if (this.comparator.compare(currentNode.getValue(),
						otherNode.getValue()) == 1) {
					temp = otherNode;						
					otherNode = currentNode;
					currentNode = temp;
				}
				
				// splice the node with the bigger key into the children list of
				// the node with the smaller key
				link(otherNode, currentNode);
				
				auxiliaryArray.set(currentDegree, null);
				currentDegree++;
			}
			
			// occupy the unique position of the current degree with the latest
			// node
			auxiliaryArray.set(currentDegree, currentNode);
			currentNode = tempRight;
			numberOfRootNodes--;
		}
		
		// reset min
		this.min = null;
		
		// go through the auxiliary array to build the new root list and 
		// determine the new min
		for (int i = 0; i < upperBound; i++) {
			if ((currentNode = auxiliaryArray.get(i)) == null) { continue; }
			
			// if root list is still empty, make this node its first element
			if (this.min == null) { this.min = currentNode; }
			
			// otherwise splice it into the list and update the min if necessary
			else {
				currentNode.spliceOut();
				this.min.spliceRight(currentNode);
				
				if (this.comparator.compare(currentNode.getValue(),
						this.min.getValue()) == -1) {
					this.min = currentNode;
				}
			}
		}
	}

//-------------------------------- link() ------------------------------------//
	/*
	 * Makes one node the child of another.
	 * @param y - node to be added as a child
	 * @param x - future parent of y 
	 */
	private void link(FibonacciHeapNode<E> y, FibonacciHeapNode<E> x) {
		// extract the node and connect it to its parent
		y.spliceOut();
		y.parent = x;
		
		// if the new parent has no previous children, make this node its only
		// only child as its own doubly-linked list
		if (x.childrenList == null) {
			x.childrenList = y;
			y.right = y;
			y.left = y;
		} 
		
		// otherwise splice the node into the parent's children list
		else { x.childrenList.spliceRight(y); }
		
		// update parent's degree and indicate that the node hasn't
		// lost any children since it was last made child to another node
		x.increaseDegree();
		y.unmark();
	}
	
//--------------------------------- cut() ------------------------------------//
	/*
	 * Destroys the link between a node and its parent, make the child node
	 * a new root node  
	 * @param node - node that will be extracted from parent and inserted
	 * 				 into root list
	 * @param parent - node's parent
	 */
	private void cut(FibonacciHeapNode<E> node, FibonacciHeapNode<E> parent) {
		// extract node and update parent's degree
		node.spliceOut();
		parent.decreaseDegree();
		
		// if node was the reference used by its parent as the link to the
		// children list, update reference with some other child in that list
		if (parent.childrenList == node) { parent.childrenList = node.right; }
		
		// if the parent has no children left, empty children list
		if (parent.degree == 0) { parent.childrenList = null; }
		
		// add node to root list
		this.min.spliceRight(node);
		
		// update node's parent reference and indicate that the node hasn't
		// lost any children since it was last made child to another node (in
		// this case, to null)
		node.parent = null;
		node.unmark();
	}
	
//----------------------------- cascadingCut() -------------------------------//
	/*
	 * Goes down through a node tree, marking, unmarking, and breaking links
	 * between nodes and parents 
	 * @param node - the child to begin the ascent through the graph from
	 */
	private void cascadingCut(FibonacciHeapNode<E> node) {
		// if root node, then done
		if (node.parent == null) { return; }
		
		// otherwise, if the node hasn't lost a child after last being added to
		// its parent node, indicate that it will. if it has already lost one,
		// extract it and recursively move up
		if (!node.isMarked()) { node.mark(); }
		else {
			cut(node, node.parent);
			cascadingCut(node.parent);
		}
	}

//--------------------------- getUpperBound() --------------------------------//
	/*
	 * Calculates the max number of possible root nodes after progressively
	 * restructuring root list 
	 * @return Fibonacci determined upper-bound to the number of root nodes 
	 */
	private int getUpperBound() {
		double PHI_FACTOR = 1.0 / Math.log((1.0 + Math.sqrt(5.0)) / 2.0);
		
		return ((int) Math.floor(Math.log(this.numberOfNodes) *
				PHI_FACTOR)) + 1;
	}
}
