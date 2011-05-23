/** @file cups-epilog.c - Epilog cups driver */

/* @file cups-epilog.c @verbatim
 *========================================================================
 * Copyright © 2002-2008 Andrews & Arnold Ltd <info@aaisp.net.uk>
 * Copyright 2008 AS220 Labs <brandon@as220.org>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *========================================================================
 *
 *
 * Author: Andrew & Arnold Ltd and Brandon Edens
 *
 *
 * Description:
 * Epilog laser engraver
 *
 * The Epilog laser engraver comes with a windows printer driver. This works
 * well with Corel Draw, and that is about it. There are other windows
 * applications, like inkscape, but these rasterise the image before sending to
 * the windows printer driver, so there is no way to use them to vector cut!
 *
 * The cups-epilog app is a cups backend, so build and link/copy to
 * /usr/lib/cups/backend/epilog. It allows you to print postscript to the laser
 * and both raster and cut. It works well with inkscape.
 *
 * With this linux driver, vector cutting is recognised by any line or curve in
 * 100% red (1.0 0.0 0.0 setrgbcolor).
 *
 * Create printers using epilog://host/Legend/options where host is the
 * hostname or IP of the epilog engraver. The options are as follows. This
 * allows you to make a printer for each different type of material.
 * af	Auto focus (0=no, 1=yes)
 * r	Resolution 75-1200
 * rs	Raster speed 1-100
 * rp	Raster power 0-100
 * vs	Vector speed 1-100
 * vp	Vector power 1-100
 * vf	Vector frequency 10-5000
 * sc	Photograph screen size in pizels, 0=threshold, +ve=line, -ve=spot, used
 *      in mono mode, default 8.
 * rm	Raster mode mono/grey/colour
 *
 * The mono raster mode uses a line or dot screen on any grey levels or
 * colours. This can be controlled with the sc parameter. The default is 8,
 * which makes a nice fine line screen on 600dpi engraving. At 600/1200 dpi,
 * the image is also lightened to allow for the size of the laser point.
 *
 * The grey raster mode maps the grey level to power level. The power level is
 * scaled to the raster power setting (unlike the windows driver which is
 * always 100% in 3D mode).
 *
 * In colour mode, the primary and secondary colours are processed as separate
 * passes, using the grey level of the colour as a power level. The power level
 * is scaled to the raster power setting. Note that red is 100% red, and non
 * 100% green and blue, etc, so 50% red, 0% green/blue is not counted as red,
 * but counts as "grey". 100% red, and 50% green/blue counts as red, half
 * power. This means you can make distinct raster areas of the page so that you
 * do not waste time moving the head over blank space between them.
 *
 * Epolog cups driver
 * Uses gs to rasterise the postscript input.
 * URI is epilog://host/Legend/options
 * E.g. epilog://host/Legend/rp=100/rs=100/vp=100/vs=10/vf=5000/rm=mono/flip/af
 * Options are as follows, use / to separate :-
 * rp   Raster power
 * rs   Raster speed
 * vp   Vector power
 * vs   Vector speed
 * vf   Vector frequency
 * w    Default image width (pt)
 * h    Default image height (pt)
 * sc   Screen (lpi = res/screen, 0=simple threshold)
 * r    Resolution (dpi)
 * af   Auto focus
 * rm   Raster mode (mono/grey/colour)
 * flip X flip (for reverse cut)
 * simulate  Don't actually send output to the printer, just generate files (mainly for debug purposes)
 * relative Print just the content starting upper left, ignore page geometries from input files
 * Raster modes:-
 * mono Screen applied to grey levels
 * grey Grey levels are power (scaled to raster power setting)
 * colour       Each colour grey/red/green/blue/cyan/magenta/yellow plotted
 * separately, lightness=power
 *
 *
 * Installation:
 * gcc -o epilog `cups-config --cflags` cups-epilog.c `cups-config --libs`
 * http://www.cups.org/documentation.php/api-overview.html
 *
 * Manual testing can be accomplished through execution akin to:
 * $ export DEVICE_URI="epilog://epilog-mini/Legend/rp=100/rs=100/vp=100/vs=10/vf=5000/rm=grey"
 * # ./epilog job user title copies options
 * $ ./epilog 123 jdoe test 1 options < hello-world.ps
 *
 */


/*************************************************************************
 * includes
 */
#include <ctype.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/signal.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>


/*************************************************************************
 * local defines
 */

/** Default on whether or not auto-focus is enabled. */
#define AUTO_FOCUS (0)

/** Default bed height (y-axis) in pts. */
#define BED_HEIGHT (864)

/** Default bed width (x-axis) in pts. */
#define BED_WIDTH (1728)

/** Number of bytes in the bitmap header. */
#define BITMAP_HEADER_NBYTES (54)

/** Default for debug mode. */
#define DEBUG (1)

/** Default for simulate */
#define SIMULATE (0)

/** Default for relative mode */
#define RELATIVE (0)

/** Basename for files generated by the program. */
#define FILE_BASENAME "epilog"

/** Number of characters allowable for a filename. */
#define FILENAME_NCHARS (128)

/** Default on whether or not the result is supposed to be flipped along the X
 * axis.
 */
#define FLIP (0)

/** Maximum allowable hostname characters. */
#define HOSTNAME_NCHARS (1024)

/** Additional offset for the X axis. */
#define HPGLX (0)

/** Additional offset for the Y axis. */
#define HPGLY (0)

/** Whether or not to rotate the incoming PDF 90 degrees clockwise. */
#define PDF_ROTATE_90 (1)

/** Accepted number of points per an inch. */
#define POINTS_PER_INCH (72)

/** Maximum wait before timing out on connecting to the printer (in seconds). */
#define PRINTER_MAX_WAIT (300)

/** Default mode for processing raster engraving (varying power depending upon
 * image characteristics).
 * Possible values are:
 * 'c' = color determines power level
 * 'g' = grey-scale levels determine power level
 * 'm' = mono mode
 * 'n' = no rasterization
 */
#define RASTER_MODE_DEFAULT 'm'

/** Default power level for raster engraving */
#define RASTER_POWER_DEFAULT (40)

/** Whether or not the raster printing is to be repeated. */
#define RASTER_REPEAT (1)

/** Default speed for raster engraving */
#define RASTER_SPEED_DEFAULT (100)

/** Default resolution is 600 DPI */
#define RESOLUTION_DEFAULT (600)

/** Pixel size of screen (0 is threshold).
 * FIXME - add more details
 */
