package de.tud.se.c2abs.vars;

import abs.frontend.ast.FieldUse;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.IntLiteral;
import de.tud.se.c2abs.absid.AbsId;

public interface VarOrParamDeclaration extends AbsId {

	public FieldUse getFieldUse();
	
	public int getArgNr();

	public default FnApp nthExp() {
		final FnApp nth = new FnApp();
		nth.setName("nth");
		nth.addParamNoTransform(getFieldUse());
		nth.addParamNoTransform(new IntLiteral(Integer.toString(getArgNr())));
		return nth;
	}
}
