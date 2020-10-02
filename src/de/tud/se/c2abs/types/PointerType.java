package de.tud.se.c2abs.types;

import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.IntLiteral;
import abs.frontend.ast.PureExp;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class PointerType extends NonArrayType implements Constants {

	public final FullType innerType;
	
	public PointerType(FullType innerType, boolean isConst) {
		this(POINTER_ + innerType, innerType);
	}
	
	public PointerType(FullType innerType) {
		this(POINTER_ + innerType, innerType);
	}
	
	private PointerType(String name, FullType innerType) {
		super(name, TypesUtils.getPointerTypeUse());
		
		this.innerType = innerType;
	}
	
	@Override
	public PureExp getWitness(boolean initialized) {
		final DataConstructorExp nullPtr = new DataConstructorExp();
		nullPtr.setConstructor(NULL_POINTER_CONSTRUCTOR);
		return Utils.createValue(nullPtr, TypesUtils.getPointerTypeUse());
	}

	@Override
	public PureExp getSize() {
		return new IntLiteral("1");
	}
	
	@Override
	public PureExp getInnerSize() {
		return innerType.getSize(); 
	}

	@Override
	public boolean matches(FullType otherType) {
		if (otherType instanceof PointerType) {
			return innerType.matches(((PointerType) otherType).innerType);
		} else {
			return false;
		}
	}
}
