/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author thommy
 */
public interface FileConverter {
    public String[] getSupportedFileExtensions();
    public void load(File image) throws IOException;
    public void load(InputStream stream) throws IOException;
    public void convert(File output) throws ConversionException, IOException;
    public void convert(OutputStream stream) throws ConversionException, IOException;
}
