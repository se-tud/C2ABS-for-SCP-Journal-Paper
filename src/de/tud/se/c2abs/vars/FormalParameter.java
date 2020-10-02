package de.tud.se.c2abs.vars;

import org.eclipse.cdt.core.dom.ast.IASTName;

import abs.frontend.ast.FieldUse;
import de.tud.se.c2abs.types.FullType;
import de.tud.se.c2abs.utils.Constants;

public class FormalParameter implements VarOrParamDeclaration, Constants {
	
	public final int argNr;
	public final IASTName name;
	public final FullType fullType;
	public final boolean isConst;

	public FormalParameter(final int argNr, final IASTName name, final FullType fullType, final boolean isConst) {
		this.argNr = argNr;
		this.name = name;
		this.fullType = fullType;
		this.isConst = isConst;
	}

	@Override
	public String getName() {
		return name.toString();
	}

	@Override
	public FullType getFullType() {
		return fullType;
	}

	@Override
	public FieldUse getFieldUse() {
		return new FieldUse(PARAMS);
	}

	@Override
	public int getArgNr() {
		return argNr;
	}

}
