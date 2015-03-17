/* 

Copyright 2015 Novartis Institutes for Biomedical Research

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package org.hibernate.collection.internal;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({ "serial", "rawtypes", "unchecked" })
public class PersistentBag extends ArrayList {

	public PersistentBag() {
		super();
	}

	public PersistentBag(Collection collection) {
		super(collection);
	}

	public PersistentBag(int size) {
		super(size);
	}
		
	public boolean wasInitialized() {
		return true;
	}
}
