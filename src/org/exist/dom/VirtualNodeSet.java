
/* eXist Open Source Native XML Database
 * Copyright (C) 2001-03,  Wolfgang M. Meier (meier@ifs.tu-darmstadt.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * $Id$
 * 
 */
package org.exist.dom;

import java.util.Iterator;

import org.dbxml.core.data.Value;
import org.exist.xquery.Constants;
import org.exist.xquery.NodeTest;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.SequenceIterator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This node set is called virtual because it is just a placeholder for
 * the set of relevant nodes. For XPath expressions like //* or //node(), 
 * it would be totally unefficient to actually retrieve all descendant nodes.
 * In many cases, the expression can be resolved at a later point in time
 * without retrieving the whole node set. 
 *
 * VirtualNodeSet basically provides method getFirstParent to retrieve the first
 * matching descendant of its context according to the primary type axis.
 *
 * Class LocationStep will always return an instance of VirtualNodeSet
 * if it finds something like descendant::* etc..
 *
 * @author Wolfgang Meier
 * @author Timo Boehme
 */
public class VirtualNodeSet extends AbstractNodeSetBase {

	protected int axis = -1;
	protected NodeTest test;
	protected NodeSet context;
	protected NodeSet realSet = null;
	protected boolean realSetIsComplete = false;
	protected boolean inPredicate = false;
	protected boolean useSelfAsContext = false;

	public VirtualNodeSet(int axis, NodeTest test, NodeSet context) {
		this.axis = axis;
		this.test = test;
		this.context = context;
	}

	public boolean contains(DocumentImpl doc, long nodeId) {
		NodeProxy first = getFirstParent(new NodeProxy(doc, nodeId), null, (axis == Constants.SELF_AXIS), 0);
		// Timo Boehme: getFirstParent returns now only real parents
		//              therefore test if node is child of context
		//return (first != null);
		return (
			(first != null)
				|| (context.get(doc, XMLUtil.getParentId(doc, nodeId)) != null));
	}

	public boolean contains(NodeProxy p) {
		NodeProxy first = getFirstParent(p, null, (axis == Constants.SELF_AXIS), 0);
		// Timo Boehme: getFirstParent returns now only real parents
		//              therefore test if node is child of context
		//return (first != null);
		return (
			(first != null)
				|| (context.get(p.getDocument(), XMLUtil.getParentId(p.getDocument(), p.gid)) != null));
	}

	public void setInPredicate(boolean predicate) {
		inPredicate = predicate;
	}

	/* (non-Javadoc)
	 * @see org.exist.dom.AbstractNodeSet#getDocumentSet()
	 */
	public DocumentSet getDocumentSet() {
	    return context.getDocumentSet();
	}

	protected NodeProxy getFirstParent(NodeProxy node, long gid, boolean includeSelf) {
		return getFirstParent(node, null, includeSelf, true, 0);
	}

	protected NodeProxy getFirstParent(
		NodeProxy node,
		NodeProxy first,
		boolean includeSelf,
		int recursions) {
		return getFirstParent(node, first, includeSelf, true, recursions);
	}

	protected NodeProxy getFirstParent(
		NodeProxy node,
		NodeProxy first,
		boolean includeSelf,
		boolean directParent,
		int recursions) {
		long pid = XMLUtil.getParentId(node.getDocument(), node.gid);
		NodeProxy parent;
		// check if the start-node should be included, e.g. to process an
		// expression like *[. = 'xxx'] 
		if (recursions == 0 && includeSelf && test.matches(node)) {
			if (axis == Constants.CHILD_AXIS) {
				// if we're on the child axis, test if
				// the node is a direct child of the context node
				if ((parent = context.get(new NodeProxy(node.getDocument(), pid))) != null) {
					node.copyContext(parent);
					if (useSelfAsContext && inPredicate) {
						node.addContextNode(node);
					} else if (inPredicate)
						node.addContextNode(parent);
					return node;
				}
			} else
				// descendant axis: remember the node and continue 
				first = node;
		}
		// if this is the first call to this method, remember the first parent node
		// and re-evaluate the method
		if (first == null) {
			if (pid < 0) {
				// given node was already document element -> no parent				
				return null;
			}
			first = new NodeProxy(node.getDocument(), pid, Node.ELEMENT_NODE);
			// Timo Boehme: we need a real parent (child from context)
			return getFirstParent(first, first, false, directParent, recursions + 1);
		}

		// is pid member of the context set?
		parent = context.get(node.getDocument(), pid);

		if (parent != null && test.matches(first)) {
			if (axis != Constants.CHILD_AXIS) {
				// if we are on the descendant-axis, we return the first node 
				// we found while walking bottom-up.
				// Otherwise, we return the last one (which is node)
				node = first;
			}
			node.copyContext(parent);
			if (useSelfAsContext && inPredicate) {
				node.addContextNode(node);
			} else if (inPredicate) {
				node.addContextNode(parent);
			}
			// Timo Boehme: we return the ancestor which is child of context
			// TODO 
			return node;
		} else if (pid < 0) {
			// no matching node has been found in the context
			return null;
		} else if (directParent && axis == Constants.CHILD_AXIS && recursions == 1) {
			// break here if the expression is like /*/n
			return null;
		} else {
			// continue for expressions like //*/n or /*//n
			parent = new NodeProxy(node.getDocument(), pid, Node.ELEMENT_NODE);
			return getFirstParent(parent, first, false, directParent, recursions + 1);
		}
	}

