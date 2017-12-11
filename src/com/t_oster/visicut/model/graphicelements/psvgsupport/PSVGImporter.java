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
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Imports psvg files from http://www.giplt.nl/svg/ by converting them to
 * VisiCut's parametric.svg format
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PSVGImporter extends ParametricSVGImporter
{

  private static FileFilter FILTER = new ExtensionFilter(".psvg", "PSVG Files");

  @Override
  public FileFilter getFileFilter()
  {
    return FILTER;
  }

  private Map<String, Double> parameters = new LinkedHashMap<String, Double>();
  private void translateAttribute(Node n, Node parent)
  {
    if ("th:attr".equals(n.getNodeName()))
    {//already translated node
      return;
    }
    //remove all $'s in variable-names
    if ("ref".equals(parent.getNodeName()))
    {
      if ("param".equals(n.getNodeName()) && n.getNodeValue().contains("$"))
      {
        n.setNodeValue(n.getNodeValue().replace("$", ""));
      }
      else if ("default".equals(n.getNodeName()))
      {
        //evaluate expressions with help of previous parameters
        String expression = "";
        for (Entry<String, Double> e:parameters.entrySet())
        {
          expression += "var "+e.getKey()+"="+e.getValue()+";";
        }
        expression += n.getNodeValue().replace("$", "");
        Double result = Helper.evaluateExpression(expression);
        String parameterName = parent.getAttributes().getNamedItem("param").getNodeValue().replace("$", "");
        parameters.put(parameterName, result);
        n.setNodeValue(""+result);
      }
    }
    //translate attribute with {expression} to th:attr="<attname>=${expression}"
    else if (n.getNodeValue().contains("{"))
    {
      String oValue = n.getNodeValue();
      String oName = n.getNodeName();
      if (oValue.contains("${"))
      {
        System.err.println("Already translated!");
      }
      oValue = oValue.replace("$", "");
      oValue = oValue.replace("{", "'+${");
      oValue = oValue.replace("}", "}+'");
      oValue = "'" + oValue + "'";
      if (oValue.startsWith("''+${"))
      {
        oValue = oValue.substring(3);
      }
      if (oValue.endsWith("+''"))
      {
        oValue = oValue.substring(0, oValue.length()-3);
      }
      
      Node attr = parent.getAttributes().getNamedItem("th:attr");
      if (attr == null)
      {//parent has not yet a th:attr, so create one
        attr = n.getOwnerDocument().createAttribute("th:attr");
        parent.getAttributes().setNamedItem(attr);
        attr.setNodeValue(oName + "=" + oValue);
      }
      else
      {//just append to existing
        attr.setNodeValue(attr.getNodeValue()+","+oName + "=" + oValue);
      }
      //TODO: replace value through evaluated version
      parent.getAttributes().removeNamedItem(oName);
    }
  }

  private void translateAttributes(Node n)
  {
    //we need to iterate over names, because indices change while mdifying
    NamedNodeMap atts = n.getAttributes();
    List<String> attributeNames = new LinkedList<String>();
    if (atts != null)
    {
      for (int i = 0; i < atts.getLength(); i++)
      {
        attributeNames.add(atts.item(i).getNodeName());
      }
      for (String name : attributeNames)
      {
        translateAttribute(atts.getNamedItem(name), n);
      }
    }
    NodeList children = n.getChildNodes();
    if (children != null)
    {
      for (int i = 0; i < children.getLength(); i++)
      {
        translateAttributes(children.item(i));
      }
    }
  }

  public File translateToParametricSvg(File inputFile) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException
  {
    parameters.clear();
    //avoid multiple conversion through nested calls
    if (inputFile.getName().endsWith(".parametric.svg"))
    {
      return inputFile;
    }
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(inputFile);
    Attr th = doc.createAttribute("xmlns:th");
    th.setValue("http://thymeleaf.com");
    doc.getDocumentElement().getAttributes().setNamedItem(th);
    translateAttributes(doc);
    //write changed dom
    File tmp = FileUtils.getNonexistingWritableFile(inputFile.getName() + ".parametric.svg");
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    Result output = new StreamResult(tmp);
    Source input = new DOMSource(doc);
    transformer.transform(input, output);
    tmp.deleteOnExit();
    return tmp;
  }

  @Override
  public ParametricPlfPart importFile(File inputFile, List<String> warnings) throws ImportException
  {
    try
    {
      return super.importFile(translateToParametricSvg(inputFile), warnings);
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }

  @Override
  public GraphicSet importSetFromFile(File inputFile, List<String> warnings, Map<String, Parameter> parameters) throws ImportException
  {
    try
    {
      return super.importSetFromFile(translateToParametricSvg(inputFile), warnings, parameters);
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }
}
