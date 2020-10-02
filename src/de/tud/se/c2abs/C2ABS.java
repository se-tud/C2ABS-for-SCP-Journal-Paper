package de.tud.se.c2abs;

import org.eclipse.core.runtime.IProgressMonitor;

import abs.backend.prettyprint.DefaultABSFormatter;
import abs.frontend.ast.*;
import abs.frontend.typechecker.InterfaceType;
import de.tud.se.c2abs.absid.AbsFuncId;
import de.tud.se.c2abs.absid.AbsId;
import de.tud.se.c2abs.expwrapper.ExpWrapper;
import de.tud.se.c2abs.expwrapper.FuncNameExpWrapper;
import de.tud.se.c2abs.expwrapper.Lvalue;
import de.tud.se.c2abs.expwrapper.LvalueExpWrapper;
import de.tud.se.c2abs.prerequisites.Prerequisites;
import de.tud.se.c2abs.subexp.FuncSubExp;
import de.tud.se.c2abs.subexp.IntArithmeticSubExp;
import de.tud.se.c2abs.subexp.PointerArithmeticSubExp;
import de.tud.se.c2abs.subexp.RelationSubExp;
import de.tud.se.c2abs.subexp.SetterSubExp;
import de.tud.se.c2abs.subexp.SimpleVarDeclSubExp;
import de.tud.se.c2abs.subexp.SubExp;
import de.tud.se.c2abs.subexp.args.IntArgument;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.types.ArrayType;
import de.tud.se.c2abs.types.SimpleType;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;
import de.tud.se.c2abs.vars.FormalParameter;
import de.tud.se.c2abs.vars.GlobalVariable;
import de.tud.se.c2abs.vars.LocalVariable;
import de.tud.se.c2abs.vars.VarOrParamDeclaration;
import de.tud.se.c2abs.vars.VarOrParamValue;
import de.tud.se.c2abs.types.FullType;
import de.tud.se.c2abs.types.PointerType;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;

/**
 * @author wasser
 */
public class C2ABS extends ASTVisitor implements Constants {
	
	private static final String MODULE_NAME = "TestModule";

	private static final String METHOD_RESULT_VAR_NAME = "result";
	private static final String RETURN_FLAG = "returnFlag";
	private static final String BREAK_FLAG = "breakFlag";
	private static final String CONTINUE_FLAG = "continueFlag";

	private static final String FAILED_TO_PARSE = "Failed to parse!";

	private static final int INITIAL_PROBLEM_SIZE = 4;

	{
		shouldVisitNames = true;
		shouldVisitDeclarations = true;
		shouldVisitInitializers = true;
		shouldVisitParameterDeclarations = true;
		shouldVisitDeclarators = true;
		shouldVisitDeclSpecifiers = true;
		shouldVisitDesignators = true;
		shouldVisitExpressions = true;
		shouldVisitStatements = true;
		shouldVisitTypeIds = true;
		shouldVisitEnumerators = true;
		shouldVisitProblems = true;
	}

	private Logger logger = Logger.getLogger(this.getClass());
	private String failureMessage;

	Model absModel;
	ModuleDecl moduleDecl;

	final Map<String, MethodImpl> methodsCalled = new HashMap<>();

	final Map<String, InterfaceDecl> classNameToInterface = new HashMap<>();

	InterfaceDecl currentInterfaceDecl;
	ClassDecl currentClassDecl;

	InterfaceDecl mainInterfaceDecl;
	ClassDecl mainClassDecl;

	MethodSig methodSig;

	boolean methodNeedsReturn;
	FullType returnType;

	// to help optimize whether or not we need to declare these flags in the method
	// body
	boolean methodContainsReturn;
	boolean methodContainsBreak;
	boolean methodContainsContinue;

	final Stack<Map<IBinding, AbsId>> cIdToAbsId = new Stack<>();

	int globalAnonStructCounter = 0;
	final Map<IBinding, FullType> cTypeToAbsType = new HashMap<>();
	final Stack<FullType> typeStack = new Stack<>();

	final Stack<Stack<StmtWrapper>> statementStack = new Stack<>();

	final Stack<ExpWrapper> expressionStack = new Stack<>();

	final List<GlobalVariable> globalVars = new ArrayList<>();
	final List<LocalVariable> localVars = new ArrayList<>();
	int numberOfParams;
	final List<FormalParameter> formalParameters = new ArrayList<>();

	IASTTranslationUnit tu;
	IProgressMonitor monitor;
	IASTProblem[] astProblems = new IASTProblem[INITIAL_PROBLEM_SIZE];

	// placeholder for the innermost declarator, so the non-array (grand)parent can
	// access it after dealing with array sizes, etc.
	IASTDeclarator innermostDeclarator;

	public C2ABS(IASTTranslationUnit tu, IProgressMonitor monitor) {
		this.tu = tu;
		this.monitor = monitor;
	}

	private int abortWithMessage(String failureMessage) {
		this.failureMessage = failureMessage;
		logger.error(failureMessage);
		return PROCESS_ABORT;
	}

	@Override
	public int visit(IASTProblem problem) {
		return abortWithMessage("Problem parsing C code: " + problem.getMessage());
	}

