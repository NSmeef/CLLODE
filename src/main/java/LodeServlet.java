

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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.mindswap.pellet.PelletOptions;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredDisjointClassesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentDataPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentObjectPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredInverseObjectPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubDataPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubObjectPropertyAxiomGenerator;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * Servlet implementation class LodeServlet
 */
public class LodeServlet extends IOException {
    private static final long serialVersionUID = 1L;
    private String xsltURL = "http://lode.sourceforge.net/xslt";
    private String cssLocation = "//eelst.cs.unibo.it/apps/LODE/";
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
                System.out.println("Printing output to " + newHtmlPath);
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
                    System.out.println(e.getMessage());
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