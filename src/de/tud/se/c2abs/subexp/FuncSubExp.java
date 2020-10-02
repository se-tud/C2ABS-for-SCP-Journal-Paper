package de.tud.se.c2abs.subexp;

import java.util.ArrayList;
import java.util.List;

import abs.frontend.ast.AsyncCall;
import abs.frontend.ast.Block;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.ExpressionStmt;
import abs.frontend.ast.FieldUse;
import abs.frontend.ast.GetExp;
import abs.frontend.ast.InterfaceTypeUse;
import abs.frontend.ast.NewExp;
import abs.frontend.ast.Opt;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.ReturnStmt;
import abs.frontend.ast.TypeUse;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.absid.AbsFuncId;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;
import de.tud.se.c2abs.vars.ActualParameter;
import de.tud.se.c2abs.vars.FormalParameter;
import de.tud.se.c2abs.vars.VarOrParamValue;

public class FuncSubExp extends SubExp implements Constants {

	private static List<ActualParameter> createActualParameters(List<FormalParameter> formalParameters,
			List<ValueArgument> arguments) {

		final int size = arguments.size();
		if (formalParameters.size() != size)
			throw new IllegalArgumentException("Formal parameters and actual arguments are different sizes!");

		final List<ActualParameter> result = new ArrayList<>(size);
		for (int i = 0; i < size; ++i) {
			result.add(new ActualParameter(formalParameters.get(i), arguments.get(i)));
		}
		return result;
	}

	private final TypeUse returnType;
	private final String className;
	private final InterfaceTypeUse interfaceTypeUse;
	private final List<ActualParameter> actualParameters;

	public FuncSubExp(AbsFuncId absFuncId, List<ValueArgument> arguments, List<PureExp> sideEffects) {
		this(TypesUtils.returnTypeFor(absFuncId.getReturnType()), absFuncId.getCallMethodName(), arguments, sideEffects,
				absFuncId.getClassName(), absFuncId.getInterfaceTypeUse(),
				createActualParameters(absFuncId.getFormalParameters(), arguments));
	}

	private FuncSubExp(TypeUse returnType, String callMethodName, List<ValueArgument> arguments,
			List<PureExp> sideEffects, String className, InterfaceTypeUse interfaceTypeUse,
			List<ActualParameter> actualParameters) {

		super(returnType, callMethodName, arguments, sideEffects);

		this.returnType = returnType;
		this.className = className;
		this.interfaceTypeUse = interfaceTypeUse;
		this.actualParameters = actualParameters;
	}

	@Override
	public void populateMethodImplBlock(Block callMethodBlock) {
		// initialize memory and location list for params
		final DataConstructorExp nil = new DataConstructorExp();
		nil.setConstructor("Nil");
		final VarDeclStmt declareParams = new VarDeclStmt();
		declareParams.setVarDecl(new VarDecl(PARAMS, TypesUtils.getListTypeUse(TypesUtils.getLocationTypeUse()), new Opt<>(nil)));
		callMethodBlock.addStmt(declareParams);
		VarOrParamValue.initializeLocationList(new VarUse(PARAMS), actualParameters, callMethodBlock);
		// I_f tmp_1 = new C_f(global, params);
		final String tmpVar1 = Utils.generateTmpVarString();
		final NewExp newExp = new NewExp();
		newExp.setClassName(className);
		newExp.addParamNoTransform(new FieldUse(GLOBAL));
		newExp.addParamNoTransform(new VarUse(PARAMS));
		final VarDeclStmt varDeclStmt1 = new VarDeclStmt();
		varDeclStmt1.setVarDecl(new VarDecl(tmpVar1, interfaceTypeUse, new Opt<>(newExp)));
		callMethodBlock.addStmt(varDeclStmt1);
		// Fut<T> tmp_2 = tmp_1!call();
		final AsyncCall asyncCall = new AsyncCall();
		asyncCall.setCallee(new VarUse(tmpVar1));
		asyncCall.setMethod("call");
		final String tmpVar2 = Utils.generateTmpVarString();
		final VarDeclStmt varDeclStmt2 = new VarDeclStmt();
		varDeclStmt2.setVarDecl(new VarDecl(tmpVar2, TypesUtils.getFutureTypeUse(returnType), new Opt<>(asyncCall)));
		callMethodBlock.addStmt(varDeclStmt2);
		if (TypesUtils.isUnitTypeUse(returnType)) {
			// tmp_2.get;
			final ExpressionStmt expressionStmt = new ExpressionStmt();
			expressionStmt.setExp(new GetExp(new VarUse(tmpVar2)));
			callMethodBlock.addStmt(expressionStmt);
		} else {
			// T tmp_3 = tmp_2.get;
			final String tmpVar3 = Utils.generateTmpVarString();
			final VarDeclStmt varDeclStmt3 = new VarDeclStmt();
			varDeclStmt3.setVarDecl(
					new VarDecl(tmpVar3, returnType.treeCopyNoTransform(), new Opt<>(new GetExp(new VarUse(tmpVar2)))));
			callMethodBlock.addStmt(varDeclStmt3);
			// return tmp_3;
			final ReturnStmt returnStmt = new ReturnStmt();
			returnStmt.setRetExp(new VarUse(tmpVar3));
			callMethodBlock.addStmt(returnStmt);
		}
	}
}
