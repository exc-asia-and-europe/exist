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
package org.exist.xquery.functions;

import org.exist.dom.QName;
import org.exist.dom.QNameable;
import org.exist.xquery.Cardinality;
import org.exist.xquery.Dependency;
import org.exist.xquery.Function;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Profiler;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.QNameValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.w3c.dom.Node;


/**
 * Implements the fn:node-name library function.
 * 
 * @author wolf
 */
public class FunNodeName extends Function {

    public final static FunctionSignature signature =
		new FunctionSignature(
			new QName("node-name", Function.BUILTIN_FUNCTION_NS),
			" Returns an expanded-QName for node kinds that can have names. For other kinds " +
			"of nodes it returns the empty sequence. If $a is the empty sequence, the " +
			"empty sequence is returned.",
			new SequenceType[] { new SequenceType(Type.NODE, Cardinality.ZERO_OR_ONE) },
			new SequenceType(Type.QNAME, Cardinality.ZERO_OR_ONE));
    
    /**
     * @param context
     * @param signature
     */
    public FunNodeName(XQueryContext context) {
        super(context, signature);
    }

	public Sequence eval(Sequence contextSequence, Item contextItem) throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);       
            context.getProfiler().message(this, Profiler.DEPENDENCIES, "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT SEQUENCE", contextSequence);
            if (contextItem != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT ITEM", contextItem.toSequence());
        }
        
        if(contextItem != null)
            contextSequence = contextItem.toSequence();
        
        Sequence result;
        Sequence seq = getArgument(0).eval(contextSequence);
        if(seq.isEmpty())
            result = Sequence.EMPTY_SEQUENCE;
        else {
            Item item = seq.itemAt(0);
            if(!Type.subTypeOf(item.getType(), Type.NODE))
                throw new XPathException(getASTNode(), "argument is not a node; got: " +
                        Type.getTypeName(item.getType()));
            
            Node n = ((NodeValue)item).getNode();
            switch(n.getNodeType()) {
                case Node.ELEMENT_NODE:
                case Node.ATTRIBUTE_NODE:
                    QName qname = ((QNameable) n).getQName();
                    result = new QNameValue(context, qname);
                    break;
                //TODO : what kind of default do we expect here ? -pb
                default:
                    result = Sequence.EMPTY_SEQUENCE;
            }
        }
        
        if (context.getProfiler().isEnabled()) 
            context.getProfiler().end(this, "", result); 
        
        return result;   
        
    }
}
