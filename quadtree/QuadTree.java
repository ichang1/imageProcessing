/**Name: Calvin Barret and Isaac Chang
 * File: QuadTree.java
 * Desc:
 * 		The file that contains the quadtree class, the protected node class, and the 
 * 		appropriate methods.
 */

package quadtree;
import java.io.BufferedWriter;
import java.io.IOException;

import java.io.FileWriter;
import java.util.Stack;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;


interface rgbFunction {
	int run(int r, int g, int b);
}

public class QuadTree {
	//brick outline threshold: 7000
	//fire outline threshold: 1200
	//glass outline threshold: 1500
	//kira outline threshold: 1000
	//cobblestone outline threshold: 300
	//noChange threshold for all: 100
	private static final int EDGETHRESHOLD = 80;
	//threshold for edge detection which determines when a pixel should be white or black
	private int threshold;
	//threshold when creating the quadtree
	private Node root;
	//root of the quadtree
	private int width;
	//width of the picture
	private int height;
	//height of the picture
	private int size;
	//size(area) of the picture
	private int numLeaves;
	//the number of leaves in the picture
	private String imageName;
	//the name of the picture
	private int[][][] colors;
	//the rgb values of the original picture stored in a triple integer array
	private int[][][] modified;
	//the rgb values for the modified image

	protected class Node {
		private int[] avgColor; //the average color of a sub picture that a node occupies
		private int startRow; //row index of top left corner
		private int startCol; //column index of top left corner
		private int width; // width of quadrant
		private int height; // height of quadrant
		private int level;	//the level the node is at in the tree (starts at 0)
		private Node nEast; //pointer to north east node
		private Node nWest; //pointer to north west node
		private Node sEast; //pointer to south east node
		private Node sWest; //pointer to south west node
		/** The constructor for a node
		 * @param avgColor	an integer array with the average rgb value of the region that the 
		 * 					node occupies. the 0th elemenet is red, 1st element is green and the 
		 * 					2nd element is blue
		 * @param startRow	the x coordinate of the top left corner of the region that the node 
		 * 					occupies in relation to the whole picture
		 * @param startCol	the y coordinate of the top left corner of the region that the node 
		 * 					occupies in relation to the whole picture
		 * @param width		the width of the region that the node occupies
		 * @param height	the height of the region that the node occupies
		 * @param level		the level of the node in the tree
		 */
		public Node(int startRow, int startCol, int width, int height, int level) {
			this.startRow = startRow;
			this.startCol = startCol;
			this.width = width;
			this.height = height;
			this.level = level;
			this.nEast = null;
			this.nWest = null;
			this.sEast = null;
			this.sWest = null;
			this.avgColor = meanColor();
		}

		/**
		 * mean rgb for the region this node is comprised of
		 * @return avg rgb as int[3]
		 */
		private int[] meanColor() {
			int[] avgColor = {0, 0, 0};
			int maxRow = this.startRow + this.height;
			int maxCol = this.startCol + this.width;
			for (int h = this.startRow; h < maxRow; h++) {
				for (int w = this.startCol; w < maxCol; w++) {
					avgColor[0] += QuadTree.this.colors[h][w][0];
					avgColor[1] += QuadTree.this.colors[h][w][1];
					avgColor[2] += QuadTree.this.colors[h][w][2];
				}
			}
			int squareSz = this.width * this.height;
			avgColor[0] /= squareSz;
			avgColor[1] /= squareSz;
			avgColor[2] /= squareSz;
			return avgColor;
		}
		
		/** Method that treats an rgb value as a point in 3 space and finds the distance between two
		 * 	rgb values which is the square error of the two rgb values
		 * @param pixelColor1	the first rgb value
		 * @param pixelColor2	the second rgb value
		 * @return	the distance (square error) between the two rgb values
		 */
		private double twoPixelsSqError(int[] pixel1, int[] pixel2) {
			int pixel1Red = pixel1[0];
			int pixel1Green = pixel1[1];
			int pixel1Blue = pixel1[2];
			int pixel2Red = pixel2[0];
			int pixel2Green = pixel2[1];
			int pixel2Blue = pixel2[2];
			return Math.pow(pixel1Red - pixel2Red, 2) +
					Math.pow(pixel1Green - pixel2Green, 2) +
					Math.pow(pixel1Blue - pixel2Blue, 2);
		}
		
