package com.t_oster.liblasercut;

import java.util.Random;

/**
 *
 * @author thommy
 */
public class BlackWhiteRaster extends TimeIntensiveOperation
{

  public static enum DitherAlgorithm
  {

    FLOYD_STEINBERG,
    AVERAGE,
    RANDOM,
    ORDERED,}
  int width;
  int height;
  private byte[][] raster;

  public BlackWhiteRaster(GreyscaleRaster src, DitherAlgorithm dither_algorithm, ProgressListener listener)
  {
    if (listener != null)
    {
      this.addProgressListener(listener);
    }
    this.width = src.getWidth();
    this.height = src.getHeight();
    raster = new byte[(src.getWidth() + 7) / 8][src.getHeight()];
    switch (dither_algorithm)
    {
      case FLOYD_STEINBERG:
        ditherFloydSteinberg(src);
        break;
      case AVERAGE:
        ditherAverage(src);
        break;
      case RANDOM:
        ditherRandom(src);
        break;
      case ORDERED:
        ditherOrdered(src);
        break;
    }
  }

  public BlackWhiteRaster(GreyscaleRaster src, DitherAlgorithm dither_algorithm)
  {
    this(src, dither_algorithm, null);
  }

  public BlackWhiteRaster(int width, int height, byte[][] raster)
  {
    this.width = width;
    this.height = height;
    this.raster = raster;
  }

  public BlackWhiteRaster(int width, int height)
  {
    this.width = width;
    this.height = height;
    this.raster = new byte[(width + 7) / 8][height];
  }

  public boolean isBlack(int x, int y)
  {
    int bx = x / 8;
    int ix = x % 8;
    return ((raster[bx][y] & 0xFF) & (int) Math.pow(2,ix)) != 0;
  }

  public void setBlack(int x, int y, boolean black)
  {
    int bx = x / 8;
    int ix = x % 8;
    raster[bx][y] = (byte) (((raster[bx][y] & 0xFF) & ~((int) Math.pow(2,ix))) | (black ? (int) Math.pow(2,ix) : 0 ));
  }

  /**
   * Returns the Byte where every bit represents one pixel 1=white and 0=black
   * @param x the x index of the byte, meaning 0 is the first 8 pixels (0-7), 1 
   * the pixels 8-15 ...
   * @param y the y offset
   * @return 
   */
  public byte getByte(int x, int y)
  {
    return raster[x][y];
  }

  public int getWidth()
  {
    return width;
  }

  public int getHeight()
  {
    return height;
  }
  
  private void ditherFloydSteinberg(GreyscaleRaster src)
  {
    int pixelcount = 0;
    /**
     * We have to copy the input image, because we will
     * alter the pixels during dither process and don't want
     * to destroy the input image
     */
    int[][] input = new int[src.getWidth()][2];
    for (int x = 0; x < src.getWidth(); x++)
    {
      input[x][1] = src.getGreyScale(x, 0);
    }
    for (int y = 0; y < src.getHeight(); y++)
    {
      // copy lower line to upper line
      // and read in next line from picture
      for (int x = 0; x < input.length; x++)
      {
        input[x][0] = input[x][1];
        if (y + 1 < src.getHeight())
        {
          input[x][1] = (src.getGreyScale(x, y + 1));
        }
      }

      for (int x = 0; x < input.length; x++)
      {
        this.setBlack(x, y, input[x][0] <= 127);
        int error = input[x][0] - ((input[x][0] <= 127) ? 0 : 255);
        if (x + 1 < input.length)
        {
          input[x + 1][0] = (input[x + 1][0] + 7 * error / 16);
          if (y + 1 < src.getHeight())
          {
            input[x + 1][1] = (input[x + 1][1] + 1 * error / 16);
          }
        }
        if (y + 1 < src.getHeight())
        {
          input[x][1] = (input[x][1] + 5 * error / 16);
          if (x > 0)
          {
            input[x - 1][1] = (input[x - 1][1] + 3 * error / 16);
          }
        }
      }
      setProgress((100 * pixelcount++) / (height));
    }
  }

  private void ditherAverage(GreyscaleRaster src)
  {
    int lumTotal = 0;
    int pixelcount = 0;

    for (int y = 0; y < height; y++)
    {
      for (int x = 0; x < width; x++)
      {
        lumTotal += src.getGreyScale(x, y);
      }
      setProgress((100 * pixelcount++) / (2 * height));
    }

    float thresh = lumTotal / height / width;
    for (int y = 0; y < height; y++)
    {
      for (int x = 0; x < width; x++)
      {
        this.setBlack(x, y, src.getGreyScale(x, y) < thresh);
      }
      setProgress((100 * pixelcount++) / (2 * height));
    }
  }

  private void ditherRandom(GreyscaleRaster src)
  {
    int pixelcount = 0;
    Random r = new Random();

    for (int y = 0; y < height; y++)
    {
      for (int x = 0; x < width; x++)
      {
        this.setBlack(x, y, src.getGreyScale(x, y) < r.nextInt(256));
      }
      setProgress((100 * pixelcount++) / (height));
    }
  }

  private void ditherOrdered(GreyscaleRaster src)
  {
    int nPatWid = 4;
    int[][] filter =
    {
      {
        16, 144, 48, 176
      },
      {
        208, 80, 240, 112
      },
      {
        64, 192, 32, 160
      },
      {
        256, 128, 224, 96
      },
    };

    int x = 0;
    int y = 0;
    int pixelcount = 0;

    for (y = 0; y < (height - nPatWid); y = y + nPatWid)
    {
      
      for (x = 0; x < (width - nPatWid); x = x + nPatWid)
      {

        for (int xdelta = 0; xdelta < nPatWid; xdelta++)
        {
          for (int ydelta = 0; ydelta < nPatWid; ydelta++)
          {
            this.setBlack(x + xdelta, y + ydelta, src.getGreyScale(x + xdelta, y + ydelta) < filter[xdelta][ydelta]);
          }
        }
      }
      for (int xdelta = 0; xdelta < nPatWid; xdelta++)
      {
        for (int ydelta = 0; ydelta < nPatWid; ydelta++)
        {

          if (((x + xdelta) < width) && ((y + ydelta) < height))
          {
            this.setBlack(x + xdelta, y + ydelta, src.getGreyScale(x + xdelta, y + ydelta) < filter[xdelta][ydelta]);
          }
        }
      }
      setProgress((100 * pixelcount++) / (height));
    }

    // y is at max; loop through x
    for (x = 0; x < (width); x = x + nPatWid)
    {
      for (int xdelta = 0; xdelta < nPatWid; xdelta++)
      {
        for (int ydelta = 0; ydelta < nPatWid; ydelta++)
        {

          if (((x + xdelta) < width) && ((y + ydelta) < height))
          {
            this.setBlack(x + xdelta, y + ydelta, src.getGreyScale(x + xdelta, y + ydelta) < filter[xdelta][ydelta]);
          }
        }
      }
    }
  }
}
