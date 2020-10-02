package de.tud.se.c2abs.subexp.args;

import abs.frontend.ast.Guard;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarDeclStmt;

public abstract class Argument {
	
	private final PureExp pureExp;
	
	Argument(PureExp pureExp) {
		this.pureExp = pureExp;
	}
	
	public PureExp getActualArgumentExp() {
		return pureExp.treeCopyNoTransform();
	}
	
	public abstract ParamDecl getParamDecl();
	public abstract Guard getGuard();
	public abstract VarDeclStmt getVarDeclStmt();
	public abstract String getMethodNameSuffix(); 
}