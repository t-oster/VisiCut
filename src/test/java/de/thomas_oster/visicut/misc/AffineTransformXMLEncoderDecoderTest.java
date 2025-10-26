package de.thomas_oster.visicut.misc;

import junit.framework.TestCase;

import java.awt.geom.AffineTransform;
import java.util.List;

public class AffineTransformXMLEncoderDecoderTest extends TestCase {

    public void testDecodeAffineTransfrom() {
        var tests = List.of(new AffineTransform(), new AffineTransform(1, 2, 3.1, 4.2, 5.3, 6.5));
        for (var at : tests) {
            var at1_encoded = AffineTransformXMLEncoderDecoder.encodeAffineTransform(at);
            assertNotNull(at1_encoded);
            var at1_decoded = AffineTransformXMLEncoderDecoder.decodeAffineTransfrom(at1_encoded);
            assertEquals(at, at1_decoded);
        }
        var oldEncodedSample = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
                                "<java version=\"1.6.0_31\" class=\"java.beans.XMLDecoder\"> \n" +
                                " <object class=\"java.awt.geom.AffineTransform\"> \n" +
                                "  <array class=\"double\" length=\"6\"> \n" +
                                "   <void index=\"0\"> \n" +
                                "    <double>0.3527777777777778</double> \n" +
                                "   </void> \n" +
                                "   <void index=\"3\"> \n" +
                                "    <double>0.3527777777777778</double> \n" +
                                "   </void> \n" +
                                "   <void index=\"4\"> \n" +
                                "    <double>-207.8862438374972</double> \n" +
                                "   </void> \n" +
                                "   <void index=\"5\"> \n" +
                                "    <double>-73.74667636085336</double> \n" +
                                "   </void> \n" +
                                "  </array> \n" +
                                " </object> \n" +
                                "</java> \n").getBytes();
        assertEquals(new AffineTransform(
                0.3527777777777778,
                0,
                0,
                0.3527777777777778,
                -207.8862438374972,
                -73.74667636085336),
                AffineTransformXMLEncoderDecoder.decodeAffineTransfrom(oldEncodedSample)
        );
    }
}