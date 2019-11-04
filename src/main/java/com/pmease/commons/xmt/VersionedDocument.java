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
package com.pmease.commons.xmt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Branch;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.InvalidXPathException;
import org.dom4j.Node;
import org.dom4j.ProcessingInstruction;
import org.dom4j.QName;
import org.dom4j.Visitor;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.EntityResolver;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.xml.Dom4JReader;
import com.thoughtworks.xstream.io.xml.Dom4JWriter;

/**
Copyright 2010 Robin Shen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
**/

/**
 * This class is the bridge between bean and XML. It implements dom4j Documentation interface 
 * and can be operated with dom4j API. It also implements Serializable interface and can be 
 * serialized/deserialized in XML form.
 * @author robin
 *
 */
@SuppressWarnings("unchecked")
public final class VersionedDocument implements Document, Serializable {

	private static final long serialVersionUID = 1L;

	private static SAXReader reader = new SAXReader();
	
	public static XStream xstream = new XStream();
	
	private transient String xml;
	
	private transient Document wrapped;
	
	static {
		reader.setStripWhitespaceText(true);
		reader.setMergeAdjacentText(true);
	}

	public VersionedDocument() {
		this.wrapped = DocumentHelper.createDocument();
	}
	
	public VersionedDocument(Document wrapped) {
		this.wrapped = wrapped;
	}
	
	public void setWrapped(Document wrapped) {
		this.wrapped = wrapped;
		this.xml = null;
	}
	
	public VersionedDocument(Element wrapped) {
		wrapped.detach();
		this.wrapped = DocumentHelper.createDocument(wrapped);
	}
	
	public void setWrapped(Element wrapped) {
		wrapped.detach();
		this.wrapped = DocumentHelper.createDocument(wrapped);
		this.xml = null;
	}
	
	public Document addComment(String comment) {
		return getWrapped().addComment(comment);
	}

	public Document addDocType(String name, String publicId, String systemId) {
		return getWrapped().addDocType(name, publicId, systemId);
	}

	public Document addProcessingInstruction(String target, String text) {
		return getWrapped().addProcessingInstruction(target, text);
	}

	public Document addProcessingInstruction(String target, Map data) {
		return getWrapped().addProcessingInstruction(target, data);
	}

	public DocumentType getDocType() {
		return getWrapped().getDocType();
	}

	public EntityResolver getEntityResolver() {
		return getWrapped().getEntityResolver();
	}

	public Element getRootElement() {
		return getWrapped().getRootElement();
	}

	public String getXMLEncoding() {
		return getWrapped().getXMLEncoding();
	}

	public void setDocType(DocumentType docType) {
		getWrapped().setDocType(docType);
	}

	public void setEntityResolver(EntityResolver entityResolver) {
		getWrapped().setEntityResolver(entityResolver);
	}

	public void setRootElement(Element rootElement) {
		getWrapped().setRootElement(rootElement);
	}

	public void setXMLEncoding(String encoding) {
		getWrapped().setXMLEncoding(encoding);
	}

	public void add(Node node) {
		getWrapped().add(node);
	}

	public void add(Comment comment) {
		getWrapped().add(comment);
	}

	public void add(Element element) {
		getWrapped().add(element);
	}

	public void add(ProcessingInstruction pi) {
		getWrapped().add(pi);
	}

	public Element addElement(String name) {
		return getWrapped().addElement(name);
	}

	public Element addElement(QName qname) {
		return getWrapped().addElement(qname);
	}

	public Element addElement(String qualifiedName, String namespaceURI) {
		return getWrapped().addElement(qualifiedName, namespaceURI);
	}

	public void appendContent(Branch branch) {
		getWrapped().appendContent(branch);
	}

	public void clearContent() {
		getWrapped().clearContent();
	}

	public List content() {
		return getWrapped().content();
	}

	public Element elementByID(String elementID) {
		return getWrapped().elementByID(elementID);
	}

	public int indexOf(Node node) {
		return getWrapped().indexOf(node);
	}

	public Node node(int index) throws IndexOutOfBoundsException {
		return getWrapped().node(index);
	}

	public int nodeCount() {
		return getWrapped().nodeCount();
	}

	public Iterator nodeIterator() {
		return getWrapped().nodeIterator();
	}

	public void normalize() {
		getWrapped().normalize();
	}

