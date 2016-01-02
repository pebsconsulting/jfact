package uk.ac.manchester.cs.jfact;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNodeSet;

import uk.ac.manchester.cs.jfact.datatypes.Datatype;
import uk.ac.manchester.cs.jfact.datatypes.DatatypeFactory;
import uk.ac.manchester.cs.jfact.datatypes.Literal;
import uk.ac.manchester.cs.jfact.kernel.ExpressionCache;
import uk.ac.manchester.cs.jfact.kernel.ReasoningKernel;
import uk.ac.manchester.cs.jfact.kernel.dl.IndividualName;
import uk.ac.manchester.cs.jfact.kernel.dl.axioms.Axioms;
import uk.ac.manchester.cs.jfact.kernel.dl.interfaces.*;

/** translation stuff */
public class TranslationMachinery implements Serializable {

    @Nonnull private final AxiomTranslator axiomTranslator;
    @Nonnull private final ClassExpressionTranslator classExpressionTranslator;
    @Nonnull private final DataRangeTranslator dataRangeTranslator;
    @Nonnull private final ObjectPropertyTranslator objectPropertyTranslator;
    @Nonnull private final DataPropertyTranslator dataPropertyTranslator;
    @Nonnull private final IndividualTranslator individualTranslator;
    @Nonnull private final EntailmentChecker entailmentChecker;
    @Nonnull private final Map<OWLAxiom, AxiomInterface> axiom2PtrMap = new HashMap<>();
    @Nonnull private final Map<AxiomInterface, OWLAxiom> ptr2AxiomMap = new HashMap<>();
    protected final ReasoningKernel kernel;
    protected final ExpressionCache em;
    protected final OWLDataFactory df;
    protected final DatatypeFactory datatypefactory;
    protected final EntityVisitorEx entityVisitor;

    /**
     * @param kernel
     *        kernel
     * @param df
     *        df
     * @param factory
     *        factory
     */
    public TranslationMachinery(ReasoningKernel kernel, OWLDataFactory df, DatatypeFactory factory) {
        this.kernel = kernel;
        datatypefactory = factory;
        em = kernel.getExpressionManager();
        this.df = df;
        axiomTranslator = new AxiomTranslator(kernel.getOntology(), df, this);
        classExpressionTranslator = new ClassExpressionTranslator(em, df, this);
        dataRangeTranslator = new DataRangeTranslator(em, df, this, datatypefactory);
        objectPropertyTranslator = new ObjectPropertyTranslator(em, df, this);
        dataPropertyTranslator = new DataPropertyTranslator(em, df, this);
        individualTranslator = new IndividualTranslator(em, df, this);
        entailmentChecker = new EntailmentChecker(kernel, df, this);
        entityVisitor = new EntityVisitorEx(this);
    }

    /**
     * @param signature
     *        signature
     * @return expressions
     */
    public List<Expression> translateExpressions(Stream<OWLEntity> signature) {
        return asList(signature.map(e -> e.accept(entityVisitor)).filter(e -> e != Axioms.dummyExpression()));
    }

    /**
     * @param axioms
     *        axioms
     */
    public void loadAxioms(Stream<OWLAxiom> axioms) {
        // TODO check valid axioms, such as those involving topDataProperty
        axioms.filter(ax -> !axiom2PtrMap.containsKey(ax)).forEach(ax -> checkAndAdd(ax, ax.accept(axiomTranslator)));
    }

    protected void checkAndAdd(OWLAxiom ax, AxiomInterface axiomPointer) {
        if (axiomPointer != Axioms.dummy()) {
            axiom2PtrMap.put(ax, axiomPointer);
            ptr2AxiomMap.put(axiomPointer, ax);
        }
    }

    /**
     * @param axiom
     *        axiom
     */
    public void retractAxiom(OWLAxiom axiom) {
        AxiomInterface ptr = axiom2PtrMap.get(axiom);
        if (ptr != null) {
            kernel.getOntology().retract(ptr);
            axiom2PtrMap.remove(axiom);
            ptr2AxiomMap.remove(ptr);
        }
    }

