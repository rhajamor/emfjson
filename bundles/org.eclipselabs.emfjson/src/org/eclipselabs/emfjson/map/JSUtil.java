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

import static org.eclipselabs.emfjson.common.Constants.EJS_REF_KEYWORD;
import static org.eclipselabs.emfjson.common.Constants.EJS_TYPE_KEYWORD;
import static org.eclipselabs.emfjson.common.ModelUtil.getEObjectURI;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSUtil {

	public static JsonNode getNode(Resource resource, URI objectURI, EClass eClass) {
		URI fileURI = objectURI.trimFragment();
		ResourceSet resourceSet = resource.getResourceSet();
		URIConverter uriConverter = resourceSet.getURIConverter();

		String slash = "/";
		String current = slash;
		try {
			InputStream inStream = uriConverter.createInputStream(fileURI);
			JsonNode root = new ObjectMapper().readTree(inStream);
			return findNode(root, resourceSet, current, objectURI);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static JsonNode findNode(JsonNode node, ResourceSet resourceSet, String fragment, URI objectURI) {
		if (node.isArray()) {
			int pos = 0;
			String idx = fragment;
			for (Iterator<JsonNode> it = node.elements(); it.hasNext();) {
				idx = fragment + pos;
				JsonNode current = it.next();

				final EClass currentEClass = getEClass(current, resourceSet);
				if (currentEClass != null) {
					EAttribute id = currentEClass.getEIDAttribute();
					if (id != null) {
						if (objectURI.trimFragment().appendFragment(current.get(id.getName()).asText()).equals(objectURI)) {
							return current;
						}
					} else {
						if (objectURI.trimFragment().appendFragment(idx).equals(objectURI)) {
							return current;
						}
					}

					for (EReference reference : currentEClass.getEAllContainments()) {
						if (current.has(reference.getName())) {
							idx += "/@" + reference.getName() + (reference.isMany() ? "." : "");
							JsonNode found = findNode(current.get(reference.getName()), resourceSet, idx, objectURI);
							if (found != null) {
								return found;
							}
						}
					}
				}

				pos++;
			}
		} else {
			final EClass currentEClass = getEClass(node, resourceSet);
			if (currentEClass != null) {
				EAttribute id = currentEClass.getEIDAttribute();
				if (id != null) {
					if (objectURI.trimFragment().appendFragment(node.get(id.getName()).asText()).equals(objectURI)) {
						return node;
					}
				} else {
					if (objectURI.trimFragment().appendFragment(fragment).equals(objectURI)) {
						return node;
					}
				}

				for (EReference reference : currentEClass.getEAllContainments()) {
					if (node.has(reference.getName())) {
						fragment += "/@" + reference.getName() + (reference.isMany() ? "." : "");
						JsonNode found = findNode(node.get(reference.getName()), resourceSet, fragment, objectURI);
						if (found != null) {
							return found;
						}
					}
				}
			}
		}

		return null;
	}

	public static EClass getEClass(JsonNode node, ResourceSet resourceSet) {
		if (node.has(EJS_TYPE_KEYWORD)) {
			return (EClass) resourceSet.getEObject(URI.createURI(node.get(EJS_TYPE_KEYWORD).asText()), false);
		} else {
			return null;
		}
	}

	public static EClass findEClass(EClass eReferenceType, JsonNode node, JsonNode root, Resource resource, Map<String, String> namespaces) {
		ResourceSet resourceSet = resource.getResourceSet();
		if (resourceSet == null) {
			resourceSet = new ResourceSetImpl();
		}

		if (eReferenceType.isAbstract()) {

			if (node.has(EJS_TYPE_KEYWORD)) {
				JsonNode typeNode = node.get(EJS_TYPE_KEYWORD);
				final URI typeURI = getEObjectURI(typeNode, eReferenceType.eResource(), namespaces);

				try {
					return (EClass) resourceSet.getEObject(typeURI, true);
				} catch (ClassCastException e) {
					return null;
				}
			} else if (node.has(EJS_REF_KEYWORD)) {
				JsonNode refNode = node.get(EJS_REF_KEYWORD);
				URI refURI = getEObjectURI(refNode, resource, namespaces);
				EObject eObject = resourceSet.getEObject(refURI, true);

				if (eObject != null) {
					return resourceSet.getEObject(refURI, false).eClass();
				}

				refNode = findNode(refURI, eReferenceType, root);
				if (refNode != null) {
					return findEClass(eReferenceType, refNode, root, resource, namespaces);
				}
			}
		}

		return eReferenceType;
	}

	public static JsonNode findNode(URI nodeURI, EClass eClass, JsonNode root) {
		EAttribute eID = eClass.getEIDAttribute();
		if (eID == null) {
			final EStructuralFeature featureName = eClass.getEStructuralFeature("name");
			if (featureName != null && featureName instanceof EAttribute) {
				eID = (EAttribute) featureName;
			} else {
				return null;
			}
		}

		String fragment = nodeURI.fragment().startsWith("//") ? nodeURI.fragment().substring(2) : nodeURI.fragment();

		for (JsonNode node : root.findParents(eID.getName())) {
			String value = node.get(eID.getName()).asText();
			if (value.equals(fragment)) {
				return node;
			}
		}
		return null;
	}

}
