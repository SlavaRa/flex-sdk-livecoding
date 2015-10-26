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

/**
 * Node
 */
public class ClassNameNode extends Node
{
	public PackageNameNode pkgname;
	public IdentifierNode  ident;

    public boolean non_nullable = false;

	public ClassNameNode(PackageNameNode pkgname, IdentifierNode ident, int pos)
	{
		super(pos);
		this.pkgname = pkgname;
		this.ident = ident;
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
		return "ClassName";
	}

    public ClassNameNode clone() throws CloneNotSupportedException
    {
        ClassNameNode result = (ClassNameNode) super.clone();

        if (ident != null) result.ident = ident.clone();
        if (pkgname != null) result.pkgname = pkgname.clone();

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ClassNameNode that = (ClassNameNode) o;

        if (non_nullable != that.non_nullable) return false;
        if (ident != null ? !ident.equals(that.ident) : that.ident != null) return false;
		return !(pkgname != null ? !pkgname.equals(that.pkgname) : that.pkgname != null);

	}

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + (pkgname != null ? pkgname.hashCode() : 0);
//        result = 31 * result + (ident != null ? ident.hashCode() : 0);
//        result = 31 * result + (non_nullable ? 1 : 0);
//        return result;
//    }
}
