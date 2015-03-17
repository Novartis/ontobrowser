# IntroductionThe OntoBrowser web service exposes ontologies in OWL and OBO format. The base URI of the web service provides a list of the ontologies currently loaded in OntoBrowser. The OntoBrowser web service is implemented as a [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer) service.

The base URI for the web service is: `/ontobrowser/ontologies`

# AuthenticationThe OntoBrowser web service requires authentication as configured for the OntoBrowser web application. The same authentication credentials (username and password) used to access the OntoBrowser web application can be used to access the web service. 
# Supported Media TypesThe following Internet Media Types are supported by the web service for GET requests:

* `application/rdf+xml`* `application/owl+xml`* `text/owl-manchester`* `text/turtle`* `text/obo`
* `application/obo`* `application/json`

Note 1: There is no internet media type registered for the OBO format so the `text/obo` and `application/obo` media types are specific to the OntoBrowser web service.
 Note 2: The base URI only supports the JSON media type (as it provides a list of the ontologies currently available)
# Supported HTTP methodsThe web service supports the GET method for exporting ontologies in OWL and OBO format. The web service also supports the PUT method for loading ontologies. The PUT method only accepts [OBO formatted](http://oboformat.googlecode.com/svn/trunk/doc/GO.format.obo-1_2.html) data.  

# Example
The following example downloads the [Mouse adult gross anatomy](http://www.obofoundry.org/cgi-bin/detail.cgi?id=adult_mouse_anatomy) ontology then loads it into OntoBrowser using the web service:

```bash
$ curl -s -S -O -L http://purl.obolibrary.org/obo/ma.obo
$ curl -s -S -H "Content-Type: application/obo;charset=utf-8" -X PUT --data-binary "@ma.obo" -u SYSTEM "http://localhost/ontobrowser/ontologies/Mouse%20adult%20gross%20anatomy"
```