#define SCREEN_DEFAULT (8)

/** Number of seconds per a minute. */
#define SECONDS_PER_MIN (60)

/** Temporary directory to store files. */
#define TMP_DIRECTORY "/tmp"

/** FIXME */
#define VECTOR_FREQUENCY_DEFAULT (500)

/** Default power level for vector cutting. */
#define VECTOR_POWER_DEFAULT (50)

/** Default speed level for vector cutting. */
#define VECTOR_SPEED_DEFAULT (30)


/*************************************************************************
 * local types
 */


/*************************************************************************
 * local variables
 */

/** Temporary buffer for building out strings. */
static char buf[102400];

/** Determines whether or not debug is enabled. */
static char debug = DEBUG;

/** Determines whether or not output should not be sended to the printer. */
static char simulate = SIMULATE;

/** Determines if just the BoundingBox should be printed (in other words print the object, not the page) */
static char relative = RELATIVE;

/** Variable to track auto-focus. */
static int focus = AUTO_FOCUS;

/** Variable to track whether or not the X axis should be flipped. */
static char flip = FLIP;

/** Height of the image (y-axis). By default this is the bed's height. */
static int height = BED_HEIGHT;

/** Job name for the print. */
static char *job_name = "";

/** User name that submitted the print job. */
static char *job_user = "";

/** Title for the job print. */
static char *job_title = "";

/** Number of copies to produce of the print. */
static char *job_copies = "";

/** Printer queue specified options. */
static char *job_options = "";

/** Variable to track the resolution of the print. */
static int resolution = RESOLUTION_DEFAULT;

/** Variable to track the mode for rasterization. One of color 'c', or
 * grey-scale 'g', mono 'm', or none 'n'
 */
static char raster_mode = RASTER_MODE_DEFAULT;

/** Variable to track the raster speed. */
static int raster_speed = RASTER_SPEED_DEFAULT;

/** Variable to track the raster power. */
static int raster_power = RASTER_POWER_DEFAULT;

/** Variable to track whether or not a rasterization should be repeated. */
static int raster_repeat = RASTER_REPEAT;

/** FIXME -- pixel size of screen, 0= threshold */
static int screen = SCREEN_DEFAULT;

/** The DEVICE_URI for the printer. */
static char *device_uri = "";

/** Options for the printer. */
static char *queue = "";

/** Variable to track the vector speed. */
static int vector_speed = VECTOR_SPEED_DEFAULT;

/** Variable to track the vector power. */
static int vector_power = VECTOR_POWER_DEFAULT;

/** Variable to track the vector frequency. FIXME */
static int vector_freq = VECTOR_FREQUENCY_DEFAULT;

/** Width of the image (x-axis). By default this is the bed's width. */
static int width = BED_WIDTH;            // default bed

/** X re-center (0 = not). */
static int x_center;

/** Track whether or not to repeat X. */
static int x_repeat = 1;

/** Y re-center (0 = not). */
static int y_center;

/** Track whether or not to repeat X. */
static int y_repeat = 1;


/*************************************************************************
 * local functions
 */
static int big_to_little_endian(uint8_t *position, int bytes);
static bool execute_ghostscript(char *filename_bitmap, char *filename_eps,
                                char *filename_vector,
                                char *bmp_mode, int resolution,
                                int height, int width);
static bool generate_raster(FILE *pjl_file, FILE *bitmap_file);
static bool generate_vector(FILE *pjl_file, FILE *vector_file);
static bool generate_pjl(FILE *bitmap_file, FILE *pjl_file, FILE *vector_file);
static bool ps_to_eps(FILE *ps_file, FILE *eps_file);
static bool process_job_title_commands(char *title);
static bool process_queue_options(char *queue);
static void range_checks(void);
static int printer_connect(const char *host, const int timeout);
static bool printer_disconnect(int socket_descriptor);
static bool printer_send(const char *host, FILE *pjl_file);


/*************************************************************************/

/**
 * Convert a big endian value stored in the array starting at the given pointer
 * position to its little endian value.
 *
 * @param position the starting location for the conversion. Each successive
 * unsigned byte is upto nbytes is considered part of the value.
 * @param nbytes the number of successive bytes to convert.
 *
 * @return An integer containing the little endian value of the successive
 * bytes.
 */
static int
big_to_little_endian(uint8_t *position, int nbytes)
{
    int i;
    int result = 0;

    for (i = 0; i < nbytes; i++) {
        result += *(position + i) << (8 * i);
    }
    return result;
}

/**
 * Execute ghostscript feeding it an ecapsulated postscript file which is then
 * converted into a bitmap image. As a byproduct output of the ghostscript
 * process is redirected to a .vector file which will contain instructions on
 * how to perform a vector cut of lines within the postscript.
 *
 * @param filename_bitmap the filename to use for the resulting bitmap file.
 * @param filename_eps the filename to read in encapsulated postscript from.
 * @param filename_vector the filename that will contain the vector
 * information.
 * @param bmp_mode a string which is one of bmp16m, bmpgray, or bmpmono.
 * @param resolution the encapsulated postscript resolution.
 * @param height the postscript height in points per inch.
 * @param width the postscript width in points per inch.
 *
 * @return Return true if the execution of ghostscript succeeds, false
 * otherwise.
 */
static bool
execute_ghostscript(char *filename_bitmap,
                    char *filename_eps,
                    char *filename_vector,
                    char *bmp_mode, int resolution,
                    int height, int width)
{
    char buf[8192];
    sprintf(buf,
            "/usr/bin/gs -q -dBATCH -dNOPAUSE -r%d -g%dx%d -sDEVICE=%s \
-sOutputFile=%s %s > %s",
            resolution,
            (width * resolution) / POINTS_PER_INCH,
            (height * resolution) / POINTS_PER_INCH,
            bmp_mode,
            filename_bitmap,
            filename_eps,
            filename_vector);
    if (debug) {
        fprintf(stderr, "%s\n", buf);
    }
    if (system(buf)) {
        return false;
    }
    return true;
}

/**
 *
 */
