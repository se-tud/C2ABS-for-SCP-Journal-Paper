package de.tud.se.c2abs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;

import abs.frontend.ast.Access;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.DataTypeUse;
import abs.frontend.ast.EqExp;
import abs.frontend.ast.FieldDecl;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.IfExp;
import abs.frontend.ast.IntLiteral;
import abs.frontend.ast.NotEqExp;
import abs.frontend.ast.NullExp;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.expwrapper.ExpWrapper;
import de.tud.se.c2abs.expwrapper.LvalueExpWrapper;
import de.tud.se.c2abs.subexp.IntArithmeticSubExp;
import de.tud.se.c2abs.subexp.PointerArithmeticSubExp;
import de.tud.se.c2abs.subexp.SetterSubExp;
import de.tud.se.c2abs.subexp.SubExp;
import de.tud.se.c2abs.subexp.args.IntArgument;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;
import de.tud.se.c2abs.types.PointerType;

public class Utils implements Constants {

	private static int globalTmpVarCounter = 0;

	public static void reset() {
		globalTmpVarCounter = 0;
	}

	public static String generateTmpVarString() {
		++globalTmpVarCounter;
		return TMP_ + globalTmpVarCounter;
	}

	public static ParamDecl getGlobalVarsParamDecl() {
		final ParamDecl globalVarsParamDecl = new ParamDecl();
		globalVarsParamDecl.setName(GLOBAL);
		globalVarsParamDecl.setAccess(TypesUtils.getListTypeUse(TypesUtils.getLocationTypeUse()));
		return globalVarsParamDecl;
	}

	public static ParamDecl getParamsParamDecl() {
		final ParamDecl paramsParamDecl = new ParamDecl();
		paramsParamDecl.setName(PARAMS);
		paramsParamDecl.setAccess(TypesUtils.getListTypeUse(TypesUtils.getLocationTypeUse()));
		return paramsParamDecl;
	}

	public static FieldDecl getLocalVarsFieldDecl() {
		final FieldDecl localVarsFieldDecl = new FieldDecl();
		localVarsFieldDecl.setName(LOCAL);
		localVarsFieldDecl.setAccess(TypesUtils.getListTypeUse(TypesUtils.getLocationTypeUse()));
		final DataConstructorExp nil = new DataConstructorExp();
		nil.setConstructor("Nil");
		localVarsFieldDecl.setInitExp(nil);
		return localVarsFieldDecl;
	}

	public static DataTypeUse getBoolType() {
		final DataTypeUse dataTypeUse = new DataTypeUse();
		dataTypeUse.setName("Bool");
		return dataTypeUse;
	}

	public static PureExp getFalseExp() {
		final DataConstructorExp dataConstructorExp = new DataConstructorExp();
		dataConstructorExp.setConstructor("False");
		return dataConstructorExp;
	}

	public static PureExp getTrueExp() {
		final DataConstructorExp dataConstructorExp = new DataConstructorExp();
		dataConstructorExp.setConstructor("True");
		return dataConstructorExp;
	}

	public static PureExp getNullExp() {
		return new NullExp();
	}

	public static PureExp convertIntToBool(PureExp exp) {
		return new NotEqExp(exp, new IntLiteral("0"));
	}
	
	public static PureExp convertPointerToBool(PureExp exp) {
		return new NotEqExp(exp, getNullExp());
	}

	public static PureExp convertBoolToInt(PureExp exp) {
		return new IfExp(exp, new IntLiteral("1"), new IntLiteral("0"));
	}

