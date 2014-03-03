<section id="about">

# EMF Binding for JSON

JSON binding for [EMF](http://www.eclipse.org/emf) (Eclipse Modeling Framework) models, that allows serialization and
deserialization of EMF Resources in a specific [JSON](http://www.json.org/) format.

This is how a model looks like in JSON format.

```javascript
{
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
    "name" : "model",
    "nsPrefix" : "model",
    "nsURI" : "http://www.example.org/model",
    "eClassifiers" : [
        {
            "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
            "name" : "Library"
        }
    ]
}
```

## license

This software is distributed under the terms of the Eclipse Public License 1.0 - [EPL](http://www.eclipse.org/legal/epl-v10.html).

## requirements

* EMF 2.7.0
* Jackson 2.0.0 or later

</section>
