/*
 * Reference generator for the Java-parity tests of VisiCutRust.
 *
 * Runs each case of visicut-rust/tests/java_parity through the original
 * Java pipeline (VisiCut SVG import, VectorProfile/RasterProfile and the
 * LibLaserCut LaserToolsTechnicsCutter of the FAU device) and writes the
 * resulting LTT bytes as expected.ltt next to the case. Started by
 * generate.sh; see the README in this folder.
 *
 * LGPL-3.0-or-later, like VisiCut and LibLaserCut.
 */

import de.thomas_oster.liblasercut.LaserCutter;
import de.thomas_oster.liblasercut.properties.LaserProperty;
import de.thomas_oster.liblasercut.dithering.DitheringAlgorithm;
import de.thomas_oster.visicut.VisicutModel;
import de.thomas_oster.visicut.managers.LaserDeviceManager;
import de.thomas_oster.visicut.model.LaserDevice;
import de.thomas_oster.visicut.model.LaserProfile;
import de.thomas_oster.visicut.model.PlfPart;
import de.thomas_oster.visicut.model.Raster3dProfile;
import de.thomas_oster.visicut.model.RasterProfile;
import de.thomas_oster.visicut.model.VectorProfile;
import de.thomas_oster.visicut.model.mapping.Mapping;
import de.thomas_oster.visicut.model.mapping.MappingSet;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LttParity
{
  public static void main(String[] args) throws Exception
  {
    if (args.length < 2)
    {
      System.err.println("usage: LttParity <device.xml> <case-dir>...");
      System.exit(2);
    }
    LaserDevice device = LaserDeviceManager.getInstance().loadFromFile(new File(args[0]));
    for (int i = 1; i < args.length; i++)
    {
      generate(device, new File(args[i]));
    }
  }

  private static void generate(LaserDevice device, File dir) throws Exception
  {
    Properties c = new Properties();
    try (InputStreamReader in = new InputStreamReader(new FileInputStream(new File(dir, "case.properties")), StandardCharsets.UTF_8))
    {
      c.load(in);
    }
    LaserCutter cutter = device.getLaserCutter();
    String operation = c.getProperty("operation");
    LaserProfile profile;
    LaserProperty property;
    String prefix;
    switch (operation)
    {
      case "cut":
      case "mark":
      {
        VectorProfile vp = new VectorProfile();
        vp.setIsCut("cut".equals(operation));
        profile = vp;
        property = cutter.getLaserPropertyForVectorPart();
        prefix = "cut".equals(operation) ? "Cut" : "Mark";
        break;
      }
      case "engrave":
      {
        RasterProfile rp = new RasterProfile();
        // Class name in de.thomas_oster.liblasercut.dithering, e.g. FloydSteinberg.
        rp.setDitherAlgorithm((DitheringAlgorithm) Class.forName("de.thomas_oster.liblasercut.dithering." + c.getProperty("dither", "FloydSteinberg")).getDeclaredConstructor().newInstance());
        rp.setColorShift(Integer.parseInt(c.getProperty("color_shift", "0")));
        rp.setInvertColors(Boolean.parseBoolean(c.getProperty("invert", "false")));
        profile = rp;
        property = cutter.getLaserPropertyForRasterPart();
        property.setProperty("engrave unidirectional", Boolean.parseBoolean(c.getProperty("unidirectional", "false")));
        property.setProperty("engrave bottom up", Boolean.parseBoolean(c.getProperty("bottom_up", "false")));
        prefix = "Engrav";
        break;
      }
      case "engrave3d":
      {
        profile = new Raster3dProfile();
        property = cutter.getLaserPropertyForRaster3dPart();
        prefix = "Eng3D";
        break;
      }
      default:
        throw new IllegalArgumentException(dir + ": unknown operation " + operation);
    }
    profile.setDPI(500);
    property.setProperty("power", Float.parseFloat(c.getProperty("power")));
    property.setProperty("speed", Float.parseFloat(c.getProperty("speed")));
    // VisiCut has no pass counter: every further LaserProperty of a profile
    // processes the same objects once more.
    List<LaserProperty> props = new LinkedList<>();
    int passes = Integer.parseInt(c.getProperty("passes", "1"));
    for (int i = 0; i < passes; i++)
    {
      props.add(property.clone());
    }
    // Further parameter sets: "power:speed:passes;power:speed:passes".
    for (String set : c.getProperty("additional", "").split(";"))
    {
      if (set.isBlank())
      {
        continue;
      }
      String[] v = set.trim().split(":");
      LaserProperty p = property.clone();
      p.setProperty("power", Float.parseFloat(v[0]));
      p.setProperty("speed", Float.parseFloat(v[1]));
      for (int i = 0; i < Integer.parseInt(v[2]); i++)
      {
        props.add(p.clone());
      }
    }

    VisicutModel model = new VisicutModel();
    model.setSelectedLaserDevice(device);
    // Added to the focus of every LaserProperty (useThicknessAsFocusOffset).
    model.setMaterialThickness(Float.parseFloat(c.getProperty("thickness_mm", "0")));
    List<String> warnings = new ArrayList<>();
    PlfPart part = model.loadGraphicFile(new File(dir, c.getProperty("svg")), warnings);
    AffineTransform t = AffineTransform.getTranslateInstance(
      Double.parseDouble(c.getProperty("x_mm")), Double.parseDouble(c.getProperty("y_mm")));
    t.concatenate(part.getGraphicObjects().getBasicTransform());
    part.getGraphicObjects().setTransform(t);
    MappingSet mapping = new MappingSet();
    mapping.add(new Mapping(null, profile));
    part.setMapping(mapping);
    model.getPlfFile().add(part);

    Map<LaserProfile, List<LaserProperty>> propmap = new HashMap<>();
    propmap.put(profile, props);
    String name = jobName(prefix, c.getProperty("name"));
    File out = new File(dir, "expected.ltt");
    model.saveJob(name, out, null, propmap);
    for (String w : warnings)
    {
      System.err.println(dir.getName() + ": " + w);
    }
    System.out.println(dir.getName() + ": " + out.length() + " bytes as " + name);
  }

  /** Same device job name as VisiCutRust (prefix, ASCII, at most 15 characters). */
  private static String jobName(String prefix, String name)
  {
    StringBuilder b = new StringBuilder();
    for (char ch : (prefix + "_" + name).toCharArray())
    {
      if (b.length() == 15)
      {
        break;
      }
      boolean ok = (ch < 128 && Character.isLetterOrDigit(ch)) || ch == '-' || ch == '_';
      b.append(ok ? ch : '_');
    }
    return b.toString();
  }
}
