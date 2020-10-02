package de.tud.se.c2abs.vars;

import java.util.Collections;
import java.util.List;

import abs.frontend.ast.PureExp;
import abs.frontend.ast.Stmt;
import de.tud.se.c2abs.subexp.args.ValueArgument;

public class ActualParameter extends VarOrParamValue {
	
	private final PureExp valueExp;
	
	public ActualParameter(FormalParameter formalParameter, ValueArgument valueArg) {
		this(formalParameter, valueArg.getValueExp());
	}

	public ActualParameter(FormalParameter formalParameter, PureExp valueExp) {
		super(formalParameter.argNr, formalParameter.name, formalParameter.fullType, formalParameter.isConst);
		
		this.valueExp = valueExp;
	}

	@Override
	boolean isDefault() {
		return false;
	}

	@Override
	boolean isInitialized() {
		throw new UnsupportedOperationException("Function arguments always have values, so this should never be called!");
	}

	@Override
	List<Stmt> getStatements() {
		return Collections.emptyList();
	}

	@Override
	PureExp getInitExp() {
		return valueExp;
	}

}