static bool
generate_raster(FILE *pjl_file, FILE *bitmap_file)
{
    int h;
    int d;
    int offx;
    int offy;
    int basex = 0;
    int basey = 0;
    int repeat;

    uint8_t bitmap_header[BITMAP_HEADER_NBYTES];

    if (x_center) {
        basex = x_center - width / 2;
    }
    if (y_center) {
        basey = y_center - height / 2;
    }
    if (basex < 0) {
        basex = 0;
    }
    if (basey < 0) {
        basey = 0;
    }
    // rasterises
    basex = basex * resolution / POINTS_PER_INCH;
    basey = basey * resolution / POINTS_PER_INCH;

    repeat = raster_repeat;
    while (repeat--) {
        /* repeated (over printed) */
        int pass;
        int passes;
        long base_offset;
        if (raster_mode == 'c') {
            passes = 7;
        } else {
            passes = 1;
        }

        /* Read in the bitmap header. */
        fread(bitmap_header, 1, BITMAP_HEADER_NBYTES, bitmap_file);

        /* Re-load width/height from bmp as it is possible that someone used
         * setpagedevice or some such
         */
        /* Bytes 18 - 21 are the bitmap width (little endian format). */
        width = big_to_little_endian(bitmap_header + 18, 4);

        /* Bytes 22 - 25 are the bitmap height (little endian format). */
        height = big_to_little_endian(bitmap_header + 22, 4);

        /* Bytes 10 - 13 base offset for the beginning of the bitmap data. */
        base_offset = big_to_little_endian(bitmap_header + 10, 4);


        if (raster_mode == 'c' || raster_mode == 'g') {
            /* colour/grey are byte per pixel power levels */
            h = width;
            /* BMP padded to 4 bytes per scan line */
            d = (h * 3 + 3) / 4 * 4;
        } else {
            /* mono */
            h = (width + 7) / 8;
            /* BMP padded to 4 bytes per scan line */
            d = (h + 3) / 4 * 4;
        }
        if (debug) {
            fprintf(stderr, "Width %d Height %d Bytes %d Line %d\n",
                    width, height, h, d);
        }

        /* Raster Orientation */
        fprintf(pjl_file, "\e*r0F");
        /* Raster power */
        fprintf(pjl_file, "\e&y%dP",
                (raster_mode == 'c' ||
                 raster_mode == 'g') ? 100 : raster_power);
        /* Raster speed */
        fprintf(pjl_file, "\e&z%dS", raster_speed);
        fprintf(pjl_file, "\e*r%dT", height * y_repeat);
        fprintf(pjl_file, "\e*r%dS", width * x_repeat);
        /* Raster compression */
        fprintf(pjl_file, "\e*b%dM", (raster_mode == 'c' || raster_mode == 'g')
                ? 7 : 2);
        /* Raster direction (1 = up) */
        fprintf(pjl_file, "\e&y1O");

        if (debug) {
            /* Output raster debug information */
            fprintf(stderr, "Raster power=%d speed=%d\n",
                    ((raster_mode == 'c' || raster_mode == 'g') ?
                     100 : raster_power),
                    raster_speed);
        }

        /* start at current position */
        fprintf(pjl_file, "\e*r1A");
        for (offx = width * (x_repeat - 1); offx >= 0; offx -= width) {
            for (offy = height * (y_repeat - 1); offy >= 0; offy -= height) {
                for (pass = 0; pass < passes; pass++) {
                    // raster (basic)
                    int y;
                    char dir = 0;

                    fseek(bitmap_file, base_offset, SEEK_SET);
                    for (y = height - 1; y >= 0; y--) {
                        int l;

                        switch (raster_mode) {
                        case 'c':      // colour (passes)
                        {
                            char *f = buf;
                            char *t = buf;
                            if (d > sizeof (buf)) {
                                perror("Too wide");
                                return false;
                            }
                            l = fread ((char *)buf, 1, d, bitmap_file);
                            if (l != d) {
                                fprintf(stderr, "Bad bit data from gs %d/%d (y=%d)\n", l, d, y);
                                return false;
                            }
                            while (l--) {
                                // pack and pass check RGB
                                int n = 0;
                                int v = 0;
                                int p = 0;
                                int c = 0;
                                for (c = 0; c < 3; c++) {
                                    if (*f > 240) {
                                        p |= (1 << c);
                                    } else {
                                        n++;
                                        v += *f;
                                    }
                                    f++;
                                }
                                if (n) {
                                    v /= n;
                                } else {
                                    p = 0;
                                    v = 255;
                                }
                                if (p != pass) {
                                    v = 255;
                                }
                                *t++ = 255 - v;
                            }
                        }
                        break;
                        case 'g':      // grey level
                        {
                            /* BMP padded to 4 bytes per scan line */
                            int d = (h + 3) / 4 * 4;
                            if (d > sizeof (buf)) {
                                fprintf(stderr, "Too wide\n");
                                return false;
                            }
                            l = fread((char *)buf, 1, d, bitmap_file);
                            if (l != d) {
                                fprintf (stderr, "Bad bit data from gs %d/%d (y=%d)\n", l, d, y);
                                return false;
                            }
                            for (l = 0; l < h; l++) {
                                buf[l] = (255 - (uint8_t)buf[l]);
                            }
                        }
                        break;
                        default:       // mono
                        {
                            int d = (h + 3) / 4 * 4;  // BMP padded to 4 bytes per scan line
                            if (d > sizeof (buf))
                            {
                                perror("Too wide");
                                return false;
                            }
                            l = fread((char *) buf, 1, d, bitmap_file);
                            if (l != d)
                            {
                                fprintf(stderr, "Bad bit data from gs %d/%d (y=%d)\n", l, d, y);
                                return false;
                            }
                        }
                        }

                        if (raster_mode == 'c' || raster_mode == 'g') {
                            for (l = 0; l < h; l++) {
                                /* Raster value is multiplied by the
                                 * power scale.
                                 */
                                buf[l] = (uint8_t)buf[l] * raster_power / 255;
                            }
                        }

                        /* find left/right of data */
                        for (l = 0; l < h && !buf[l]; l++) {
                            ;
                        }

                        if (l < h) {
                            /* a line to print */
                            int r;
                            int n;
                            unsigned char pack[sizeof (buf) * 5 / 4 + 1];
                            for (r = h - 1; r > l && !buf[r]; r--) {
                                ;
                            }
                            r++;
                            fprintf(pjl_file, "\e*p%dY", basey + offy + y);
                            fprintf(pjl_file, "\e*p%dX", basex + offx +
                                    ((raster_mode == 'c' || raster_mode == 'g') ? l : l * 8));
                            if (dir) {
                                fprintf(pjl_file, "\e*b%dA", -(r - l));
                                // reverse bytes!
                                for (n = 0; n < (r - l) / 2; n++){
                                    unsigned char t = buf[l + n];
                                    buf[l + n] = buf[r - n - 1];
                                    buf[r - n - 1] = t;
                                }
                            } else {
                                fprintf(pjl_file, "\e*b%dA", (r - l));
                            }
                            dir = 1 - dir;
                            // pack
                            n = 0;
                            while (l < r) {
                                int p;
                                for (p = l; p < r && p < l + 128 && buf[p]
                                         == buf[l]; p++) {
                                    ;
                                }
                                if (p - l >= 2) {
                                    // run length
                                    pack[n++] = 257 - (p - l);
                                    pack[n++] = buf[l];
                                    l = p;
                                } else {
                                    for (p = l;
                                         p < r && p < l + 127 &&
                                             (p + 1 == r || buf[p] !=
                                              buf[p + 1]);
                                         p++) {
                                        ;
                                    }

                                    pack[n++] = p - l - 1;
                                    while (l < p) {
                                        pack[n++] = buf[l++];
                                    }
                                }
                            }
                            fprintf(pjl_file, "\e*b%dW", (n + 7) / 8 * 8);
                            r = 0;
                            while (r < n)
                                fputc(pack[r++], pjl_file);
                            while (r & 7)
                            {
                                r++;
                                fputc(0x80, pjl_file);
                            }
                        }
                    }
                }
            }
        }
        fprintf(pjl_file, "\e*rC");       // end raster
        fputc(26, pjl_file);      // some end of file markers
        fputc(4, pjl_file);
    }
    return true;
}

