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

package flex2.compiler.mxml.rep;

import flex2.compiler.mxml.gen.CodeFragmentList;
import flex2.compiler.mxml.gen.TextGen;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.reflect.*;
import flex2.compiler.mxml.rep.init.*;
import flex2.compiler.util.IteratorList;
import flex2.compiler.util.NameFormatter;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections.iterators.TransformIterator;

import java.util.*;

/**
 * This class represents a non-language node in a MXML document.
 */
public class Model implements LineNumberMapped
{
    private Type type;

    /**
     * line number where this model occurred in xml.  -1 if this model
     * is synthetic and has no creation site in MXML.
     */
    private int xmlLineNumber;

    /**
     * this is the guaranteed-unique name used by the value initializer
     * TODO see below
     */
    private String definitionName;

    /**
     * this is the ID we have given this instance in mxml.  it will either be user assigned or
     * assigned automatically by the compiler.
     * NOTE: in anonymous cases (e.g. XML), this may be a child element name, thus not guaranteed unique.
     */
    // TODO id should be the guaranteed-unique one; childName or something for the other one.
    private String id;

    /**
     * this flag is true if the id (see above) was assigned automatically
     * by the compiler.  Conversely, this flag is false if the user
     * explicitly assigned the id
     */
    private boolean idIsAutogenerated = false;

    /**
     * indicates whether this Model is a child of an AnonymousObjectGraph
     */
    private boolean isAnonymous = false;

    private MxmlDocument document;
    protected final StandardDefs standardDefs;

    private Model parent;
    private String parentIndex;
    private String parentIndexState;

    private boolean inspectable = false;

    private List<Model> repeaterParents;

    private Map<String, Initializer> properties;
    private Map<String, Initializer> styles;
    private Map<String, Initializer> effects;
    private Map<String, Initializer> events;
    private Collection<String> states;

    /*
     * The DesignLayer associated with this model.
     */
    public DesignLayer layerParent;
    
    private boolean described;  //  TODO remove when DI is generalized
    
    /*
     * Denotes whether or not this node is state-specific.
     */
    private boolean stateSpecific = false;
    
    /*
     * Denotes whether or not this node is to be instantiated immediately
     * upon document instantiation (runtime).
     */
    public boolean earlyInit = false;
    
    /*
     * used to force declare a model, even when not normally applicable.
     */
    private boolean ensureDeclaration = false;
    
    /*
     * used to force a models property declaration to be bindable.
     */
    private boolean ensureBindable = false;
    
    /*
     * indicates whether this Model should be initialized within the document descriptor.
     */
    private boolean descriptorInit = true;

    /*
     * indicates that this model serves as the rvalue of an ITransientDeferredInstance type.
     */
    private boolean isTransient = false;
    
    public Model(MxmlDocument document, Type type, int line)
    {
        this(document, type, null, line);
    }

    public Model(MxmlDocument document, Type type, Model parent, int line)
    {
        assert (type != null);

        this.document = document;
        this.standardDefs = document.getStandardDefs();
        this.type = type;
        this.parent = parent;
        setXmlLineNumber(line);

        document.ensureId(this);
        setDefinitionName(getId());
    }

    public final boolean isDeclared()
    {
        //  TODO first clause is necessary due to id being abused in AOG case (see AOGBuilder ~line 85.
        //  This breaks meaning of id - need to find another way to enable correct databinding codegen in AOG case.
        return !isAnonymous && document.isDeclared(this);
    }

    public final StandardDefs getStandardDefs()
    {
        return standardDefs;
    }

    public final Type getType()
    {
        return type;
    }

    public final String getDefinitionName()
    {
        return definitionName;
    }

    public final void setDefinitionName(String definitionName)
    {
        this.definitionName = definitionName;
    }

    public final String getId()
    {
        return id;
    }

    public final void setId(String id, boolean idIsAutogenerated)
    {
        this.id = id;
        this.idIsAutogenerated = idIsAutogenerated;
    }

    public final boolean getIdIsAutogenerated()
    {
        return idIsAutogenerated;
    }

    public final void ensureBindable()
    {
    	ensureBindable = true;	
    }
    
    public final boolean getBindabilityEnsured()
    {
    	return ensureBindable;
    }
    
    public final boolean getIsAnonymous()
    {
        return isAnonymous;
    }

    public final void setIsAnonymous(boolean isAnonymous)
    {
        this.isAnonymous = isAnonymous;
    }

    public final boolean getIsTransient()
    {
        return isTransient;
    }

    public final void setIsTransient(boolean isTransient)
    {
        this.isTransient = isTransient;
    }
    
    public final MxmlDocument getDocument()
    {
        return document;
    }

    public final void setParent(Model parent)
    {
        this.parent = parent;
    }

