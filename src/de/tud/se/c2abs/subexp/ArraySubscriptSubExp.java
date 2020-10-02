package de.tud.se.c2abs.subexp;

import java.util.Arrays;

import abs.frontend.ast.AddAddExp;
import abs.frontend.ast.Block;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.MultMultExp;
import abs.frontend.ast.ReturnStmt;
import de.tud.se.c2abs.subexp.args.IntArgument;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.TypesUtils;

public class ArraySubscriptSubExp extends SubExp {

	private final LocationArgument arrayArg;
	private final IntArgument sizeArg;
	private final ValueArgument indexArg;

	public ArraySubscriptSubExp(LocationArgument arrayArg, IntArgument sizeArg, ValueArgument indexArg) {
		super(TypesUtils.getLocationTypeUse(), SUBSCRIPT, Arrays.asList(arrayArg, sizeArg, indexArg), null);

		this.arrayArg = arrayArg;
		this.sizeArg = sizeArg;
		this.indexArg = indexArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// return Loc(mem(arrayArg), offset(arrayArg) + (sizeArg * indexArg));
		final DataConstructorExp newLocExp = new DataConstructorExp();
		newLocExp.setConstructor(LOCATION_CONSTRUCTOR);
		final FnApp memExp = new FnApp();
		memExp.setName(MEMORY_SELECTOR);
		memExp.addParamNoTransform(arrayArg.getVarUse());
		newLocExp.addParamNoTransform(memExp);
		final FnApp offsetExp = new FnApp();
		offsetExp.setName(OFFSET_SELECTOR);
		offsetExp.addParamNoTransform(arrayArg.getVarUse());
		newLocExp.addParamNoTransform(new AddAddExp(offsetExp, new MultMultExp(sizeArg.getVarUse(), indexArg.getInnerExp())));
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(newLocExp);
		block.addStmt(returnStmt);
	}

}
