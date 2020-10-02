package de.tud.se.c2abs.vars;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTName;

import abs.frontend.ast.PureExp;
import abs.frontend.ast.Stmt;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.expwrapper.ExpWrapper;
import de.tud.se.c2abs.types.FullType;

public abstract class Variable extends VarOrParamValue implements VarOrParamDeclaration {
	
	private final ExpWrapper expWrapper;

	public Variable(int argNr, IASTName name, FullType fullType, boolean isConst, ExpWrapper expWrapper) {
		super(argNr, name, fullType, isConst);

		this.expWrapper = expWrapper;
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
	boolean isDefault() {
		return expWrapper == null;
	}

	@Override
	List<Stmt> getStatements() {
		return expWrapper.extractStatements();
	}

	@Override
	PureExp getInitExp() {
		if (expWrapper == null || expWrapper.isFuture) {
			return fullType.getNonArrayType().getWitness(false);
		} else {
			return Utils.createValue(expWrapper.getExp(), fullType.getNonArrayType().getTypeUse());
		}
	}

}
