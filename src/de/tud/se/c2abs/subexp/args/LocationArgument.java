package de.tud.se.c2abs.subexp.args;

import abs.frontend.ast.ClaimGuard;
import abs.frontend.ast.GetExp;
import abs.frontend.ast.Guard;
import abs.frontend.ast.Opt;
import abs.frontend.ast.ParamDecl;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarDecl;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.expwrapper.Lvalue;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class LocationArgument extends Argument implements Constants {

	private final boolean isFuture;
	
	public LocationArgument(Lvalue lvalue) {
		this(lvalue.isFuture, lvalue.getExp());
	}

	private LocationArgument(boolean isFuture, PureExp pureExp) {
		super(pureExp);
		
		this.isFuture = isFuture;
	}

	@Override
	public ParamDecl getParamDecl() {
		final ParamDecl paramDecl = new ParamDecl();
		paramDecl.setAccess(
				isFuture ? TypesUtils.getFutureTypeUse(TypesUtils.getLocationTypeUse()) : TypesUtils.getLocationTypeUse());
		paramDecl.setName(isFuture ? FUT_ + LOCATION_ARG : LOCATION_ARG);
		return paramDecl;
	}

	@Override
	public Guard getGuard() {
		return isFuture ? new ClaimGuard(new VarUse(FUT_ + LOCATION_ARG)) : null;
	}

	@Override
	public VarDeclStmt getVarDeclStmt() {
		if (isFuture) {
			final VarDeclStmt varDeclStmt = new VarDeclStmt();
			varDeclStmt.setVarDecl(new VarDecl(LOCATION_ARG, TypesUtils.getLocationTypeUse(),
					new Opt<>(new GetExp(new VarUse(FUT_ + LOCATION_ARG)))));
			return varDeclStmt;
		}
		return null;
	}

	public VarUse getVarUse() {
		return new VarUse(LOCATION_ARG);
	}

	@Override
	public String getMethodNameSuffix() {
		return isFuture ? _FUT : _VAL;
	}
}