	public ProcessingInstruction processingInstruction(String target) {
		return getWrapped().processingInstruction(target);
	}

	public List processingInstructions() {
		return getWrapped().processingInstructions();
	}

	public List processingInstructions(String target) {
		return getWrapped().processingInstructions(target);
	}

	public boolean remove(Node node) {
		return getWrapped().remove(node);
	}

	public boolean remove(Comment comment) {
		return getWrapped().remove(comment);
	}

	public boolean remove(Element element) {
		return getWrapped().remove(element);
	}

	public boolean remove(ProcessingInstruction pi) {
		return getWrapped().remove(pi);
	}

	public boolean removeProcessingInstruction(String target) {
		return getWrapped().removeProcessingInstruction(target);
	}

	public void setContent(List content) {
		getWrapped().setContent(content);
	}

	public void setProcessingInstructions(List listOfPIs) {
		getWrapped().setProcessingInstructions(listOfPIs);
	}

	public void accept(Visitor visitor) {
		getWrapped().accept(visitor);
	}

	public String asXML() {
		return toXML();
	}

	public Node asXPathResult(Element parent) {
		return getWrapped().asXPathResult(parent);
	}

	public XPath createXPath(String xpathExpression)
			throws InvalidXPathException {
		return getWrapped().createXPath(xpathExpression);
	}

	public Node detach() {
		return getWrapped().detach();
	}

	public Document getDocument() {
		return getWrapped().getDocument();
	}

	public String getName() {
		return getWrapped().getName();
	}

	public short getNodeType() {
		return getWrapped().getNodeType();
	}

	public String getNodeTypeName() {
		return getWrapped().getNodeTypeName();
	}

	public Element getParent() {
		return getWrapped().getParent();
	}

	public String getPath() {
		return getWrapped().getPath();
	}

	public String getPath(Element context) {
		return getWrapped().getPath(context);
	}

	public String getStringValue() {
		return getWrapped().getStringValue();
	}

	public String getText() {
		return getWrapped().getText();
	}

	public String getUniquePath() {
		return getWrapped().getUniquePath();
	}

	public String getUniquePath(Element context) {
		return getWrapped().getUniquePath(context);
	}

	public boolean hasContent() {
		return getWrapped().hasContent();
	}

	public boolean isReadOnly() {
		return getWrapped().isReadOnly();
	}

	public boolean matches(String xpathExpression) {
		return getWrapped().matches(xpathExpression);
	}

	public Number numberValueOf(String xpathExpression) {
		return getWrapped().numberValueOf(xpathExpression);
	}

	public List selectNodes(String xpathExpression) {
		return getWrapped().selectNodes(xpathExpression);
	}

	public List selectNodes(String xpathExpression,
			String comparisonXPathExpression) {
		return getWrapped().selectNodes(xpathExpression, comparisonXPathExpression);
	}

	public List selectNodes(String xpathExpression,
			String comparisonXPathExpression, boolean removeDuplicates) {
		return getWrapped().selectNodes(xpathExpression, comparisonXPathExpression, 
				removeDuplicates);
	}

	public Object selectObject(String xpathExpression) {
		return getWrapped().selectObject(xpathExpression);
	}

	public Node selectSingleNode(String xpathExpression) {
		return getWrapped().selectSingleNode(xpathExpression);
	}

	public void setDocument(Document document) {
		getWrapped().setDocument(document);
	}

	public void setName(String name) {
		getWrapped().setName(name);
	}

	public void setParent(Element parent) {
		getWrapped().setParent(parent);
	}

	public void setText(String text) {
		getWrapped().setText(text);
	}

	public boolean supportsParent() {
		return getWrapped().supportsParent();
	}

	public String valueOf(String xpathExpression) {
		return getWrapped().valueOf(xpathExpression);
	}

	public void write(Writer writer) throws IOException {
		getWrapped().write(writer);
	}
	
	/**
	 * Clone this versioned document as another versioned document.
	 */
    public Object clone() {
    	return new VersionedDocument((Document) getWrapped().clone());
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
    	oos.defaultWriteObject();
    	if (xml != null)
    		oos.writeObject(xml);
    	else
    		oos.writeObject(toXML());
    }

    private void readObject(ObjectInputStream ois) 
    		throws ClassNotFoundException, IOException {
    	ois.defaultReadObject();
    	xml = (String) ois.readObject();
    }
    
