package de.tud.se.c2abs.vars;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTName;

import abs.frontend.ast.AssignStmt;
import abs.frontend.ast.Block;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.IntLiteral;
import abs.frontend.ast.NewExp;
import abs.frontend.ast.Opt;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.Stmt;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarOrFieldUse;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.types.FullType;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public abstract class VarOrParamValue implements Constants {

	public static void initializeLocationList(VarOrFieldUse locationList, List<? extends VarOrParamValue> vars, Block block) {
		// initialize memory for vars +
		// construct location list:
		// locList = appendright(locList, Loc(mem_1, 0));
		// ...
		// locList = appendright(locList, Loc(mem_n, 0));
		for (VarOrParamValue var : vars) {
			// Mem mem_j = new Mem(copy(val_j, 1)); // or IntValue(arg_j); or PtrValue(arg_j);
			final FnApp initialValues = new FnApp();
			initialValues.setName("copy");
			initialValues.addParamNoTransform(var.getInitExp());
			initialValues.addParamNoTransform(var.fullType.getSize());
			final NewExp newMemExp = new NewExp();
			newMemExp.setClassName(MEMORY_CLASS);
			newMemExp.addParamNoTransform(initialValues);
			final String memVarName = MEMORY_SELECTOR + "_" + var.argNr;
			final VarDeclStmt memVarDeclStmt = new VarDeclStmt();
			memVarDeclStmt.setVarDecl(new VarDecl(memVarName, TypesUtils.getMemoryTypeUse(), new Opt<>(newMemExp)));
			block.addStmt(memVarDeclStmt);
			// extend location list: locList = appendright(locList, Loc(mem_j, 0));
			final AssignStmt assignStmt = new AssignStmt();
			assignStmt.setVar(locationList.treeCopyNoTransform());
			final FnApp appendrightExp = new FnApp();
			appendrightExp.setName("appendright");
			appendrightExp.addParam(locationList.treeCopyNoTransform());
			final DataConstructorExp locExp = new DataConstructorExp();
			locExp.setConstructor(LOCATION_CONSTRUCTOR);
			locExp.addParamNoTransform(new VarUse(memVarName));
			locExp.addParamNoTransform(new IntLiteral("0"));
			appendrightExp.addParam(locExp);
			assignStmt.setValue(appendrightExp);
			block.addStmt(assignStmt);
		}
	}

	public final int argNr;
	public final IASTName name;
	public final FullType fullType;
	public final boolean isConst;

	VarOrParamValue(final int argNr, final IASTName name, final FullType fullType, final boolean isConst) {
		this.argNr = argNr;
		this.name = name;
		this.fullType = fullType;
		this.isConst = isConst;
	}

	abstract boolean isDefault();

	abstract boolean isInitialized();

	abstract List<Stmt> getStatements();

	abstract PureExp getInitExp();
}
