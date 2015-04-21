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

import macromedia.asc.util.Context;
import macromedia.asc.semantics.Value;

public class BinaryInterfaceDefinitionNode extends BinaryClassDefNode
{
	public BinaryInterfaceDefinitionNode(Context cx, PackageDefinitionNode pkgdef, AttributeListNode attrs, IdentifierNode name, ListNode interfaces, StatementListNode statements)
	{
		super(cx, pkgdef, attrs, name, null, interfaces, statements);
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

	public boolean isInterface()
	{
		return true;
	}

	public String toString()
	{
		return "BinaryInterfaceDefinition";
	}

    public BinaryInterfaceDefinitionNode clone() throws CloneNotSupportedException
    {
        BinaryInterfaceDefinitionNode result = (BinaryInterfaceDefinitionNode) super.clone();

        return result;
    }
}