	public NodeProxy nodeHasParent(
		DocumentImpl doc,
		long gid,
		boolean directParent,
		boolean includeSelf,
		int level) {
		final NodeProxy p =
			getFirstParent(new NodeProxy(doc, gid), null, includeSelf, directParent, 0);
		if (p != null)
			addInternal(p);
		return p;
	}

	public NodeProxy nodeHasParent(
		NodeProxy node,
		boolean directParent,
		boolean includeSelf,
		int level) {
		final NodeProxy p = getFirstParent(node, null, includeSelf, directParent, 0);
		if (p != null)
			addInternal(p);
		return p;
	}

	private void addInternal(NodeProxy p) {
		if (realSet == null)
			realSet = new ExtArrayNodeSet(256);
		realSet.add(p);
		realSetIsComplete = false;
	}

	public NodeProxy parentWithChild(
		DocumentImpl doc,
		long gid,
		boolean directParent,
		boolean includeSelf,
		int level) {
		NodeProxy first =
			getFirstParent(new NodeProxy(doc, gid), null, includeSelf, directParent, 0);
		if (first != null)
			addInternal(first);
		return first;
	}

	public NodeProxy parentWithChild(
		NodeProxy proxy,
		boolean directParent,
		boolean includeSelf,
		int level) {
		NodeProxy first = getFirstParent(proxy, null, includeSelf, directParent, 0);
		if (first != null) {
			addInternal(first);
		}
		return first;
	}

	private final NodeSet getNodes() {
		ExtArrayNodeSet result = new ExtArrayNodeSet();
		Node p, c;
		NodeProxy proxy;
		NodeList cl;
		Iterator domIter;
		for (Iterator i = context.iterator(); i.hasNext();) {
			proxy = (NodeProxy) i.next();
			if (proxy.gid < 0) {
				/* // commented out by Timo Boehme (document element is already part of virtual node set (not parent!))
								proxy.gid = proxy.doc.getDocumentElementId();
				*/
				if(proxy.getDocument().getResourceType() == DocumentImpl.BINARY_FILE)
					// skip binary resources
					continue;
				// -- inserted by Timo Boehme --
				NodeProxy docElemProxy =
					new NodeProxy(proxy.getDoc(), 1, Node.ELEMENT_NODE);
				docElemProxy.setInternalAddress(proxy.getDocument().getFirstChildAddress());
				if (test.matches(docElemProxy))
					result.add(docElemProxy);
				if (axis == Constants.DESCENDANT_AXIS
					|| axis == Constants.DESCENDANT_SELF_AXIS
					|| axis == Constants.DESCENDANT_ATTRIBUTE_AXIS) {
					domIter = docElemProxy.getDocument().getBroker().getNodeIterator(docElemProxy);
					NodeImpl node = (NodeImpl) domIter.next();
					node.setOwnerDocument(docElemProxy.getDocument());
					node.setGID(docElemProxy.gid);
					docElemProxy.setMatches(proxy.getMatches());
					addChildren(docElemProxy, result, node, domIter, 0);
				}
				continue;
				// -- end of insertion --
			} else {
				if(axis == Constants.SELF_AXIS && test.matches(proxy)) {
					if(inPredicate)
						proxy.addContextNode(proxy);
					result.add(proxy);
				} else {
					domIter = proxy.getDocument().getBroker().getNodeIterator(proxy);
					NodeImpl node = (NodeImpl) domIter.next();
					node.setOwnerDocument(proxy.getDocument());
					node.setGID(proxy.gid);
					addChildren(proxy, result, node, domIter, 0);
				}
			}
		}
		return result;
	}

