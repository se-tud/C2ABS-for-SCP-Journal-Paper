package de.tud.se.c2abs.subexp;

import java.util.Collections;

import abs.frontend.ast.Block;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.ReturnStmt;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.utils.TypesUtils;

public class ArrayDecaySubExp extends SubExp {

	private final LocationArgument arrayArg;

	public ArrayDecaySubExp(LocationArgument arrayArg) {
		super(TypesUtils.getValueTypeUse(), DECAY, Collections.singletonList(arrayArg), null);

		this.arrayArg = arrayArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// return PtrVal(Ptr(arrayArg));
		// the offset doesn't change, but the FullType of the surrounding ExpWrapper
		// will be the subscript type of the array
		final DataConstructorExp ptrExp = new DataConstructorExp();
		ptrExp.setConstructor(POINTER_CONSTRUCTOR);
		ptrExp.addParamNoTransform(arrayArg.getVarUse());
		final DataConstructorExp valExp = new DataConstructorExp();
		valExp.setConstructor(POINTER_VALUE_CONSTRUCTOR);
		valExp.addParam(ptrExp);
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(valExp);
		block.addStmt(returnStmt);
	}

}