    public final Model getParent()
    {
        return parent;
    }

    public final void setParentIndex(String index)
    {
        this.parentIndex = index;
    }
    
    public final void setParentIndex(String index, String state)
    {
        this.parentIndex = index;
        this.parentIndexState = state;
    }

    public final void setParentIndex(int index)
    {
        this.parentIndex = Integer.toString(index);
    }

    public final String getParentIndex()
    {
        return parentIndex;
    }
    
    public final String getParentIndexState()
    {
        return parentIndexState;
    }

    public final int getXmlLineNumber()
    {
        return xmlLineNumber;
    }

    public final void setXmlLineNumber(int xmlLineNumber)
    {
        this.xmlLineNumber = xmlLineNumber;
    }

    public final boolean getInspectable()
    {
        return inspectable;
    }

    public final void setInspectable(boolean inspectable)
        {
        this.inspectable = inspectable;
        }

    public final int getRepeaterLevel()
    {
        return getRepeaterParents().size();
    }

    public final List<Model> getRepeaterParents()
    {
        if (repeaterParents == null)
        {
            repeaterParents = new ArrayList<>();

            if (parent != null)
            {
                repeaterParents.addAll(parent.getRepeaterParents());

                if (standardDefs.isRepeater(parent.getType()))
                {
                    repeaterParents.add(parent);
                }
            }
        }

        return repeaterParents;
    }

    /**
     *
     */
    public void setProperty(String name, Model value)
    {
        setProperty(name, value, value.getXmlLineNumber());
    }

    /**
     *
     */
    public void setProperty(Property property, Object value, int line)
    {
        ValueInitializer initializer = new StaticPropertyInitializer(property, value, line, standardDefs);
        
        // Register this property and value as pertaining only to a specific state, otherwise
        // assign to this instance (base state).
        if (property.isStateSpecific())
        {
            document.registerStateSpecificProperty(this, property.getName(), initializer, property.getStateName());
        }
        else
        {
            (properties != null ? properties : (properties = new LinkedHashMap<>())).put(property.getName(), initializer);
        }
    }

    /**
     *
     */
    public void setDynamicProperty(Type type, String name, Object value, String state, int line)
    {
        ValueInitializer initializer = new DynamicPropertyInitializer(type, name, value, line, standardDefs);
        
        // Register this property and value as pertaining only to a specific state, otherwise
        // assign to this instance (base state).
        if (state != null)
        {
            document.registerStateSpecificProperty(this, name, initializer, state);
        }
        else
        {
            (properties != null ? properties : (properties = new LinkedHashMap<>())).put(name, initializer);
        }
    }

    /**
     * TODO legacy shim. Convert all callers to either setProperty(property, value) or setDynamicProperty(type, name, value)
     */
    public void setProperty(String name, Object value, int line)
    {
        Property property = type.getProperty(name);
        if (property != null)
        {
            setProperty(property, value, line);
        }
        else
        {
            setDynamicProperty(type.getTypeTable().objectType, name, value, null, line);
        }
    }

    /**
     *
     */
    public final boolean hasProperty(String name)
    {
        return getProperties().containsKey(name);
    }

    /**
     *
     */
    public boolean hasBindings()
    {
        return bindingsOnly(getProperties().values().iterator()).hasNext() ||
                bindingsOnly(getStyles().values().iterator()).hasNext() ||
                bindingsOnly(getEffects().values().iterator()).hasNext();
    }

    /**
     * Returns true if the specified property is the target of a binding.
     */
    public boolean hasDataBoundProperty(String name) 
    {
        Initializer initializer = (Initializer) getProperties().get(name);
        return initializer != null && initializer.isBinding();
    }
    
    /**
     * Returns true if the specified style is the target of a binding.
     */
    public boolean hasDataBoundStyle(String name) 
    {
        Initializer initializer = (Initializer) getStyles().get(name);
        return initializer != null && initializer.isBinding();
    }
    
    /**
     * Returns true if the specified event property is the target of a binding.
     */
    public boolean hasDataBoundEvent(String name) 
    {
        Initializer initializer = (Initializer) getEvents().get(name);
        return initializer != null && initializer.isBinding();
    }
    
    /**
     * Returns true if the specified effect property is the target of a binding.
     */
    public boolean hasDataBoundEffect(String name) 
    {
        Initializer initializer = (Initializer) getEffects().get(name);
        return initializer != null && initializer.isBinding();
    }
    
    /**
     * Note that we do *not* filter out bindings by default for property initializers.
     */
    public final Iterator<Initializer> getPropertyInitializerIterator()
    {
        return getPropertyInitializerIterator(true);
    }