	private final void addChildren(
		NodeProxy contextNode,
		NodeSet result,
		NodeImpl node,
		Iterator iter,
		int recursions) {
		if (node.hasChildNodes()) {
			NodeImpl child;
			Value value;
			NodeProxy p;
			for (int i = 0; i < node.getChildCount(); i++) {
				child = (NodeImpl) iter.next();
				if(child == null)
					LOG.debug("CHILD == NULL; doc = " + 
							((DocumentImpl)node.getOwnerDocument()).getName());
				if(node.getOwnerDocument() == null)
					LOG.debug("DOC == NULL");
				child.setOwnerDocument(node.getOwnerDocument());
				child.setGID(node.firstChildID() + i);
				p = new NodeProxy(child.ownerDocument, child.gid, child.getNodeType());
				p.setInternalAddress(child.internalAddress);
				p.setMatches(contextNode.getMatches());
				if (test.matches(child)) {
					if (((axis == Constants.CHILD_AXIS
						|| axis == Constants.ATTRIBUTE_AXIS)
						&& recursions == 0) ||
						(axis == Constants.DESCENDANT_AXIS
						|| axis == Constants.DESCENDANT_SELF_AXIS
						|| axis == Constants.DESCENDANT_ATTRIBUTE_AXIS)) {
						result.add(p);
						p.copyContext(contextNode);
						if (useSelfAsContext && inPredicate) {
							p.addContextNode(p);
						} else if (inPredicate)
							p.addContextNode(contextNode);
					}
				}
				addChildren(contextNode, result, child, iter, recursions + 1);
			}
		} else if (test.matches(node)) {
			if((axis == Constants.CHILD_AXIS || axis == Constants.ATTRIBUTE_AXIS)
				&& recursions > 0)
				return;
			NodeProxy p = new NodeProxy(node.ownerDocument, node.gid, node.getNodeType());
			p.setInternalAddress(node.internalAddress);
			p.setMatches(contextNode.getMatches());
			result.add(p);
			p.copyContext(contextNode);
			if (useSelfAsContext && inPredicate) {
				p.addContextNode(p);
			} else if (inPredicate)
				p.addContextNode(contextNode);
		}
	}

	private final void realize() {
		if (realSet != null && realSetIsComplete)
			return;
		realSet = getNodes();
		realSetIsComplete = true;
	}

	public void setSelfIsContext() {
		useSelfAsContext = true;
	}

	/* (non-Javadoc)
	 * @see org.exist.dom.NodeSet#hasIndex()
	 */
	public boolean hasIndex() {
		// Always return false: there's no index
		return false;
	}

	/* the following methods are normally never called in this context,
	 * we just provide them because they are declared abstract
	 * in the super class
	 */

	public void add(DocumentImpl doc, long nodeId) {
	}

	public void add(Node node) {
	}

	public void add(NodeProxy proxy) {
	}

	public void addAll(NodeList other) {
	}

	public void addAll(NodeSet other) {
	}

	public void set(int position, DocumentImpl doc, long nodeId) {
	}

	public void remove(NodeProxy node) {
	}

	public int getLength() {
		realize();
		return realSet.getLength();
	}

	public Node item(int pos) {
		realize();
		return realSet.item(pos);
	}

	public NodeProxy get(int pos) {
		realize();
		return realSet.get(pos);
	}

	public Item itemAt(int pos) {
		realize();
		return realSet.itemAt(pos);
	}

	public NodeProxy get(DocumentImpl doc, long gid) {
		realize();
		return realSet.get(doc, gid);
	}

	public NodeProxy get(NodeProxy proxy) {
		realize();
		return realSet.get(proxy);
	}

	public Iterator iterator() {
		realize();
		return realSet.iterator();
	}

	/* (non-Javadoc)
	 * @see org.exist.dom.NodeSet#iterate()
	 */
	public SequenceIterator iterate() {
		realize();
		return realSet.iterate();
	}

	/* (non-Javadoc)
	 * @see org.exist.dom.AbstractNodeSet#unorderedIterator()
	 */
	public SequenceIterator unorderedIterator() {
		realize();
		return realSet.unorderedIterator();
	}
	
	public NodeSet intersection(NodeSet other) {
		realize();
		return realSet.intersection(other);
	}

	public NodeSet union(NodeSet other) {
		realize();
		return realSet.union(other);
	}
}