/**
 *
 */
static bool
generate_vector(FILE *pjl_file, FILE *vector_file)
{
    char up = 1;           // output status
    char newline = 1;      // input status (last was M)
    char started = 0;
    int sx = 0;
    int sy = 0;
    int lx = 0;
    int ly = 0;
    int power = 100;
    int offx;
    int offy;
    int basex = 0;
    int basey = 0;

    if (x_center) {
        basex = x_center - width / 2;
    }
    if (y_center) {
        basey = y_center - height / 2;
    }
    if (basex < 0) {
        basex = 0;
    }
    if (basey < 0) {
        basey = 0;
    }
    // rasterises
    basex = basex * resolution / POINTS_PER_INCH;
    basey = basey * resolution / POINTS_PER_INCH;


    for (offy = height * (y_repeat - 1); offy >= 0; offy -= height) {
        for (offx = width * (x_repeat - 1); offx >= 0; offx -= width) {
            char passstart = 0;
            rewind(vector_file);
            while (fgets((char *) buf, sizeof (buf), vector_file)) {
                if (isalpha(*buf)) {
                    int x,
                        y;
                    if (!passstart) {
                        passstart = 1;
                        fprintf(pjl_file, "IN;");
                        fprintf(pjl_file, "XR%04d;", vector_freq);
                        fprintf(pjl_file, "YP%03d;", vector_power);
                        fprintf(pjl_file, "ZS%03d;", vector_speed);


                    }
                    switch (*buf) {
                    case 'M': // move
                        if (sscanf((char *) buf + 1, "%d,%d", &y, &x)
                            == 2) {
                            sx = x;
                            sy = y;
                            newline = 1;
                        }
                        break;
                    case 'C': // close - only makes sense after an "L"
                        if (newline == 0 && up == 0 && (lx != sx || ly
                                                        != sy)) {
                            fprintf(pjl_file, ",%d,%d", basex + offx + sx +
                                    HPGLX, basey + offy + sy + HPGLY);
                        }
                        break;
                    case 'P': // power
                        if (sscanf((char *)buf + 1, "%d", &x) == 1
                            && x != power) {
                            int epower;
                            power = x;
                            if (!started) {
                                started = 1;
                                /* XXX disabled as current code path inserts
                                 *  this statement AFTER the IN; statement.
                                 */
                                /* start HPGL */
/*                                 fprintf(pjl_file, "\e%%1B");     */
                            }
                            if (!up) {
                                fprintf(pjl_file, ";PU");
                            }
                            up = 1;
                            epower = (power * vector_power + 50) / 100;
                            if (vector_speed && vector_speed < 100) {
                                int espeed = vector_speed;
                                int efreq = vector_freq;
                                if (epower && x < 100) {
                                    int r;
                                    int q;
                                    r = 10000 / x; // power, up to set power level (i.e. x=100)
                                    q = 10000 / espeed;
                                    if (q < r)
                                        r = q;
                                    q = 500000 / efreq;
                                    if (q < r)
                                        r = q;
                                    epower = (50 + epower * r) / 100;
                                    espeed = (50 + espeed * r) / 100;
                                    efreq = (50 + espeed * r) / 100;
                                }
                                fprintf(pjl_file, ";ZS%03d;XR%04d;", espeed, efreq);
                            }
                            fprintf(pjl_file, ";YP%03d;", epower);
                        }
                        break;
                    case 'L': // line
                        if (!started) {
                            started = 1;
                            //fprintf(pjl_file, "\e%%1B;");      // start HPGL
                        }
                        if (newline) {
                            if (!up)
                                fprintf(pjl_file, ";");
                            fprintf(pjl_file, "PU%d,%d",
                                    basex + offx + sx + HPGLX,
                                    basey + offy + sy + HPGLY);
                            up = 1;
                            newline = 0;
                        }
                        if (up) {
                            fprintf(pjl_file, ";PD");
                        } else {
                            fprintf(pjl_file, ",");
                        }
                        up = 0;
                        if (sscanf ((char *) buf + 1, "%d,%d", &y, &x)
                            == 2) {
                            fprintf (pjl_file, "%d,%d", basex + offx + x +
                                     HPGLX, basey + offy + y + HPGLY);
                        }
                        lx = x;
                        ly = y;
                        break;
                    }
                    if (*buf == 'X')
                        break;
                }
            }
        }
    }
    if (started) {
        if (up == 0)
            fprintf(pjl_file, ";");
        fprintf(pjl_file, "\e%%0B");      // end HLGL
    }
    fprintf(pjl_file, "\e%%1BPU");  // start HLGL, and pen up, end
    return true;
}


/**
 *
 */
