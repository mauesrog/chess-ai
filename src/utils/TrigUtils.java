package utils;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class TrigUtils {
	private int width, height; 
	Double length;
	
	public TrigUtils(int w, int h, Double l) {
		width = w;
		height = h;
		length = l;
	}
	
	public void drawDCircle(Graphics g, int x, int y, int r) {
		Double delta = r - Math.sqrt(2.0) * r;
		g.fillOval((int) (x + width / 2.0 + delta), (int) (height / 2.0 - y + delta), r, r);
	}
	
	public void drawLine(Graphics g, int x1, int y1, int x2, int y2) {
		g.drawLine((int) (x1 + width / 2.0), (int) (height / 2.0 - y1),
				(int) (width / 2.0 + x2), (int) (height / 2.0 - y2));
	}
	
	public int getCoord(int x, Double q, int type) {
		return type == 0 ? x + (int) (length * Math.cos(Math.toRadians(q))) :
			x + (int) (length * Math.sin(Math.toRadians(q)));
	}
	
	public Double getDistance(int x1, int y1, int dx2, int dy2) {
		return Math.sqrt((x1 - dx2 - width / 2.0) * (x1 - dx2 - width / 2.0) + (y1 + dy2 - height / 2.0) * (y1 + dy2 - height / 2.0));
	}
	
	public int[][] getEndpoints(int x0, int y0, Double q[]) {
		int [][]links = new int[q.length+1][2];  
		int prevx = x0, prevy = y0, newx, newy;
		Double totalQ = 0.0;
		
		links[0][0] = prevx;
		links[0][1] = prevy;
		
		for (int i = 0; i < q.length; i++) {
			totalQ += q[i];
			newx = getCoord(prevx, totalQ, 0);
			newy = getCoord(prevy, totalQ, 1);
			
			links[i+1][0] = newx;
			links[i+1][1] = newy;
			
			prevx = newx;
			prevy = newy;
		}
		
		return links;
	}
	
	public Double getRandomAngle() {
		return (double) Math.round(ThreadLocalRandom.current().nextDouble(0, 359));
	}
	
	public ArrayList<Integer> getUnterminatedIndices(Double stepSize, Double[] q1, Double[]q2) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		
		for (int i = 0; i < q1.length; i++) {
			if (Math.abs(q2[i] - q1[i]) > stepSize) { 
				indices.add(i);	
			}
		}
		
		return indices;
	}
}
