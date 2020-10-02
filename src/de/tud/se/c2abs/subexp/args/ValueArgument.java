package de.tud.se.c2abs.subexp.args;

import abs.frontend.ast.ClaimGuard;
import abs.frontend.ast.GetExp;
import abs.frontend.ast.Guard;
import abs.frontend.ast.Opt;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.TypeUse;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.expwrapper.ExpWrapper;
import de.tud.se.c2abs.types.PointerType;
import de.tud.se.c2abs.types.SimpleType;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class ValueArgument extends Argument implements Constants {
	
	public static enum Type { INT_VALUE, POINTER_VALUE }

	public static ValueArgument create(String name, ExpWrapper expWrapper) {
		if (expWrapper.getFullType() == SimpleType.SIGNED_INT_TYPE)
			return createIntValueArg(expWrapper.isFuture, name, expWrapper.getExp());
		else if (expWrapper.getFullType() instanceof PointerType)
			return createPointerValueArg(expWrapper.isFuture, name, expWrapper.getExp());
		else
			throw new IllegalArgumentException("A value argument may only be an int or pointer value!");
	}
	
	public static ValueArgument createIntValueArg(boolean isFuture, String name, PureExp pureExp) {
		return new ValueArgument(isFuture, Type.INT_VALUE, name, pureExp);
	}
	
	public static ValueArgument createPointerValueArg(boolean isFuture, String name, PureExp pureExp) {
		return new ValueArgument(isFuture, Type.POINTER_VALUE, name, pureExp);
	}

	public final Type type;
	public final boolean isFuture;
	private final String name;

	private ValueArgument(boolean isFuture, Type type, String name, PureExp pureExp) {
		super(pureExp);
		
		this.isFuture = isFuture;
		this.type = type;
		this.name = name;
		
		if (type == null)
			throw new IllegalArgumentException("Type may not be null");
	}

	@Override
	public ParamDecl getParamDecl() {
		final ParamDecl paramDecl = new ParamDecl();
		paramDecl.setAccess(
				isFuture ? TypesUtils.getFutureTypeUse(TypesUtils.getValueTypeUse()) : getInnerTypeUse());
		paramDecl.setName(isFuture ? FUT_ + name : name);
		return paramDecl;
	}

	@Override
	public Guard getGuard() {
		return isFuture ? new ClaimGuard(new VarUse(FUT_ + name)) : null;
	}

	@Override
	public VarDeclStmt getVarDeclStmt() {
		if (isFuture) {
			final VarDeclStmt varDeclStmt = new VarDeclStmt();
			varDeclStmt.setVarDecl(new VarDecl(name, TypesUtils.getValueTypeUse(),
					new Opt<>(new GetExp(new VarUse(FUT_ + name)))));
			return varDeclStmt;
		}
		return null;
	}

	
	public PureExp getValueExp() {
		return isFuture ? new VarUse(name) : Utils.createValue(new VarUse(name), getInnerTypeUse());
	}

	public PureExp getInnerExp() {
		return isFuture ? Utils.getInnerValue(new VarUse(name), getInnerTypeUse()) : new VarUse(name);
	}

	@Override
	public String getMethodNameSuffix() {
		return isFuture ? _FUT : _VAL;
	}
	
	public TypeUse getInnerTypeUse() {
		switch (type) {
		case INT_VALUE:
			return TypesUtils.getIntTypeUse();
		case POINTER_VALUE:
			return TypesUtils.getPointerTypeUse();
		}
		throw new IllegalStateException("Type may not be null");
	}

}
