/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mcp14.Binpacking;

/**
 *
 * @author Sughosh Kumar
 */

public class Constants {
    /**
     * Division factor when trying different positions to drop the piece along
     * the X axes. Increasing this factor will produce smaller horizontal steps.
     */
    public static final int DIVE_HORIZONTAL_DISPLACEMENT_FACTOR = 3;
    /**
     * Division factor for the displacement of pieces when trying to Increasing
     * this factor will produce smaller horizontal steps.
     */
    public static final int DX_SWEEP_FACTOR = 10;
    /**
     * Division factor for the displacement of pieces when trying to place
     * pieces inside others in the maximal rectangles optimization step.
     * Increasing this factor will produce smaller vertical steps.
     */
    public static final int DY_SWEEP_FACTOR = 2;
    /**
     * This is the vector of angles for which we will try to drop a piece in the
     * simulation algorithm. Adding angles to the vector will result in that we
     * will try them until a valid position is found or until all options are
     * exhausted.
     */
    public static final int[] ROTATION_ANGLES = { 0, 90 };

}