/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 *  $Id$
 */
package org.exist.memtree;

import org.exist.dom.QName;
import org.exist.dom.QNameable;
import org.exist.storage.DBBroker;
import org.exist.xquery.value.Type;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


/**
 * A dynamically constructed namespace node. Used to track namespace
 * declarations in elements. Implements Attr, so it can be treated as a normal
 * attribute.
 * 
 * @author wolf
 */
public class NamespaceNode extends NodeImpl implements Attr, QNameable {

    /**
     * @param doc
     * @param nodeNumber
     */
    public NamespaceNode(DocumentImpl doc, int nodeNumber) {
        super(doc, nodeNumber);
    }
    
    /* (non-Javadoc)
     * @see org.exist.memtree.NodeImpl#getNodeType()
     */
    public short getNodeType() {
        return NodeImpl.NAMESPACE_NODE;
    }

    /* (non-Javadoc)
     * @see org.exist.memtree.NodeImpl#getType()
     */
    public int getType() {
        return Type.NAMESPACE;
    }
    
    public String getPrefix() {
    	return "xmlns";
    }

	public boolean getSpecified() {
		return true;
	}

	public QName getQName() {
		return (QName)document.namePool.get(document.namespaceCode[nodeNumber]);
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getLocalName()
	 */
	public String getLocalName() {
		return getQName().getLocalName();
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeName()
	 */
	public String getNodeName() {
		return getQName().toString();
	}
	
	public String getName() {
		return getQName().toString();
	}

	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Attr#getValue()
	 */
	public String getValue() {
		return getQName().getNamespaceURI();
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Attr#setValue(java.lang.String)
	 */
	public void setValue(String value) throws DOMException {
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Attr#getOwnerElement()
	 */
	public Element getOwnerElement() {
		return (Element)document.getNode(document.namespaceParent[nodeNumber]);
	}

	/** ? @see org.w3c.dom.Attr#getSchemaTypeInfo()
	 */
	public TypeInfo getSchemaTypeInfo() {
		// maybe TODO - new DOM interfaces - Java 5.0
		return null;
	}

	/** ? @see org.w3c.dom.Attr#isId()
	 */
	public boolean isId() {
		// maybe TODO - new DOM interfaces - Java 5.0
		return false;
	}
}
