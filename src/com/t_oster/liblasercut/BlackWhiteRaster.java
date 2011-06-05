package com.t_oster.liblasercut;

/**
 *
 * @author thommy
 */
public class BlackWhiteRaster {

    public static final int DITHER_FLOYD_STEINBERG = 1;
    
    int width;
    int height;
    private byte[][] raster;

    public BlackWhiteRaster(GreyscaleRaster src, int dither_algorithm) {
        this.width = src.getWidth();
        this.height = src.getHeight();
        raster = new byte[(src.getWidth()+7)/8][src.getHeight()];
        switch (dither_algorithm) {
            case DITHER_FLOYD_STEINBERG:
                ditherFloydSteinberg(src);
        }
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
        this.raster = new byte[(width+7)/8][height];
    }
    
    public boolean isBlack(int x, int y) {
        int bx = x/8;
        int ix = x%8;
        return (raster[bx][y] & (1<<ix)) == 0; 
    }

    public void setBlack(int x, int y, boolean black) {
        int bx = x/8;
        int ix = x%8;
        raster[bx][y] = (byte) ((raster[bx][y] & ~(1<<ix))|(black ? 0 : 1<<ix));
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
    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void ditherFloydSteinberg(GreyscaleRaster src) {
        /**
         * We have to copy the input image, because we will
         * alter the pixels during dither process and don't want
         * to destroy the input image
         */
        int[][] input = new int[src.getWidth()][2];
        for (int x = 0; x < src.getWidth(); x++) {
            for (int y = 0; y < 1; y++) {
                input[x][y+1] = (src.getGreyScale(x, y) & 0xFF);
            }
        }
        for (int y = 0; y < input[0].length; y++) {
            // copy lower line to upper line
            // and read in next line from picture
            for (int x = 0; x< input.length;x++)
            {
                input[x][0] = input[x][1];
                if (y+1 < src.getHeight()){
                    input[x][1] = (src.getGreyScale(x, y+1));
                }
            }
            
            for (int x = 0; x < input.length; x++) {
                this.setBlack(x, y, input[x][0] <= 127);
                int error = input[x][0] - ((input[x][0] <= 127) ? 0 : 255);
                if (x + 1 < input.length) {
                    input[x + 1][0] = (input[x + 1][0] + 7 * error / 16);
                    if (y + 1 < src.getHeight()) {
                        input[x + 1][1] = (input[x + 1][1] + 1 * error / 16);
                    }
                }
                if (y + 1 < src.getHeight()) {
                    input[x][1] = (input[x][1] + 5 * error / 16);
                    if (x > 0) {
                        input[x - 1][1] = (input[x - 1][1] + 3 * error / 16);
                    }
                }
            }
        }
    }
}
