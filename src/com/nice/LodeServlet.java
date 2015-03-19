package com.nice;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Copyright (c) 2010-2013, Silvio Peroni <essepuntato@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.mindswap.pellet.PelletOptions;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Servlet implementation class LodeServlet
 */
public class LodeServlet extends IOException {
    private static final long serialVersionUID = 1L;
    private String xsltURL = "http://lode.sourceforge.net/xslt";
    private String cssLocation = "http://eelst.cs.unibo.it/apps/LODE/";
    private int maxTentative = 3;

    public LodeServlet() {
        super();
    }

    protected void doGet(String filepath) throws IOException {
        SourceExtractor extractor = new SourceExtractor();
        File f = new File(filepath);

        for (int i = 0; i < maxTentative; i++) {
            try {

                String content = "";

                boolean useOWLAPI = true;
//				boolean considerImportedOntologies =
//					new Boolean(request.getParameter("imported"));
//				boolean considerImportedClosure =
//					new Boolean(request.getParameter("closure"));
//				boolean useReasoner =
//					new Boolean(request.getParameter("reasoner"));

//				if (considerImportedOntologies || considerImportedClosure || useReasoner) {
//					useOWLAPI = true;
//				}
//
                String lang = "en";



                if (useOWLAPI) {
                    content = parseWithOWLAPI(f, useOWLAPI, false,
                            false, false);
                } else {
                    content = extractor.exec(f);
                }

                content = applyXSLTTransformation(content, "", lang); //ontology ur

                String newHtmlPath = filepath.substring(0, filepath.lastIndexOf('.')) + ".html";
                File fOut = new File(newHtmlPath);
                if(!fOut.exists())
                {
                    fOut.createNewFile();
                }

                FileOutputStream outStream = new FileOutputStream(fOut);
                outStream.write(content.getBytes());
                outStream.flush();
                outStream.close();
                i = maxTentative;
            } catch (Exception e) {
                if (i + 1 == maxTentative) {
                    //out.println(getErrorPage(e));
                }
            }
        }
    }

//	private void resolvePaths(HttpServletRequest request) {
//		xsltURL = getServletContext().getRealPath("extraction.xsl");
//		String requestURL = request.getRequestURL().toString();
//		int index = requestURL.lastIndexOf("/");
//		cssLocation = requestURL.substring(0, index) + File.separator;
//	}



    private String parseWithOWLAPI(File ont,
                                   boolean useOWLAPI,
                                   boolean considerImportedOntologies,
                                   boolean considerImportedClosure,
                                   boolean useReasoner)
            throws OWLOntologyCreationException, OWLOntologyStorageException, URISyntaxException {
        String result = "";

        if (useOWLAPI) {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ont);

            if (considerImportedClosure || considerImportedOntologies) {
                Set<OWLOntology> setOfImportedOntologies = new HashSet<OWLOntology>();
                if (considerImportedOntologies) {
                    setOfImportedOntologies.addAll(ontology.getDirectImports());
                } else {
                    setOfImportedOntologies.addAll(ontology.getImportsClosure());
                }
                for (OWLOntology importedOntology : setOfImportedOntologies) {
                    manager.addAxioms(ontology, importedOntology.getAxioms());
                }
            }

            if (useReasoner) {
                ontology = parseWithReasoner(manager, ontology);
            }

            StringDocumentTarget parsedOntology = new StringDocumentTarget();

            manager.saveOntology(ontology, new RDFXMLOntologyFormat(), parsedOntology);
            result = parsedOntology.toString();
        }

