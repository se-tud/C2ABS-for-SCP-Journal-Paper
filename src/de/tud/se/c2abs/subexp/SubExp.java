package de.tud.se.c2abs.subexp;

import java.util.ArrayList;
import java.util.List;

import abs.frontend.ast.TypeUse;
import abs.frontend.ast.AndGuard;
import abs.frontend.ast.AsyncCall;
import abs.frontend.ast.AwaitStmt;
import abs.frontend.ast.Block;
import abs.frontend.ast.Call;
import abs.frontend.ast.Guard;
import abs.frontend.ast.MethodImpl;
import abs.frontend.ast.MethodSig;
import abs.frontend.ast.Opt;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.subexp.args.Argument;
import de.tud.se.c2abs.subexp.args.SideEffect;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public abstract class SubExp implements Constants {

	private static String createMethodName(String methodPrefix, List<? extends Argument> arguments,
			List<PureExp> sideEffects) {
		final StringBuilder methodName = new StringBuilder(methodPrefix);
		for (Argument arg : arguments) {
			methodName.append(arg.getMethodNameSuffix());
		}
		if (sideEffects != null) {
			methodName.append('_').append(sideEffects.size());
		}
		return methodName.toString();
	}

	private static List<? extends Argument> createArgList(List<? extends Argument> arguments,
			List<PureExp> sideEffects) {
		if (sideEffects == null)
			return arguments;

		final List<Argument> result = new ArrayList<>(arguments);
		int i = 0;
		for (PureExp se : sideEffects) {
			result.add(new SideEffect(++i, se));
		}
		return result;
	}

	final String tmpVar;
	final TypeUse type;

	private final String methodName;
	private final List<? extends Argument> arguments;

	protected SubExp(TypeUse type, String methodPrefix, List<? extends Argument> arguments, List<PureExp> sideEffects) {
		this(Utils.generateTmpVarString(), type, createMethodName(methodPrefix, arguments, sideEffects),
				createArgList(arguments, sideEffects));
	}

	protected SubExp(TypeUse type) {
		this(Utils.generateTmpVarString(), type, null, null);
	}

	private SubExp(String tmpVar, TypeUse type, String methodName, List<? extends Argument> arguments) {
		this.tmpVar = tmpVar;
		this.type = type;
		this.methodName = methodName;
		this.arguments = arguments;
	}

	public String getTmpVar() {
		return tmpVar;
	}

	public VarDeclStmt getVarDeclStmt() {
		final VarDeclStmt result = new VarDeclStmt();
		result.setVarDecl(new VarDecl(tmpVar, TypesUtils.getFutureTypeUse(type), new Opt<>(getCall())));
		return result;
	}

	public Call getCall() {
		final Call call = new AsyncCall();
		call.setCallee(new VarUse("this"));
		call.setMethod(methodName);
		for (Argument arg : arguments) {
			call.addParamNoTransform(arg.getActualArgumentExp());
		}
		return call;
	}

	public String getMethodName() {
		return methodName;
	}

	private MethodSig createMethodSig() {
		final MethodSig methodSig = new MethodSig();
		methodSig.setReturnType(type.treeCopyNoTransform());
		methodSig.setName(methodName);
		for (Argument arg : arguments) {
			methodSig.addParamNoTransform(arg.getParamDecl());
		}
		return methodSig;
	}

	public MethodImpl createMethodImpl() {
		// create method signature
		final MethodImpl methodImpl = new MethodImpl();
		methodImpl.setMethodSig(createMethodSig());
		// create method block
		final Block block = new Block();
		Guard methodGuard = null;
		// await any future params
		for (Argument arg : arguments) {
			final Guard argGuard = arg.getGuard();
			if (argGuard != null) {
				if (methodGuard == null) {
					methodGuard = argGuard;
				} else {
					methodGuard = new AndGuard(methodGuard, argGuard);
				}
			}
		}
		if (methodGuard != null) {
			final AwaitStmt awaitStmt = new AwaitStmt();
			awaitStmt.setGuard(methodGuard);
			block.addStmt(awaitStmt);
		}
		// add variable declaration statements getting the values of the future
		// parameters
		for (Argument arg : arguments) {
			final VarDeclStmt varDeclStmt = arg.getVarDeclStmt();
			if (varDeclStmt != null) {
				block.addStmt(varDeclStmt);
			}
		}
		// add subclass specific statements
		populateMethodImplBlock(block);
		methodImpl.setBlock(block);
		return methodImpl;
	}

	abstract void populateMethodImplBlock(Block block);

}
