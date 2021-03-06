/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package flash.swf;

import flash.swf.tags.DefineTag;
import flash.swf.tags.DoABC;
import flash.swf.tags.FrameLabel;
import flash.swf.tags.ImportAssets;
import flash.swf.tags.SymbolClass;
import flash.swf.tags.DefineFont;
import flash.swf.types.ActionList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents one SWF frame.  Each frame runs its initActions,
 * doActions, and control tags in a specific order, so we group them
 * this way while forming the movie.
 */
public class Frame
{
	public List<ActionList> doActions;
	public List<Tag> controlTags;
	public FrameLabel label;
	public List<ImportAssets> imports;
	public int pos = 1;	

	private Map<String, DefineTag> exports;
	private List<DefineTag> exportDefs;

	public List<DoABC> doABCs;

	public SymbolClass symbolClass;

	public List<DefineFont> fonts;

	public Frame()
	{
		exports = new HashMap<>();
		exportDefs = new ArrayList<>();
		doActions = new ArrayList<>();
		controlTags = new ArrayList<>();
		imports = new ArrayList<>();
		fonts = new ArrayList<>();

		doABCs = new ArrayList<>();
		symbolClass = new SymbolClass();
	}

	public Iterator<Tag> getReferences()
	{
		ArrayList<Tag> list = new ArrayList<>();

		// exported symbols
		list.addAll(exportDefs.stream().collect(Collectors.toList()));

        list.addAll( symbolClass.class2tag.values() );

		// definitions for control tags
		for (Tag tag : controlTags) {
			for (Iterator k = tag.getReferences(); k.hasNext(); ) {
				DefineTag def = (DefineTag) k.next();
				list.add(def);
			}
		}

		return list.iterator();
	}

    public void mergeSymbolClass(SymbolClass symbolClass)
    {
        this.symbolClass.class2tag.putAll( symbolClass.class2tag );
    }
	public void addSymbolClass(String className, DefineTag symbol)
	{      
        // FIXME: error below should be possible... need to figure out why it is happening when running 'ant frameworks'
		//DefineTag tag = (DefineTag)symbolClass.class2tag.get(className);
        //if (tag != null && ! tag.equals(symbol))
        //{
        //    throw new IllegalStateException("Attempted to define SymbolClass for " + className + " as both " +
        //            symbol + " and " + tag);
        //}
        this.symbolClass.class2tag.put( className, symbol );
	}

	public boolean hasSymbolClasses()
	{
		return !symbolClass.class2tag.isEmpty();
	}

	public void addExport(DefineTag def)
	{
		Object old = exports.put(def.name, def);
		if (old != null)
		{
			exportDefs.remove(old);
		}
		exportDefs.add(def);
	}

	public boolean hasExports()
	{
		return !exports.isEmpty();
	}

	public Iterator<DefineTag> exportIterator()
	{
		return exportDefs.iterator();
	}

	public void removeExport(String name)
	{
		Object d = exports.remove(name);
		if (d != null)
		{
			exportDefs.remove(d);
		}
	}

	public void setExports(Map definitions)
	{
		for (Object o : definitions.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			DefineTag def = (DefineTag) entry.getValue();
			addExport(def);
		}
	}

	public boolean hasFonts()
	{
		return !fonts.isEmpty();
	}

	public void addFont(DefineFont tag)
	{
		fonts.add(tag);
	}

	public Iterator<DefineFont> fontsIterator()
	{
		return fonts.iterator();
	}
}