static bool
generate_pjl(FILE *bitmap_file, FILE *pjl_file,
                              FILE *vector_file)
{
    int i;

    /* Print the printer job language header. */
    fprintf(pjl_file, "\e%%-12345X@PJL JOB NAME=%s\r\n", job_title);
    fprintf(pjl_file, "\eE@PJL ENTER LANGUAGE=PCL\r\n");
    /* Set autofocus on or off. */
    fprintf(pjl_file, "\e&y%dA", focus);
    /* Left (long-edge) offset registration.  Adjusts the position of the
     * logical page across the width of the page.
     */
    fprintf(pjl_file, "\e&l0U");
    /* Top (short-edge) offset registration.  Adjusts the position of the
     * logical page across the length of the page.
     */
    fprintf(pjl_file, "\e&l0Z");

    /* Resolution of the print. */
    fprintf(pjl_file, "\e&u%dD", resolution);
    /* X position = 0 */
    fprintf(pjl_file, "\e*p0X");
    /* Y position = 0 */
    fprintf(pjl_file, "\e*p0Y");
    /* PCL resolution. */
    fprintf(pjl_file, "\e*t%dR", resolution);

    /* If raster power is enabled and raster mode is not 'n' then add that
     * information to the print job.
     */
    if (raster_power && raster_mode != 'n') {

        /* FIXME unknown purpose. */
        fprintf(pjl_file, "\e&y0C");

        /* We're going to perform a raster print. */
        generate_raster(pjl_file, bitmap_file);
    }

    /* If vector power is > 0 then add vector information to the print job. */
    if (vector_power) {
        fprintf(pjl_file, "\eE@PJL ENTER LANGUAGE=PCL\r\n");
        /* Page Orientation */
        fprintf(pjl_file, "\e*r0F");
        fprintf(pjl_file, "\e*r%dT", height * y_repeat);
        fprintf(pjl_file, "\e*r%dS", width * x_repeat);
        fprintf(pjl_file, "\e*r1A");
        fprintf(pjl_file, "\e*rC");
        fprintf(pjl_file, "\e%%1B");

        /* We're going to perform a vector print. */
        generate_vector(pjl_file, vector_file);
    }

    /* Footer for printer job language. */
    /* Reset */
    fprintf(pjl_file, "\eE");
    /* Exit language. */
    fprintf(pjl_file, "\e%%-12345X");
    /* End job. */
    fprintf(pjl_file, "@PJL EOJ \r\n");
    /* Pad out the remainder of the file with 0 characters. */
    for(i = 0; i < 4096; i++) {
        fputc(0, pjl_file);
    }
    return true;
}

/**
 * Convert the given postscript file (ps) converting it to an encapsulated
 * postscript file (eps).
 *
 * @param ps_file a file handle pointing to an opened postscript file that
 * is to be converted.
 * @param eps_file a file handle pointing to the opened encapsulated
 * postscript file to store the result.
 *
 * @return Return true if the function completes its task, false otherwise.
 */
static bool
ps_to_eps(FILE *ps_file, FILE *eps_file)
{
	fprintf(stderr, "Debug: ps_to_eps called");
    int xoffset = 0;
    int yoffset = 0;

    int l;
    while (fgets((char *)buf, sizeof (buf), ps_file)) {
        fprintf(eps_file, "%s", (char *)buf);
        if (*buf != '%') {
            break;
        }
        if ((relative && !strncasecmp((char *) buf, "%%BoundingBox:", 13)) || (!relative && !strncasecmp((char *) buf, "%%PageBoundingBox:", 18))) {
            int lower_left_x;
            int lower_left_y;
            int upper_right_x;
            int upper_right_y;
            if (sscanf((char *)buf + 14, "%d %d %d %d",
                       &lower_left_x,
                       &lower_left_y,
                       &upper_right_x,
                       &upper_right_y) == 4) {
                xoffset = lower_left_x;
                yoffset = lower_left_y;
		fprintf(stderr, "Debug: Previous size:%dx%d", width, height);
                width = (upper_right_x - lower_left_x);
                height = (upper_right_y - lower_left_y);
		fprintf(stderr, "Debug: EPS has size:%dx%d", width, height);                
		fprintf(eps_file, "/setpagedevice{pop}def\n"); // use bbox
                if (xoffset || yoffset) {
                    fprintf(eps_file, "%d %d translate\n", -xoffset, -yoffset);
                }
                if (flip) {
                    fprintf(eps_file, "%d 0 translate -1 1 scale\n", width);
                }
            }
        }
        if (!strncasecmp((char *) buf, "%!", 2)) {
            fprintf
                (eps_file,
                 "/==={(        )cvs print}def/stroke{currentrgbcolor 0.0 \
eq exch 0.0 eq and exch 0.0 ne and{(P)=== currentrgbcolor pop pop 100 mul \
round  cvi = flattenpath{transform(M)=== round cvi ===(,)=== round cvi \
=}{transform(L)=== round cvi ===(,)=== round cvi =}{}{(C)=}pathforall \
newpath}{stroke}ifelse}bind def/showpage{(X)= showpage}bind def\n");
            if (raster_mode != 'c' && raster_mode != 'g') {
                if (screen == 0) {
                    fprintf(eps_file, "{0.5 ge{1}{0}ifelse}settransfer\n");
                } else {
                    int s = screen;
                    if (resolution >= 600) {
                        // adjust for overprint
                        fprintf(eps_file,
                                "{dup 0 ne{%d %d div add}if}settransfer\n",
                                resolution / 600, s);
                    }
                    fprintf(eps_file, "%d 30{%s}setscreen\n", resolution / s,
                            (screen > 0) ? "pop abs 1 exch sub" :
                            "180 mul cos exch 180 mul cos add 2 div");
                }
            }
        }
    }
    while ((l = fread ((char *) buf, 1, sizeof (buf), ps_file)) > 0) {
        fwrite ((char *) buf, 1, l, eps_file);
    }
    return true;
}


/**
 * The print job title can affect the functioning of the software. Job titles
 * can take three forms:
 * xRXxRYx specify that the execution of the job should repeat.
 * cCXcCYc specify the center location for the print (in inches).
 * nofocus if autofocus is to be disabled.
 *
 * @param the print job title.
 * @return True if the system is able to process the job title, false otherwise.
 */
static bool
process_job_title_commands(char *title)
{
    // xRXxRYx for repeat, cCXcCYc is centre (mm)
    char *p = title;
    char *d;
    if (*p++ == 'x') {
        if (isdigit(*p)) {
            d = p;
            while (isdigit(*p)) {
                p++;
            }
            if (*p++ == 'x') {
                x_repeat = atoi(d);
                title = p;
                if (isdigit(*p)) {
                    d = p;
                    while (isdigit(*p)) {
                        p++;
                    }
                    if (*p++ == 'x') {
                        y_repeat = atoi(d);
                        title = p;
                    }
                }
            }
        }
    }
    p = title;
    if (*p++ == 'c') {
        if (isdigit(*p)) {
            d = p;
            while (isdigit(*p)) {
                p++;
            }
            if (*p++ == 'c') {
                x_center = atoi(d) * POINTS_PER_INCH;
                title = p;
                if (isdigit(*p)) {
                    d = p;
                    while (isdigit(*p)) {
                        p++;
                    }
                    if (*p++ == 'c') {
                        y_center = atoi(d) * POINTS_PER_INCH;
                        title = p;
                    }
                }
            }
        }
    }
    if (!strncmp(job_title, "nofocus", 7)) {
        title += 7;
        focus = 0;
    }
    return true;
}

