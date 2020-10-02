package de.tud.se.c2abs.subexp;

import java.util.Collections;

import abs.frontend.ast.Block;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.ReturnStmt;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.TypesUtils;

public class DerefSubExp extends SubExp {
	
	private final ValueArgument pointerArg;

	public DerefSubExp(ValueArgument pointerArg) {
		super(TypesUtils.getLocationTypeUse(), DEREF, Collections.singletonList(pointerArg), null);
		
		this.pointerArg = pointerArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// return loc(ptrArg);
		final ReturnStmt returnStmt = new ReturnStmt();
		final FnApp locExp = new FnApp();
		locExp.setName(LOCATION_SELECTOR);
		locExp.addParamNoTransform(pointerArg.getInnerExp());
		returnStmt.setRetExp(locExp);
		block.addStmt(returnStmt);
	}

}