		/**
		 * 
		 * @return avg rgb square error in node as double
		 */
		public double avgSqError() {
			double error = 0.0;
			int maxRow = this.startRow + this.height;
			int maxCol = this.startCol + this.width;
			for (int h = this.startRow; h < maxRow; h++) {
				for (int w = this.startCol; w < maxCol; w++) {
					int[] pixel = QuadTree.this.colors[h][w];
					error += twoPixelsSqError(pixel, this.avgColor);
				}
			}
			int size = this.height * this.width;
			error /= size;
			return error;
		}
	}

	public QuadTree(int[][][] colors) {
		this.numLeaves = 1;
		this.width = colors[0].length;
		this.height = colors.length;
		this.size = this.width*this.height;
		this.colors = colors;	
		this.modified = new int[height][width][3];
		this.threshold = 100;
		this.root = new Node(0, 0, this.width, this.height, 0);
	}
	
	/**
	 * check if should split this node into 4 
	 * @return boolean
	 */
	public boolean reachThreshold(Node node) {
		if (node.avgSqError() > this.threshold) {
			//if big error, don't stop cutting 
			return false;
		} else {
		//if small error, do stop cutting
			return true;
		}
	}

	/** Method that inserts the children of the quadtree
	 * @param root the root of the sub quad tree which the chidlren are inserted at
	 * @param pixelColors the matrix of rgb values of the root node
	 */
	private void insertChildren(Node root) {
		if (root.height > 1 && root.width > 1 && !reachThreshold(root)) {
			//if the matrix is not really long or really wide then it can be split into four 
			//sub images if need be. Helps handle pictures that do not have dimensions of a
			//power of 2
			numLeaves += 3; //lose itself as leaf and gain 4 new leaves
			int midWidth = root.width / 2;
			int midHeight = root.height / 2;
			root.nWest = new Node(root.startRow, 
								  root.startCol,
								  midWidth, 
							      midHeight,
								  root.level + 1);
			root.nEast = new Node(root.startRow,
								  root.startCol + midWidth,
								  root.width - midWidth,
								  midHeight,
								  root.level + 1);
			root.sWest = new Node(root.startRow + midHeight,
								  root.startCol,
								  midWidth,
								  root.height - midHeight,
								  root.level + 1);
			root.sEast = new Node(root.startRow + midHeight,
							      root.startCol + midWidth,
							      root.width - midWidth,
							      root.height - midHeight,
								  root.level + 1);
			insertChildren(root.nWest);
			insertChildren(root.nEast);
			insertChildren(root.sWest);
			insertChildren(root.sEast);
		} 
	}

	public void insert(){
		insertChildren(this.root);
	}