/**
 * Process the queue options which take a form akin to:
 * @verbatim
 * /rp=100/rs=100/vp=100/vs=10/vf=5000/rm=mono/flip/af
 * @endverbatim
 * Each option is separated by the '/' character and can be specified as
 * /name=value or just /name (for boolean values).
 *
 * This function has the side effect of modifying the global variables
 * specifying major settings for the device.
 *
 * @param queue a string containing the options that are to be interpreted and
 * assigned to the global variables controlling printer characteristics.
 *
 * @return True if the function is able to complete its task, false otherwise.
 */
static bool
process_queue_options(char *queue_options)
{
    char *o = strchr(queue_options, '/');

    if (!queue_options) {
        fprintf(stderr, "URI syntax epilog://host/queue_optionsname\n");
        return false;
    }
    *queue_options++ = 0;

    if (o) {
        *o++ = 0;
        while (*o) {
            char *t = o,
                *v = "1";
            while (isalpha(*o)) {
                o++;
            }
            if (*o == '=') {
                *o++ = 0;
                v = o;
                while (*o && (isalnum(*o) || *o == '-')) {
                    o++;
                }
            }
            while (*o && !isalpha(*o)) {
                *o++ = 0;
            }
            if (!strcasecmp(t, "af")) {
                focus = atoi(v);
            }
            if (!strcasecmp(t, "r")) {
                resolution = atoi(v);
            }
            if (!strcasecmp(t, "rs")) {
                raster_speed = atoi(v);
            }
            if (!strcasecmp(t, "rp")) {
                raster_power = atoi(v);
            }
            if (!strcasecmp(t, "rm")) {
                raster_mode = tolower(*v);
            }
            if (!strcasecmp(t, "rr")) {
                raster_repeat = atoi(v);
            }
            if (!strcasecmp(t, "vs")) {
                vector_speed = atoi(v);
            }
            if (!strcasecmp(t, "vp")) {
                vector_power = atoi(v);
            }
            if (!strcasecmp(t, "vf")) {
                vector_freq = atoi(v);
            }
            if (!strcasecmp(t, "sc")) {
                screen = atoi(v);
            }
            if (!strcasecmp(t, "w")) {
                width = atoi(v);
            }
            if (!strcasecmp(t, "h")) {
                height = atoi(v);
            }
            if (!strcasecmp(t, "flip")) {
                flip = 1;
            }
            if (!strcasecmp(t, "debug")) {
                debug = 1;
            }
             if (!strcasecmp(t, "simulate")) {
                simulate = 1;
            }
             if (!strcasecmp(t, "relative")) {
                relative = 1;
            }
        }
    }
    return true;
}

/**
 * Perform range validation checks on the major global variables to ensure
 * their values are sane. If values are outside accepted tolerances then modify
 * them to be the correct value.
 *
 * @return Nothing
 */
static void
range_checks(void)
{
    if (raster_power > 100) {
        raster_power = 100;
    }
    if (raster_power < 0) {
        raster_power = 0;
    }

    if (raster_speed > 100) {
        raster_speed = 100;
    }
    if (raster_speed < 1) {
        raster_speed = 1;
    }

    if (resolution > 1200) {
        resolution = 1200;
    }
    if (resolution < 75) {
        resolution = 75;
    }

    if (screen < 1) {
        screen = 1;
    }

    if (vector_freq < 10) {
        vector_freq = 10;
    }
    if (vector_freq > 5000) {
        vector_freq = 5000;
    }

    if (vector_power > 100) {
        vector_power = 100;
    }
    if (vector_power < 0) {
        vector_power = 0;
    }

    if (vector_speed > 100) {
        vector_speed = 100;
    }
    if (vector_speed < 1) {
        vector_speed = 1;
    }
}

/**
 * Connect to a printer.
 *
 * @param host The hostname or IP address of the printer to connect to.
 * @param timeout The number of seconds to wait before timing out on the
 * connect operation.
 * @return A socket descriptor to the printer.
 */
static int
printer_connect(const char *host, const int timeout)
{
    int socket_descriptor = -1;
    int i;

    for (i = 0; i < timeout; i++) {
        struct addrinfo *res;
        struct addrinfo *addr;
        struct addrinfo base = { 0, PF_UNSPEC, SOCK_STREAM };
        int error_code = getaddrinfo(host, "printer", &base, &res);

        /* Set an alarm to go off if the program has gone out to lunch. */
        alarm(SECONDS_PER_MIN);

        /* If getaddrinfo did not return an error code then we attempt to
         * connect to the printer and establish a socket.
         */
        if (!error_code) {
            for (addr = res; addr; addr = addr->ai_next) {
                socket_descriptor = socket(addr->ai_family, addr->ai_socktype,
                                           addr->ai_protocol);
                if (socket_descriptor >= 0) {
                    if (!connect(socket_descriptor, addr->ai_addr,
                                 addr->ai_addrlen)) {
                        break;
                    } else {
                        close(socket_descriptor);
                        socket_descriptor = -1;
                    }
                }
            }
            freeaddrinfo(res);
        }
        if (socket_descriptor >= 0) {
            break;
        }

        /* Sleep for a second then try again. */
        sleep(1);
    }
    if (i >= timeout) {
        fprintf(stderr, "Cannot connect to %s\n", host);
        return -1;
    }
    /* Disable the timeout alarm. */
    alarm(0);
    /* Return the newly opened socket descriptor */
    return socket_descriptor;
}

/**
 * Disconnect from a printer.
 *
 * @param socket_descriptor the descriptor of the printer that is to be
 * disconnected from.
 * @return True if the printer connection was successfully closed, false otherwise.
 */