	@Override
	public int visit(IASTDeclaration declaration) {
		if (declaration instanceof IASTProblemDeclaration) {
			return abortWithMessage("Problem parsing C code, expected declaration: "
					+ ((IASTProblemDeclaration) declaration).getProblem().getMessage());
		} else if (declaration instanceof IASTFunctionDefinition) {
			// this is dealt with in the leave method

		} else if (declaration instanceof IASTSimpleDeclaration) {
			// this is dealt with in the leave method
		} else {
			return abortWithMessage("Unknown declaration type: " + declaration.getClass());
		}

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(IASTDeclaration declaration) {
		// pop the type for this declaration off the stack
		typeStack.pop();

		if (declaration instanceof IASTFunctionDefinition) {
			// initialize method block modeling function call
			final Block block = new Block();

			// initialize local vars in method block
			VarOrParamValue.initializeLocationList(new FieldUse(LOCAL), localVars, block);

			if (methodContainsReturn) {
				final VarDecl var = new VarDecl(RETURN_FLAG, Utils.getBoolType(), new Opt<Exp>(Utils.getFalseExp()));
				final VarDeclStmt initFlag = new VarDeclStmt();
				initFlag.setVarDecl(var);
				block.addStmt(initFlag);
			}

			if (methodNeedsReturn) {
				final PureExp witness = returnType.getWitness(false);
				final Opt<Exp> opt = witness == null ? new Opt<>() : new Opt<>(witness);
				final VarDecl var = new VarDecl(METHOD_RESULT_VAR_NAME, TypesUtils.returnTypeFor(returnType), opt);
				final VarDeclStmt declareResult = new VarDeclStmt();
				declareResult.setVarDecl(var);
				block.addStmt(declareResult);
			}

			if (methodContainsBreak) {
				final VarDecl var = new VarDecl(BREAK_FLAG, Utils.getBoolType(), new Opt<Exp>(Utils.getFalseExp()));
				final VarDeclStmt initFlag = new VarDeclStmt();
				initFlag.setVarDecl(var);
				block.addStmt(initFlag);
			}

			if (methodContainsContinue) {
				final VarDecl var = new VarDecl(CONTINUE_FLAG, Utils.getBoolType(), new Opt<Exp>(Utils.getFalseExp()));
				final VarDeclStmt initFlag = new VarDeclStmt();
				initFlag.setVarDecl(var);
				block.addStmt(initFlag);
			}

			populateBlockFromStatementStack(block);

			if (methodNeedsReturn) {
				if (!methodContainsReturn) {
					return abortWithMessage("Method with non-void return type does not contain a return statement");
				}
				final ReturnStmt returnResult = new ReturnStmt();
				returnResult.setRetExp(new VarUse(METHOD_RESULT_VAR_NAME));
				block.addStmt(returnResult);
			}

			// add method implementation for function
			final MethodImpl methodImpl = new MethodImpl();
			methodImpl.setMethodSig(methodSig.treeCopyNoTransform());
			methodImpl.setBlock(block);
			currentClassDecl.addMethod(methodImpl);
			// add called helper methods to class
			for (final MethodImpl helper : methodsCalled.values()) {
				currentClassDecl.addMethod(helper.treeCopyNoTransform());
			}
			// add class to module
			moduleDecl.addDecl(currentClassDecl);

			// remove variable scope
			cIdToAbsId.pop();
		} else if (declaration instanceof IASTSimpleDeclaration) {
			// dealt with in leave(IASTDeclarator, IASTNode)
		}

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(IASTParameterDeclaration declaration) {
		typeStack.pop();

		return PROCESS_CONTINUE;
	}

	@Override
	public int visit(IASTDeclarator declarator) {
		if (declarator instanceof IASTFieldDeclarator) {
			return abortWithMessage("Bit field types are currently not allowed");
		}

		if (declarator instanceof ICASTKnRFunctionDeclarator) {
			return abortWithMessage("K&R style function declarations are currently not allowed");
		}

		if (declarator instanceof IASTStandardFunctionDeclarator) {
			final IASTStandardFunctionDeclarator functionDeclarator = (IASTStandardFunctionDeclarator) declarator;
			numberOfParams = functionDeclarator.getParameters().length;
			formalParameters.clear();
		}

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(IASTDeclarator declarator) {
		if (declarator instanceof IASTFieldDeclarator) {
			return abortWithMessage("Bit field types are currently not allowed");
		}

		if (declarator instanceof ICASTKnRFunctionDeclarator) {
			return abortWithMessage("K&R style function declarations are currently not allowed");
		}

		if (declarator.getNestedDeclarator() == null) {
			// this is the innermost declarator
			innermostDeclarator = declarator;
		}

		if (!(declarator.getParent() instanceof IASTArrayDeclarator)) {
			return leave(innermostDeclarator, declarator.getParent());
		}

		return PROCESS_CONTINUE;
	}

	private FullType convertType(FullType result, IASTDeclarator declarator) {
		if (declarator.getParent() instanceof IASTArrayDeclarator) {
			final IASTArrayDeclarator parent = (IASTArrayDeclarator) declarator.getParent();
			result = convertType(result, parent);
		}
		if (declarator.getPointerOperators() != null) {
			for (IASTPointerOperator pointerOp : declarator.getPointerOperators()) {
				IASTPointer pointer = (IASTPointer) pointerOp;
				result = new PointerType(result, pointer.isConst());
			}
		}
		if (declarator instanceof IASTArrayDeclarator) {
			for (final IASTArrayModifier arrayMod : ((IASTArrayDeclarator) declarator).getArrayModifiers()) {
				if (arrayMod.getConstantExpression() == null) {
					throw new UnsupportedOperationException(
							"At the moment only arrays with fully specified dimensions are allowed");
				}

				final ExpWrapper arrayDimWrapper = expressionStack.pop();

				if (Utils.isConstant(arrayDimWrapper)) {
					result = ArrayType.create(result, arrayDimWrapper.getExp());
				} else {
					throw new UnsupportedOperationException("At the moment variable length arrays are not allowed");
				}
			}

		}

		return result;
	}

	private int leave(IASTDeclarator declarator, IASTNode nonArrayParent) {
		if (declarator instanceof IASTStandardFunctionDeclarator) {
			final IASTStandardFunctionDeclarator functionDeclarator = (IASTStandardFunctionDeclarator) declarator;
			if (functionDeclarator.takesVarArgs()) {
				return abortWithMessage("Functions with varargs are currently not allowed");
			}

			final boolean inFuncDef;
			if (nonArrayParent instanceof IASTFunctionDefinition) {
				inFuncDef = true;
			} else if (nonArrayParent instanceof IASTSimpleDeclaration) {
				inFuncDef = false;
			} else {
				return abortWithMessage("Function declaration found within a " + nonArrayParent.getClass());
			}

			final IASTName functionName = functionDeclarator.getName();
			final IBinding functionBinding = functionName.resolveBinding();
			AbsFuncId absFuncId = (AbsFuncId) findAbsId(functionBinding);
			if (absFuncId != null) {
				if (!inFuncDef) {
					// we are declaring this function name twice
					return abortWithMessage("Function " + functionName + " is declared twice");
				}
				if (absFuncId.isDefined()) {
					// we are defining this function twice
					return abortWithMessage("Function " + functionName + " is defined twice");
				}
			}

			final FullType baseType = typeStack.peek();
			if (baseType == null) {
				return abortWithMessage("Function return type is undefined struct");
			}
			final FullType returnType = convertType(baseType, declarator);

			final String className = Utils.createClassName(functionName.toString());

			// create method signature for function
			methodSig = new MethodSig();
			methodSig.setName("call");
			methodSig.setReturnType(TypesUtils.returnTypeFor(returnType));

			// initialize map of methods called from this method
			methodsCalled.clear();

			for (int i = 0; i < numberOfParams; ++i) {
				final FormalParameter formalParameter = formalParameters.get(i);

				if (formalParameter.fullType instanceof ArrayType) {
					return abortWithMessage("We currently do not allow arrays as function parameters");
				}
				cIdToAbsId.peek().put(formalParameter.name.resolveBinding(), formalParameter);
			}

			if (absFuncId == null) {
				// add function to cIdToAbsId
				absFuncId = new AbsFuncId(returnType, functionName.toString(), formalParameters, inFuncDef);
				cIdToAbsId.peek().put(functionBinding, absFuncId);

				// create a new interface for this method
				final String interfaceName = Utils.createInterfaceName(functionName.toString());
				final InterfaceDecl interfaceDecl = new InterfaceDecl();
				interfaceDecl.setName(interfaceName);
				interfaceDecl.addBody(methodSig);
				moduleDecl.addDecl(interfaceDecl);
				classNameToInterface.put(className, interfaceDecl);
			}

			if (inFuncDef) {
				if (returnType == SimpleType.VOID_TYPE) {
					methodNeedsReturn = false;
					this.returnType = null;
				} else {
					methodNeedsReturn = true;
					this.returnType = returnType;
				}

				// initialize flags
				methodContainsReturn = false;
				methodContainsBreak = false;
				methodContainsContinue = false;

				// get the interface for this function definition
				currentInterfaceDecl = classNameToInterface.get(className);
				// create a new class implementing the interface
				currentClassDecl = new ClassDecl();
				currentClassDecl.setName(className);
				currentClassDecl.addImplementedInterfaceUse(new InterfaceType(currentInterfaceDecl).toUse());
				// add parameters to class
				currentClassDecl.addParamNoTransform(Utils.getGlobalVarsParamDecl());
				currentClassDecl.addParamNoTransform(Utils.getParamsParamDecl());
				// add fields to class
				currentClassDecl.addField(Utils.getLocalVarsFieldDecl());
				// save main function class for later
				if ("main".equals(functionName.toString())) {
					mainInterfaceDecl = currentInterfaceDecl;
					mainClassDecl = currentClassDecl;
				}

				// create new scope
				cIdToAbsId.push(new HashMap<>());
				statementStack.push(new Stack<>());
			}
		} else if (nonArrayParent instanceof IASTParameterDeclaration) {
			final FullType baseType = typeStack.peek();
			if (baseType == null) {
				return abortWithMessage("Parameter type is undefined struct");
			}
			final FullType type = convertType(baseType, declarator);
			if (declarator.getName().toString().isEmpty()) {
				if (type == SimpleType.VOID_TYPE && numberOfParams == 1) {
					numberOfParams = 0;
					return PROCESS_CONTINUE;
				} else {
					return abortWithMessage("Unnamed parameter found");
				}
			} else if (type == SimpleType.VOID_TYPE) {
				return abortWithMessage("Void parameter named");
			}

			// update method signature
			final IASTName name = declarator.getName();
			formalParameters.add(new FormalParameter(formalParameters.size(), name, type, false));

		} else if (nonArrayParent instanceof IASTSimpleDeclaration) {
			final IASTSimpleDeclaration declaration = (IASTSimpleDeclaration) nonArrayParent;

			final boolean isTopLevel = declaration.getParent() instanceof IASTTranslationUnit;
			switch (declaration.getDeclSpecifier().getStorageClass()) {
			case IASTDeclSpecifier.sc_typedef:
				return abortWithMessage("Currently typedefs are not allowed");
			case IASTDeclSpecifier.sc_extern:
				return abortWithMessage("Currently extern variable declarations are not allowed");
			case IASTDeclSpecifier.sc_static:
				if (!isTopLevel) {
					return abortWithMessage("Currently static local variables are not allowed");
				}
			}

			final FullType baseType = typeStack.peek();
			final FullType fullType = convertType(baseType, declarator);

			ExpWrapper expWrapper;
			if (declarator.getInitializer() != null) {
				if (fullType instanceof ArrayType) {
					return abortWithMessage("At the moment array initializers are not allowed");
				}

				// initialize the variable if an initializer is given
				if (declarator.getInitializer() instanceof IASTEqualsInitializer) {
					expWrapper = expressionStack.pop();
					addMethodsCalledIn(expWrapper);
				} else {
					return abortWithMessage("Unexpected declarator type: " + declarator.getInitializer().getClass());
				}
				if (!fullType.matches(expWrapper.getFullType())) {
					// types do not match!
					return abortWithMessage("Types do not match: " + fullType + " != " + expWrapper.getFullType());
				}
			} else {
				expWrapper = null;
			}

			final IASTName varName = declarator.getName();

			if (isTopLevel) {
				if (expWrapper != null) {
					if (!expWrapper.getSideEffects().isEmpty()) {
						// no side effects allowed for static/extern vars
						return abortWithMessage(
								"No (reachable) side effects allowed in initialization of static/extern vars");
					}
					if (expWrapper.isFuture) {
						// only constants allowed in static/extern vars
						return abortWithMessage("Only constants allowed in initialization of static/extern vars");
					}
				}

				// create global variable
				GlobalVariable globalVar = new GlobalVariable(globalVars.size(), varName, fullType, false, expWrapper);
				// add to global vars
				globalVars.add(globalVar);
				// add to scope
				cIdToAbsId.peek().put(varName.resolveBinding(), globalVar);
			} else {
				// create local variable
				LocalVariable localVar = new LocalVariable(localVars.size(), varName, fullType, false, expWrapper);
				// add to local vars
				localVars.add(localVar);
				// add to scope
				cIdToAbsId.peek().put(varName.resolveBinding(), localVar);

				final List<Stmt> statements = new ArrayList<>();
				if (expWrapper != null && expWrapper.isFuture) {
					// calculate value and assign it to the local var
					statements.addAll(expWrapper.extractStatements());
					// Val tmp1 = fv.get;
					final String tmpVar1 = Utils.generateTmpVarString();
					final VarDeclStmt varDeclStmt1 = new VarDeclStmt();
					varDeclStmt1.setVarDecl(new VarDecl(tmpVar1, TypesUtils.getValueTypeUse(),
							new Opt<>(new GetExp(expWrapper.getExp()))));
					statements.add(varDeclStmt1);
					// Fut<Unit> tmp2 = mem(nth(localVars, n))!setVal(0, tmp1);
					final String tmpVar2 = Utils.generateTmpVarString();
					final VarDeclStmt varDeclStmt2 = new VarDeclStmt();
					final FnApp memExp = new FnApp();
					memExp.setName(MEMORY_SELECTOR);
					memExp.addParam(localVar.nthExp());
					final AsyncCall call = new AsyncCall();
					call.setCallee(memExp);
					call.setMethod(SET_VALUE_METHOD);
					call.addParamNoTransform(new IntLiteral("0"));
					call.addParamNoTransform(new VarUse(tmpVar1));
					varDeclStmt2.setVarDecl(new VarDecl(tmpVar2,
							TypesUtils.getFutureTypeUse(TypesUtils.getUnitTypeUse()), new Opt<>(call)));
					statements.add(varDeclStmt2);
					// tmp2.get;
					final ExpressionStmt getStmt = new ExpressionStmt();
					getStmt.setExp(new GetExp(new VarUse(tmpVar2)));
					statements.add(getStmt);
				}
				statementStack.peek().push(new StmtWrapper(statements));
			}
		} else {
			return abortWithMessage("Declarator found within unexpected parent: " + nonArrayParent.getClass());
		}

		return PROCESS_CONTINUE;
	}

	private void addMethodsCalledIn(ExpWrapper expWrapper) {
		for (final SubExp subExp : expWrapper.getSubExpressions()) {
			if (subExp instanceof SimpleVarDeclSubExp) {
				continue;
			}
			final String methodName = subExp.getMethodName();
			if (!methodsCalled.containsKey(methodName)) {
				final MethodImpl methodImpl = subExp.createMethodImpl();
				methodsCalled.put(methodName, methodImpl);
			}
		}
	}

	@Override
	public int visit(IASTDeclSpecifier declSpec) {
		if (declSpec instanceof IASTCompositeTypeSpecifier) {
			final IASTCompositeTypeSpecifier compositeTypeSpec = (IASTCompositeTypeSpecifier) declSpec;

			switch (compositeTypeSpec.getKey()) {
			case IASTCompositeTypeSpecifier.k_struct:
				return abortWithMessage("Currently structs are not allowed");
			case IASTCompositeTypeSpecifier.k_union:
				return abortWithMessage("Currently unions are not allowed");
			default:
				return abortWithMessage("Unknown composite type: " + compositeTypeSpec.getKey());
			}
		}

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(IASTDeclSpecifier declSpec) {
		if (declSpec instanceof IASTSimpleDeclSpecifier) {
			// store the TypeUse type generated for the decl spec for later use
			typeStack.push(TypesUtils.convertType((IASTSimpleDeclSpecifier) declSpec));
		} else {
			return abortWithMessage("Only simple type declarations are allowed");
		}

		return PROCESS_CONTINUE;
	}

	@Override
	public int visit(IASTExpression expression) {
		if (expression instanceof IASTProblemExpression) {
			return abortWithMessage("Problem parsing C code, expected expression: "
					+ ((IASTProblemExpression) expression).getProblem().getMessage());
		}

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(IASTExpression expression) {
		final PureExp exp;
		final FullType fullType;
		final java.util.List<SubExp> subExpressions;
		final java.util.List<PureExp> sideEffects;
		final boolean isFuture;
		if (expression instanceof IASTLiteralExpression) {
			subExpressions = Collections.emptyList();
			sideEffects = Collections.emptyList();
			isFuture = false;
			final IASTLiteralExpression literalExp = ((IASTLiteralExpression) expression);
			switch (literalExp.getKind()) {
			case IASTLiteralExpression.lk_integer_constant:
				exp = new IntLiteral(literalExp.toString());
				fullType = SimpleType.SIGNED_INT_TYPE;
				break;
			default:
				return abortWithMessage(
						"Unknown literal found: IASTLiteralExpression.getKind() = " + literalExp.getKind());
			}
		} else if (expression instanceof IASTIdExpression) {
			final IASTName name = ((IASTIdExpression) expression).getName();

			final AbsId absId = findAbsId(name.resolveBinding());
			if (absId == null) {
				return abortWithMessage("Could not find ABS ID for: " + name);
			}

			if (absId instanceof AbsFuncId) {
				expressionStack.push(new FuncNameExpWrapper((AbsFuncId) absId));
			} else if (absId instanceof VarOrParamDeclaration) {
				expressionStack.push(LvalueExpWrapper.createFromVar((VarOrParamDeclaration) absId));
			} else {
				return abortWithMessage("Unknown ABS ID type: " + absId.getClass());
			}

			return PROCESS_CONTINUE;
		} else if (expression instanceof IASTUnaryExpression) {
			final ExpWrapper expWrapper = expressionStack.pop();
			final ValueArgument arg = ValueArgument.createIntValueArg(expWrapper.isFuture, ARG, expWrapper.getExp());
			int operator = ((IASTUnaryExpression) expression).getOperator();
			switch (operator) {
			case IASTUnaryExpression.op_bracketedPrimary:
				expressionStack.push(expWrapper);
				return PROCESS_CONTINUE;
			case IASTUnaryExpression.op_minus:
				fullType = expWrapper.getFullType();
				if (expWrapper.isFuture) {
					final IntArithmeticSubExp minusSubExp = new IntArithmeticSubExp(Utils.UNARY_MINUS, arg);
					subExpressions = new ArrayList<>(expWrapper.getSubExpressions());
					subExpressions.add(minusSubExp);
					isFuture = true;
					exp = new VarUse(minusSubExp.getTmpVar());
				} else {
					subExpressions = expWrapper.getSubExpressions();
					isFuture = false;
					exp = new MinusExp(expWrapper.getExp());
				}
				sideEffects = expWrapper.getSideEffects();
				break;
			case IASTUnaryExpression.op_not:
				fullType = expWrapper.getFullType();
				if (expWrapper.isFuture) {
					final IntArithmeticSubExp notSubExp = new IntArithmeticSubExp(Utils.NOT, arg);
					subExpressions = new ArrayList<>(expWrapper.getSubExpressions());
					subExpressions.add(notSubExp);
					isFuture = true;
					exp = new VarUse(notSubExp.getTmpVar());
				} else {
					subExpressions = expWrapper.getSubExpressions();
					isFuture = false;
					exp = Utils.negateInt(expWrapper.getExp());
				}
				sideEffects = expWrapper.getSideEffects();
				break;
			case IASTUnaryExpression.op_prefixDecr:
				expressionStack.push(Utils.modifyField(true, false, (LvalueExpWrapper) expWrapper));
				return PROCESS_CONTINUE;
			case IASTUnaryExpression.op_prefixIncr:
				expressionStack.push(Utils.modifyField(true, true, (LvalueExpWrapper) expWrapper));
				return PROCESS_CONTINUE;
			case IASTUnaryExpression.op_postFixDecr:
				expressionStack.push(Utils.modifyField(false, false, (LvalueExpWrapper) expWrapper));
				return PROCESS_CONTINUE;
			case IASTUnaryExpression.op_postFixIncr:
				expressionStack.push(Utils.modifyField(false, true, (LvalueExpWrapper) expWrapper));
				return PROCESS_CONTINUE;
			case IASTUnaryExpression.op_amper:
				// create pointer
				expressionStack.push(((LvalueExpWrapper) expWrapper).lvalue.addressOf());
				return PROCESS_CONTINUE;
			case IASTUnaryExpression.op_star:
				// deref
				expressionStack.push(LvalueExpWrapper.createDeref(expWrapper));
				return PROCESS_CONTINUE;
			default:
				return abortWithMessage(
						"Unknown unary operator found: IASTUnaryExpression.getOperator() = " + operator);
			}
		} else if (expression instanceof IASTBinaryExpression) {
			final ExpWrapper expWrapper2 = expressionStack.pop();
			final ExpWrapper expWrapper1 = expressionStack.pop();
			int operator = ((IASTBinaryExpression) expression).getOperator();
			return dealWithBinaryExpression(expWrapper1, operator, expWrapper2);
		} else if (expression instanceof IASTConditionalExpression) {
			return abortWithMessage(
					"Currently ternary and Elvis operators are not allowed");
		} else if (expression instanceof IASTFunctionCallExpression) {
			final IASTFunctionCallExpression functionCallExpression = (IASTFunctionCallExpression) expression;
			if (functionCallExpression.getFunctionNameExpression() instanceof IASTIdExpression) {
				final String functionName = ((IASTIdExpression) functionCallExpression.getFunctionNameExpression())
						.getName().toString();
				final Stack<ExpWrapper> arguments = new Stack<>();
				for (IASTInitializerClause initializerClause : functionCallExpression.getArguments()) {
					if (initializerClause instanceof IASTExpression) {
						arguments.push(expressionStack.pop());
					} else {
						// we only allow expressions as function arguments
						return abortWithMessage(
								"Argument to function is not an expression type: " + initializerClause.getClass());
					}
				}

				// pop the function name off the stack and make sure it matches
				final ExpWrapper funcNameExpWrapper = expressionStack.pop();
				final AbsFuncId absFuncId;
				if (funcNameExpWrapper instanceof FuncNameExpWrapper) {
					absFuncId = ((FuncNameExpWrapper) funcNameExpWrapper).getAbsFuncId();
					if (!functionName.equals(absFuncId.getName())) {
						return abortWithMessage(
								"Function names do not match: " + functionName + " != " + absFuncId.getName());
					}
				} else {
					return abortWithMessage(
							"Function name expression expected, but found: " + funcNameExpWrapper.getClass());
				}

				subExpressions = new ArrayList<>();
				sideEffects = new ArrayList<>();
				final List<ValueArgument> args = new ArrayList<>();
				int i = 0;
				while (!arguments.isEmpty()) {
					final String argName = ARG + (++i);
					final ExpWrapper expWrapper = arguments.pop();
					subExpressions.addAll(expWrapper.getSubExpressions());
					sideEffects.addAll(expWrapper.getSideEffects());
					if (expWrapper.getFullType() == SimpleType.SIGNED_INT_TYPE) {
						args.add(ValueArgument.createIntValueArg(expWrapper.isFuture, argName, expWrapper.getExp()));
					} else if (expWrapper.getFullType() instanceof PointerType) {
						args.add(
								ValueArgument.createPointerValueArg(expWrapper.isFuture, argName, expWrapper.getExp()));
					} else {
						throw new IllegalArgumentException();
					}
				}
				final FuncSubExp funcSubExp = new FuncSubExp(absFuncId, args, sideEffects);
				subExpressions.add(funcSubExp);

				fullType = absFuncId.getReturnType();
				exp = new VarUse(funcSubExp.getTmpVar());

				// we've already included the side effects in the evaluation of the call
				expressionStack.push(new ExpWrapper(true, subExpressions, exp, fullType, Collections.emptyList()));
				return PROCESS_CONTINUE;
			} else {
				// we only allow explicit named functions (at least for now)
				return abortWithMessage("Currently only function calls to named functions are allowed");
			}
		} else if (expression instanceof IASTArraySubscriptExpression) {
			// pop the lvalue and the index off the expression stack
			final ExpWrapper index = expressionStack.pop();
			final ExpWrapper arrayOrPointer = expressionStack.pop();
			if (SimpleType.SIGNED_INT_TYPE != index.getFullType()) {
				return abortWithMessage("Subscript is not an int value");
			}
			expressionStack.push(LvalueExpWrapper.createSubscript(arrayOrPointer, index));
			return PROCESS_CONTINUE;
		} else {
			return abortWithMessage("Unknown expression type: " + expression.getClass());
		}

		expressionStack.push(new ExpWrapper(isFuture, subExpressions, exp, fullType, sideEffects));
		return PROCESS_CONTINUE;
	}

	private int dealWithBinaryExpression(ExpWrapper expWrapper1, int operator, ExpWrapper expWrapper2) {
		final PureExp exp;
		final FullType fullType;
		final java.util.List<SubExp> subExpressions;
		final java.util.List<PureExp> sideEffects;
		final boolean isFuture;
		boolean isRelation = false;
		boolean isPointerArithmetic = false;
		switch (operator) {
		case IASTBinaryExpression.op_logicalAnd:
		case IASTBinaryExpression.op_logicalOr:
			return abortWithMessage(
					"Short circuit operators && and || are currently unavailable, we need to create additional methods for each occurrence");
		case IASTBinaryExpression.op_minus:
		case IASTBinaryExpression.op_plus:
			if (SimpleType.SIGNED_INT_TYPE == expWrapper1.getFullType()
					&& SimpleType.SIGNED_INT_TYPE == expWrapper2.getFullType()) {
				fullType = SimpleType.SIGNED_INT_TYPE;
			} else if (expWrapper1.getFullType() instanceof PointerType
					&& SimpleType.SIGNED_INT_TYPE == expWrapper2.getFullType()) {
				isPointerArithmetic = true;
				fullType = expWrapper1.getFullType();
			} else {
				return abortWithMessage("Types are wrong!");
			}
			break;
		case IASTBinaryExpression.op_modulo:
		case IASTBinaryExpression.op_multiply:
			if (SimpleType.SIGNED_INT_TYPE == expWrapper1.getFullType()
					&& SimpleType.SIGNED_INT_TYPE == expWrapper2.getFullType()) {
				fullType = SimpleType.SIGNED_INT_TYPE;
			} else {
				return abortWithMessage("Types are wrong!");
			}
			break;
		case IASTBinaryExpression.op_equals:
		case IASTBinaryExpression.op_notequals:
		case IASTBinaryExpression.op_greaterEqual:
		case IASTBinaryExpression.op_greaterThan:
		case IASTBinaryExpression.op_lessEqual:
		case IASTBinaryExpression.op_lessThan:
			// ensure both arguments are of the same type
			if (!expWrapper1.getFullType().matches(expWrapper2.getFullType())) {
				return abortWithMessage("Trying to check equality between expressions of different types!");
			}
			isRelation = true;
			fullType = SimpleType.SIGNED_INT_TYPE;
			break;
		case IASTBinaryExpression.op_assign:
			// ensure both arguments are of the same type
			if (!expWrapper1.getFullType().matches(expWrapper2.getFullType())) {
				return abortWithMessage("Types are wrong!");
			}
			fullType = expWrapper2.getFullType();
			break;
		case IASTBinaryExpression.op_binaryAndAssign:
		case IASTBinaryExpression.op_binaryOrAssign:
		case IASTBinaryExpression.op_moduloAssign:
		case IASTBinaryExpression.op_multiplyAssign:
			if (SimpleType.SIGNED_INT_TYPE == expWrapper1.getFullType()
					&& SimpleType.SIGNED_INT_TYPE == expWrapper2.getFullType()) {
				fullType = SimpleType.SIGNED_INT_TYPE;
			} else {
				return abortWithMessage("Types are wrong!");
			}
			break;
		case IASTBinaryExpression.op_minusAssign:
		case IASTBinaryExpression.op_plusAssign:
			if (SimpleType.SIGNED_INT_TYPE == expWrapper1.getFullType()
					&& SimpleType.SIGNED_INT_TYPE == expWrapper2.getFullType()) {
				fullType = SimpleType.SIGNED_INT_TYPE;
			} else if (expWrapper1.getFullType() instanceof PointerType
					&& SimpleType.SIGNED_INT_TYPE == expWrapper2.getFullType()) {
				isPointerArithmetic = true;
				fullType = expWrapper1.getFullType();
			} else {
				return abortWithMessage("Types are wrong!");
			}
			break;
		default:
			return abortWithMessage("Unknown binary operator found: IASTBinaryExpression.getOperator() = " + operator);
		}
		final String opName;
		final SubExp subExp;
		final Lvalue lvalue;
		final SetterSubExp setterSubExp;
		switch (operator) {
		case IASTBinaryExpression.op_logicalAnd:
		case IASTBinaryExpression.op_logicalOr:
			return abortWithMessage(
					"Short circuit operators && and || are currently unavailable, we need to create additional methods for each occurrence");
		case IASTBinaryExpression.op_assign:
			lvalue = ((LvalueExpWrapper) expWrapper1).lvalue;
			isFuture = expWrapper2.isFuture;
			subExpressions = new ArrayList<>(lvalue.getSubExpressions());
			subExpressions.addAll(expWrapper2.getSubExpressions());
			final ValueArgument valueArg;
			if (expWrapper2.getFullType() == SimpleType.SIGNED_INT_TYPE) {
				valueArg = ValueArgument.createIntValueArg(isFuture, VALUE, expWrapper2.getExp());
			} else if (expWrapper2.getFullType() instanceof PointerType) {
				valueArg = ValueArgument.createPointerValueArg(isFuture, VALUE, expWrapper2.getExp());
			} else {
				throw new IllegalArgumentException();
			}
			setterSubExp = SetterSubExp.create(new LocationArgument(lvalue), valueArg);
			subExpressions.add(setterSubExp);
			sideEffects = new ArrayList<>(lvalue.getSideEffects());
			sideEffects.addAll(expWrapper2.getSideEffects());
			sideEffects.add(new VarUse(setterSubExp.getTmpVar()));
			exp = expWrapper2.getExp();
			break;
		case IASTBinaryExpression.op_binaryAndAssign:
		case IASTBinaryExpression.op_binaryOrAssign:
		case IASTBinaryExpression.op_minusAssign:
		case IASTBinaryExpression.op_moduloAssign:
		case IASTBinaryExpression.op_multiplyAssign:
		case IASTBinaryExpression.op_plusAssign:
			lvalue = ((LvalueExpWrapper) expWrapper1).lvalue;
			subExpressions = new ArrayList<>(expWrapper1.getSubExpressions());
			subExpressions.addAll(expWrapper2.getSubExpressions());
			if (isPointerArithmetic) {
				final boolean isPointerPlus = operator == IASTBinaryExpression.op_plusAssign;
				final ValueArgument pointerArg = ValueArgument.createPointerValueArg(expWrapper1.isFuture, POINTER_ARG,
						expWrapper1.getExp());
				final IntArgument sizeArg = new IntArgument(SIZE_ARG, expWrapper1.getFullType().getInnerSize());
				final ValueArgument offsetArg = ValueArgument.createIntValueArg(expWrapper2.isFuture, OFFSET_PARAM,
						expWrapper2.getExp());
				subExp = new PointerArithmeticSubExp(isPointerPlus, pointerArg, sizeArg, offsetArg);
			} else {
				switch (operator) {
				case IASTBinaryExpression.op_minusAssign:
					opName = MINUS;
					break;
				case IASTBinaryExpression.op_multiplyAssign:
					opName = MULT;
					break;
				case IASTBinaryExpression.op_plusAssign:
					opName = PLUS;
					break;
				default:
					return abortWithMessage(
							"Unknown binary operator found: IASTBinaryExpression.getOperator() = " + operator);
				}
				final ValueArgument arg1 = ValueArgument.createIntValueArg(expWrapper1.isFuture, ARG1,
						expWrapper1.getExp());
				final ValueArgument arg2 = ValueArgument.createIntValueArg(expWrapper2.isFuture, ARG2,
						expWrapper2.getExp());
				subExp = new IntArithmeticSubExp(opName, arg1, arg2);
			}
			subExpressions.add(subExp);
			final ValueArgument newValue = ValueArgument.createIntValueArg(true, VALUE, new VarUse(subExp.getTmpVar()));
			setterSubExp = SetterSubExp.create(new LocationArgument(lvalue), newValue);
			subExpressions.add(setterSubExp);
			sideEffects = new ArrayList<>(expWrapper1.getSideEffects());
			sideEffects.addAll(expWrapper2.getSideEffects());
			sideEffects.add(new VarUse(setterSubExp.getTmpVar()));
			isFuture = true;
			exp = new VarUse(subExp.getTmpVar());
			break;
		default:
			isFuture = expWrapper1.isFuture || expWrapper2.isFuture;
			// side effects from both, but no further ones
			sideEffects = new ArrayList<>(expWrapper1.getSideEffects());
			sideEffects.addAll(expWrapper2.getSideEffects());
			// sub expressions from both, adding one more later if isFuture
			subExpressions = new ArrayList<>(expWrapper1.getSubExpressions());
			subExpressions.addAll(expWrapper2.getSubExpressions());
			if (isPointerArithmetic) {
				final boolean isPointerPlus = operator == IASTBinaryExpression.op_plus;
				final ValueArgument pointerArg = ValueArgument.createPointerValueArg(expWrapper1.isFuture, POINTER_ARG,
						expWrapper1.getExp());
				final IntArgument sizeArg = new IntArgument(SIZE_ARG, expWrapper1.getFullType().getInnerSize());
				final ValueArgument offsetArg = ValueArgument.createIntValueArg(expWrapper2.isFuture, OFFSET_PARAM,
						expWrapper2.getExp());
				final PointerArithmeticSubExp pointerArithmeticSubExp = new PointerArithmeticSubExp(isPointerPlus,
						pointerArg, sizeArg, offsetArg);
				if (isFuture) {
					// add the additional op-call sub expression
					subExpressions.add(pointerArithmeticSubExp);
					exp = new VarUse(pointerArithmeticSubExp.getTmpVar());
				} else {
					exp = pointerArithmeticSubExp.createResult(false);
				}
			} else if (isFuture) {
				opName = Utils.getOpName(operator);
				final ValueArgument arg1 = ValueArgument.create(ARG1, expWrapper1);
				final ValueArgument arg2 = ValueArgument.create(ARG2, expWrapper2);
				if (isRelation) {
					subExp = new RelationSubExp(opName, arg1, arg2);
				} else {
					subExp = new IntArithmeticSubExp(opName, arg1, arg2);
				}
				// add the additional op-call sub expression
				subExpressions.add(subExp);
				exp = new VarUse(subExp.getTmpVar());
			} else {
				switch (operator) {
				case IASTBinaryExpression.op_minus:
					exp = new SubAddExp(expWrapper1.getExp(), expWrapper2.getExp());
					break;
				case IASTBinaryExpression.op_multiply:
					exp = new MultMultExp(expWrapper1.getExp(), expWrapper2.getExp());
					break;
				case IASTBinaryExpression.op_plus:
					exp = new AddAddExp(expWrapper1.getExp(), expWrapper2.getExp());
					break;
				case IASTBinaryExpression.op_equals:
					exp = Utils.convertBoolToInt(new EqExp(expWrapper1.getExp(), expWrapper2.getExp()));
					break;
				case IASTBinaryExpression.op_greaterEqual:
					exp = Utils.convertBoolToInt(new GTEQExp(expWrapper1.getExp(), expWrapper2.getExp()));
					break;
				case IASTBinaryExpression.op_greaterThan:
					exp = Utils.convertBoolToInt(new GTExp(expWrapper1.getExp(), expWrapper2.getExp()));
					break;
				case IASTBinaryExpression.op_lessEqual:
					exp = Utils.convertBoolToInt(new LTEQExp(expWrapper1.getExp(), expWrapper2.getExp()));
					break;
				case IASTBinaryExpression.op_lessThan:
					exp = Utils.convertBoolToInt(new LTExp(expWrapper1.getExp(), expWrapper2.getExp()));
					break;
				case IASTBinaryExpression.op_notequals:
					exp = Utils.convertBoolToInt(new NotEqExp(expWrapper1.getExp(), expWrapper2.getExp()));
					break;
				default:
					return abortWithMessage(
							"Unknown binary operator found: IASTBinaryExpression.getOperator() = " + operator);
				}
			}
			break;
		}

		expressionStack.push(new ExpWrapper(isFuture, subExpressions, exp, fullType, sideEffects));
		return PROCESS_CONTINUE;
	}

	private AbsId findAbsId(IBinding binding) {
		for (int i = cIdToAbsId.size() - 1; i >= 0; --i) {
			final AbsId absId = cIdToAbsId.get(i).get(binding);
			if (absId != null) {
				return absId;
			}
		}
		return null;
	}

	@Override
	public int visit(IASTStatement statement) {
		if (statement instanceof IASTProblemStatement) {
			return abortWithMessage("Problem parsing C code, expected statement: "
					+ ((IASTProblemStatement) statement).getProblem().getMessage());
		} else if (statement instanceof IASTCompoundStatement) {
			// enter new scope
			cIdToAbsId.push(new HashMap<>());
			statementStack.push(new Stack<>());
		}

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(IASTStatement statement) {
		if (statement instanceof IASTNullStatement) {
			statementStack.peek().push(new StmtWrapper(new SkipStmt()));
		} else if (statement instanceof IASTCompoundStatement) {
			final Block block = new Block();
			final int containsFlags = populateBlockFromStatementStack(block);
			statementStack.peek().push(new StmtWrapper(block, containsFlags));
			cIdToAbsId.pop();
		} else if (statement instanceof IASTExpressionStatement) {
			final ExpWrapper expWrapper = expressionStack.pop();
			addMethodsCalledIn(expWrapper);

			statementStack.peek().push(new StmtWrapper(expWrapper.extractStatements()));
		} else if (statement instanceof IASTReturnStatement) {
			final ArrayList<Stmt> statements = new ArrayList<>();
			final IASTExpression returnValue = ((IASTReturnStatement) statement).getReturnValue();
			if (returnValue != null && returnType != null) {
				final ExpWrapper expWrapper = expressionStack.pop();
				addMethodsCalledIn(expWrapper);

				// ensure that the expression type matches the return type
				if (!returnType.matches(expWrapper.getFullType())) {
					return abortWithMessage("Function return type does not match type of expression returned: "
							+ returnType + " != " + expWrapper.getFullType());
				}

				statements.addAll(expWrapper.extractStatements());

				final AssignStmt assignment = new AssignStmt();
				assignment.setVar(new VarUse(METHOD_RESULT_VAR_NAME));

				if (expWrapper.isFuture) {
					assignment.setValue(new GetExp(expWrapper.getExp()));
				} else {
					assignment.setValue(Utils.createValue(expWrapper.getExp(), expWrapper.getFullType().getTypeUse()));
				}
				statements.add(assignment);
			} else if (returnValue != null && returnType == null) {
				return abortWithMessage("Function return type is void, but return value given");
			} else if (returnValue == null && returnType != null) {
				return abortWithMessage("Function return type is not void, but no return value given");
			}
			// set return flag
			methodContainsReturn = true;
			final AssignStmt setFlag = new AssignStmt();
			setFlag.setVar(new VarUse(RETURN_FLAG));
			setFlag.setValue(Utils.getTrueExp());

			statements.add(setFlag);
			statementStack.peek().push(new StmtWrapper(statements, true));
		} else if (statement instanceof IASTDeclarationStatement) {
			final IASTDeclaration declaration = ((IASTDeclarationStatement) statement).getDeclaration();
			if (declaration instanceof IASTSimpleDeclaration) {
				// a number of statements have been added to the stack in leave(IASTDeclaration)
				final java.util.List<Stmt> statements = new ArrayList<>();
				for (@SuppressWarnings("unused")
				final IASTDeclarator declarator : ((IASTSimpleDeclaration) declaration).getDeclarators()) {
					final StmtWrapper stmtWrapper = statementStack.peek().pop();
					statements.addAll(0, stmtWrapper.getStatements());
				}
				statementStack.peek().push(new StmtWrapper(statements));
			}
		} else if (statement instanceof IASTIfStatement) {
			final ExpWrapper ifConditionWrapper = expressionStack.pop();
			addMethodsCalledIn(ifConditionWrapper);
			final StmtWrapper elseStmtWrapper = ((IASTIfStatement) statement).getElseClause() == null ? null
					: statementStack.peek().pop();
			final StmtWrapper thenStmtWrapper = statementStack.peek().pop();

			final java.util.List<Stmt> statements = new ArrayList<>();
			if (!ifConditionWrapper.getSubExpressions().isEmpty()) {
				for (final SubExp subExp : ifConditionWrapper.getSubExpressions()) {
					statements.add(subExp.getVarDeclStmt());
				}
			}

			// initialize guard
			Guard guard = null;
			if (ifConditionWrapper.isFuture) {
				// add await evaluation of future
				guard = new ClaimGuard(ifConditionWrapper.getExp());
			}

			// add await evaluation of side effects to guard
			for (final PureExp sideEffect : ifConditionWrapper.getSideEffects()) {
				final ClaimGuard claimGuard = new ClaimGuard(sideEffect);
				guard = guard == null ? claimGuard : new AndGuard(guard, claimGuard);
			}

			if (guard != null) {
				// add await statement
				final AwaitStmt awaitStmt = new AwaitStmt();
				awaitStmt.setGuard(guard);
				statements.add(awaitStmt);
			}

			PureExp condition;
			if (ifConditionWrapper.isFuture) {
				// add var decl getting the value from the future
				final String tmpVar = Utils.generateTmpVarString();
				final VarDeclStmt varDeclStmt = new VarDeclStmt();
				varDeclStmt.setVarDecl(new VarDecl(tmpVar, TypesUtils.getValueTypeUse(),
						new Opt<>(new GetExp(ifConditionWrapper.getExp()))));
				statements.add(varDeclStmt);
				// set the condition
				condition = Utils.getInnerValue(new VarUse(tmpVar), ifConditionWrapper.getFullType().getTypeUse());
			} else {
				condition = ifConditionWrapper.getExp();
			}
			// convert Int or Pointer to Bool
			if (ifConditionWrapper.getFullType() == SimpleType.SIGNED_INT_TYPE) {
				condition = Utils.convertIntToBool(condition);
			} else if (ifConditionWrapper.getFullType() instanceof PointerType) {
				condition = Utils.convertPointerToBool(condition);
			} else {
				throw new IllegalArgumentException("If-condition must be an int or pointer!");
			}

			final IfStmt ifStmt = new IfStmt();
			ifStmt.setCondition(condition);
			ifStmt.setThen(thenStmtWrapper.getBlock());
			int containsFlags = thenStmtWrapper.getFlags();
			if (elseStmtWrapper != null) {
				ifStmt.setElse(elseStmtWrapper.getBlock());
				containsFlags = containsFlags | elseStmtWrapper.getFlags();
			}

			statementStack.peek().push(new StmtWrapper(statements, ifStmt, containsFlags));
		} else if (statement instanceof IASTWhileStatement) {
			final ExpWrapper conditionWrapper = expressionStack.pop();
			addMethodsCalledIn(conditionWrapper);
			final java.util.List<Stmt> auxiliaryStatements = new ArrayList<>();
			for (final SubExp subExp : conditionWrapper.getSubExpressions()) {
				auxiliaryStatements.add(subExp.getVarDeclStmt());
			}
			Guard guard = null;
			if (conditionWrapper.isFuture) {
				guard = new ClaimGuard(conditionWrapper.getExp());
			}
			for (final PureExp sideEffect : conditionWrapper.getSideEffects()) {
				final ClaimGuard claimGuard = new ClaimGuard(sideEffect);
				guard = guard == null ? claimGuard : new AndGuard(guard, claimGuard);
			}
			if (guard != null) {
				final AwaitStmt awaitStmt = new AwaitStmt();
				awaitStmt.setGuard(guard);
				auxiliaryStatements.add(awaitStmt);
			}

			PureExp condition;
			if (conditionWrapper.isFuture) {
				final String tmpVar = Utils.generateTmpVarString();
				final VarDeclStmt varDeclStmt = new VarDeclStmt();
				varDeclStmt.setVarDecl(new VarDecl(tmpVar, TypesUtils.getValueTypeUse(),
						new Opt<>(new GetExp(conditionWrapper.getExp().treeCopyNoTransform()))));
				auxiliaryStatements.add(varDeclStmt);
				condition = Utils.getInnerValue(new VarUse(tmpVar), conditionWrapper.getFullType().getTypeUse());
			} else {
				condition = conditionWrapper.getExp().treeCopyNoTransform();
			}
			// convert Int or Pointer to Bool
			if (conditionWrapper.getFullType() == SimpleType.SIGNED_INT_TYPE) {
				condition = Utils.convertIntToBool(condition);
			} else if (conditionWrapper.getFullType() instanceof PointerType) {
				condition = Utils.convertPointerToBool(condition);
			} else {
				throw new IllegalArgumentException("While-condition must be an int or pointer!");
			}

			final StmtWrapper whileBodyWrapper = statementStack.peek().pop();
			final boolean containsReturn = whileBodyWrapper.containsReturn();
			final boolean containsBreak = whileBodyWrapper.containsBreak();
			PureExp flagSet = null;
			if (containsReturn) {
				flagSet = new VarUse(RETURN_FLAG);
			}
			if (containsBreak) {
				final PureExp breakFlagSet = new VarUse(BREAK_FLAG);
				flagSet = flagSet == null ? breakFlagSet : new OrBoolExp(flagSet, breakFlagSet);
			}
			if (flagSet != null) {
				condition = new AndBoolExp(new NegExp(flagSet), condition);
			}
			final Block whileBody = whileBodyWrapper.getBlock();
			if (whileBodyWrapper.containsContinue()) {
				// reset continue flag after end of loop body but before continue block
				final AssignStmt resetContinueFlag = new AssignStmt();
				resetContinueFlag.setVar(new VarUse(CONTINUE_FLAG));
				resetContinueFlag.setValue(Utils.getFalseExp());
				whileBody.addStmt(resetContinueFlag);
			}
			final Block continueBlock;
			if (containsReturn || containsBreak) {
				continueBlock = new Block();
				final IfStmt ifStmt = new IfStmt();
				ifStmt.setCondition(new NegExp(flagSet.treeCopyNoTransform()));
				ifStmt.setThen(continueBlock);
				whileBody.addStmt(ifStmt);
			} else {
				continueBlock = whileBody;
			}
			for (Stmt auxiliaryStmt : auxiliaryStatements) {
				if (auxiliaryStmt instanceof VarDeclStmt) {
					final VarDecl varDecl = ((VarDeclStmt) auxiliaryStmt).getVarDecl();
					final AssignStmt assignStmt = new AssignStmt();
					assignStmt.setVar(new VarUse(varDecl.getName()));
					assignStmt
							.setValue(varDecl.getInitExpOptNoTransform().getChildNoTransform(0).treeCopyNoTransform());
					continueBlock.addStmt(assignStmt);
				} else {
					continueBlock.addStmt(auxiliaryStmt.treeCopyNoTransform());
				}
			}

			// add auxiliary statements
			final java.util.List<Stmt> statements = new ArrayList<>(auxiliaryStatements);
			// add while loop
			final WhileStmt whileStmt = new WhileStmt();
			whileStmt.setCondition(condition);
			whileStmt.setBody(whileBody);
			statements.add(whileStmt);
			if (containsBreak) {
				// reset break flag after leaving loop
				final AssignStmt resetBreakFlag = new AssignStmt();
				resetBreakFlag.setVar(new VarUse(BREAK_FLAG));
				resetBreakFlag.setValue(Utils.getFalseExp());
				statements.add(resetBreakFlag);
			}
			statementStack.peek().push(new StmtWrapper(statements, containsReturn));
		} else if (statement instanceof IASTForStatement) {
			final IASTForStatement forStatement = (IASTForStatement) statement;
			// pop body off statement stack
			final StmtWrapper forBodyWrapper = statementStack.peek().pop();
			final boolean containsReturn = forBodyWrapper.containsReturn();
			final boolean containsBreak = forBodyWrapper.containsBreak();
			// pop init off statement stack
			final StmtWrapper initWrapper = statementStack.peek().pop();
			if (initWrapper.getFlags() != 0) {
				return abortWithMessage("Init statement of for-loop is not allowed to contain return/break/continue!");
			}
			// if not null, pop iterationExpression off expression stack
			final ExpWrapper iterationWrapper;
			if (forStatement.getIterationExpression() == null) {
				iterationWrapper = null;
			} else {
				iterationWrapper = expressionStack.pop();
				addMethodsCalledIn(iterationWrapper);
			}
			// if not null, pop condition off expression stack
			final ExpWrapper conditionWrapper;
			if (forStatement.getConditionExpression() == null) {
				conditionWrapper = null;
			} else {
				conditionWrapper = expressionStack.pop();
				addMethodsCalledIn(conditionWrapper);
			}
			// aux=(init, cond-aux) // init will be added later
			final java.util.List<Stmt> conditionStatements = new ArrayList<>();
			if (conditionWrapper != null) {
				for (final SubExp subExp : conditionWrapper.getSubExpressions()) {
					conditionStatements.add(subExp.getVarDeclStmt());
				}
			}
			Guard guard = null;
			if (conditionWrapper != null && conditionWrapper.isFuture) {
				guard = new ClaimGuard(conditionWrapper.getExp());
			}
			if (conditionWrapper != null) {
				for (final PureExp sideEffect : conditionWrapper.getSideEffects()) {
					final ClaimGuard claimGuard = new ClaimGuard(sideEffect);
					guard = guard == null ? claimGuard : new AndGuard(guard, claimGuard);
				}
			}
			if (guard != null) {
				final AwaitStmt awaitStmt = new AwaitStmt();
				awaitStmt.setGuard(guard);
				conditionStatements.add(awaitStmt);
			}
			PureExp condition;
			if (conditionWrapper == null) {
				condition = Utils.getTrueExp();
			} else {
				if (conditionWrapper.isFuture) {
					final String tmpVar = Utils.generateTmpVarString();
					final VarDeclStmt varDeclStmt = new VarDeclStmt();
					varDeclStmt.setVarDecl(new VarDecl(tmpVar, TypesUtils.getValueTypeUse(),
							new Opt<>(new GetExp(conditionWrapper.getExp().treeCopyNoTransform()))));
					conditionStatements.add(varDeclStmt);
					condition = Utils.getInnerValue(new VarUse(tmpVar), conditionWrapper.getFullType().getTypeUse());
				} else {
					condition = conditionWrapper.getExp().treeCopyNoTransform();
				}
				// convert Int or Pointer to Bool
				if (conditionWrapper.getFullType() == SimpleType.SIGNED_INT_TYPE) {
					condition = Utils.convertIntToBool(condition);
				} else if (conditionWrapper.getFullType() instanceof PointerType) {
					condition = Utils.convertPointerToBool(condition);
				} else {
					throw new IllegalArgumentException("While-condition must be an int or pointer!");
				}
			}
			// stmt=while(!return & !break & cond-exp) for-body;
			PureExp flagSet = null;
			if (containsReturn) {
				flagSet = new VarUse(RETURN_FLAG);
			}
			if (containsBreak) {
				final PureExp breakFlagSet = new VarUse(BREAK_FLAG);
				flagSet = flagSet == null ? breakFlagSet : new OrBoolExp(flagSet, breakFlagSet);
			}
			if (flagSet != null) {
				if (conditionWrapper == null) {
					condition = new NegExp(flagSet);
				} else {
					condition = new AndBoolExp(new NegExp(flagSet), condition);
				}
			}

			// for-body=(body[if(!return & !break & !continue){...}],
			// if (!return & !break) continue-block)
			// continue-block=(iterationExpression-aux, cond-aux[no-decl])
			final Block forBody = forBodyWrapper.getBlock();
			if (forBodyWrapper.containsContinue()) {
				// reset continue flag after end of loop body but before continue block
				final AssignStmt resetContinueFlag = new AssignStmt();
				resetContinueFlag.setVar(new VarUse(CONTINUE_FLAG));
				resetContinueFlag.setValue(Utils.getFalseExp());
				forBody.addStmt(resetContinueFlag);
			}
			final Block continueBlock;
			if (containsReturn || containsBreak) {
				continueBlock = new Block();
				final IfStmt ifStmt = new IfStmt();
				ifStmt.setCondition(new NegExp(flagSet.treeCopyNoTransform()));
				ifStmt.setThen(continueBlock);
				forBody.addStmt(ifStmt);
			} else {
				continueBlock = forBody;
			}
			// add the iteration statements to the continue block first
			if (iterationWrapper != null) {
				for (final SubExp subExp : iterationWrapper.getSubExpressions()) {
					continueBlock.addStmt(subExp.getVarDeclStmt());
				}
				Guard iterationGuard = null;
				if (iterationWrapper.isFuture) {
					iterationGuard = new ClaimGuard(iterationWrapper.getExp());
				}
				for (final PureExp sideEffect : iterationWrapper.getSideEffects()) {
					final ClaimGuard claimGuard = new ClaimGuard(sideEffect);
					iterationGuard = iterationGuard == null ? claimGuard : new AndGuard(iterationGuard, claimGuard);
				}
				if (iterationGuard != null) {
					final AwaitStmt awaitStmt = new AwaitStmt();
					awaitStmt.setGuard(iterationGuard);
					continueBlock.addStmt(awaitStmt);
				}
			}
			// then add the statements needed to update the condition expression
			for (Stmt conditionStmt : conditionStatements) {
				if (conditionStmt instanceof VarDeclStmt) {
					final VarDecl varDecl = ((VarDeclStmt) conditionStmt).getVarDecl();
					final AssignStmt assignStmt = new AssignStmt();
					assignStmt.setVar(new VarUse(varDecl.getName()));
					assignStmt
							.setValue(varDecl.getInitExpOptNoTransform().getChildNoTransform(0).treeCopyNoTransform());
					continueBlock.addStmt(assignStmt);
				} else {
					continueBlock.addStmt(conditionStmt.treeCopyNoTransform());
				}
			}

			// add init statements
			final java.util.List<Stmt> statements = new ArrayList<>(initWrapper.getStatements());
			// add condition statements
			statements.addAll(conditionStatements);
			// add while loop to statements
			final WhileStmt whileStmt = new WhileStmt();
			whileStmt.setCondition(condition);
			whileStmt.setBody(forBody);
			statements.add(whileStmt);
			if (containsBreak) {
				// reset break flag after leaving loop
				final AssignStmt resetBreakFlag = new AssignStmt();
				resetBreakFlag.setVar(new VarUse(BREAK_FLAG));
				resetBreakFlag.setValue(Utils.getFalseExp());
				statements.add(resetBreakFlag);
			}
			statementStack.peek().push(new StmtWrapper(statements, containsReturn));
		} else if (statement instanceof IASTBreakStatement) {
			// set break flag
			methodContainsBreak = true;
			final AssignStmt setBreakFlag = new AssignStmt();
			setBreakFlag.setVar(new VarUse(BREAK_FLAG));
			setBreakFlag.setValue(Utils.getTrueExp());
			statementStack.peek().push(new StmtWrapper(setBreakFlag, StmtWrapper.BREAK_FLAG));
		} else if (statement instanceof IASTContinueStatement) {
			// set continue flag
			methodContainsContinue = true;
			final AssignStmt setContinueFlag = new AssignStmt();
			setContinueFlag.setVar(new VarUse(CONTINUE_FLAG));
			setContinueFlag.setValue(Utils.getTrueExp());
			statementStack.peek().push(new StmtWrapper(setContinueFlag, StmtWrapper.CONTINUE_FLAG));
		} else if (statement instanceof IASTSwitchStatement) {
			return abortWithMessage("Switch statements not yet implemented");
		} else if (statement instanceof IASTGotoStatement) {
			return abortWithMessage("Goto statements not yet implemented (and might never be)");
		} else {
			return abortWithMessage("Unknown statement type: " + statement.getClass());
		}

		return PROCESS_CONTINUE;
	}

	private int populateBlockFromStatementStack(Block block) {
		Block currentBlock = block;
		int containsFlags = 0b000;
		int lastStmtContainsFlags = 0b000;
		for (StmtWrapper stmtWrapper : statementStack.pop()) {
			if (lastStmtContainsFlags != 0b000) {
				// we could have encountered a return/break/continue,
				// so check the flags and skip the remaining statements in this block if any are
				// set
				final Block newBlock = new Block();
				final IfStmt ifStmt = new IfStmt();
				PureExp flagSet = null;
				if ((lastStmtContainsFlags & StmtWrapper.RETURN_FLAG) != 0) {
					flagSet = new VarUse(RETURN_FLAG);
				}
				if ((lastStmtContainsFlags & StmtWrapper.BREAK_FLAG) != 0) {
					final PureExp breakFlagSet = new VarUse(BREAK_FLAG);
					flagSet = flagSet == null ? breakFlagSet : new OrBoolExp(flagSet, breakFlagSet);
				}
				if ((lastStmtContainsFlags & StmtWrapper.CONTINUE_FLAG) != 0) {
					final PureExp continueFlagSet = new VarUse(CONTINUE_FLAG);
					flagSet = flagSet == null ? continueFlagSet : new OrBoolExp(flagSet, continueFlagSet);
				}
				ifStmt.setCondition(new NegExp(flagSet));
				ifStmt.setThen(newBlock);
				currentBlock.addStmt(ifStmt);
				currentBlock = newBlock;
			}
			for (Stmt stmt : stmtWrapper.getStatements()) {
				currentBlock.addStmt(stmt);
			}
			lastStmtContainsFlags = stmtWrapper.getFlags();
			containsFlags = containsFlags | lastStmtContainsFlags;
		}

		return containsFlags;
	}

	public IASTProblem[] getASTProblems() {
		return astProblems;
	}

	public String parse() {
		try {
			// initialize values
			cIdToAbsId.clear();
			cTypeToAbsType.clear();
			statementStack.clear();
			expressionStack.clear();
			typeStack.clear();
			mainInterfaceDecl = null;
			mainClassDecl = null;

			Utils.reset();

			classNameToInterface.clear();

			// create ABS model
			absModel = new Model();
			final CompilationUnit compilationUnit = new CompilationUnit();
			absModel.addCompilationUnitNoTransform(compilationUnit);
			compilationUnit.setName("TestCU");
			// create the main module
			moduleDecl = new ModuleDecl();
			compilationUnit.addModuleDeclNoTransform(moduleDecl);
			moduleDecl.setName(MODULE_NAME);

			// add prerequisites
			Prerequisites.addDeclsTo(moduleDecl);

			// create a main block, so ABS won't complain
			final MainBlock mainBlock = new MainBlock();
			moduleDecl.setBlock(mainBlock);

			// create new top level map for global variables
			cIdToAbsId.push(new HashMap<>());

			try {
				failureMessage = null;
				if (tu.accept(this)) {
					if (!statementStack.isEmpty()) {
						return FAILED_TO_PARSE + "\n\n" + "Not all statements have been processed";
					}
					if (!expressionStack.isEmpty()) {
						return FAILED_TO_PARSE + "\n\n" + "Not all expressions have been processed";
					}
					if (!typeStack.isEmpty()) {
						return FAILED_TO_PARSE + "\n\n" + "Not all types have been processed";
					}

					// populate the main block if a main function found
					if (mainInterfaceDecl != null) {
						// create interface and class to initialize (add them to the model later)
						final String initMethodName = "initM";
						final String initInterfaceName = "InitI";
						final String initClassName = "InitC";
						final String initObjectName = "initO";
						final MethodSig initMethodSig = new MethodSig();
						initMethodSig.setReturnType(TypesUtils.getUnitTypeUse());
						initMethodSig.setName(initMethodName);
						final InterfaceDecl initInterfaceDecl = new InterfaceDecl();
						initInterfaceDecl.setName(initInterfaceName);
						initInterfaceDecl.addBodyNoTransform(initMethodSig);
						final MethodImpl initMethodImpl = new MethodImpl();
						initMethodImpl.setMethodSig(initMethodSig.treeCopyNoTransform());
						final Block initBlock = new Block();
						initMethodImpl.setBlock(initBlock);
						final InterfaceTypeUse initInterfaceTypeUse = new InterfaceTypeUse();
						initInterfaceTypeUse.setName(initInterfaceName);
						final ClassDecl initClassDecl = new ClassDecl();
						initClassDecl.setName(initClassName);
						initClassDecl.addImplementedInterfaceUse(initInterfaceTypeUse);
						initClassDecl.addParamNoTransform(Utils.getGlobalVarsParamDecl());
						initClassDecl.addMethod(initMethodImpl);

						// create empty location list
						final DataConstructorExp nil = new DataConstructorExp();
						nil.setConstructor("Nil");

						// the main block just instantiates an Init object and calls initMethod() on it
						final NewExp newInitExp = new NewExp();
						newInitExp.setClassName(initClassName);
						newInitExp.addParam(nil);
						final VarDeclStmt instantiateInitStmt = new VarDeclStmt();
						instantiateInitStmt.setVarDecl(new VarDecl(initObjectName,
								initInterfaceTypeUse.treeCopyNoTransform(), new Opt<>(newInitExp)));
						mainBlock.addStmt(instantiateInitStmt);
						final AsyncCall callInit = new AsyncCall();
						callInit.setCallee(new VarUse(initObjectName));
						callInit.setMethod(initMethodName);
						final VarDeclStmt callInitStmt = new VarDeclStmt();
						callInitStmt.setVarDecl(new VarDecl(FUTURE_RESULT,
								TypesUtils.getFutureTypeUse(TypesUtils.getUnitTypeUse()), new Opt<>(callInit)));
						mainBlock.addStmt(callInitStmt);
						final AwaitStmt awaitStmt = new AwaitStmt();
						awaitStmt.setGuard(new ClaimGuard(new VarUse(FUTURE_RESULT)));
						mainBlock.addStmt(awaitStmt);

						// populating init block starting here:
						// initialize global vars
						VarOrParamValue.initializeLocationList(new FieldUse(GLOBAL), globalVars, initBlock);

						// create new main class
						final NewExp newExpMain = new NewExp();
						newExpMain.setClassName(mainClassDecl.getName());
						newExpMain.addParamNoTransform(new FieldUse(GLOBAL));
						newExpMain.addParamNoTransform(nil.treeCopyNoTransform());
						final VarDecl varDeclMain = new VarDecl("main", new InterfaceType(mainInterfaceDecl).toUse(),
								new Opt<>(newExpMain));
						final VarDeclStmt varDeclStmtMain = new VarDeclStmt();
						varDeclStmtMain.setVarDecl(varDeclMain);
						initBlock.addStmt(varDeclStmtMain);

						// call main class...
						final Call callExp = new AsyncCall();
						callExp.setCallee(new VarUse("main"));
						callExp.setMethod("call");
						// ... saving result
						final VarDeclStmt varDeclStmtCallMain = new VarDeclStmt();
						varDeclStmtCallMain.setVarDecl(new VarDecl("fv",
								TypesUtils.getFutureTypeUse(TypesUtils.getValueTypeUse()), new Opt<>(callExp)));
						initBlock.addStmt(varDeclStmtCallMain);
						final VarDeclStmt varDeclStmtGet = new VarDeclStmt();
						varDeclStmtGet.setVarDecl(new VarDecl("v", TypesUtils.getValueTypeUse(),
								new Opt<>(new GetExp(new VarUse("fv")))));
						initBlock.addStmt(varDeclStmtGet);

						// print result
						final FnApp intValV = new FnApp();
						intValV.setName(INT_VALUE_SELECTOR);
						intValV.addParam(new VarUse("v"));
						final FnApp toString = new FnApp();
						toString.setName("toString");
						toString.addParamNoTransform(intValV);
						final FnApp println = new FnApp();
						println.setName("println");
						println.addParamNoTransform(toString);
						final ExpressionStmt expressionStmt = new ExpressionStmt();
						expressionStmt.setExp(println);
						initBlock.addStmt(expressionStmt);

						moduleDecl.addDecl(initInterfaceDecl);
						moduleDecl.addDecl(initClassDecl);
					}

					// check the model for problems
					absModel.doFullTraversal();

					// create pretty printed output
					OutputStream out = new ByteArrayOutputStream();
					PrintWriter writer = new PrintWriter(out, true);
					absModel.doPrettyPrint(writer, new DefaultABSFormatter(writer));
					String result = out.toString();
					// clean up indentations
					result = result.replaceAll("  ", "        ").replaceAll(";}", ";\n}");
					// remove module name, as everything is within the same module
					result = result.replaceAll(MODULE_NAME + "\\.", "");

					return result;
				} else if (failureMessage == null) {
					return FAILED_TO_PARSE + "\n\n" + "Failure creating the AST from C code";
				} else {
					return FAILED_TO_PARSE + "\n\n" + failureMessage;
				}
			} catch (Exception e) {
				logger.error("Caught exception while transforming C code into an ABS model", e);
				return FAILED_TO_PARSE + "\n\nException: \n\n" + e.getMessage();
			}
		} catch (Exception e) {
			logger.error("Caught exception while initializing ABS model", e);
			return FAILED_TO_PARSE + "\n\nException: \n\n" + e.getMessage();
		}
	}
}