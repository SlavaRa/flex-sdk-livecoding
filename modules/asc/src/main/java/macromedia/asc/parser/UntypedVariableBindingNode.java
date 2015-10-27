/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package macromedia.asc.parser;

import macromedia.asc.semantics.*;
import macromedia.asc.util.*;

/**
 * Node
 */
public class UntypedVariableBindingNode extends Node
{
	public IdentifierNode identifier;
	public Node initializer;
	public ReferenceValue ref;

	public UntypedVariableBindingNode(IdentifierNode identifier, Node initializer)
	{
		ref = null;
		this.identifier = identifier;
		this.initializer = initializer;
	}

	public Value evaluate(Context cx, Evaluator evaluator)
	{
		if (evaluator.checkFeature(cx, this))
		{
			return evaluator.evaluate(cx, this);
		}
		else
		{
			return null;
		}
	}

	public int pos()
	{
		return identifier.pos();
	}

	public String toString()
	{
		return "VariableBinding";
	}

    public UntypedVariableBindingNode clone() throws CloneNotSupportedException
    {
        UntypedVariableBindingNode result = (UntypedVariableBindingNode) super.clone();

        if (identifier != null) result.identifier = identifier.clone();
        if (initializer != null) result.initializer = initializer.clone();
        if (ref != null) result.ref = ref.clone();

        return result;
    }

    @Override
    public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		UntypedVariableBindingNode that = (UntypedVariableBindingNode) o;

		return !(identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) && !(initializer != null ? !initializer.equals(that.initializer) : that.initializer != null) && !(ref != null ? !ref.equals(that.ref) : that.ref != null);

	}

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
//        result = 31 * result + (initializer != null ? initializer.hashCode() : 0);
//        result = 31 * result + (ref != null ? ref.hashCode() : 0);
//        return result;
//    }
}