static bool
printer_disconnect(int socket_descriptor)
{
    int error_code;
    error_code = close(socket_descriptor);
    /* Report on possible errors to standard error. */
    if (error_code == -1) {
        switch (errno) {
        case EBADF:
            perror("Socket descriptor given was not valid.");
            break;
        case EINTR:
            perror("Closing socket descriptor was interrupted by a signal.");
            break;
        case EIO:
            perror("I/O error occurred during closing of socket descriptor.");
            break;
        }
    }

    /* Return status of disconnect operation to the calling function. */
    if (error_code) {
        return false;
    } else {
        return true;
    }
}

/**
 *
 */
static bool
printer_send(const char *host, FILE *pjl_file)
{
    char localhost[HOSTNAME_NCHARS] = "";
    unsigned char lpdres;
    int socket_descriptor = -1;

    gethostname(localhost, sizeof(localhost));
    {
        char *d = strchr(localhost, '.');
        if (d) {
            *d = 0;
        }
    }

    /* Connect to the printer. */
    socket_descriptor = printer_connect(host, PRINTER_MAX_WAIT);

    // talk to printer
    sprintf(buf, "\002%s\n", queue);
    write(socket_descriptor, (char *)buf, strlen(buf));
    read(socket_descriptor, &lpdres, 1);
    if (lpdres) {
        fprintf (stderr, "Bad response from %s, %u\n", host, lpdres);
        return false;
    }
    sprintf(buf, "H%s\n", localhost);
    sprintf(buf + strlen(buf) + 1, "P%s\n", job_user);
    sprintf(buf + strlen(buf) + 1, "J%s\n", job_title);
    sprintf(buf + strlen(buf) + 1, "ldfA%s%s\n", job_name, localhost);
    sprintf(buf + strlen(buf) + 1, "UdfA%s%s\n", job_name, localhost);
    sprintf(buf + strlen(buf) + 1, "N%s\n", job_title);
    sprintf(buf + strlen(buf) + 1, "\002%d cfA%s%s\n", (int)strlen(buf), job_name, localhost);
    write(socket_descriptor, buf + strlen(buf) + 1, strlen(buf + strlen(buf) + 1));
    read(socket_descriptor, &lpdres, 1);
    if (lpdres) {
        fprintf(stderr, "Bad response from %s, %u\n", host, lpdres);
        return false;
    }
    write(socket_descriptor, (char *)buf, strlen(buf) + 1);
    read(socket_descriptor, &lpdres, 1);
    if (lpdres) {
        fprintf(stderr, "Bad response from %s, %u\n", host, lpdres);
        return false;
    }
    {
        {
            struct stat file_stat;
            if (fstat(fileno(pjl_file), &file_stat)) {
                perror(buf);
                return false;
            }
            sprintf((char *) buf, "\003%u dfA%s%s\n", (int) file_stat.st_size, job_name, localhost);
        }
        write(socket_descriptor, (char *)buf, strlen(buf));
        read(socket_descriptor, &lpdres, 1);
        if (lpdres) {
            fprintf(stderr, "Bad response from %s, %u\n", host, lpdres);
            return false;
        }
        {
            int l;
            while ((l = fread((char *)buf, 1, sizeof (buf), pjl_file)) > 0) {
                write(socket_descriptor, (char *)buf, l);
            }
        }
    }
    // dont wait for a response...
    printer_disconnect(socket_descriptor);
    return true;
}

/**
 * Main entry point for the program.
 *
 * @param argc The number of command line options passed to the program.
 * @param argv An array of strings where each string represents a command line
 * argument.
 * @return An integer where 0 represents successful termination, any other
 * value represents an error code.
 */
