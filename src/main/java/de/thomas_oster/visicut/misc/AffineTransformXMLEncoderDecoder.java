package de.thomas_oster.visicut.misc;

import java.awt.geom.AffineTransform;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AffineTransformXMLEncoderDecoder {
    public static AffineTransform decodeAffineTransfrom(byte[] input) {
        double[] values = new double[6]; // Standardmäßig mit 0.0 gefüllt

        Pattern pattern = Pattern.compile("<void index=\"(\\d)\">\\s*<double>(-?\\d+\\.\\d+)</double>\\s*</void>");
        Matcher matcher = pattern.matcher(new String(input));

        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            double value = Double.parseDouble(matcher.group(2));
            if (index >= 0 && index < 6) {
                values[index] = value;
            }
        }

        return new AffineTransform(values);
    }

    public static byte[] encodeAffineTransform(AffineTransform at) {
        var bos = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(bos);
        encoder.setPersistenceDelegate(AffineTransform.class, new PersistenceDelegate()
        {//Fix for older java versions
            protected Expression instantiate(Object oldInstance, Encoder out)
            {
                AffineTransform tx = (AffineTransform) oldInstance;
                double[] coeffs = new double[6];
                tx.getMatrix(coeffs);
                return new Expression(oldInstance,
                        oldInstance.getClass(),
                        "new",
                        new Object[]
                                {
                                        coeffs
                                });
            }
        });
        encoder.writeObject(at);
        encoder.close();
        return bos.toByteArray();
    }
}
