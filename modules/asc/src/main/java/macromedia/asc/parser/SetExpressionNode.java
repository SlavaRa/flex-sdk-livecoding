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

import macromedia.asc.util.*;
import macromedia.asc.semantics.*;
import static macromedia.asc.util.BitSet.*;

/**
 * Node
 */
public class SetExpressionNode extends SelectorNode
{
	public ArgumentListNode args;
	public TypeInfo value_type;
    public boolean is_constinit;
    public boolean is_initializer;
    
    public ReferenceValue getRef(Context cx)
	{
		return ref;
	}

	public BitSet gen_bits;

	public SetExpressionNode(Node expr, ArgumentListNode args)
	{
		this.expr  = expr;
		this.args  = args;
		ref = null;
		gen_bits = null;
		void_result = false;
        is_constinit = false;
        is_initializer = false;
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

	public boolean void_result;

	public void voidResult()
	{
		void_result = true;
		expr.voidResult();
	}

	public boolean isSetExpression()
	{
		return true;
	}

    public boolean isQualified()
    {
        QualifiedIdentifierNode qin = expr instanceof QualifiedIdentifierNode ? (QualifiedIdentifierNode) expr : null;
        return qin != null && qin.qualifier != null;
    }
    
    public boolean isAttributeIdentifier()
    {
    	return expr instanceof IdentifierNode && ((IdentifierNode) expr).isAttr();
    }
    
    public boolean isAny()
    {
    	return expr instanceof IdentifierNode && ((IdentifierNode) expr).isAny();
    }

    public BitSet getGenBits()
	{
		return gen_bits;
	}

	public BitSet getKillBits()
	{
		if (ref != null && ref.slot != null)
		{
			if (ref.slot.getDefBits() != null)
			{
				return xor(ref.slot.getDefBits(), gen_bits);
			}
			else
			{
				return gen_bits;
			}
		}
		else
		{
			return null;
		}
	}

	public String toString()
	{
		return "SetExpression";
	}

    public boolean hasSideEffect() 
    {
        return true;
    }

    public SetExpressionNode clone() throws CloneNotSupportedException
    {
        SetExpressionNode result = (SetExpressionNode) super.clone();

        if (args != null) result.args = args.clone();
        if (gen_bits != null) result.gen_bits = BitSet.copy(gen_bits);
        if (value_type != null) result.value_type = value_type.clone();

        return result;
    }

    @Override
    public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		SetExpressionNode that = (SetExpressionNode) o;

		if (is_constinit != that.is_constinit) return false;
		if (is_initializer != that.is_initializer) return false;
		if (void_result != that.void_result) return false;
		return !(args != null ? !args.equals(that.args) : that.args != null) && !(gen_bits != null ? !gen_bits.equals(that.gen_bits) : that.gen_bits != null) && !(value_type != null ? !value_type.equals(that.value_type) : that.value_type != null);

	}

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + (args != null ? args.hashCode() : 0);
//        result = 31 * result + (value_type != null ? value_type.hashCode() : 0);
//        result = 31 * result + (is_constinit ? 1 : 0);
//        result = 31 * result + (is_initializer ? 1 : 0);
//        result = 31 * result + (gen_bits != null ? gen_bits.hashCode() : 0);
//        result = 31 * result + (void_result ? 1 : 0);
//        return result;
//    }
}
