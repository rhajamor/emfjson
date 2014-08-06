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

import static org.eclipselabs.emfjson.common.Constants.EJS_TYPE_KEYWORD;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipselabs.emfjson.common.Constants;
import org.eclipselabs.emfjson.common.ModelUtil;
import org.eclipselabs.emfjson.resource.UUIDResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class Deserializer {

	boolean useProxyAttributes = false;
	boolean useUUID = false;

	private EAtttributeDeserializer eAtttributeDeserializer;
	private EReferenceDeserializer eReferenceDeserializer;
	private EReferenceResolver resolver;
	private NamespaceDeserializer namespaceDeserializer;

	private Map<EObject, JsonNode> processed = new HashMap<EObject, JsonNode>();
	private Map<String, String> namespaces = new HashMap<String, String>();

	private ProxyFactory proxyFactory;

	Deserializer() { 
		this.eAtttributeDeserializer = new EAtttributeDeserializer();
		this.proxyFactory = new ProxyFactory(this);
		this.eReferenceDeserializer = new EReferenceDeserializer(this);
		this.resolver = new EReferenceResolver(this);
		this.namespaceDeserializer = new NamespaceDeserializer();
	}

	EObject from(ObjectNode node, EClass eClass, Resource resource) {
		EObject eObject = null;
		ResourceSet resourceSet = resource.getResourceSet();
		if (resourceSet == null) {
			resourceSet = new ResourceSetImpl();
		}

		namespaces.putAll(namespaceDeserializer.deSerialize(node));

		if (eClass == null && node.has(EJS_TYPE_KEYWORD)) {
			URI eClassURI = ModelUtil.getEObjectURI(node.get(EJS_TYPE_KEYWORD), resource, getNamespaces());
			eClass = getEClass(eClassURI, resourceSet);
		}

		if (eClass != null && eClass instanceof EClass) {
			eObject = EcoreUtil.create(eClass);
			getProcessed().put(eObject, node);

			if (useUUID && node.get(Constants.EJS_UUID_ANNOTATION) != null) {
				String uuid = node.get(Constants.EJS_UUID_ANNOTATION).asText();
				if (resource instanceof UUIDResource) {
					((UUIDResource) resource).setID(eObject, uuid);
				}
			}

			eAtttributeDeserializer.deSerialize(eObject, node);
			eReferenceDeserializer.deSerialize(eObject, node, resource);
		}

		return eObject;
	}

	EObject from(ObjectNode node, Resource resource) {
		return from(node, null, resource);
	}

	EList<EObject> from(ArrayNode node, Resource resource) {
		return from(node, null, resource);
	}

	EList<EObject> from(ArrayNode node, EClass rootClass, Resource resource) {
		final EList<EObject> returnList = new BasicEList<EObject>();
		EObject eObject;

		for (Iterator<JsonNode> it = node.elements(); it.hasNext();) {
			JsonNode element = it.next();
			if (element.isObject()) {
				eObject = from((ObjectNode) element, rootClass, resource);
				if (eObject != null) {
					returnList.add(eObject);
				}
			}
		}

		return returnList;
	}

	void resolve(Resource resource) {
		resolver.resolve(getProcessed(), resource);
		processed.clear();
	}

	EClass getEClass(URI uri, ResourceSet resourceSet) {
		return (EClass) resourceSet.getEObject(uri, true);
	}

	EAtttributeDeserializer getEAtttributeDeserializer() {
		return eAtttributeDeserializer;
	}

	EReferenceDeserializer getEReferenceDeserializer() {
		return eReferenceDeserializer;
	}

	NamespaceDeserializer getNamespaceDeserializer() {
		return namespaceDeserializer;
	}

	ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	Map<String, String> getNamespaces() {
		return namespaces;
	}

	Map<EObject, JsonNode> getProcessed() {
		return processed;
	}

	void setUseProxyAttributes(boolean useProxyAttributes) {
		this.useProxyAttributes = useProxyAttributes;
	}

	void setUseUUID(boolean useUUID) {
		this.useUUID = useUUID;
	}

}
