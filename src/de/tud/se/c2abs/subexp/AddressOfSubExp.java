package de.tud.se.c2abs.subexp;

import java.util.Collections;

import abs.frontend.ast.Block;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.ReturnStmt;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.utils.TypesUtils;

public class AddressOfSubExp extends SubExp {
	
	private final LocationArgument locationArg;

	public AddressOfSubExp(LocationArgument locationArg) {
		super(TypesUtils.getValueTypeUse(), ADDRESS_OF, Collections.singletonList(locationArg), null);

		this.locationArg = locationArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// ptr: Ptr(locArg)
		final DataConstructorExp ptrExp = new DataConstructorExp();
		ptrExp.setConstructor(POINTER_CONSTRUCTOR);
		ptrExp.addParamNoTransform(locationArg.getVarUse());
		// return PtrVal(ptr);
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(Utils.createValue(ptrExp, TypesUtils.getPointerTypeUse()));
		block.addStmt(returnStmt);
	}

}
