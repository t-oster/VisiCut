/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.t_oster.visicut.model.graphicelements.psvgsupport;

import com.t_oster.uicomponents.parameter.Parameter;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.VariablesMap;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ParametricSVGImporter implements Importer
{

  private static FileFilter FILTER = new ExtensionFilter(".parametric.svg", "Parametric SVG files");
  
  public Map<String, Parameter> parseParameters(File inputFile, List<String> warnings) throws ParserConfigurationException, SAXException, IOException
  {
    
    Map<String, Parameter> result = new LinkedHashMap<String, Parameter>();
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(inputFile);

    NodeList defs = doc.getElementsByTagName("ref");
    for (int i = 0; i < defs.getLength(); i++)
    {
      Node n = defs.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE)
      {
        Parameter parameter = new Parameter();
        NamedNodeMap attributes = n.getAttributes();
        Node param = attributes.getNamedItem("param");
        if (param == null)
        {
          continue;
        }
        Node deflt = attributes.getNamedItem("default");
        Node label = attributes.getNamedItem("label");
        Locale l = Locale.getDefault();
        Node label_i10n = attributes.getNamedItem("label_"+l.getLanguage()+"-"+l.getCountry());
        Node min = attributes.getNamedItem("min");
        Node max = attributes.getNamedItem("max");
        if (label_i10n != null)
        {
          parameter.label = label_i10n.getNodeValue();
        }
        else if (label != null)
        {
          parameter.label = label.getNodeValue();
        }
        else
        {
          parameter.label = param.getNodeValue();
        }
        Node typeNode = attributes.getNamedItem("type");
        String type = typeNode == null ? "Double" : typeNode.getNodeValue();
        String[] possibleValues = null;
        if (type.contains("(") && type.endsWith(")"))
        {
          String betweenBrackets = type.substring(1+type.indexOf("("),type.length()-1);
          possibleValues = betweenBrackets.split(",");
        }
        if (type.startsWith("Double"))
        {
          parameter.deflt = deflt != null ? Double.parseDouble(deflt.getNodeValue()) : null;
          parameter.value = parameter.deflt != null ? parameter.deflt : (Double) 0.0;
          if (possibleValues != null)
          {
            parameter.possibleValues = new Double[possibleValues.length];
            for (int k = 0; k < possibleValues.length; k++)
            {
              parameter.possibleValues[k] = Double.parseDouble(possibleValues[k]);
            }
          }
          if (min != null)
          {
            parameter.minValue = Double.parseDouble(min.getNodeValue());
          }
          if (max != null)
          {
            parameter.maxValue = Double.parseDouble(max.getNodeValue());
          }
        }
        else if (type.startsWith("Integer"))
        {
          parameter.deflt = deflt != null ? Integer.parseInt(deflt.getNodeValue()) : null;
          parameter.value = parameter.deflt != null ? parameter.deflt : (Integer) 0;
          if (possibleValues != null)
          {
            parameter.possibleValues = new Integer[possibleValues.length];
            for (int k = 0; k < possibleValues.length; k++)
            {
              parameter.possibleValues[k] = Integer.parseInt(possibleValues[k]);
            }
          }
          if (min != null)
          {
            parameter.minValue = Integer.parseInt(min.getNodeValue());
          }
          if (max != null)
          {
            parameter.maxValue = Integer.parseInt(max.getNodeValue());
          }
        }
        else if (type.startsWith("Boolean"))
        {
          parameter.deflt = deflt != null ? Boolean.parseBoolean(deflt.getNodeValue()) : null;
          parameter.value = parameter.deflt != null ? parameter.deflt : (Boolean) false;
        }
        else if (type.startsWith("String"))
        {
          parameter.deflt = deflt != null ? deflt.getNodeValue() : null;
          parameter.value = parameter.deflt != null ? parameter.deflt : "";
          if (possibleValues != null)
          {
            parameter.possibleValues = possibleValues;
          }
        }
        else
        {
          warnings.add("Unknown Parameter Type '"+type+"' for parameter '"+param.getNodeValue()+"'");
        }
        result.put(param.getNodeValue(), parameter);
      }
    }
    return result;
  }
  
  public FileFilter getFileFilter()
  {
    return FILTER;
  }
  
  public ParametricPlfPart importFile(File inputFile, List<String> warnings) throws ImportException
  {
    try
    {
      Map<String, Parameter> parameters =  this.parseParameters(inputFile, warnings);
      if (new File(inputFile.getAbsolutePath()+".parameters").exists())
      {
        try
        {
          FileInputStream in = new FileInputStream(new File(inputFile.getAbsolutePath()+".parameters"));
          ParametricPlfPart.unserializeParameterValues(parameters, in);
          in.close();
        }
        catch (Exception e)
        {
          warnings.add("Error loading .parameters file for "+inputFile.getName()+": "+e.getMessage());
        }  
      }
      return this.importFile(inputFile, warnings, parameters);
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }
  
  private TemplateEngine _templateEngine = null;
  private TemplateEngine getTemplateEngine()
  {
    if (_templateEngine == null)
    {
      _templateEngine = new TemplateEngine();
      FileTemplateResolver ftr = new FileTemplateResolver();
      ftr.setCacheable(false);
      _templateEngine.setTemplateResolver(ftr);
    }
    return _templateEngine;
  }
  
  private IContext getContext(final Map<String, Parameter> parameters)
  {
    final VariablesMap<String, Object> map = new VariablesMap<String, Object>();
    for (Entry<String, Parameter> e : parameters.entrySet())
    {
      map.put(e.getKey(), e.getValue().value);
    }
    return new IContext(){
  
      public VariablesMap<String, Object> getVariables()
      {
        return map;
      }

      public Locale getLocale()
      {
        return Locale.getDefault();
      }

      public void addContextExecutionInfo(String string)
      {
      }
    };
  }
  
  public GraphicSet importSetFromFile(final File inputFile, final List<String> warnings, final Map<String, Parameter> parameters) throws ImportException
  {
    try
    {
      
      SVGImporter svg = new SVGImporter();
      double resolution = svg.determineResolution(inputFile, warnings);
      final PipedOutputStream out = new PipedOutputStream();
      PipedInputStream in = new PipedInputStream(out);
      new Thread()
      {
        @Override
        public void run()
        {
          TemplateEngine te = getTemplateEngine();
          Writer w = new OutputStreamWriter(out);
          te.process(inputFile.getAbsolutePath(), getContext(parameters), w);
          try
          {
            w.close();
          }
          catch (IOException ex)
          {
            warnings.add(ex.getMessage());
          }
        }
      }.start();    
      return (new SVGImporter()).importSetFromFile(in, inputFile.getName(), resolution, warnings);
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }
  
  public ParametricPlfPart importFile(File inputFile, List<String> warnings, Map<String, Parameter> parameters) throws ImportException
  {
    try
    {
      ParametricPlfPart result = new ParametricPlfPart();
      result.setSourceFile(inputFile);
      result.setParameters(parameters);
      result.setGraphicObjects(this.importSetFromFile(inputFile, warnings, parameters));
      return result;
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }

}
