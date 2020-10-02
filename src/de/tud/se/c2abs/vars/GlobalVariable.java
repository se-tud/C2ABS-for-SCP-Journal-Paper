package de.tud.se.c2abs.vars;

import org.eclipse.cdt.core.dom.ast.IASTName;

import abs.frontend.ast.FieldUse;
import de.tud.se.c2abs.expwrapper.ExpWrapper;
import de.tud.se.c2abs.types.FullType;

public class GlobalVariable extends Variable {

	public GlobalVariable(int argNr, IASTName name, FullType fullType, boolean isConst, ExpWrapper expWrapper) {
		super(argNr, name, fullType, isConst, expWrapper);
	}

	@Override
	boolean isInitialized() {
		return true;
	}

	@Override
	public FieldUse getFieldUse() {
		return new FieldUse(GLOBAL);
	}

	@Override
	public int getArgNr() {
		return argNr;
	}

}