package de.tud.se.c2abs.subexp.args;

import abs.frontend.ast.Guard;
import abs.frontend.ast.IntLiteral;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.utils.TypesUtils;

public class IntArgument extends Argument {
	
	private final String name;
	
	public IntArgument(String name, int i) {
		this(name, new IntLiteral(Integer.toString(i)));
	}
	
	public IntArgument(String name, PureExp pureExp) {
		super(pureExp);
		
		this.name = name;
	}

	@Override
	public ParamDecl getParamDecl() {
		final ParamDecl paramDecl = new ParamDecl();
		paramDecl.setAccess(TypesUtils.getIntTypeUse());
		paramDecl.setName(name);
		return paramDecl;
	}

	@Override
	public Guard getGuard() {
		return null;
	}

	@Override
	public VarDeclStmt getVarDeclStmt() {
		return null;
	}

	public VarUse getVarUse() {
		return new VarUse(name);
	}

	@Override
	public String getMethodNameSuffix() {
		return "";
	}
	
}
