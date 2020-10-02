package de.tud.se.c2abs.types;

import abs.frontend.ast.PureExp;
import abs.frontend.ast.TypeUse;
import de.tud.se.c2abs.utils.TypesUtils;

public abstract class FullType {

	private final String name;
	private final TypeUse typeUse;
	public final int dimensions;
	
	FullType(String name, TypeUse typeUse, int dimensions) {
		this.name = name;
		this.typeUse = typeUse;
		this.dimensions = dimensions;
	}
	
	public TypeUse getTypeUse() {
		return typeUse.treeCopyNoTransform();
	}

	public TypeUse getFutureTypeUse() {
		return TypesUtils.getFutureTypeUse(typeUse.treeCopyNoTransform());
	}
	
	public abstract PureExp getWitness(boolean initialized);
	
	public abstract NonArrayType getNonArrayType();

	public abstract boolean matches(FullType otherType);
	
	@Override
	public String toString() {
		return name;
	}
	
	public abstract PureExp getSize();

	// override in ArrayType and PointerType
	public PureExp getInnerSize() {
		throw new UnsupportedOperationException("Cannot get the inner size of anything but arrays or pointers!");
	}
}
