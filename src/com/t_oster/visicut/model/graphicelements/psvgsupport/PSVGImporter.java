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
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

  private Map<String, Object> parseParameters(File inputFile, List<String> warnings) throws ParserConfigurationException, SAXException, IOException
  {
    
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(inputFile);

    NodeList defs = doc.getElementsByTagName("ref");
    int totalPersons = defs.getLength();
    for (int i = 0; i < defs.getLength(); i++)
    {
      Node n = defs.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE)
      {
        NamedNodeMap attributes = n.getAttributes();
        Node param = attributes.getNamedItem("param");
        Node deflt = attributes.getNamedItem("default");
        if (param != null)
        {
          if (deflt != null)
          {
            result.put(param.getNodeValue(), Double.parseDouble(deflt.getNodeValue()));
          }
          else
          {
            warnings.add("Parameter "+param.getNodeValue()+" has no default");
          }
        }
      }
    }
    return result;
  }
  
  public FileFilter getFileFilter()
  {
    return new ExtensionFilter(".psvg", "Parametric SVG files");
  }

  public GraphicSet importFile(File inputFile, List<String> warnings) throws ImportException
  {
    try
    {
      Map<String, Object> parameters = this.parseParameters(inputFile, warnings);
      Configuration cfg = new Configuration();
      // Specify the data source where the template files come from.
      // Here I set a file directory for it:
      cfg.setDirectoryForTemplateLoading(inputFile.getParentFile());
      // Specify how templates will see the data-model. This is an advanced topic...
      // but just use this:
      cfg.setObjectWrapper(new DefaultObjectWrapper());
      cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
      
      Template t = cfg.getTemplate(inputFile.getName());
      File tmpFile = FileUtils.getNonexistingWritableFile(inputFile.getName()+".svg");
      FileWriter w = new FileWriter(tmpFile);
      t.process(parameters, w);
      w.close();
      return (new SVGImporter()).importFile(tmpFile, warnings);
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }

}
