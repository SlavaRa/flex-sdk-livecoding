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

package macromedia.asc.embedding;

import macromedia.asc.embedding.avmplus.ActionBlockEmitter;
import macromedia.asc.semantics.*;
import macromedia.asc.util.*;

/*
 * jkamerer ported from c++ start
 */

public class CompilerState
{
    ContextStatics statics;
    Context mainCX;
    ActionBlockEmitter emitter;
    Builder globalBuilder;
    ObjectValue global;

    CompilerState()
	{
		statics = null;
		mainCX = null;
		emitter = null;
		globalBuilder = null;
		global = null;
	}

	/*
	 * jkamerer ported from c++ end
	 */

}
