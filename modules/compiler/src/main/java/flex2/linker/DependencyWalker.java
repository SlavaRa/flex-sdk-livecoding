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

package flex2.linker;

import flex2.compiler.util.graph.Visitor;
import macromedia.abc.Optimizer;

import java.util.*;

/**
 * Walk the dependency graph implied by a collection of linkables, and visit
 * each linkable; prerequisites form a DAG and are visited in DFS order.
 * (Non-prerequisite connected linkables are visited in an arbitrary order.)
 *
 * Topological sort of DAG G is equivilent to the DFS of the graph G'
 * where for every edge (u,v) in G there is an edge (v,u) in the transposed
 * graph G'.
 *
 * This is handy since dependencies in Flex are a transposed DAG
 * (edges point to predecessors, not successors).
 */
public class DependencyWalker
{
    /**
     * A value object which maintains external definitions, included
     * definitions, unresolved definitions.
     */
    public static class LinkState
    {
        public LinkState( Collection linkables, Set extdefs, Set includes, Set<String> unresolved )
                throws LinkerException
        {
            this.extdefs = extdefs;
	        this.includes = includes;
            this.unresolved = unresolved;

            // Build the defname -> linkable map and check for non-unique linkables

            for (Object linkable : linkables) {
                Linkable l = (Linkable) linkable;

                if (lmap.containsKey(l.getName())) {
                    throw new LinkerException.DuplicateSymbolException(l.getName());
                }
                LinkableContext lc = new LinkableContext(l);
                lmap.put(l.getName(), lc);

                String external = null;
                for (Iterator di = l.getDefinitions(); di.hasNext(); ) {
                    String def = (String) di.next();
                    LinkableContext c = defs.get(def);
                    if (c != null) {
                        throw new LinkerException.MultipleDefinitionsException(def, l.getName(), c.l.getName());
                    }
                    defs.put(def, lc);

                    if (extdefs.contains(def)) {
                        external = def;
                    } else if (external != null) {
                        throw new LinkerException.PartialExternsException(lc.l.getName(), def, external);
                    }
                }
            }
        }

        public Set<String> getUnresolved()
        {
            return unresolved;
        }

        public Set getExternal()
        {
            return extdefs;
        }

	    public Set getIncludes()
	    {
	        return includes;
	    }

        public Set<String> getDefNames()
        {
            return defs.keySet();
        }

        public Collection<LinkableContext> getLinkables()
        {
            return lmap.values();
        }

        public Collection<Linkable> getVisitedLinkables()
        {
            return vmap.values();
        }

        Map<String, Linkable> vmap = new HashMap<>();
        Map<String, LinkableContext> lmap = new HashMap<>();
        Map<String, LinkableContext> defs = new HashMap<>();
        Set extdefs;
	    Set includes;
        Set<String> unresolved;
    }

    /**
     * @param defs     the base definition set to start traversal, if null, link all.
     * @param state      a (mostly opaque) state object that can be used for multiple traversals
     * @param v             the visitor to invoke for each linkable
     * @throws LinkerException
     */
    public static void traverse( List<String> defs, LinkState state, boolean allowExternal, boolean exportIncludes, 
                                 boolean includeInheritanceDependenciesOnly, Visitor<Linkable> v )
            throws LinkerException
    {
        if (defs == null)
        {
            // If we want inheritance dependencies only, skip populating defs with all the non-external names.
            defs = new LinkedList<>();
            if (!includeInheritanceDependenciesOnly)
            {
                for (String def : state.getDefNames()) {
                    if (!state.getExternal().contains(def)) {
                        defs.add(def);
                    }
                }
            }
        }

	    if (exportIncludes)
	    {
            for (Object o : state.getIncludes()) {
                String def = (String) o;
                defs.add(def);
            }
	    }

        Stack<LinkableContext> stack = new Stack<>();           // holds contexts
        LinkedList<LinkableContext> queue = new LinkedList<>(); // holds contexts

        for (String defname : defs) {
            LinkableContext start = resolve(defname, state, allowExternal, exportIncludes,
                    includeInheritanceDependenciesOnly);
            if (start == null)
                continue;

            queue.add(start);
        }

        while (!queue.isEmpty())
        {
            LinkableContext qc = queue.removeFirst();

            if (qc.visited)
                continue;

            qc.progress = true;
            stack.push( qc );

            while (!stack.isEmpty())
            {
                LinkableContext c = stack.peek();

                if (c.visited)
                {
                    stack.pop();
                    continue;
                }

                if (c.pi.hasNext())
                {
                    LinkableContext prereq = resolve( (String) c.pi.next(), state, 
                                                      allowExternal, 
                                                      exportIncludes, 
                                                      includeInheritanceDependenciesOnly);
                    if (prereq != null)
                    {
                        if (prereq.progress)
                        {
                            throw new LinkerException.CircularReferenceException( c.l.getName() );
                        }
                        if (!prereq.visited)
                        {
                            prereq.progress = true;
                            stack.push( prereq );
                        }
                    }
                    continue;
                }

//                if (c.visited)
//                {
//                    throw new DependencyException( DependencyException.CIRCULAR,
//                                                   c.l.getName(),
//                                                   "prerequisites of " + c.l.getName() + " contain a circular reference" );
//                }


                v.visit( c.l );
                c.visited = true;
                c.progress = false;
                state.vmap.put( c.l.getName(), c.l );
                stack.pop();

                while (c.di.hasNext())
                {
                    LinkableContext dc = resolve( (String) c.di.next(), state, 
                                                   allowExternal, 
                                                   exportIncludes,
                                                   includeInheritanceDependenciesOnly);

                    if ((dc == null) || dc.visited)
                        continue;

                    queue.add( dc );
                }
            }
        }
    }

