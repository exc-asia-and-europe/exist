package org.exist.xmldb;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.exist.EXistException;
import org.exist.dom.NodeProxy;
import org.exist.dom.SortedNodeSet;
import org.exist.security.User;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.serializers.Serializer;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SerializerPool;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.Type;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

public class LocalResourceSet implements ResourceSet {

	protected BrokerPool brokerPool;
	protected LocalCollection collection;
	protected Vector resources = new Vector();
	protected Properties outputProperties;
	private User user;

	private LocalResourceSet() {}
	public LocalResourceSet(
		User user,
		BrokerPool pool,
		LocalCollection col,
		Properties properties,
		Sequence val,
		String sortExpr)
		throws XMLDBException {
		if(col == null)
			throw new NullPointerException("Collection cannot be null");
		this.user = user;
		this.brokerPool = pool;
		this.outputProperties = properties;
		this.collection = col;
		if(val.isEmpty())
			return;
		if(Type.subTypeOf(val.getItemType(), Type.NODE) && sortExpr != null) {
			SortedNodeSet sorted = new SortedNodeSet(brokerPool, user, sortExpr, collection.getAccessContext());
			try {
				sorted.addAll(val);
			} catch (XPathException e) {
				throw new XMLDBException(ErrorCodes.INVALID_RESOURCE,
					e.getMessage(), e);
			}
			val = sorted;
		}
		Item item;
		for(SequenceIterator i = val.iterate(); i.hasNext(); ) {
			item = i.nextItem();
			resources.add(item);
		}
	}

	public void addResource(Resource resource) throws XMLDBException {
		resources.add(resource);
	}

	public void clear() throws XMLDBException {
		resources.clear();
	}

	public ResourceIterator getIterator() throws XMLDBException {
		return new NewResourceIterator();
	}

	public ResourceIterator getIterator(long start) throws XMLDBException {
		return new NewResourceIterator(start);
	}

	public Resource getMembersAsResource() throws XMLDBException {
        SAXSerializer handler = (SAXSerializer) SerializerPool.getInstance().borrowObject(SAXSerializer.class);
		StringWriter writer = new StringWriter();
		handler.setOutput(writer, outputProperties);
		
		DBBroker broker = null;
		try {
			broker = brokerPool.get(user);
			// configure the serializer
			Serializer serializer = broker.getSerializer();
			serializer.reset();
			collection.properties.setProperty(Serializer.GENERATE_DOC_EVENTS, "false");
			serializer.setProperties(outputProperties);
			serializer.setUser(user);
			serializer.setSAXHandlers(handler, handler);

			//	serialize results
			handler.startDocument();
			handler.startPrefixMapping("exist", Serializer.EXIST_NS);
			AttributesImpl attribs = new AttributesImpl();
			attribs.addAttribute(
				"",
				"hitCount",
				"hitCount",
				"CDATA",
				Integer.toString(resources.size()));
			handler.startElement(
						Serializer.EXIST_NS,
						"result",
						"exist:result",
						attribs);
			Item current;
			char[] value;
			for(Iterator i = resources.iterator(); i.hasNext(); ) {
				current = (Item)i.next();
				if(Type.subTypeOf(current.getType(), Type.NODE)) {
					((NodeValue)current).toSAX(broker, handler);
				} else {
					value = current.toString().toCharArray();
					handler.characters(value, 0, value.length);
				}
			}
			handler.endElement(Serializer.EXIST_NS, "result", "exist:result");
			handler.endPrefixMapping("exist");
			handler.endDocument();
		} catch (EXistException e) {
			throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, "serialization error", e);
		} catch (SAXException e) {
			throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, "serialization error", e);
		} finally {
			brokerPool.release(broker);
		}
		Resource res = new LocalXMLResource(user, brokerPool, collection, "");
		res.setContent(writer.toString());
        SerializerPool.getInstance().returnObject(handler);
		return res;
	}

	public Resource getResource(long pos) throws XMLDBException {
		if (pos < 0 || pos >= resources.size())
			return null;
		Object r = resources.get((int) pos);
		LocalXMLResource res = null;
		if (r instanceof NodeProxy) {
			NodeProxy p = (NodeProxy) r;
			// the resource might belong to a different collection
			// than the one by which this resource set has been
			// generated: adjust if necessary.
			LocalCollection coll = collection;
			if (p.getDocument().getCollection() == null
				|| coll.getCollection().getId() != p.getDocument().getCollection().getId()) {
				coll = new LocalCollection(user, brokerPool, null, p.getDocument().getCollection().getName(), coll.getAccessContext());
				coll.properties = outputProperties;
			}
			res = new LocalXMLResource(user, brokerPool, coll, p);
		} else if (r instanceof Node) {
			res = new LocalXMLResource(user, brokerPool, collection, "");
			res.setContentAsDOM((Node)r);
		} else if (r instanceof AtomicValue) {
			res = new LocalXMLResource(user, brokerPool, collection, "");
			res.setContent(r);
		} else if (r instanceof Resource)
			return (Resource) r;
		res.setProperties(outputProperties);
		return res;
	}

	/**
	 *  Gets the size attribute of the LocalResourceSet object
	 *
	 *@return                     The size value
	 *@exception  XMLDBException  Description of the Exception
	 */
	public long getSize() throws XMLDBException {
		return resources.size();
	}

	/**
	 *  Description of the Method
	 *
	 *@param  pos                 Description of the Parameter
	 *@exception  XMLDBException  Description of the Exception
	 */
	public void removeResource(long pos) throws XMLDBException {
		resources.removeElementAt((int) pos);
	}

	/**
	 *  Description of the Class
	 *
	 *@author     wolf
	 *@created    3. Juni 2002
	 */
	class NewResourceIterator implements ResourceIterator {

		long pos = 0;

		/**  Constructor for the NewResourceIterator object */
		public NewResourceIterator() {
		}

		/**
		 *  Constructor for the NewResourceIterator object
		 *
		 *@param  start  Description of the Parameter
		 */
		public NewResourceIterator(long start) {
			pos = start;
		}

		/**
		 *  Description of the Method
		 *
		 *@return                     Description of the Return Value
		 *@exception  XMLDBException  Description of the Exception
		 */
		public boolean hasMoreResources() throws XMLDBException {
			return pos < getSize();
		}

		/**
		 *  Description of the Method
		 *
		 *@return                     Description of the Return Value
		 *@exception  XMLDBException  Description of the Exception
		 */
		public Resource nextResource() throws XMLDBException {
			return getResource(pos++);
		}
	}
}
