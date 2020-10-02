package de.tud.se.c2abs.expwrapper;

import java.util.ArrayList;
import java.util.List;

import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.subexp.AddressOfSubExp;
import de.tud.se.c2abs.subexp.SubExp;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.types.FullType;
import de.tud.se.c2abs.types.PointerType;
import de.tud.se.c2abs.utils.Constants;

public class Lvalue extends ExpWrapper implements Constants {

	public Lvalue(boolean isFuture, List<SubExp> subExpressions, PureExp exp, FullType fullType,
			List<PureExp> sideEffects) {

		super(isFuture, subExpressions, exp, fullType, sideEffects);
	}

	public ExpWrapper addressOf() {
		if (isFuture) {
			final List<SubExp> subExpressions = new ArrayList<>(getSubExpressions());
			final AddressOfSubExp addressOfSubExp = new AddressOfSubExp(new LocationArgument(this));
			subExpressions.add(addressOfSubExp);
			return new ExpWrapper(true, subExpressions, new VarUse(addressOfSubExp.getTmpVar()),
					new PointerType(getFullType()), getSideEffects());
		} else {
			final DataConstructorExp ptrExp = new DataConstructorExp();
			ptrExp.setConstructor(POINTER_CONSTRUCTOR);
			ptrExp.addParamNoTransform(getExp());
			return new ExpWrapper(false, getSubExpressions(), ptrExp, new PointerType(getFullType()), getSideEffects());
		}
	}

}