	public static ExpWrapper modifyField(boolean prefix, boolean increase, LvalueExpWrapper lvalueExpWrapper) {
		// lvalue = lvalueExp +/- 1; return lvalueExp or lvalueExp +/- 1

		final List<SubExp> modifiedSubExpressions = new ArrayList<>(lvalueExpWrapper.getSubExpressions());

		final boolean isPointerArithmetic = lvalueExpWrapper.getFullType() instanceof PointerType;
		final SubExp subExp;
		if (isPointerArithmetic) {
			final ValueArgument pointerArg = ValueArgument.createPointerValueArg(true, POINTER_ARG,
					lvalueExpWrapper.getExp());
			final IntArgument sizeArg = new IntArgument(SIZE_ARG, lvalueExpWrapper.getFullType().getInnerSize());
			final ValueArgument offsetArg = ValueArgument.createIntValueArg(false, OFFSET, new IntLiteral("1"));
			subExp = new PointerArithmeticSubExp(increase, pointerArg, sizeArg, offsetArg);
		} else {
			final String opName = increase ? PLUS : MINUS;
			final ValueArgument arg1 = ValueArgument.createIntValueArg(true, ARG1, lvalueExpWrapper.getExp());
			final ValueArgument arg2 = ValueArgument.createIntValueArg(false, ARG2, new IntLiteral("1"));
			subExp = new IntArithmeticSubExp(opName, arg1, arg2);
		}
		modifiedSubExpressions.add(subExp);
		final String tmpVar2 = subExp.getTmpVar();
		final ValueArgument valArg;
		if (isPointerArithmetic) {
			valArg = ValueArgument.createPointerValueArg(true, VALUE, new VarUse(tmpVar2));
		} else {
			valArg = ValueArgument.createIntValueArg(true, VALUE, new VarUse(tmpVar2));
		}
		final LocationArgument locArg = new LocationArgument(lvalueExpWrapper.lvalue);
		final SetterSubExp setterSubExp = SetterSubExp.create(locArg, valArg);
		modifiedSubExpressions.add(setterSubExp);

		final List<PureExp> modifiedSideEffects = new ArrayList<>(lvalueExpWrapper.getSideEffects());
		final String tmpVar3 = setterSubExp.getTmpVar();
		modifiedSideEffects.add(new VarUse(tmpVar3));

		final PureExp exp = prefix ? new VarUse(tmpVar2) : lvalueExpWrapper.getExp();
		return new ExpWrapper(true, modifiedSubExpressions, exp, lvalueExpWrapper.getFullType(), modifiedSideEffects);
	}

	public static PureExp negateInt(PureExp exp) {
		return new IfExp(new EqExp(exp, new IntLiteral("0")), new IntLiteral("1"), new IntLiteral("0"));
	}

	public static boolean isConstant(ExpWrapper expWrapper) {
		if (expWrapper.isFuture) {
			return false;
		}
		return expWrapper.getSideEffects().isEmpty() && expWrapper.getSubExpressions().isEmpty();
	}

	public static String getOpName(int operator) {
		switch (operator) {
		case IASTBinaryExpression.op_minus:
			return MINUS;
		case IASTBinaryExpression.op_multiply:
			return MULT;
		case IASTBinaryExpression.op_plus:
			return PLUS;
		case IASTBinaryExpression.op_equals:
			return EQUALS;
		case IASTBinaryExpression.op_greaterEqual:
			return GREATER_EQUALS;
		case IASTBinaryExpression.op_greaterThan:
			return GREATER_THAN;
		case IASTBinaryExpression.op_lessEqual:
			return LESS_EQUALS;
		case IASTBinaryExpression.op_lessThan:
			return LESS_THAN;
		case IASTBinaryExpression.op_notequals:
			return NOT_EQUALS;
		default:
			throw new UnsupportedOperationException("Do not recognize binary expression op: " + operator);
		}
	}

	public static String createClassName(String functionName) {
		return FUNCTION_ + functionName;
	}

	public static String createInterfaceName(String functionName) {
		return FUNCTION_ + functionName;
	}

	public static String createLocalVarsName(String functionName) {
		return LOCAL_VARS_ + functionName;
	}

	public static String createLocalVarsInterfaceName(String functionName) {
		return LOCAL_VARS_ + functionName;
	}

	public static PureExp createValue(PureExp innerExp, Access innerType) {
		DataConstructorExp constructorExpr = new DataConstructorExp();
		if (TypesUtils.isIntTypeUse(innerType)) {
			constructorExpr.setConstructor(INT_VALUE_CONSTRUCTOR);
		} else if (TypesUtils.isPointerTypeUse(innerType)) {
			constructorExpr.setConstructor(POINTER_VALUE_CONSTRUCTOR);
		} else {
			throw new UnsupportedOperationException(
					"Cannot create value from inner type that is not a pointer or int!");
		}
		constructorExpr.addParamNoTransform(innerExp);
		return constructorExpr;
	}

	public static PureExp getInnerValue(VarUse value, Access innerType) {
		FnApp selectorExpr = new FnApp();
		if (TypesUtils.isIntTypeUse(innerType)) {
			selectorExpr.setName(INT_VALUE_SELECTOR);
		} else if (TypesUtils.isPointerTypeUse(innerType)) {
			selectorExpr.setName(POINTER_VALUE_SELECTOR);
		} else {
			throw new UnsupportedOperationException(
					"Cannot get inner type for any value that does not wrap a pointer or int!");
		}
		selectorExpr.addParamNoTransform(value);
		return selectorExpr;
	}
}
