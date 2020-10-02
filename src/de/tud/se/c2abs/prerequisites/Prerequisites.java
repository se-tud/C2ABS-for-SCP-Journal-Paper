package de.tud.se.c2abs.prerequisites;

import abs.frontend.ast.AssignStmt;
import abs.frontend.ast.Block;
import abs.frontend.ast.ClassDecl;
import abs.frontend.ast.ConstructorArg;
import abs.frontend.ast.DataConstructor;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.DataTypeDecl;
import abs.frontend.ast.EqExp;
import abs.frontend.ast.ExpFunctionDef;
import abs.frontend.ast.FieldUse;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.IfExp;
import abs.frontend.ast.IntLiteral;
import abs.frontend.ast.InterfaceDecl;
import abs.frontend.ast.MethodImpl;
import abs.frontend.ast.MethodSig;
import abs.frontend.ast.ModuleDecl;
import abs.frontend.ast.Name;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.ParametricFunctionDecl;
import abs.frontend.ast.ReturnStmt;
import abs.frontend.ast.SubAddExp;
import abs.frontend.ast.TypeParameterDecl;
import abs.frontend.ast.TypeParameterUse;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class Prerequisites implements Constants {

	public static void addDeclsTo(ModuleDecl moduleDecl) {
		// data Val = IntVal(Int getInt) | PtrVal(Ptr getPtr);
		DataTypeDecl valDecl = new DataTypeDecl();
		valDecl.setName(VALUE_TYPE);
		DataConstructor intValConstructor = new DataConstructor();
		intValConstructor.setName(INT_VALUE_CONSTRUCTOR);
		ConstructorArg intValSelector = new ConstructorArg();
		intValSelector.setTypeUse(TypesUtils.getIntTypeUse());
		intValSelector.setSelectorName(new Name(INT_VALUE_SELECTOR));
		intValConstructor.addConstructorArg(intValSelector);
		valDecl.addDataConstructor(intValConstructor);
		DataConstructor ptrValConstructor = new DataConstructor();
		ptrValConstructor.setName(POINTER_VALUE_CONSTRUCTOR);
		ConstructorArg ptrValSelector = new ConstructorArg();
		ptrValSelector.setTypeUse(TypesUtils.getPointerTypeUse());
		ptrValSelector.setSelectorName(new Name(POINTER_VALUE_SELECTOR));
		ptrValConstructor.addConstructorArg(ptrValSelector);
		valDecl.addDataConstructor(ptrValConstructor);
		moduleDecl.addDecl(valDecl);

		// data Ptr = NullPtr | Ptr(Loc loc);
		DataTypeDecl ptrDecl = new DataTypeDecl();
		ptrDecl.setName(POINTER_TYPE);
		DataConstructor nullPtrConstructor = new DataConstructor();
		nullPtrConstructor.setName(NULL_POINTER_CONSTRUCTOR);
		ptrDecl.addDataConstructor(nullPtrConstructor);
		DataConstructor ptrConstructor = new DataConstructor();
		ptrConstructor.setName(POINTER_CONSTRUCTOR);
		ConstructorArg locSelector = new ConstructorArg();
		locSelector.setTypeUse(TypesUtils.getLocationTypeUse());
		locSelector.setSelectorName(new Name(LOCATION_SELECTOR));
		ptrConstructor.addConstructorArg(locSelector);
		ptrDecl.addDataConstructor(ptrConstructor);
		moduleDecl.addDecl(ptrDecl);

		// data Loc = Loc(Mem mem, Int offset);
		DataTypeDecl locDecl = new DataTypeDecl();
		locDecl.setName(LOCATION_TYPE);
		DataConstructor locConstructor = new DataConstructor();
		locConstructor.setName(LOCATION_CONSTRUCTOR);
		ConstructorArg memSelector = new ConstructorArg();
		memSelector.setTypeUse(TypesUtils.getMemoryTypeUse());
		memSelector.setSelectorName(new Name(MEMORY_SELECTOR));
		locConstructor.addConstructorArg(memSelector);
		ConstructorArg offsetSelector = new ConstructorArg();
		offsetSelector.setTypeUse(TypesUtils.getIntTypeUse());
		offsetSelector.setSelectorName(new Name(OFFSET_SELECTOR));
		locConstructor.addConstructorArg(offsetSelector);
		locDecl.addDataConstructor(locConstructor);
		moduleDecl.addDecl(locDecl);

		// interface Mem { Val getVal(Int offset); Unit setVal(Int offset, Val val); }
		InterfaceDecl memInterfaceDecl = new InterfaceDecl();
		memInterfaceDecl.setName(MEMORY_INTERFACE);
		MethodSig getValSig = new MethodSig();
		getValSig.setReturnType(TypesUtils.getValueTypeUse());
		getValSig.setName(GET_VALUE_METHOD);
		ParamDecl offsetParam = new ParamDecl();
		offsetParam.setAccess(TypesUtils.getIntTypeUse());
		offsetParam.setName(OFFSET_PARAM);
		getValSig.addParamNoTransform(offsetParam);
		memInterfaceDecl.addBody(getValSig);
		MethodSig setValSig = new MethodSig();
		setValSig.setReturnType(TypesUtils.getUnitTypeUse());
		setValSig.setName(SET_VALUE_METHOD);
		setValSig.addParamNoTransform(offsetParam.treeCopyNoTransform());
		ParamDecl valParam = new ParamDecl();
		valParam.setAccess(TypesUtils.getValueTypeUse());
		valParam.setName(NEW_VALUE_PARAM);
		setValSig.addParamNoTransform(valParam);
		memInterfaceDecl.addBody(setValSig);
		moduleDecl.addDecl(memInterfaceDecl);

		// def List<A> replace<A>(List<A> l, Int i, A a) =
		// if i == 0 then Cons(a, tail(l)) else Cons(head(l), replace(tail(l), i-1, a));
		TypeParameterUse aTypeUse = new TypeParameterUse();
		aTypeUse.setName("A");
		ParametricFunctionDecl replaceDecl = new ParametricFunctionDecl();
		replaceDecl.setTypeUse(TypesUtils.getListTypeUse(aTypeUse));
		replaceDecl.setName(REPLACE_FUNCTION);
		replaceDecl.addTypeParameter(new TypeParameterDecl("A"));
		ParamDecl listParam = new ParamDecl();
		listParam.setAccess(TypesUtils.getListTypeUse(aTypeUse));
		listParam.setName("l");
		replaceDecl.addParamNoTransform(listParam);
		ParamDecl intParam = new ParamDecl();
		intParam.setAccess(TypesUtils.getIntTypeUse());
		intParam.setName("i");
		replaceDecl.addParamNoTransform(intParam);
		ParamDecl aParam = new ParamDecl();
		aParam.setAccess(aTypeUse);
		aParam.setName("a");
		replaceDecl.addParamNoTransform(aParam);
		DataConstructorExp thenExp = new DataConstructorExp();
		thenExp.setConstructor("Cons");
		thenExp.addParamNoTransform(new VarUse("a"));
		FnApp tailLExp = new FnApp();
		tailLExp.setName("tail");
		tailLExp.addParamNoTransform(new VarUse("l"));
		thenExp.addParamNoTransform(tailLExp);
		DataConstructorExp elseExp = new DataConstructorExp();
		elseExp.setConstructor("Cons");
		FnApp headLExp = new FnApp();
		headLExp.setName("head");
		headLExp.addParamNoTransform(new VarUse("l"));
		elseExp.addParamNoTransform(headLExp);
		FnApp replaceExp = new FnApp();
		replaceExp.setName(REPLACE_FUNCTION);
		replaceExp.addParamNoTransform(tailLExp.treeCopyNoTransform());
		replaceExp.addParamNoTransform(new SubAddExp(new VarUse("i"), new IntLiteral("1")));
		replaceExp.addParamNoTransform(new VarUse("a"));
		elseExp.addParamNoTransform(replaceExp);
		replaceDecl.setFunctionDef(
				new ExpFunctionDef(new IfExp(new EqExp(new VarUse("i"), new IntLiteral("0")), thenExp, elseExp)));
		moduleDecl.addDecl(replaceDecl);

		// class Mem(List<Val> vals) implements Mem {
		// Val getVal(Int offset) { return nth(this.vals, offset); }
		// Unit setVal(Int offset, Val newVal) { this.vals = replace(this.vals, offset, newVal); }
		// }
		ClassDecl memClassDecl = new ClassDecl();
		memClassDecl.setName(MEMORY_CLASS);
		ParamDecl valuesParam = new ParamDecl();
		valuesParam.setAccess(TypesUtils.getListTypeUse(TypesUtils.getValueTypeUse()));
		valuesParam.setName(VALUES_PARAM);
		memClassDecl.addParamNoTransform(valuesParam);
		memClassDecl.addImplementedInterfaceUse(TypesUtils.getMemoryTypeUse());
		MethodImpl getValMethod = new MethodImpl();
		getValMethod.setMethodSig(getValSig.treeCopyNoTransform());
		Block getValBlock = new Block();
		ReturnStmt returnStmt = new ReturnStmt();
		FnApp nthExp = new FnApp();
		nthExp.setName("nth");
		nthExp.addParamNoTransform(new FieldUse(VALUES_PARAM));
		nthExp.addParamNoTransform(new VarUse(OFFSET_PARAM));
		returnStmt.setRetExp(nthExp);
		getValBlock.addStmt(returnStmt);
		getValMethod.setBlock(getValBlock);
		memClassDecl.addMethod(getValMethod);
		MethodImpl setValMethod = new MethodImpl();
		setValMethod.setMethodSig(setValSig.treeCopyNoTransform());
		Block setValBlock = new Block();
		AssignStmt assignStmt = new AssignStmt();
		assignStmt.setVar(new FieldUse(VALUES_PARAM));
		replaceExp = new FnApp();
		replaceExp.setName(REPLACE_FUNCTION);
		replaceExp.addParamNoTransform(new FieldUse(VALUES_PARAM));
		replaceExp.addParamNoTransform(new VarUse(OFFSET_PARAM));
		replaceExp.addParamNoTransform(new VarUse(NEW_VALUE_PARAM));
		assignStmt.setValue(replaceExp);
		setValBlock.addStmt(assignStmt);
		setValMethod.setBlock(setValBlock);
		memClassDecl.addMethod(setValMethod);
		moduleDecl.addDecl(memClassDecl);
	}
}