    /**
     *
     */
    public final Iterator<Initializer> getPropertyInitializerIterator(boolean includeBindings)
    {
        return includeBindings ?
                getProperties().values().iterator() :
                excludeBindings(getProperties().values().iterator());
    }

    /**
     *
     */
    public boolean isEmpty()
    {
        return properties == null && styles == null && effects == null && events == null;
    }

    /**
     * TODO make this private once RemoteObjectBuilder usage has been removed
     */
    public final Map<String, Initializer> getProperties()
    {
        return properties != null ? properties : Collections.<String, Initializer>emptyMap();
    }

    /**
     * TODO legacy - delete once AnonymousObjectGraphBuilder usage has been removed
     */
    public Object getProperty(String name)
    {
        ValueInitializer initializer = (ValueInitializer)getProperties().get(name);
        return initializer != null ? initializer.getValue() : null;
    }

    /**
     *
     */
    public final void setStyle(String name, Object value, int line)
    {
        Style style = type.getStyle(name);
        assert style != null : "style '" + name + "' not defined on type '" + type.getName() + "'";
        setStyle(style,value, line);
    }
    
    /**
     *
     */
    public final void setStyle(Style style, Object value, int line)
    {
        StyleInitializer styleInitializer = new StyleInitializer(style, value, line, standardDefs);
        
        // Register this style and value as pertaining only to a specific state, otherwise
        // assign to this instance (base state).
        if (style.isStateSpecific())
        {
            document.registerStateSpecificStyle(this, style.getName(), styleInitializer, style.getStateName());
        }
        else
        {
            (styles != null ? styles : (styles = new LinkedHashMap<>())).put(style.getName(), styleInitializer);
        }
    }

    public final Iterator<Initializer> getStyleInitializerIterator()
    {
        return excludeBindings(getStyles().values().iterator());
    }

    public final boolean hasStyle(String name)
    {
        return getStyles().containsKey(name);
    }

    private Map<String, Initializer> getStyles()
    {
        return styles != null ? styles : Collections.<String, Initializer>emptyMap();
    }

    /**
     * Note: this is a little irregular; effect rvalues are either class names or bindings.
     */
    public final void setEffect(String name, Object value, Type effectType, int line)
    {
        Effect effect = type.getEffect(name);
        setEffect(effect, value, effectType, line);
    }
    
    /**
     * Note: this is a little irregular; effect rvalues are either class names or bindings.
     */
    public final void setEffect(Effect effect, Object value, Type effectType, int line)
    {
        EffectInitializer effectInitializer = new EffectInitializer(effect, value, effectType, line, standardDefs);
        
        // Register this effect and value as pertaining only to a specific state, otherwise
        // assign to this instance (base state).
        if (effect.isStateSpecific())
        {
            document.registerStateSpecificStyle(this, effect.getName(), effectInitializer, effect.getStateName());
        }
        else
        {
            (effects != null ? effects : (effects = new LinkedHashMap<>())).put(effect.getName(), effectInitializer);
        }
    }

    public final Iterator<Initializer> getEffectInitializerIterator()
    {
        return excludeBindings(getEffects().values().iterator());
    }

    public final boolean hasEffect(String name)
    {
        return getEffects().containsKey(name);
    }

    public final Map<String, Initializer> getEffects()
    {
        return effects != null ? effects : Collections.<String, Initializer>emptyMap();
    }

    public String getEffectNames()
    {
        Iterator eventNameIter = new TransformIterator(getEffectInitializerIterator(), new Transformer()
        {
            public Object transform(Object object)
            {
                return TextGen.quoteWord(((EffectInitializer)object).getName());
            }
        });

        return TextGen.toCommaList(eventNameIter);
    }

    public String getEffectEventNames()
    {
        Iterator eventNameIter = new TransformIterator(getEffectInitializerIterator(), new Transformer()
        {
            public Object transform(Object object)
            {
                return TextGen.quoteWord(((EffectInitializer)object).getEventName());
            }
        });

        return TextGen.toCommaList(eventNameIter);
    }

    /**
     *
     */
    public Iterator getStyleAndEffectInitializerIterator()
    {
        return new IteratorChain(getStyleInitializerIterator(), getEffectInitializerIterator());
    }

    /**
     *
     */
    public final void setEvent(Event event, String text, int line)
    {
        EventHandler handler;
        
        // If this handler will be only assigned for a state other than base (default),
        // ensure we generate a corresponding import.
        if (!event.isStateSpecific())
        {
            document.addImport(NameFormatter.toDot(event.getType().getName()), line);
            handler = new EventHandler(this, event, text);
        }
        else
        {
            handler = new EventHandler(this, event, text, event.getStateName());
        }

        handler.setXmlLineNumber(line);
        EventInitializer eventInitializer = new EventInitializer(handler);

        // Register this handler and value as pertaining only to a specific state, otherwise
        // assign to this instance (base state).
        if (event.isStateSpecific())
        {
            document.addStateSpecificEventInitializer(eventInitializer);
            document.registerStateSpecificEventHandler(this, event.getName(), eventInitializer, event.getStateName());
        }
        else
        {
            (events != null ? events : (events = new LinkedHashMap<>())).put(event.getName(), eventInitializer);
        }
    }
    
