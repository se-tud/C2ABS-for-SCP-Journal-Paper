package de.tud.se.c2abs.subexp;

import java.util.Collections;

import abs.frontend.ast.AsyncCall;
import abs.frontend.ast.Block;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.GetExp;
import abs.frontend.ast.Opt;
import abs.frontend.ast.ReturnStmt;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class GetterSubExp extends SubExp implements Constants {
	
	private final LocationArgument locationArg;

	public GetterSubExp(final LocationArgument locationArg) {
		super(TypesUtils.getValueTypeUse(), GET_VALUE_METHOD, Collections.singletonList(locationArg), null);
		
		this.locationArg = locationArg;
	}

	@Override
	void populateMethodImplBlock(Block block) {
		// Fut<Val> fv = mem(loc)!getVal(offset(loc));
		final AsyncCall asyncCall = new AsyncCall();
		final FnApp memExp = new FnApp();
		memExp.setName(MEMORY_SELECTOR);
		memExp.addParamNoTransform(locationArg.getVarUse());
		asyncCall.setCallee(memExp);
		asyncCall.setMethod(GET_VALUE_METHOD);
		final FnApp offsetExp = new FnApp();
		offsetExp.setName(OFFSET_SELECTOR);
		offsetExp.addParamNoTransform(locationArg.getVarUse());
		asyncCall.addParamNoTransform(offsetExp);
		final VarDeclStmt varDeclStmt = new VarDeclStmt();
		varDeclStmt.setVarDecl(
				new VarDecl(FUTURE_RESULT, TypesUtils.getFutureTypeUse(TypesUtils.getValueTypeUse()), new Opt<>(asyncCall)));
		block.addStmt(varDeclStmt);
		// make blocking get
		final VarDeclStmt getStmt = new VarDeclStmt();
		getStmt.setVarDecl(new VarDecl(RESULT, TypesUtils.getValueTypeUse(), new Opt<>(new GetExp(new VarUse(FUTURE_RESULT)))));
		block.addStmt(getStmt);
		// return value
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(new VarUse(RESULT));
		block.addStmt(returnStmt);
	}
}