    protected ConceptExpression pointer(OWLClassExpression classExpression) {
        return classExpression.accept(classExpressionTranslator);
    }

    protected DataExpression pointer(OWLDataRange dataRange) {
        return dataRange.accept(dataRangeTranslator);
    }

    protected ObjectRoleExpression pointer(OWLObjectPropertyExpression propertyExpression) {
        OWLObjectPropertyExpression simp = propertyExpression.getSimplified();
        if (simp.isAnonymous()) {
            OWLObjectInverseOf inv = (OWLObjectInverseOf) simp;
            return em.inverse(objectPropertyTranslator.getPointerFromEntity(inv.getInverse().asOWLObjectProperty()));
        } else {
            return objectPropertyTranslator.getPointerFromEntity(simp.asOWLObjectProperty());
        }
    }

    protected DataRoleExpression pointer(OWLDataPropertyExpression propertyExpression) {
        return dataPropertyTranslator.getPointerFromEntity(propertyExpression.asOWLDataProperty());
    }

    protected synchronized IndividualName pointer(OWLIndividual individual) {
        if (!individual.isAnonymous()) {
            return individualTranslator.getPointerFromEntity(individual.asOWLNamedIndividual());
        } else {
            return em.individual(IRI.create(individual.toStringID()));
        }
    }

    protected synchronized Datatype<?> pointer(OWLDatatype datatype) {
        checkNotNull(datatype, "datatype cannot be null");
        return datatypefactory.getKnownDatatype(datatype.getIRI());
    }

    protected synchronized Literal<?> pointer(OWLLiteral literal) {
        String value = literal.getLiteral();
        if (literal.isRDFPlainLiteral()) {
            value = value + '@' + literal.getLang();
        }
        IRI string = literal.getDatatype().getIRI();
        Datatype<?> knownDatatype = datatypefactory.getKnownDatatype(string);
        return knownDatatype.buildLiteral(value);
    }

    protected NodeSet<OWLNamedIndividual> translateNodeSet(Stream<IndividualExpression> pointers) {
        OWLNamedIndividualNodeSet ns = new OWLNamedIndividualNodeSet();
        // XXX skipping anonymous individuals - counterintuitive but
        // that's the specs for you
        pointers.filter(p -> p instanceof IndividualName).forEach(p -> {
            OWLNamedIndividual i = individualTranslator.getEntityFromPointer((IndividualName) p);
            if (i != null) {
                ns.addEntity(i);
            }
        });
        return ns;
    }

    /**
     * @param inds
     *        inds
     * @return individual set
     */
    public List<IndividualExpression> translate(Stream<? extends OWLIndividual> inds) {
        return asList(inds.map(this::pointer));
    }

    /** @return class translation */
    public ClassExpressionTranslator getClassExpressionTranslator() {
        return classExpressionTranslator;
    }

    /** @return data range translator */
    public DataRangeTranslator getDataRangeTranslator() {
        return dataRangeTranslator;
    }

    /** @return object property translator */
    public ObjectPropertyTranslator getObjectPropertyTranslator() {
        return objectPropertyTranslator;
    }

    /** @return data property translator */
    public DataPropertyTranslator getDataPropertyTranslator() {
        return dataPropertyTranslator;
    }

    /** @return individual translator */
    public IndividualTranslator getIndividualTranslator() {
        return individualTranslator;
    }

    /** @return entailemnt checker */
    public EntailmentChecker getEntailmentChecker() {
        return entailmentChecker;
    }

    /**
     * @param trace
     *        trace
     * @return trnslated set
     */
    public Set<OWLAxiom> translateTAxiomSet(Stream<AxiomInterface> trace) {
        return asSet(trace.map(ptr2AxiomMap::get));
    }
}
