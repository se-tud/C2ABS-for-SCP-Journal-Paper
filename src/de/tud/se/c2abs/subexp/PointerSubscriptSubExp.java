package de.tud.se.c2abs.subexp;

import java.util.Arrays;

import abs.frontend.ast.AddAddExp;
import abs.frontend.ast.Block;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.MultMultExp;
import abs.frontend.ast.ReturnStmt;
import de.tud.se.c2abs.subexp.args.IntArgument;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.TypesUtils;

public class PointerSubscriptSubExp extends SubExp {

	private final ValueArgument pointerArg;
	private final IntArgument sizeArg;
	private final ValueArgument indexArg;

	public PointerSubscriptSubExp(ValueArgument pointerArg, IntArgument sizeArg, ValueArgument indexArg) {
		super(TypesUtils.getLocationTypeUse(), POINTER_SUBSCRIPT, Arrays.asList(pointerArg, sizeArg, indexArg), null);

		this.pointerArg = pointerArg;
		this.sizeArg = sizeArg;
		this.indexArg = indexArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// return Loc(mem(loc(pointerArg)), offset(loc(pointerArg)) + (sizeArg * indexArg));
		final DataConstructorExp newLocExp = new DataConstructorExp();
		newLocExp.setConstructor(LOCATION_CONSTRUCTOR);
		final FnApp memExp = new FnApp();
		memExp.setName(MEMORY_SELECTOR);
		final FnApp locSelExp = new FnApp();
		locSelExp.setName(LOCATION_SELECTOR);
		locSelExp.addParamNoTransform(pointerArg.getInnerExp());
		memExp.addParamNoTransform(locSelExp);
		newLocExp.addParamNoTransform(memExp);
		final FnApp offsetExp = new FnApp();
		offsetExp.setName(OFFSET_SELECTOR);
		offsetExp.addParamNoTransform(locSelExp.treeCopyNoTransform());
		newLocExp.addParamNoTransform(new AddAddExp(offsetExp, new MultMultExp(sizeArg.getVarUse(), indexArg.getInnerExp())));
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(newLocExp);
		block.addStmt(returnStmt);
	}

}
