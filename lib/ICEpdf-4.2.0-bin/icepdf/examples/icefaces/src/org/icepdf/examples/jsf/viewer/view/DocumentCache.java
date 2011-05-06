package org.icepdf.examples.jsf.viewer.view;

import org.icepdf.core.pobjects.Document;

import java.util.HashMap;

/**
 * Simple document cache which is used to limit the total number of open
 * document on the public server and hopefully reduce memory for a large
 * number of viewers.
 *
 */
public class DocumentCache extends HashMap<String, Document> {}
