package de.tud.se.c2abs.types;

import abs.frontend.ast.TypeUse;

public abstract class NonArrayType extends FullType {

	public NonArrayType(String name, TypeUse type) {
		super(name, type, 0);
	}
	
	@Override
	public NonArrayType getNonArrayType() {
		return this;
	}
}
