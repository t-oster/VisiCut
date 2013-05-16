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

import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class PSVGImporter implements Importer
{

  public static FileFilter FILTER = new ExtensionFilter(".parametric.svg", "Parametric SVG files");
   
  public Map<String, Object> parseParameters(File inputFile, List<String> warnings) throws ParserConfigurationException, SAXException, IOException
  {
    
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(inputFile);

    NodeList defs = doc.getElementsByTagName("ref");
    for (int i = 0; i < defs.getLength(); i++)
    {
      Node n = defs.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE)
      {
        NamedNodeMap attributes = n.getAttributes();
        Node param = attributes.getNamedItem("param");
        Node deflt = attributes.getNamedItem("default");
        Node typeNode = attributes.getNamedItem("type");
        String type = typeNode == null ? "Double" : typeNode.getNodeValue();
        if ("Double".equals(type))
        {
          result.put(param.getNodeValue(), deflt != null ? Double.parseDouble(deflt.getNodeValue()) : (Double) 0.0);
        }
        else if ("Integer".equals(type))
        {
          result.put(param.getNodeValue(), deflt != null ? Integer.parseInt(deflt.getNodeValue()) : (Integer) 0);
        }
        else if ("Boolean".equals(type))
        {
          result.put(param.getNodeValue(), deflt != null ? Boolean.parseBoolean(deflt.getNodeValue()) : (Boolean) false);
        }
        else if ("String".equals(type))
        {
          result.put(param.getNodeValue(), deflt != null ? deflt.getNodeValue() : "");
        }
        else if ("List".equals(type))
        {
          
        }
        else
        {
          warnings.add("Unknown Parameter Type '"+type+"' for parameter '"+param.getNodeValue()+"'");
        }
      }
    }
    return result;
  }
  
  public FileFilter getFileFilter()
  {
    return FILTER;
  }

  public GraphicSet importFile(File inputFile, List<String> warnings) throws ImportException
  {
    try
    {
      Map<String, Object> parameters = this.parseParameters(inputFile, warnings);
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
  
  private IContext getContext(final Map<String, Object> parameters)
  {
    final VariablesMap<String, Object> map = new VariablesMap<String, Object>(parameters);
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
  
  public GraphicSet importFile(File inputFile, List<String> warnings, Map<String, Object> parameters) throws ImportException
  {
    try
    {
      TemplateEngine te = this.getTemplateEngine();
      File tmpFile = FileUtils.getNonexistingWritableFile(inputFile.getName()+".svg");
      FileWriter w = new FileWriter(tmpFile);
      te.process(inputFile.getAbsolutePath(), this.getContext(parameters), w);
      w.close();
      return (new SVGImporter()).importFile(tmpFile, warnings);
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }

}
