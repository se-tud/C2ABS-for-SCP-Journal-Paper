package de.tud.se.c2abs.types;

import abs.frontend.ast.IntLiteral;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.TypeUse;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.utils.TypesUtils;

public class SimpleType extends NonArrayType {

	public static final SimpleType SIGNED_INT_TYPE = new SimpleType("Int", TypesUtils.getIntTypeUse(),
			Utils.createValue(new IntLiteral("0"), TypesUtils.getIntTypeUse()));
	public static final SimpleType BOOL_TYPE = new SimpleType("Bool", Utils.getBoolType(), Utils.getFalseExp());
	public static final SimpleType VOID_TYPE = new SimpleType("Unit", TypesUtils.getUnitTypeUse(), null);

	private final PureExp witness;

	private SimpleType(String name, TypeUse type, PureExp witness) {
		super(name, type);
		this.witness = witness;
	}

	@Override
	public PureExp getWitness(boolean initialized) {
		return witness.treeCopyNoTransform();
	}

	@Override
	public PureExp getSize() {
		return new IntLiteral("1");
	}

	@Override
	public boolean matches(FullType otherType) {
		return this == otherType;
	}

}