	/** Method for determining whether a pixel is on the border of something
	 * @param row	the x coordinate of the pixel in relation to the whole picture
	 * @param col	the y coordinate of the pixel in relation to the whole picture
	 * @param minRow	the x coordinate of the top border of the sub picture in relation to the 
	 * 					whole picture
	 * @param maxRow	the x coordinate of the bottom border of the sub picture in relation to the 
	 * 					whole picture
	 * @param minCol	the y coordinate of the left border of the sub picture in relation to the 
	 * 					whole picture
	 * @param maxCol	the y coordinate of the rightt border of the sub picture in relation to the 
	 * 					whole picture
	 * @return	boolean value of whether the pixel is on the border of the sub picture
	 */
	private boolean onBorder(int row, int col, int minRow, int maxRow, int minCol, int maxCol) {
		//if the x coordinate or y coordinate of the pixel is on one of the borders, then it is 
		//on the border
		if (row == minRow || row == maxRow) {
			return true;
		} else if (col == minCol || col == maxCol) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * dfs to get leaf nodes. modify array in place with leaves
	 * @param root	node to start at 
	 * @param leaves	array to be modified in place
	 */
	private void dfsLeaves(Node[] leaves) {
		Stack<Node> s = new Stack<Node>();
		s.push(this.root);
		int i = 0;
		while (!s.isEmpty()){
			Node n = s.pop();
			if (n.nWest == null &&
				n.nEast == null &&
				n.sWest == null &&
				n.sEast == null){
				leaves[i] = n;
				i += 1;
			} else{
				if (n.nWest != null){
					s.push(n.nWest);
				}
				if (n.nEast != null){
					s.push(n.nEast);
				}
				if (n.sWest != null){
					s.push(n.sWest);
				}
				if (n.sEast != null){
					s.push(n.sEast);
				}
			}
		}
	}

	/** Method that gets the leaves of the quad tree
	 * @return ArrayList of the leaf nodes of the quad tree
	 */
	private Node[] getLeaves(){
		Node[] leaves = new Node[this.numLeaves];
		dfsLeaves(leaves);
		return leaves;
	}
	
	/** Method that fills up a triple integer array in the region that a node occupies with the
	 * 	node's average color of that region
	 * @param leaf	the leaf node 
	 */
	private void nodeToRectangle(Node leaf) {
		int minRow = leaf.startRow;
		int maxRow = minRow + leaf.height;
		int minCol = leaf.startCol;
		int maxCol = minCol + leaf.width;
		for (int row = minRow; row < maxRow; row++ ) {
			for (int col = minCol; col < maxCol; col++) {
				this.modified[row][col][0] = leaf.avgColor[0];
				this.modified[row][col][1] = leaf.avgColor[1];
				this.modified[row][col][2] = leaf.avgColor[2];
			}
		}
	}

	/** Method that fills up a triple integer array in the region that a node occupies with the
	 * 	node's average color of that region, and outlines the border of the region 
	 * @param leaf	the leaf node 
	 */
	private void outlineLeafCompressed(Node leaf) {
		int minRow = leaf.startRow;
		int maxRow = minRow + leaf.height;
		int minCol = leaf.startCol;
		int maxCol = minCol + leaf.width;
		for (int row = minRow; row < maxRow; row++ ) {
			for (int col = minCol; col < maxCol; col++) {
				if (onBorder(row, col, minRow, maxRow - 1, minCol, maxCol - 1)) {
					//if on the border, draw the outline of the black square
					this.modified[row][col][0] = 0;
					this.modified[row][col][1] = 0;
					this.modified[row][col][2] = 0;
				} else {
					//else, fill the inside of the square with the average color
					this.modified[row][col][0] = leaf.avgColor[0];
					this.modified[row][col][1] = leaf.avgColor[1];
					this.modified[row][col][2] = leaf.avgColor[2];
				}
			}
		}
	}

	/** Method that fills a triple integer array that is the same size as the picture with the 
	 * rgb values of each pixel with an outline filter
	 */
	public void outlineCompressed() {
		Node[] leaves = getLeaves();
		for (Node leaf: leaves) {
		// fill the triple integer array with the average colors of each node in their respective
		//regions and outline those regions
			outlineLeafCompressed(leaf);
		}
	}


	private void outlineLeaf(Node leaf){
		int minRow = leaf.startRow;
		int maxRow = minRow + leaf.height;
		int minCol = leaf.startCol;
		int maxCol = minCol + leaf.width;
		for (int row = minRow; row < maxRow; row++ ) {
			for (int col = minCol; col < maxCol; col++) {
				if (onBorder(row, col, minRow, maxRow - 1, minCol, maxCol - 1)) {
					//if on the border, draw the outline of the black square
					this.modified[row][col][0] = 0;
					this.modified[row][col][1] = 0;
					this.modified[row][col][2] = 0;
				} else {
					//else, fill the inside of the square with the average color
					this.modified[row][col][0] = this.colors[row][col][0];
					this.modified[row][col][1] = this.colors[row][col][1];
					this.modified[row][col][2] = this.colors[row][col][2];
				}
			}
		}
	}

	public void outline(){
		Node[] leaves = getLeaves();
		for (Node leaf: leaves) {
		// fill the triple integer array with the average colors of each node in their respective
		//regions and outline those regions
			outlineLeaf(leaf);
		}
	}

	/** Method that gets the level of the quad tree that has a compression level just above the
	 * compression level asked for
	 * @param compressionLevel	the compression level asked for
	 * @return	the approximate compression level 
	 */
	private int compressionLevelToDepth(double compressionLevel) {
		int k = 0;
		while (Math.pow(4, k)/size < compressionLevel) {
			k ++;
		}
		return k;
	}
	
	private void bfsNodesAtLevel(ArrayList<Node> nodesAtDepth, int level){
		Queue<Node> q = new LinkedList<Node>();
		q.add(this.root);
		while (!q.isEmpty()){
			Node n = q.poll();
			if (n.level == level){
				nodesAtDepth.add(n);
			}
			if (n.nWest != null){
				q.add(n.nWest);
			}
			if (n.nEast != null){
				q.add(n.nEast);
			}
			if (n.sWest != null){
				q.add(n.sWest);
			}
			if (n.sEast != null){
				q.add(n.sEast);
			}
		}
	}
	
	/** Method that gets the nodes at the requested level of the quad tree
	 * @param level the requested level
	 * @return ArrayList of the nodes at the requested level
	 */
	private ArrayList<Node> getLeavesAtLevel(int level){
		ArrayList<Node> nodesAtDepth = new ArrayList<Node>();
		bfsNodesAtLevel(nodesAtDepth, level);
		return nodesAtDepth;
	}
	
	/** Method that fills a triple integer array that is the same size as the picture with the 
	 * rgb values of each pixel with a compressed filter to a compression level that is 
	 * approximately equal to the requested level
	 * @param compressionLevel
	 * @return 	the triple integer array of rgb values of the outline filter
	 */
	private void compressToLevel(double compressionLevel) {
		ArrayList<Node> leaves = getLeavesAtLevel(
					compressionLevelToDepth(compressionLevel) //approximate compression level
					);
		for (Node leaf: leaves) {
		// fill the triple integer array with the average colors of each node in their respective
		//regions 
			nodeToRectangle(leaf);
		}
	}	
	
	/**
	 * get the convoluted rgb value for one pixel with an n by n kernel 
	 * where n is odd
	 * @param kernel	n by n kernel of weights
	 * @param row	row of rgb 
	 * @param col	col of rgb
	 * @return	convoluted rgb as int[3]
	 */
	private int[] kernelN(double[][] kernel, int row, int col){
		int redOut = 0;
		int greenOut = 0;
		int blueOut = 0;
		int mid = kernel.length / 2;
		for (int r = row - mid; r <= row + mid; r++) {
			for(int c = col - 1; c <= col + 1; c++) {
				double weight = kernel[r - (row - mid)][c - (col - mid)];
				if (r <= 0 && c <= 0){
					// top left corner
					redOut += (int)(weight*colors[0][0][0]);
					greenOut += (int)(weight*colors[0][0][1]);
					blueOut += weight*colors[0][0][2];
				} else if (r < 0 && c >= 1 && c < this.width){
					// top
					redOut += (int)(weight*colors[0][c][0]);
					greenOut += (int)(weight*colors[0][c][1]);
					blueOut += (int)(weight*colors[0][c][2]);
				} else if (r <= 0 && c >= this.width - 1){
					// top right corner
					redOut += (int)(weight*colors[0][this.width - 1][0]);
					greenOut += (int)(weight*colors[0][this.width - 1][1]);
					blueOut += (int)(weight*colors[0][this.width - 1][2]);
				} else if (r > 0 && r < this.height - 1 && c >= this.width){
					// right
					redOut += (int)(weight*colors[r][this.width - 1][0]);
					greenOut += (int)(weight*colors[r][this.width - 1][1]);
					blueOut += (int)(weight*colors[r][this.width - 1][2]);
				} else if (r >= this.height - 1 && c >= this.width - 1){
					// bottom right corner
					redOut += (int)(weight*colors[this.height - 1][this.width - 1][0]);
					greenOut += (int)(weight*colors[this.height - 1][this.width - 1][1]);
					blueOut += (int)(weight*colors[this.height - 1][this.width - 1][2]);
				} else if (r >= this.height && c >= 0 && c < this.width){
					// bottom
					redOut += weight*colors[this.height - 1][c][0];
					greenOut += weight*colors[this.height - 1][c][1];
					blueOut += weight*colors[this.height - 1][c][2];
				} else if (r >= this.height - 1 && c <= 0){
					// bottom left corner
					redOut += (int)(weight*colors[this.height - 1][0][0]);
					greenOut += (int)(weight*colors[this.height - 1][0][1]);
					blueOut += (int)(weight*colors[this.height - 1][0][2]);
				} else if (r > 0 && r < this.height - 1 && c < 0){
					// left
					redOut += (int)(weight*colors[r][0][0]);
					greenOut += (int)(weight*colors[r][0][1]);
					blueOut += (int)(weight*colors[r][0][2]);
				} else {
					// inside
					redOut += (int)(weight*colors[r][c][0]);
					greenOut += (int)(weight*colors[r][c][1]);
					blueOut += (int)(weight*colors[r][c][2]);
				}
			}
		}
		int[] newRGB = {redOut, greenOut, blueOut};
		return newRGB;
	}

	private void convolution(double[][] kernel){
		for (int r = 0; r < this.height; r++){
			for(int c = 0; c < this.width; c++){
				int[] rgbNew = kernelN(kernel, r, c);	
				this.modified[r][c][0] = rgbNew[0];
				this.modified[r][c][1] = rgbNew[1];
				this.modified[r][c][2] = rgbNew[2]; 
			}
		}
	}

	public void sharpen(){
		double[][] kernel = {
			{-1.0/9, -1.0/9, -1.0/9},
			{-1.0/9, 1.0, -1.0/9},
			{-1.0/9, -1.0/9, -1.0/9}
		};
		convolution(kernel);
	}

	private void edgeConvolution(double[][] kernel){
		for (int r = 0; r < this.height; r++){
			for(int c = 0; c < this.width; c++){
				int[] rgbNew = kernelN(kernel, r, c);	
				if (rgbNew[0] > EDGETHRESHOLD &&
					rgbNew[1] > EDGETHRESHOLD &&
					rgbNew[2] > EDGETHRESHOLD){
						//make white if close to white
						this.modified[r][c][0] = 255;
						this.modified[r][c][1] = 255;
						this.modified[r][c][2] = 255;
				} else {
					// too much variation, make black
					this.modified[r][c][0] = 0;
					this.modified[r][c][1] = 0;
					this.modified[r][c][2] = 0;
				} 
			}
		}
	}

	public void edgeDetection(){
		double[][] kernel = {
			{-1.0, -1.0, -1.0},
			{-1.0, 8.0, -1.0},
			{-1.0, -1.0, -1.0}
		};
		edgeConvolution(kernel);
	}

	private void imgFunc(rgbFunction[] f){
		for (int r = 0; r < this.height; r++){
			for (int c = 0; c < this.width; c++){
				int red = colors[r][c][0];
				int green = colors[r][c][1];
				int blue = colors[r][c][2];
				this.modified[r][c][0] = f[0].run(red,green,blue);
				this.modified[r][c][1] = f[1].run(red,green,blue);
				this.modified[r][c][2] = f[2].run(red,green,blue);
			}
		}
	}

	public void grayScale(){
		rgbFunction[] rgbNew = new rgbFunction[] {
			(r, g, b) -> (int)(0.3*r + 0.59*g + 0.11*b),
			(r, g, b) -> (int)(0.3*r + 0.59*g + 0.11*b),
			(r, g, b) -> (int)(0.3*r + 0.59*g + 0.11*b)
		};
		imgFunc(rgbNew);
	}

	public void noChange(){
		rgbFunction[] rgbNew = new rgbFunction[] {
			(r, g, b) -> (int)(r),
			(r, g, b) -> (int)(g),
			(r, g, b) -> (int)(b)
		};
		imgFunc(rgbNew);
	}

	public int[][][] getModified(){
		return this.modified;
	}

	public int getNumLeaves(){
		return this.numLeaves;
	}

	public void setThreshold(int threshold){
		this.threshold = threshold;
	}
}