    /**
     * Convert the versioned document to UTF8 encoded XML.
     * @return
     */
	public String toXML() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputFormat format = new OutputFormat();
		format.setEncoding("UTF8");
		format.setIndent(true);
        format.setNewlines(true);
		try {
			new XMLWriter(baos, format).write(getWrapped());
			return baos.toString("UTF8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Construct the document from a XML text.
	 * @param xml UTF8 encoded XML text
	 * @return
	 */
	public static VersionedDocument fromXML(String xml) {
		synchronized (reader) {
			try {
				return new VersionedDocument(reader.read(new ByteArrayInputStream(xml.getBytes("UTF8"))));
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (DocumentException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private Document getWrapped() {
		if (wrapped == null) {
			if (xml != null) {
				wrapped = fromXML(xml).getWrapped();
				xml = null;
			}
		}
		return wrapped;
	}
	
	/**
	 * Construct the versioned document from specified bean object.
	 * @param bean
	 * @return
	 */
	public static VersionedDocument fromBean(Object bean) {
		Document dom = DocumentHelper.createDocument();
		xstream.marshal(bean, new Dom4JWriter(dom));
		VersionedDocument versionedDom = new VersionedDocument(dom);
		if (bean != null)
			versionedDom.setVersion(MigrationHelper.getVersion(bean.getClass()));
		return versionedDom;
	}
	
	/**
	 * Convert this document to bean. Migration will performed if necessary.
	 * During the migration, content of the document will also get updated 
	 * to reflect current migration result.
	 * @return
	 */
	public Object toBean() {
		return toBean(null, null);
	}
	
	/**
	 * Convert this document to bean. Migration will performed if necessary.
	 * During the migration, content of the document will also get updated 
	 * to reflect current migration result.
	 * @param listener the migration listener to receive migration events. Set to 
	 * null if you do not want to receive migration events.
	 * @return
	 */
	public Object toBean(MigrationListener listener) {
		return toBean(listener, null);
	}

	/**
	 * Convert this document to bean. Migration will performed if necessary.
	 * During the migration, content of the document will also get updated 
	 * to reflect current migration result.
	 * @param beanClass class of the bean. Class information in current document 
	 * will be used if this param is set to null
	 * @return
	 */
	public Object toBean(Class<?> beanClass) {
		return toBean(null, beanClass);
	}

	/**
	 * Convert this document to bean. Migration will performed if necessary.
	 * During the migration, content of the document will also get updated 
	 * to reflect current migration result.
	 * @param listener the migration listener to receive migration events. Set to 
	 * null if you do not want to receive migration events.
	 * @param beanClass class of the bean. Class information in current document 
	 * will be used if this param is set to null
	 * @return
	 */
	public Object toBean(MigrationListener listener, Class<?> beanClass) {	
		Dom4JReader domReader = new Dom4JReader(this);
		Class<?> origBeanClass = HierarchicalStreams.readClassType(domReader, 
				xstream.getMapper());
		if (origBeanClass == null)
			return null;
		if (beanClass == null)
			beanClass = origBeanClass;
		else 
			getRootElement().setName(xstream.getMapper().serializedClass(beanClass));
		if (getVersion() != null) {
                    if (MigrationHelper.migrate(getVersion(), beanClass, this)) {
                            setVersion(MigrationHelper.getVersion(beanClass));
                            Object bean = xstream.unmarshal(domReader);
                            if (listener != null) 
                                    listener.migrated(bean);
                            return bean;
                    } else {
                            return xstream.unmarshal(domReader);
                    }
		} else {
                    /**
                     * MODIFIED BY Thomas Oster in order to allow for migration of 
                     * older XML documents without a version attribute
                     */
                    if (MigrationHelper.migrate("0", beanClass, this)) {
                            setVersion(MigrationHelper.getVersion(beanClass));
                            Object bean = xstream.unmarshal(domReader);
                            if (listener != null) 
                                    listener.migrated(bean);
                            return bean;
                    } else {
                            return xstream.unmarshal(domReader);
                    }
                    /**
                     * END OF MODIFICATION
                     */
		}
	}
	
	/**
	 * Get version of the document
	 * @return
	 */
	public String getVersion() {
		return getRootElement().attributeValue("version");
	}
	
	/**
	 * Set version of the document
	 * @param version
	 */
	public void setVersion(String version) {
		getRootElement().addAttribute("version", version);
	}
	
}
