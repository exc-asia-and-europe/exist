/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-07 The eXist Project
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
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id$
 */
package org.exist.xquery.modules.cache;

import org.apache.log4j.Logger;
import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.FunctionDef;

/**
 * XQuery Extension module for store data in global cache
 * 
 * @author Evgeny Gazdovsky <gazdovsky@gmail.com>
 * @version 1.0
 */
public class CacheModule extends AbstractInternalModule {

    private final static Logger logger = Logger.getLogger(CacheModule.class);

    public final static String NAMESPACE_URI = "http://exist-db.org/xquery/cache";

	public final static String PREFIX = "cache";

	private final static FunctionDef[] functions = {
		new FunctionDef(PutFunction.signatures[0], PutFunction.class),
		new FunctionDef(GetFunction.signatures[0], GetFunction.class),
		new FunctionDef(CacheFunction.signatures[0], CacheFunction.class),
		new FunctionDef(ClearFunction.signatures[0], ClearFunction.class),
		new FunctionDef(ClearFunction.signatures[1], ClearFunction.class),
		new FunctionDef(RemoveFunction.signatures[0], RemoveFunction.class)
	};

	public CacheModule() {
		super(functions);
		logger.info("Instantiating Cache module");
	}

	public String getNamespaceURI() {
		return NAMESPACE_URI;
	}

	public String getDefaultPrefix() {
		return PREFIX;
	}

	public String getDescription() {
		return "Global cache for store/share data between sessions";
	}
	
}
