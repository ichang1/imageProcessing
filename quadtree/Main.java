package quadtree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.io.FileWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		final int LENGTH_LOCATION_IN_ARRAY = 1;	// Stores the location of the image length in the P3 PPM header
		final int WIDTH_LOCATION_IN_ARRAY = 0;	// Stores the location of the image width in the P3 PPM header
		final int THRESHOLD = 30;	// Base threshold for file print out (Used by QuadTree to build image)
		
		boolean outline = false;
		boolean compression = false;
		boolean edgeDetection= false;
		boolean custom = false;
		String image = "";
		String output = "";

		for (int i = 0; i < args.length; i++){
			if (args[i].equals("-i")){
				image = args[i+1];			// Filename
			} else if (args[i].equals("-o")){
                output = args[i+1];			// Name we want to save as 
            } else if (args[i].equals("-c")){
			    compression = true;			// Compression tag found
            } else if (args[i].equals("-e")){
			    edgeDetection = true;		// Edge detection tag found
            } else if (args[i].equals("-x")){
			    custom = true;				// Custom filter tag found
            } else if (args[i].equals("-t")){
			    outline = true;				// Outline tag found
            }
		}
		
		File toRead = new File("images/" + image);
		if (toRead.exists() == false){
			throw new FileNotFoundException(image + " not found in images/ directory");
		}
		BufferedImage img = null;
		try {
			img = ImageIO.read(toRead);	
		} catch (IOException e) {
		}
		int imgHeight = img.getHeight();
		int imgWidth = img.getWidth();
		int pixel_raw[];
		int[][][] colors = new int[imgHeight][imgWidth][3];
		for (int h = 0; h < imgHeight; h++){
			for (int w = 0; w < imgWidth; w++){
				pixel_raw = img.getRaster().getPixel(w, h, new int[3]);
				int[] pixel = {pixel_raw[0], pixel_raw[1], pixel_raw[2]};
				colors[h][w] = pixel;
			}
		}	
		QuadTree tree = new QuadTree(colors);
		tree.setThreshold(1000);
		tree.insert();
		tree.edgeDetection();
		int[][][] modified = tree.getModified();
		for (int h = 0; h < imgHeight; h++){
			for (int w = 0; w < imgWidth; w++){
				img.getRaster().setPixel(w, h, modified[h][w]);
			}
		}
		File out = new File("images/out.jpg");
		try {
			ImageIO.write(img, "jpg", out);
			System.out.println("Done");
		} catch (IOException e){

		}
		
		
	
	}
}