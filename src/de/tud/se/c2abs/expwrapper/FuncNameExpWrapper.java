package de.tud.se.c2abs.expwrapper;

import java.util.List;

import abs.frontend.ast.PureExp;
import de.tud.se.c2abs.absid.AbsFuncId;
import de.tud.se.c2abs.subexp.SubExp;

public class FuncNameExpWrapper extends ExpWrapper {
	
	private final AbsFuncId absFuncId;
	
	public FuncNameExpWrapper(AbsFuncId absFuncId) {
		super(false, null, null, null, null);
		this.absFuncId = absFuncId;
	}
	
	public AbsFuncId getAbsFuncId() {
		return absFuncId;
	}

	@Override
	public List<SubExp> getSubExpressions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<PureExp> getSideEffects() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PureExp getExp() {
		throw new UnsupportedOperationException();
	}

}
