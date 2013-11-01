package uk.ac.manchester.cs.jfact.split;

/* This file is part of the JFact DL reasoner
 Copyright 2011-2013 by Ignazio Palmisano, Dmitry Tsarkov, University of Manchester
 This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA*/
import java.io.Serializable;

import uk.ac.manchester.cs.jfact.kernel.dl.*;
import uk.ac.manchester.cs.jfact.kernel.dl.interfaces.*;
import uk.ac.manchester.cs.jfact.visitors.DLExpressionVisitor;
import uk.ac.manchester.cs.jfact.visitors.DLExpressionVisitorAdapter;
import conformance.PortedFrom;

/** update the signature by adding all signature elements from the expression */
@PortedFrom(file = "tSignatureUpdater.h", name = "TExpressionSignatureUpdater")
class TExpressionSignatureUpdater extends DLExpressionVisitorAdapter implements
        DLExpressionVisitor, Serializable {
    private static final long serialVersionUID = 11000L;
    /** Signature to be filled */
    @PortedFrom(file = "tSignatureUpdater.h", name = "sig")
    private final TSignature sig;

    /** helper for concept arguments */
    @PortedFrom(file = "tSignatureUpdater.h", name = "vC")
    private void vC(ConceptArg expr) {
        expr.getConcept().accept(this);
    }

    /** helper for individual arguments */
    @PortedFrom(file = "tSignatureUpdater.h", name = "vI")
    private void vI(IndividualExpression expr) {
        // should no longer be needed: IndividualNames are NamedEntities
        // themselves
        if (expr instanceof NamedEntity) {
            sig.add((NamedEntity) expr);
        }
    }

    /** helper for object role arguments */
    @PortedFrom(file = "tSignatureUpdater.h", name = "vOR")
    private void vOR(ObjectRoleArg expr) {
        expr.getOR().accept(this);
    }

    /** helper for object role arguments */
    @PortedFrom(file = "tSignatureUpdater.h", name = "vDR")
    private void vDR(DataRoleArg expr) {
        expr.getDataRoleExpression().accept(this);
    }

    /** helper for the named entity */
    @PortedFrom(file = "tSignatureUpdater.h", name = "vE")
    private void vE(NamedEntity e) {
        sig.add(e);
    }

    /** array helper */
    @PortedFrom(file = "tSignatureUpdater.h", name = "processArray")
    private void processArray(NAryExpression<? extends Expression> expr) {
        for (Expression p : expr.getArguments()) {
            p.accept(this);
        }
    }

    public TExpressionSignatureUpdater(TSignature s) {
        sig = s;
    }

    // concept expressions
    @Override
    public void visit(ConceptName expr) {
        vE(expr);
    }

    @Override
    public void visit(ConceptNot expr) {
        vC(expr);
    }

    @Override
    public void visit(ConceptAnd expr) {
        processArray(expr);
    }

    @Override
    public void visit(ConceptOr expr) {
        processArray(expr);
    }

    @Override
    public void visit(ConceptOneOf<?> expr) {
        processArray(expr);
    }

    @Override
    public void visit(ConceptObjectSelf expr) {
        vOR(expr);
    }

    @Override
    public void visit(ConceptObjectValue expr) {
        vOR(expr);
        vI(expr.getIndividual());
    }

    @Override
    public void visit(ConceptObjectExists expr) {
        vOR(expr);
        vC(expr);
    }

    @Override
    public void visit(ConceptObjectForall expr) {
        vOR(expr);
        vC(expr);
    }

    @Override
    public void visit(ConceptObjectMinCardinality expr) {
        vOR(expr);
        vC(expr);
    }

    @Override
    public void visit(ConceptObjectMaxCardinality expr) {
        vOR(expr);
        vC(expr);
    }

    @Override
    public void visit(ConceptObjectExactCardinality expr) {
        vOR(expr);
        vC(expr);
    }

    @Override
    public void visit(ConceptDataValue expr) {
        vDR(expr);
    }

    @Override
    public void visit(ConceptDataExists expr) {
        vDR(expr);
    }

    @Override
    public void visit(ConceptDataForall expr) {
        vDR(expr);
    }

    @Override
    public void visit(ConceptDataMinCardinality expr) {
        vDR(expr);
    }

    @Override
    public void visit(ConceptDataMaxCardinality expr) {
        vDR(expr);
    }

    @Override
    public void visit(ConceptDataExactCardinality expr) {
        vDR(expr);
    }

    // individual expressions
    @Override
    public void visit(IndividualName expr) {
        vE(expr);
    }

    // object role expressions
    @Override
    public void visit(ObjectRoleName expr) {
        vE(expr);
    }

    @Override
    public void visit(ObjectRoleInverse expr) {
        vOR(expr);
    }

    @Override
    public void visit(ObjectRoleChain expr) {
        processArray(expr);
    }

    @Override
    public void visit(ObjectRoleProjectionFrom expr) {
        vOR(expr);
        vC(expr);
    }

    @Override
    public void visit(ObjectRoleProjectionInto expr) {
        vOR(expr);
        vC(expr);
    }

    // data role expressions
    @Override
    public void visit(DataRoleName expr) {
        vE(expr);
    }
}