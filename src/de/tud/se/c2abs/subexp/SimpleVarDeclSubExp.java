package de.tud.se.c2abs.subexp;

import abs.frontend.ast.TypeUse;
import abs.frontend.ast.Block;
import abs.frontend.ast.Exp;
import abs.frontend.ast.MethodImpl;
import abs.frontend.ast.Opt;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;

public class SimpleVarDeclSubExp extends SubExp {
	
	private final Exp exp;

	public SimpleVarDeclSubExp(TypeUse type, Exp exp) {
		super(type);
		
		this.exp = exp;
	}

	@Override
	public VarDeclStmt getVarDeclStmt() {
		final VarDeclStmt result = new VarDeclStmt();
		result.setVarDecl(new VarDecl(tmpVar, type, new Opt<>(exp)));
		return result;
	}

	@Override
	public MethodImpl createMethodImpl() {
		throw new UnsupportedOperationException();
	}

	@Override
	void populateMethodImplBlock(Block block) {
		throw new UnsupportedOperationException();
	}
}
