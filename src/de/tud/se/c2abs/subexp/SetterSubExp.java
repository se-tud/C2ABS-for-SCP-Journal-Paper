package de.tud.se.c2abs.subexp;

import java.util.Arrays;

import abs.frontend.ast.AsyncCall;
import abs.frontend.ast.Block;
import abs.frontend.ast.ExpressionStmt;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.GetExp;
import abs.frontend.ast.Opt;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class SetterSubExp extends SubExp implements Constants {

	public static SetterSubExp create(final LocationArgument locationArg, final ValueArgument valueArg) {
		if (valueArg.isFuture)
			return new SetterSubExp(SET_VALUE_METHOD, locationArg, valueArg);
		switch (valueArg.type) {
		case INT_VALUE:
			return new SetterSubExp(SET_INT_VALUE_METHOD, locationArg, valueArg);
		case POINTER_VALUE:
			return new SetterSubExp(SET_POINTER_VALUE_METHOD, locationArg, valueArg);
		default:
			throw new IllegalStateException("Type may not be null");
		}
	}
	
	private final LocationArgument locationArg;
	private final ValueArgument valueArg;

	private SetterSubExp(final String setterName, final LocationArgument locationArg, final ValueArgument valueArg) {
		super(TypesUtils.getUnitTypeUse(), setterName, Arrays.asList(locationArg, valueArg), null);

		this.locationArg = locationArg;
		this.valueArg = valueArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// Fut<Unit> se = mem(loc)!setVal(offset(loc), val);
		final AsyncCall asyncCall = new AsyncCall();
		final FnApp memExp = new FnApp();
		memExp.setName(MEMORY_SELECTOR);
		memExp.addParamNoTransform(locationArg.getVarUse());
		asyncCall.setCallee(memExp);
		asyncCall.setMethod(SET_VALUE_METHOD);
		final FnApp offsetExp = new FnApp();
		offsetExp.setName(OFFSET_SELECTOR);
		offsetExp.addParamNoTransform(locationArg.getVarUse());
		asyncCall.addParamNoTransform(offsetExp);
		asyncCall.addParamNoTransform(valueArg.getValueExp());
		final VarDeclStmt varDeclStmt = new VarDeclStmt();
		varDeclStmt.setVarDecl(new VarDecl(SIDE_EFFECT, TypesUtils.getFutureTypeUse(TypesUtils.getUnitTypeUse()),
				new Opt<>(asyncCall)));
		block.addStmt(varDeclStmt);
		// make blocking get
		final ExpressionStmt expressionStmt = new ExpressionStmt();
		expressionStmt.setExp(new GetExp(new VarUse(SIDE_EFFECT)));
		block.addStmt(expressionStmt);
	}
}
