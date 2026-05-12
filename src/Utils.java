import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Font;

public class Utils {

  Utils() {}
 

   /** 
   * Loads image from filename into a Color (pixels decribed with rgb values) matrix.
   * 
   * @param filename the name of the imge in the filesystem.
   * @return Color matrix.
   */
  public static Color[][] loadImage(String filename) {
    BufferedImage buffImg = loadImageFile(filename);
    Color[][] colorImg = convertTo2DFromBuffered(buffImg);
    return colorImg;
  }

  /**
   * Converts image from a Color matrix to a .jpg file.
   * 
   * @param image the matrix of Color objects.
   * @param filename to the image.
   */
  public static void writeImage(Color[][] image,String filename){
    File outputfile = new File(filename);
		var bufferedImage = Utils.matrixToBuffered(image);
		try {
          ImageIO.write(bufferedImage, "jpg", outputfile);
        } catch (IOException e) {
          System.out.println("Could not write image "+filename+" !");
          e.printStackTrace();
          System.exit(1);
        }
  }


  /**
   * Loads in a BufferedImage from the specified path to be processed.
   * @param filename The path to the file to read.
   * @return a BufferedImage if able to be read, NULL otherwise.
   */
  private static BufferedImage loadImageFile(String filename) {
    BufferedImage img = null;
    try {
      img = ImageIO.read(new File(filename));
    } catch (IOException e) {
      System.out.println("Could not load image "+filename+" !");
      e.printStackTrace();
      System.exit(1);
    }
    return img;
  }

  
  /**
   *  Copy a Color matrix to another Color matrix. 
   *  Useful if one does not want to modify the original image.
   * 
   * @param image the source matrix
   * @return a copy of the image
   */
  
  public static Color[][] copyImage(Color[][] image) {
    Color[][] copy = new Color[image.length][image[0].length];
    for (int i = 0; i < image.length; i++) {
      for (int j = 0; j < image[i].length; j++) {
        copy[i][j] = image[i][j];
      }
    }
    return copy;
  }
  
  
  /**
   * Converts a matrix of Colors into a BufferedImage to 
   *  write on the filesystem.
   * 
   * @param image the matrix of Colors
   * @return the image ready for writing to filesystem
   */
  private static BufferedImage matrixToBuffered(Color[][] image) {
    int width = image.length;
    int height = image[0].length;
    BufferedImage bImg = new BufferedImage(width, height, 1);

    for (int x = 0; x < width; x++) {
      for(int y = 0; y < height; y++) {
        bImg.setRGB(x,  y, image[x][y].getRGB());
      }
    }
    return bImg;
  }

  /**
   * Converts a file loaded into a BufferedImage to a 
   * matrix of Colors
   * 
   * @param image the BufferedImage to convert
   * @return the matrix of Colors
   */

  private static Color[][] convertTo2DFromBuffered(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    Color[][] result = new Color[width][height];

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // Get the integer RGB, and separate it into individual components.
        // (BufferedImage saves RGB as a single integer value).
        int pixel = image.getRGB(x, y);
        //int alpha = (pixel >> 24) & 0xFF;
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        result[x][y] = new Color(red, green, blue);
      }
    }
    return result;
  }

  /**
   * Compute a luminosity histogram (0..255) from a Color matrix.
   * Uses the same luminosity formula as Filters.computeLuminosity.
   */
  public static int[] computeLuminosityHistogram(Color[][] image) {
    int[] hist = new int[256];
    for (int x = 0; x < image.length; x++) {
      for (int y = 0; y < image[x].length; y++) {
        Color p = image[x][y];
        int r = p.getRed();
        int g = p.getGreen();
        int b = p.getBlue();
        int lum = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
        if (lum < 0) lum = 0; if (lum > 255) lum = 255;
        hist[lum]++;
      }
    }
    return hist;
  }

  /**
   * Write a simple PNG bar-plot of the histogram to disk.
   * width should be >= 256 for reasonable rendering.
   */
  public static void writeHistogramPNG(int[] hist, String filename, int width, int height) {
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    try {
      // background
      g.setColor(java.awt.Color.WHITE);
      g.fillRect(0, 0, width, height);

      // find max for scaling
      int max = 1;
      long total = 0;
      for (int v : hist) { if (v > max) max = v; total += v; }

      // margins
      int top = 10, bottom = 30, left = 10, right = 10;
      int plotW = width - left - right;
      int plotH = height - top - bottom;

      // draw baseline
      g.setColor(java.awt.Color.LIGHT_GRAY);
      g.setStroke(new BasicStroke(1f));
      g.drawRect(left, top, plotW, plotH);

      // draw bars
      double barW = (double) plotW / 256.0;
      g.setColor(java.awt.Color.DARK_GRAY);
      for (int i = 0; i < 256; i++) {
        double h = ((double) hist[i] / (double) max) * (double) plotH;
        int bx = left + (int) Math.floor(i * barW);
        int bw = Math.max(1, (int) Math.ceil(barW));
        int by = top + plotH - (int) Math.round(h);
        g.fillRect(bx, by, bw, (int) Math.round(h));
      }

      // draw some labels: total and max
      g.setColor(java.awt.Color.BLACK);
      g.setFont(new Font("SansSerif", Font.PLAIN, 12));
      g.drawString(String.format("total=%d", total), left + 4, height - 10);
      g.drawString(String.format("max=%d", max), width - right - 60, height - 10);

    } finally {
      g.dispose();
    }

    try {
      ImageIO.write(img, "png", new File(filename));
    } catch (IOException e) {
      System.err.println("Could not write histogram image: " + filename + " -> " + e.getMessage());
    }
  }

}