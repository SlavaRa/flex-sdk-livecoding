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

package macromedia.asc.parser;

import macromedia.asc.util.*;
import macromedia.asc.semantics.*;

/**
 * Node
 */

public class UsePrecisionNode extends UsePragmaNode {

	public int precision;

	public UsePrecisionNode(Node idNode, Node precisionNode)
	{
		super(idNode, precisionNode);
		precision = 34; // until proven otherwise
		if (precisionNode instanceof LiteralNumberNode) {
			this.precision = ((LiteralNumberNode)precisionNode).intValue();
		}
		// else ?
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

	public String toString()
	{
		return "UsePrecision " + precision;
	}

    public UsePrecisionNode clone() throws CloneNotSupportedException
    {

		return (UsePrecisionNode) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UsePrecisionNode that = (UsePrecisionNode) o;

		return precision == that.precision;

	}

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + precision;
//        return result;
//    }
}