    static LinkableContext resolve( String name, LinkState state, boolean allowExternal, boolean exportIncludes,
                                    boolean includeInheritianceDependenciesOnly) throws LinkerException
    {
        if (allowExternal && (state.extdefs != null) && state.extdefs.contains( name ))
        {
            state.unresolved.add( name );
            return null;
        }

	    if (! exportIncludes && (state.includes != null) && state.includes.contains( name ))
	    {
		    state.includes.remove(name);
	    }

        LinkableContext lc = state.defs.get( name );

        if (lc == null)
        {
            if (state.unresolved == null)
                throw new LinkerException.UndefinedSymbolException( name );
            else
                state.unresolved.add( name );
        }
        else
        {
            if (lc.l.isNative())
            {
                state.unresolved.add( name );   // natives are always external
                return null;
            }
            if (!allowExternal && state.extdefs.contains( name ))
            {
                state.extdefs.remove( name );   // not external anymore, we had to resolve it.
            }
            lc.activate(includeInheritianceDependenciesOnly);
        }
        return lc;
    }

    public static String dump(  LinkState state )
    {
        StringBuilder buf = new StringBuilder( 2048 );
        buf.append( "<report>\n" );
        buf.append( "  <scripts>\n" );
        for (Linkable linkable : state.getVisitedLinkables()) {
            CULinkable l = (CULinkable) linkable;

            buf.append("    <script name=\"")
                    .append(l.getName())
                    .append("\" mod=\"")
                    .append(l.getLastModified())
                    .append("\" size=\"")
                    .append(l.getSize())
                    // optimizedsize is often considerably smaller than size
                    .append("\" optimizedsize=\"")
                    .append(Optimizer.optimize(l.getUnit().bytes).size())
                    .append("\">\n");

            for (Iterator defs = l.getDefinitions(); defs.hasNext(); ) {
                buf.append("      <def id=\"").append((String) defs.next()).append("\" />\n");
            }
            for (Iterator pre = l.getPrerequisites(); pre.hasNext(); ) {
                buf.append("      <pre id=\"").append((String) pre.next()).append("\" />\n");
            }
            for (Iterator dep = l.getDependencies(); dep.hasNext(); ) {
                buf.append("      <dep id=\"").append((String) dep.next()).append("\" />\n");
            }
            buf.append("    </script>\n");
        }
        buf.append( "  </scripts>\n" );

        if ((state.getExternal() != null) || (state.getUnresolved() != null))
        {
            buf.append( "  <external-defs>\n");
            for (Object o : state.getExternal()) {
                String ext = (String) o;
                if (!state.getUnresolved().contains(ext))    // only print exts we actually depended on
                    continue;

                buf.append("    <ext id=\"").append(ext).append("\" />\n");
            }
            for (String unr : state.getUnresolved()) {
                if (state.getExternal().contains(unr))
                    continue;
                buf.append("    <missing id=\"").append(unr).append("\" />\n");
            }
            buf.append( "  </external-defs>\n");
        }

        buf.append( "</report>\n" );

        return buf.toString();
    }

    static private class LinkableContext
    {
        public LinkableContext( Linkable l )
        {
            this.l = l;
        }
        public void activate(boolean includeInheritianceDependenciesOnly)
        {
            if (!active)
            {
                active = true;
                pi = l.getPrerequisites();
                
                if (!includeInheritianceDependenciesOnly)
                    di = l.getDependencies();
                else
                    di = Collections.EMPTY_LIST.iterator();
            }
        }
        public String toString()
        {
            return l.getName() + " " + (visited? "v":"") + (progress? "p":"");
        }
        public final Linkable l;
        public Iterator pi;
        public Iterator di;
        public boolean active = false;
        public boolean visited = false;
        public boolean progress = false;
    }

}
