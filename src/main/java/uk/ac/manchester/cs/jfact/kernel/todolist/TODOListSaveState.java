package uk.ac.manchester.cs.jfact.kernel.todolist;

/* This file is part of the JFact DL reasoner
 Copyright 2011-2013 by Ignazio Palmisano, Dmitry Tsarkov, University of Manchester
 This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA*/
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import conformance.PortedFrom;

/** class for saving/restoring array Todo table */
public class TODOListSaveState implements Serializable {
    private static final long serialVersionUID = 11000L;
    /** save state of all regular queues */
    protected final int[][] backup;
    /** save number-of-entries to do */
    @PortedFrom(file = "ToDoList.h", name = "noe")
    protected int noe;
    protected int backupID_sp;
    protected int backupID_ep;
    /** save whole array */
    protected List<ToDoEntry> waitingQueue;
    /** save start point of queue of entries */
    protected int sp;
    /** save end point of queue of entries */
    protected int ep;
    /** save flag of queue's consistency */
    protected boolean queueBroken;

    /** @param options */
    public TODOListSaveState(int options) {
        backup = new int[options][2];
    }

    @Override
    public String toString() {
        return noe + " " + backupID_sp + "," + backupID_ep + " " + waitingQueue + " "
                + sp + " " + ep + " " + queueBroken + " " + Arrays.toString(backup);
    }
}