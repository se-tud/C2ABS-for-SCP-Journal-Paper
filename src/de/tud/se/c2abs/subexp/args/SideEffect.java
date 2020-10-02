package de.tud.se.c2abs.subexp.args;

import abs.frontend.ast.ClaimGuard;
import abs.frontend.ast.Guard;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class SideEffect extends Argument implements Constants {
	
	private final String name;
	
	public SideEffect(int sideEffectNr, PureExp pureExp) {
		super(pureExp);
		
		name = SIDE_EFFECT_ + sideEffectNr;
	}

	@Override
	public ParamDecl getParamDecl() {
		final ParamDecl paramDecl = new ParamDecl();
		paramDecl.setAccess(TypesUtils.getFutureTypeUse(TypesUtils.getUnitTypeUse()));
		paramDecl.setName(name);
		return paramDecl;
	}

	@Override
	public Guard getGuard() {
		return new ClaimGuard(new VarUse(name));
	}

	@Override
	public VarDeclStmt getVarDeclStmt() {
		return null;
	}

	@Override
	public String getMethodNameSuffix() {
		return "";
	}

}