        return result;
    }

    private String addImportedAxioms(String result, List<String> removed) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(result.getBytes()));

            NodeList ontologies =
                    document.getElementsByTagNameNS("http://www.w3.org/2002/07/owl#", "Ontology");
            if (ontologies.getLength() > 0) {
                Element ontology = (Element) ontologies.item(0);

                for (String toBeAdded : removed) {
                    Element importElement =
                            document.createElementNS("http://www.w3.org/2002/07/owl#","owl:imports");
                    importElement.setAttributeNS(
                            "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:resource", toBeAdded);
                    ontology.appendChild(importElement);
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StreamResult output = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, output);

            return output.getWriter().toString();
        } catch (ParserConfigurationException e) {
            return result;
        } catch (SAXException e) {
            return result;
        } catch (IOException e) {
            return result;
        } catch (TransformerConfigurationException e) {
            return result;
        } catch (TransformerException e) {
            return result;
        }
    }


    private OWLOntology parseWithReasoner(OWLOntologyManager manager, OWLOntology ontology) {
        try {
            PelletOptions.load(new URL("http://" + cssLocation + "pellet.properties"));
            PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);
            reasoner.getKB().prepare();
            List<InferredAxiomGenerator<? extends OWLAxiom>> generators=
                    new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
            generators.add(new InferredSubClassAxiomGenerator());
            generators.add(new InferredClassAssertionAxiomGenerator());
            generators.add(new InferredDisjointClassesAxiomGenerator());
            generators.add(new InferredEquivalentClassAxiomGenerator());
            generators.add(new InferredEquivalentDataPropertiesAxiomGenerator());
            generators.add(new InferredEquivalentObjectPropertyAxiomGenerator());
            generators.add(new InferredInverseObjectPropertiesAxiomGenerator());
            generators.add(new InferredPropertyAssertionGenerator());
            generators.add(new InferredSubDataPropertyAxiomGenerator());
            generators.add(new InferredSubObjectPropertyAxiomGenerator());

            InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, generators);

            OWLOntologyID id = ontology.getOntologyID();
            Set<OWLImportsDeclaration> declarations = ontology.getImportsDeclarations();
            Set<OWLAnnotation> annotations = ontology.getAnnotations();

            Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations = new HashMap<OWLEntity,Set<OWLAnnotationAssertionAxiom>>();
            for (OWLClass aEntity  : ontology.getClassesInSignature()) {
                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
            }
            for (OWLObjectProperty aEntity : ontology.getObjectPropertiesInSignature()) {
                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
            }
            for (OWLDataProperty aEntity : ontology.getDataPropertiesInSignature()) {
                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
            }
            for (OWLNamedIndividual aEntity : ontology.getIndividualsInSignature()) {
                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
            }
            for (OWLAnnotationProperty aEntity : ontology.getAnnotationPropertiesInSignature()) {
                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
            }
            for (OWLDatatype aEntity : ontology.getDatatypesInSignature()) {
                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
            }

            manager.removeOntology(ontology);
            OWLOntology inferred = manager.createOntology(id);
            iog.fillOntology(manager, inferred);

            for (OWLImportsDeclaration decl : declarations) {
                manager.applyChange(new AddImport(inferred, decl));
            }
            for (OWLAnnotation ann : annotations) {
                manager.applyChange(new AddOntologyAnnotation(inferred, ann));
            }
            for (OWLClass aEntity : inferred.getClassesInSignature()) {
                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
            }
            for (OWLObjectProperty aEntity : inferred.getObjectPropertiesInSignature()) {
                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
            }
            for (OWLDataProperty aEntity : inferred.getDataPropertiesInSignature()) {
                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
            }
            for (OWLNamedIndividual aEntity : inferred.getIndividualsInSignature()) {
                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
            }
            for (OWLAnnotationProperty aEntity : inferred.getAnnotationPropertiesInSignature()) {
                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
            }
            for (OWLDatatype aEntity : inferred.getDatatypesInSignature()) {
                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
            }

            return inferred;
        } catch (FileNotFoundException e1) {
            return ontology;
        } catch (MalformedURLException e1) {
            return ontology;
        } catch (IOException e1) {
            return ontology;
        } catch (OWLOntologyCreationException e) {
            return ontology;
        }
    }

    private void applyAnnotations(
            OWLEntity aEntity, Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations,
            OWLOntologyManager manager, OWLOntology ontology) {
        Set<OWLAnnotationAssertionAxiom> entitySet = entityAnnotations.get(aEntity);
        if (entitySet != null) {
            for (OWLAnnotationAssertionAxiom ann : entitySet) {
                manager.addAxiom(ontology, ann);
            }
        }
    }

    private String getErrorPage(Exception e) {
        return
                "<html>" +
                        "<head><title>LODE error</title></head>" +
                        "<body>" +
                        "<h2>" +
                        "LODE error" +
                        "</h2>" +
                        "<p><strong>Reason: </strong>" +
                        e.getMessage() +
                        "</p>" +
                        "</body>" +
                        "</html>";
    }

    private String applyXSLTTransformation(String source, String ontologyUrl, String lang)
            throws TransformerException {
        TransformerFactory tfactory = new net.sf.saxon.TransformerFactoryImpl();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Transformer transformer =
                tfactory.newTransformer(
                        new StreamSource(xsltURL));

        transformer.setParameter("css-location", cssLocation);
        transformer.setParameter("lang", lang);
        transformer.setParameter("ontology-url", ontologyUrl);
        transformer.setParameter("source", cssLocation + "source");

        StreamSource inputSource = new StreamSource(new StringReader(source));

        transformer.transform(
                inputSource,
                new StreamResult(output));

        return output.toString();
    }
}