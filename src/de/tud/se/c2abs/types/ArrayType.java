package de.tud.se.c2abs.types;

import abs.frontend.ast.MultMultExp;
import abs.frontend.ast.PureExp;
import de.tud.se.c2abs.utils.Constants;

public class ArrayType extends FullType implements Constants {

	public static ArrayType create(FullType subscriptType, PureExp length) {
		if (subscriptType instanceof ArrayType) {
			final ArrayType arraySubscriptType = (ArrayType) subscriptType;
			return new ArrayType(arraySubscriptType, arraySubscriptType.innerType, length);
		} else {
			return new ArrayType((NonArrayType) subscriptType, length);
		}
	}

	public final NonArrayType innerType;
	private final ArrayType arraySubscriptType;
	private final PureExp length;

	private ArrayType(NonArrayType innerType, PureExp length) {
		super(ARRAY + "_" + innerType, null, 1);
		
		this.innerType = innerType;
		this.arraySubscriptType = null;
		this.length = length;
	}
	
	private ArrayType(ArrayType arraySubscriptType, NonArrayType innerType, PureExp length) {
		super((arraySubscriptType.dimensions + 1) + "D_" + ARRAY + "_" + innerType,
				null, arraySubscriptType.dimensions + 1);

		this.innerType = innerType;
		this.arraySubscriptType = arraySubscriptType;
		this.length = length;
	}

	@Override
	public PureExp getWitness(boolean initialized) {
		throw new UnsupportedOperationException("Can never use arrays in such a way as to require a witness!");
	}

	@Override
	public NonArrayType getNonArrayType() {
		return innerType;
	}

	public FullType getSubscriptType() {
		if (arraySubscriptType == null) {
			return innerType;
		} else {
			return arraySubscriptType;
		}
	}
	
	public PointerType decay() {
		return new PointerType(getSubscriptType());
	}

	@Override
	public PureExp getSize() {
		return new MultMultExp(length.treeCopyNoTransform(), getSubscriptType().getSize()); 
	}
	
	@Override
	public PureExp getInnerSize() {
		return getSubscriptType().getSize(); 
	}
	
	@Override
	public boolean matches(FullType otherType) {
		if (otherType instanceof ArrayType) {
			final ArrayType arrayType = (ArrayType) otherType;
			return getSubscriptType().matches(arrayType.getSubscriptType());
		} else {
			return false;
		}
	}
	
}