int
main(int argc, char *argv[])
{

    /** The printer hostname. */
    static char *host = "";

    /* Strings designating filenames. */
    char file_basename[FILENAME_NCHARS];
    char filename_bitmap[FILENAME_NCHARS];
    char filename_cups_debug[FILENAME_NCHARS];
    char filename_eps[FILENAME_NCHARS];
    char filename_pdf[FILENAME_NCHARS];
    char filename_pjl[FILENAME_NCHARS];
    char filename_ps[FILENAME_NCHARS];
    char filename_vector[FILENAME_NCHARS];

    /* File handles. */
    FILE *file_debug;
    FILE *file_bitmap;
    FILE *file_eps;
    FILE *file_cups;
    FILE *file_pdf;
    FILE *file_ps;
    FILE *file_ps_output;
    FILE *file_pjl;
    FILE *file_vector;

    /* Temporary variables. */
    int l;

    /*
     * Process the command line arguments specified by the user. Proper command
     * line arguments are fed to this program from cups and will be of a form akin
     * to:
     * @verbatim
     * job_number user job_title num_copies queue_options
     * 123        jdoe myjob     1          asdf
     * @endverbatim
     */
    if (argc == 1) {
        printf("direct epilog \"Unknown\" \"Epilog laser (thin red lines vector cut)\"\n");
        return 1;
    }
    if (argc > 1) {
        job_name = argv[1];
    }
    if (argc > 2) {
        job_user = argv[2];
    }
    if (argc > 3) {
        job_title = argv[3];
    }
    if (argc > 4) {
        job_copies = argv[4];
    }
    if (argc > 5) {
        job_options = argv[5];
    }

    /* Gather the site information from the user's environment variable. */
    device_uri = getenv("DEVICE_URI");
    if (!device_uri) {
        fprintf(stderr, "No $DEVICE_URI set\n");
        return 0;
    }
    host = strstr(device_uri, "//");
    if (!host) {
        fprintf(stderr, "URI syntax epilog://host/queuename\n");
        return 0;
    }
    host += 2;

    /* Process the queue arguments. */
    queue = strchr(host, '/');
    if (!process_queue_options(queue)) {
        fprintf(stderr, "Error processing epilog queue options.");
        return 0;
    }

    /* Perform a check over the global values to ensure that they have values
     * that are within a tolerated range.
     */
    range_checks();

    /* Determine and set the names of all files that will be manipulated by the
     * program.
     */
    sprintf(file_basename, "%s/%s-%d", TMP_DIRECTORY, FILE_BASENAME, getpid());
    sprintf(filename_bitmap, "%s.bmp", file_basename);
    sprintf(filename_eps, "%s.eps", file_basename);
    sprintf(filename_pjl, "%s.pjl", file_basename);
    sprintf(filename_vector, "%s.vector", file_basename);

    /* Gather the postscript file from either standard input or a filename
     * specified as a command line argument.
     */
    if (argc > 6) {
        file_cups = fopen(argv[6], "r");
    } else {
        file_cups = stdin;
    }
    if (!file_cups) {
        perror((argc > 6) ? argv[6] : "stdin");
        return 1;
    }

    /* Write out the incoming cups data if debug is enabled. */
    if (debug) {
        /* We save the incoming cups data to the filesystem. */
        sprintf(filename_cups_debug, "%s.cups", file_basename);
        file_debug = fopen(filename_cups_debug, "w");

        /* Check that file handle opened. */
        if (!file_debug) {
            perror(filename_cups_debug);
            return 1;
        }

        /* Write cups data to the filesystem. */
        while ((l = fread((char *)buf, 1, sizeof(buf), file_cups)) > 0) {
            fwrite((char *)buf, 1, l, file_debug);
        }
        fclose(file_debug);
        /* In case file_cups pointed to stdin we close the existing file handle
         * and switch over to using the debug file handle.
         */
        fclose(file_cups);
        file_cups = fopen(filename_cups_debug, "r");
    }

    /* Check whether the incoming data is ps or pdf data. */
    fread((char *)buf, 1, 4, file_cups);
    rewind(file_cups);
    if (strncasecmp((char *)buf, "%PDF", 4) == 0) {
        /* We have a pdf file. */

        /* Setup the filename for the output pdf file. */
        sprintf(filename_pdf, "%s.pdf", file_basename);

        /* Open the destination pdf file. */
        file_pdf = fopen(filename_pdf, "w");
        if (!file_pdf) {
            perror(filename_pdf);
            return 1;
        }

        /* Write the cups data out to the file_pdf. */
        while ((l = fread((char *)buf, 1, sizeof(buf), file_cups)) > 0) {
            fwrite((char *)buf, 1, l, file_pdf);
        }

        fclose(file_cups);
        fclose(file_pdf);

        if (PDF_ROTATE_90) {
            /* Configuration specifies that PDF will be rotated 90 degrees
             * clockwise. Use the program pdftk to rotate the pdf.
             */
            sprintf(buf, "/usr/bin/pdftk %s cat endE output %s_rotated.pdf",
                    filename_pdf, file_basename);
            /* If debug is enabled then print the command to be executed. */
            if (debug) {
                fprintf(stderr, "%s\n", buf);
            }
            /* Execute the pdftk command. */
            if (system(buf)) {
                fprintf(stderr, "Failure to execute pdftk. Quitting...");
                return 1;
            }
            /* Cleanup old pdf file if debug not enabled. */
            if (!debug) {
                /* Debug is disabled so remove non-rotated pdf file. */
                if (unlink(filename_pdf)) {
                    perror(filename_pdf);
                }
            }
            /* Reset the pdf filename to the rotated file's name. */
            sprintf(filename_pdf, "%s_rotated.pdf", file_basename);
        }

        /* Setup the postscript output filename. */
        sprintf(filename_ps, "%s.ps", file_basename);

        /* Execute the command pdf2ps to convert the pdf file to ps. */
        sprintf(buf, "/usr/bin/pdf2ps %s %s", filename_pdf, filename_ps);
        if (debug) {
            fprintf(stderr, "%s\n", buf);
        }
        if (system(buf)) {
            fprintf(stderr, "Failure to execute pdf2ps. Quitting...");
            return 1;
        }

        if (!debug) {
            /* Debug is disabled so remove generated pdf file. */
            if (unlink(filename_pdf)) {
                perror(filename_pdf);
            }
        }

        /* Set file_ps to the generated ps file. */
        file_ps  = fopen(filename_ps, "r");
    } else {
        /* Input file is postscript. Set the file_ps handle to file_cups. */
        file_ps = file_cups;
    }

    /* Open the encapsulated postscript file for writing. */
    file_eps = fopen(filename_eps, "w");
    if (!file_eps) {
        perror(filename_eps);
        return 1;
    }
    /* Convert postscript to encapsulated postscript. */
    if (!ps_to_eps(file_ps, file_eps)) {
        perror("Error converting postscript to encapsulated postscript.");
        fclose(file_eps);
        return 1;
    }
    /* Cleanup after encapsulated postscript creation. */
    fclose(file_eps);
    if (file_ps != stdin) {
        fclose(file_ps);
        if (!debug && unlink(filename_ps)) {
            perror(filename_ps);
        }
    }

    if(!execute_ghostscript(filename_bitmap,
                            filename_eps,
                            filename_vector,
                            (raster_mode == 'c') ? "bmp16m" :
                            (raster_mode == 'g') ? "bmpgray" : "bmpmono",
                            resolution, height, width)) {
        perror("Failure to execute ghostscript command.\n");
        return 1;
    }

    /* The print job title has the capability to modify characteristics of the
     * print. Check for embedded information and possibly update the job title
     * before sending the title to the printer.
     */
    if (!*job_title || !strcasecmp(job_title, "_stdin_") ||
        !strcasecmp(job_title, "(stdin)") || !strncasecmp (job_title, "print ", 6)) {
        sprintf(job_title, "%dx%din", width / POINTS_PER_INCH,
                height / POINTS_PER_INCH);
    } else {
        process_job_title_commands(job_title);
    }

    /* Open file handles needed by generation of the printer job language
     * file.
     */
    file_bitmap = fopen(filename_bitmap, "r");
    file_vector = fopen(filename_vector, "r");
    file_pjl = fopen(filename_pjl, "w");
    if (!file_pjl) {
        perror(filename_pjl);
        return 1;
    }
    /* Execute the generation of the printer job language (pjl) file. */
    if (!generate_pjl(file_bitmap, file_pjl, file_vector)) {
        perror("Generation of pjl file failed.\n");
        fclose(file_pjl);
        return 1;
    }
    /* Close open file handles. */
    fclose(file_bitmap);
    fclose(file_pjl);
    fclose(file_vector);

    /* Cleanup unneeded files provided that debug mode is disabled. */
    if (!debug) {
        if (unlink(filename_bitmap)) {
            perror(filename_bitmap);
        }
        if (unlink(filename_eps)) {
            perror(filename_eps);
        }
        if (unlink(filename_vector)) {
            perror(filename_vector);
        }
    }

    /* Open printer job language file. */
    file_pjl = fopen(filename_pjl, "r");
    if (!file_pjl) {
        perror(filename_pjl);
        return 1;
    }
    /* Send print job to printer. */
    if (!simulate && !printer_send(host, file_pjl)) {
        perror("Could not send pjl file to printer.\n");
        return 1;
    }
    fclose(file_pjl);
    if (!debug) {
        if (unlink(filename_pjl)) {
            perror(filename_pjl);
        }
    }

    return 0;
}

