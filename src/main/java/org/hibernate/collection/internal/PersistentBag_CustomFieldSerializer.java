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

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.Collection_CustomFieldSerializerBase;

public final class PersistentBag_CustomFieldSerializer extends CustomFieldSerializer<org.hibernate.collection.internal.PersistentBag> {
	/*	
	public static String concreteType() {
		return "java.util.ArrayList";
	}
	*/
	
	public static void serialize(SerializationStreamWriter streamWriter,
			org.hibernate.collection.internal.PersistentBag instance) throws SerializationException {
		if(instance.wasInitialized()) {
			Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
		} else {
			streamWriter.writeInt(0);
		}
	}
	
	public static void deserialize(SerializationStreamReader streamReader,
			org.hibernate.collection.internal.PersistentBag instance) throws SerializationException {
		if(instance.wasInitialized()) {
			Collection_CustomFieldSerializerBase.deserialize(streamReader, instance);
		} else {
			int size = streamReader.readInt();
			for (int i = 0; i < size; ++i) {
				streamReader.readObject();
			}
		}
	}
		
	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter,
			org.hibernate.collection.internal.PersistentBag instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
	
	@Override
	public void deserializeInstance(SerializationStreamReader streamReader,
			org.hibernate.collection.internal.PersistentBag instance) throws SerializationException {
		deserialize(streamReader, instance);
	}
}