    public final Iterator<Initializer> getEventInitializerIterator()
    {
        return getEvents().values().iterator();
    }

    public final boolean hasEvent(String name)
    {
        return getEvents().containsKey(name);
    }
    
    public final Initializer getEventInitializer(String name)
    {
    	return getEvents().get(name);
    }

    private Map<String, Initializer> getEvents()
    {
        return events != null ? events : Collections.<String, Initializer>emptyMap();
    }

    /**
     *  iterator containing definitions from our initializers
     */
    public Iterator<CodeFragmentList> getSubDefinitionsIterator()
    {
        IteratorList iterList = new IteratorList();

        addDefinitionIterators(iterList, getPropertyInitializerIterator());
        addDefinitionIterators(iterList, getStyleInitializerIterator());
        addDefinitionIterators(iterList, getEffectInitializerIterator());
        addDefinitionIterators(iterList, getEventInitializerIterator());

        return iterList.toIterator();
    }

    /**
     *
     */
    protected static void addDefinitionIterators(IteratorList iterList, Iterator<? extends Initializer> initIter)
    {
        while (initIter.hasNext())
        {
            iterList.add(initIter.next().getDefinitionsIterator());
        }
    }

	/**
	 *  iterator containing our initializers
	 */
	public Iterator<Initializer> getSubInitializerIterator()
	{
		IteratorList iterList = new IteratorList();

		iterList.add(getPropertyInitializerIterator());
		iterList.add(getStyleInitializerIterator());
		iterList.add(getEffectInitializerIterator());
		iterList.add(getEventInitializerIterator());

		return iterList.toIterator();
	}

    /**
     *
     */
    protected Iterator<Initializer> excludeBindings(Iterator<? extends Initializer> iter)
    {
        return bindingFilter(iter, false);
    }

    /**
     *
     */
    protected Iterator<Initializer> bindingsOnly(Iterator<? extends Initializer> iter)
    {
        return bindingFilter(iter, true);
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    protected Iterator<Initializer> bindingFilter(Iterator<? extends Initializer> iter, final boolean include)
    {
        return new FilterIterator(iter, new Predicate()
        {
            public boolean evaluate(Object object)
            {
                return (((Initializer) object).isBinding()) == include;
            }
        });
    }

    /**
     *
     */
    public final void setDescribed(boolean described)
    {
        this.described = described;
    }

    /**
     *
     */
    public final boolean isDescribed()
    {
        return described;
    }
    
    /**
     * Denotes whether or not this node is state-specific.
     */
    public final void setStateSpecific(boolean stateful)
    {
        this.stateSpecific = stateful;
    }

    /**
     * Denotes whether or not this node is state-specific.
     */
    public final boolean isStateSpecific()
    {
        return stateSpecific;
    }
    
    /**
     * Denotes whether or not this node is to be created immediate
     * upon document initialization (runtime).
     */
    public final void setEarlyInit(boolean earlyInit)
    {
        this.earlyInit = earlyInit;
    }

    /**
     * Denotes whether or not this node is to be created immediate
     * upon document initialization (runtime).
     */
    public final boolean isEarlyInit()
    {
        return earlyInit;
    }
    
    /**
     * Sets the list of states which this node is to apply.
     */
    public final void setStates(Collection<String> states)
    {
        this.states = states;
    }
    
    /**
     * Returns list of states for which this node applies.
     */
    public final Collection<String> getStates()
    {
        return states;
    }
    
    /**
     * Returns true if this object is to be applied for a given state.
     */
    public final boolean hasState(String state) {
        return states != null && states.contains(state);
    }
    
    /**
     * used to force declare a model, even when not normally applicable.
     */
    public final void ensureDeclaration()
    {
        this.ensureDeclaration = true;
    }

    /**
     * used to force declare a model, even when not normally applicable.
     */
    public final boolean isDeclarationEnsured()
    {
        return ensureDeclaration;
    }
    
    /**
     * indicates whether this Model should be initialized within the document descriptor.
     */
    public final void setDescriptorInit(boolean descriptorInit)
    {
        this.descriptorInit = descriptorInit;
    }

    /**
     * indicates whether this Model should be initialized within the document descriptor.
     */
    public final boolean isDescriptorInit()
    {
        return descriptorInit;
    }
    
    /**
     * comment field for asdoc generation.
     */
    public String comment;
}
