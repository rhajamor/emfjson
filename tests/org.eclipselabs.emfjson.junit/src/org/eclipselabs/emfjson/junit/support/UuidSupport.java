package org.eclipselabs.emfjson.junit.support;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipselabs.emfjson.common.Constants;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class UuidSupport {

	private ResourceSet resourceSet = new ResourceSetImpl();

	protected String uuid(EObject object) {
		return EcoreUtil.getURI(object).fragment();
	}

	protected String uuid(JsonNode node) {
		return node.get(Constants.EJS_UUID_ANNOTATION).asText();
	}
	
	protected Resource createUuidResource(String name) {
		Resource resource = new XMIResourceImpl(URI.createURI(name)) {
			@Override
			protected boolean useUUIDs() {
				return true;
			}
		};
		resourceSet.getResources().add(resource);
		return resource;
	}
}
