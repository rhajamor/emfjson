/*******************************************************************************
 * Copyright (c) 2013 Guillaume Hillairet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Hillairet - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.emfjson.map;

import static java.lang.Boolean.TRUE;
import static org.eclipselabs.emfjson.EMFJs.OPTION_INDENT_OUTPUT;
import static org.eclipselabs.emfjson.EMFJs.OPTION_PROXY_ATTRIBUTES;
import static org.eclipselabs.emfjson.EMFJs.OPTION_ROOT_ELEMENT;
import static org.eclipselabs.emfjson.EMFJs.OPTION_SERIALIZE_NAMESPACES;
import static org.eclipselabs.emfjson.EMFJs.OPTION_SERIALIZE_REF_TYPE;
import static org.eclipselabs.emfjson.EMFJs.OPTION_SERIALIZE_TYPE;
import static org.eclipselabs.emfjson.EMFJs.OPTION_USE_UUID;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * EObjectMapper can be used by clients to serialize of deserialize an EObject or 
 * the content of an existing Resource into a {@link JsonNode}. 
 * 
 * 
 * @author ghillairet
 * @since 0.6.0
 */
public class EObjectMapper {

	private boolean serializeTypes = true;
	private boolean serializeRefTypes = true;
	private boolean useProxyAttributes = false;
	private boolean serializeNamespaces = false;
	private boolean useUUID = false;
	private boolean indentOutput = false;

	private EClass rootClass = null;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public EObjectMapper() {}

	public Object from(InputStream inputStream, Resource resource, Map<?, ?> options) {
		JsonNode node = null;
		try {
			node = objectMapper.readTree(inputStream);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return node == null ? null : from(node, resource, options);
	}

	public Object from(URL url, Resource resource, Map<?, ?> options) {
		JsonNode node = null;
		try {
			node = objectMapper.readTree(url);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return node == null ? null : from(node, resource, options);
	}

	public Object from(JsonNode node, Resource resource, Map<?, ?> options) {
		if (node == null)
			return null;

		configureDeserializer(options);

		if (node.isArray()) {
			return from((ArrayNode) node, resource);
		} else if (node.isObject()) {
			return from((ObjectNode) node, resource);
		} else {
			return null;
		}
	}

	public EObject from(ObjectNode node, Resource resource) {
		final Deserializer from = new Deserializer();
		from.setUseProxyAttributes(useProxyAttributes);
		from.setUseUUID(useUUID);

		final EList<EObject> contents = resource.getContents();
		final EObject result = from.from(node, rootClass, resource);
		if (result != null) {
			if (!contents.isEmpty()) {
				contents.clear();
			}
			contents.add(result);
			from.resolve(resource);
		}

		return result;
	}

	public EList<EObject> from(ArrayNode node, Resource resource) {
		final Deserializer from = new Deserializer();
		from.setUseProxyAttributes(useProxyAttributes);
		from.setUseUUID(useUUID);

		final EList<EObject> contents = resource.getContents();
		final EList<EObject> result = from.from(node, rootClass, resource);
		if (!contents.isEmpty()) {
			contents.clear();
		}
		contents.addAll(result);
		from.resolve(resource);

		return result;
	}

	public JsonNode to(Resource resource, Map<?, ?> options) {
		configureSerializer(options);

		Serializer to = new Serializer();
		to.setSerializeNamespaces(serializeNamespaces);
		to.setSerializeRefTypes(serializeRefTypes);
		to.setSerializeTypes(serializeTypes);

		return to.to(resource, objectMapper);
	}

	public ObjectNode to(EObject eObject) {
		if (eObject == null) throw new IllegalArgumentException("EObject is null");
		if (eObject.eResource() == null) throw new IllegalArgumentException("EObject must be contained in a Resource");

		return to(eObject, eObject.eResource());
	}

	public ObjectNode to(EObject eObject, Resource resource) {
		Serializer to = new Serializer();
		to.setSerializeNamespaces(serializeNamespaces);
		to.setSerializeRefTypes(serializeRefTypes);
		to.setSerializeTypes(serializeTypes);
		to.setUseUUID(useUUID);

		return to.to(eObject, resource, objectMapper);
	}

	public void write(OutputStream outStream, Resource resource, Map<?, ?> options) {
		write(outStream, to(resource, options));
	}

	public void write(OutputStream output, JsonNode current) {
		if (indentOutput) {
			objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		}

		try {
			objectMapper.writeValue(output, current);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void configureDeserializer(Map<?, ?> options) {
		if (options == null) {
			options = Collections.emptyMap();
		}

		if (options.containsKey(OPTION_ROOT_ELEMENT)) {
			Object optionEClass = options.get(OPTION_ROOT_ELEMENT);
			if (optionEClass instanceof EClass) {
				configure(OPTION_ROOT_ELEMENT, (EClass) optionEClass);
			}
		}

		configure(OPTION_PROXY_ATTRIBUTES, TRUE.equals(options.get(OPTION_PROXY_ATTRIBUTES)));
	}

	private void configureSerializer(Map<?, ?> options) {
		if (options == null) {
			options = Collections.emptyMap();
		}

		boolean serializeTypes = true;
		boolean serializeRefTypes = true;
		boolean serializeNamespaces = false;
		boolean indent = true;
		boolean useUUID = false;

		if (options.containsKey(OPTION_INDENT_OUTPUT)) {
			try {
				indent = (Boolean) options.get(OPTION_INDENT_OUTPUT);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		if (options.containsKey(OPTION_SERIALIZE_TYPE)) {
			try {
				serializeTypes = (Boolean) options.get(OPTION_SERIALIZE_TYPE);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		if (options.containsKey(OPTION_SERIALIZE_REF_TYPE)) {
			try {
				serializeRefTypes = (Boolean) options.get(OPTION_SERIALIZE_REF_TYPE);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		if (options.containsKey(OPTION_SERIALIZE_NAMESPACES)) {
			try {
				serializeNamespaces = (Boolean) options.get(OPTION_SERIALIZE_NAMESPACES);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		if (options.containsKey(OPTION_USE_UUID)) {
			try {
				useUUID = (Boolean) options.get(OPTION_USE_UUID);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}

		configure(OPTION_SERIALIZE_TYPE, serializeTypes);
		configure(OPTION_SERIALIZE_REF_TYPE, serializeRefTypes);
		configure(OPTION_SERIALIZE_NAMESPACES, serializeNamespaces);
		configure(OPTION_INDENT_OUTPUT, indent);
		configure(OPTION_USE_UUID, useUUID);
	}

	public void configure(String key, Object value) {
		if (OPTION_SERIALIZE_TYPE.equals(key)) {
			serializeTypes = (Boolean) value;
		}
		if (OPTION_SERIALIZE_REF_TYPE.equals(key)) {
			serializeRefTypes = (Boolean) value;
		}
		if (OPTION_SERIALIZE_NAMESPACES.equals(key)) {
			serializeNamespaces = (Boolean) value;
		}
		if (OPTION_ROOT_ELEMENT.equals(key)) {
			rootClass = (EClass) value;
		}
		if (OPTION_PROXY_ATTRIBUTES.equals(key)) {
			useProxyAttributes = (Boolean) value;
		}
		if (OPTION_INDENT_OUTPUT.equals(key)) {
			indentOutput = (Boolean) value;
		}
		if (OPTION_USE_UUID.equals(key)) {
			useUUID = (Boolean) value;
		}
	}

}
