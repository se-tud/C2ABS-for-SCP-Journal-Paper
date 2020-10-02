package de.tud.se.c2abs.subexp;

import java.util.Arrays;

import abs.frontend.ast.AddAddExp;
import abs.frontend.ast.Block;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.LetExp;
import abs.frontend.ast.MultMultExp;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.ReturnStmt;
import abs.frontend.ast.SubAddExp;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.subexp.args.IntArgument;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class PointerArithmeticSubExp extends SubExp implements Constants {

	private static String createMethodPrefix(boolean isPointerPlus) {
		return isPointerPlus ? POINTER_PLUS : POINTER_MINUS;
	}

	private final boolean isPointerPlus;
	private final ValueArgument pointerArg;
	private final IntArgument sizeArg;
	private final ValueArgument offsetArg;

	public PointerArithmeticSubExp(boolean isPointerPlus, ValueArgument pointerArg, IntArgument sizeArg,
			ValueArgument offsetArg) {

		super(TypesUtils.getValueTypeUse(), createMethodPrefix(isPointerPlus),
				Arrays.asList(pointerArg, sizeArg, offsetArg), null);

		this.isPointerPlus = isPointerPlus;
		this.pointerArg = pointerArg;
		this.sizeArg = sizeArg;
		this.offsetArg = offsetArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// return PtrVal(newPtr);
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(Utils.createValue(createResult(true), TypesUtils.getPointerTypeUse()));
		block.addStmt(returnStmt);
	}

	public PureExp createResult(boolean isFuture) {
		final String locVar = LOCATION_ARG;
		final ParamDecl locParamDecl = new ParamDecl();
		locParamDecl.setName(locVar);
		locParamDecl.setAccess(TypesUtils.getLocationTypeUse());
		final LetExp outerLetExp = new LetExp();
		LetExp innerLetExp = outerLetExp;
		outerLetExp.setVar(locParamDecl);
		if (isFuture) {
			// let Loc locArg = loc(ptrArg) in ...
			final FnApp locExp = new FnApp();
			locExp.setName(LOCATION_SELECTOR);
			locExp.addParamNoTransform(pointerArg.getInnerExp());
			innerLetExp.setVal(locExp);
		} else {
			// let Loc locArg = loc(actualArg1) in ...
			final FnApp locExp = new FnApp();
			locExp.setName(LOCATION_SELECTOR);
			locExp.addParamNoTransform(pointerArg.getActualArgumentExp());
			innerLetExp.setVal(locExp);
			// let Int sizeArg = actualArg2 in ...
			LetExp newInnerLetExp = new LetExp();
			innerLetExp.setExp(newInnerLetExp);
			innerLetExp = newInnerLetExp;
			innerLetExp.setVar(sizeArg.getParamDecl());
			innerLetExp.setVal(sizeArg.getActualArgumentExp());
			// let Int offsetArg = actualArg3 in ...
			newInnerLetExp = new LetExp();
			innerLetExp.setExp(newInnerLetExp);
			innerLetExp = newInnerLetExp;
			innerLetExp.setVar(offsetArg.getParamDecl());
			innerLetExp.setVal(offsetArg.getActualArgumentExp());
		}
		// newPtr:
		// Ptr(Loc(mem(loc(ptrArg)), offset(loc(ptrArg)) +/- (sizeArg * offsetArg)))
		final DataConstructorExp newPtrExp = new DataConstructorExp();
		newPtrExp.setConstructor(POINTER_CONSTRUCTOR);
		final DataConstructorExp newLocExp = new DataConstructorExp();
		newLocExp.setConstructor(LOCATION_CONSTRUCTOR);
		final FnApp memExp = new FnApp();
		memExp.setName(MEMORY_SELECTOR);
		memExp.addParamNoTransform(new VarUse(locVar));
		newLocExp.addParamNoTransform(memExp);
		final FnApp offsetExp = new FnApp();
		offsetExp.setName(OFFSET_SELECTOR);
		offsetExp.addParamNoTransform(new VarUse(locVar));
		final MultMultExp multExp = new MultMultExp(sizeArg.getVarUse(), offsetArg.getInnerExp());
		newLocExp.addParamNoTransform(
				isPointerPlus ? new AddAddExp(offsetExp, multExp) : new SubAddExp(offsetExp, multExp));
		newPtrExp.addParamNoTransform(newLocExp);
		innerLetExp.setExp(newPtrExp);
		return outerLetExp;
	}
}
