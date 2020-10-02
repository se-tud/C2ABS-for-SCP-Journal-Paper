package de.tud.se.c2abs.utils;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;

import abs.frontend.ast.Access;
import abs.frontend.ast.DataTypeUse;
import abs.frontend.ast.InterfaceTypeUse;
import abs.frontend.ast.ParametricDataTypeUse;
import abs.frontend.ast.TypeUse;
import de.tud.se.c2abs.types.SimpleType;
import de.tud.se.c2abs.types.FullType;

public class TypesUtils implements Constants {

	public static InterfaceTypeUse getMemoryTypeUse() {
		InterfaceTypeUse memTypeUse = new InterfaceTypeUse();
		memTypeUse.setName(MEMORY_INTERFACE);
		return memTypeUse;
	}

	public static DataTypeUse getValueTypeUse() {
		DataTypeUse valTypeUse = new DataTypeUse();
		valTypeUse.setName(VALUE_TYPE);
		return valTypeUse;
	}

	public static DataTypeUse getLocationTypeUse() {
		DataTypeUse locTypeUse = new DataTypeUse();
		locTypeUse.setName(LOCATION_TYPE);
		return locTypeUse;
	}

	public static DataTypeUse getPointerTypeUse() {
		DataTypeUse ptrTypeUse = new DataTypeUse();
		ptrTypeUse.setName(POINTER_TYPE);
		return ptrTypeUse;
	}

	public static boolean isPointerTypeUse(Access type) {
		if (type instanceof DataTypeUse) {
			return POINTER_TYPE.equals(((DataTypeUse) type).getName());
		}
		return false;
	}

	public static DataTypeUse getIntTypeUse() {
		final DataTypeUse dataTypeUse = new DataTypeUse();
		dataTypeUse.setName("Int");
		return dataTypeUse;
	}

	public static boolean isIntTypeUse(final Access type) {
		if (type instanceof DataTypeUse) {
			return "Int".equals(((DataTypeUse) type).getName());
		}
		return false;
	}

	public static DataTypeUse getUnitTypeUse() {
		final DataTypeUse dataTypeUse = new DataTypeUse();
		dataTypeUse.setName("Unit");
		return dataTypeUse;
	}

	public static boolean isUnitTypeUse(final TypeUse type) {
		if (type instanceof DataTypeUse) {
			return "Unit".equals(((DataTypeUse) type).getName());
		}
		return false;
	}

	public static ParametricDataTypeUse getListTypeUse(TypeUse typeUse) {
		ParametricDataTypeUse listTypeUse = new ParametricDataTypeUse();
		listTypeUse.setName("List");
		listTypeUse.addParamNoTransform(typeUse.treeCopyNoTransform());
		return listTypeUse;
	}

	public static ParametricDataTypeUse getFutureTypeUse(TypeUse type) {
		final ParametricDataTypeUse parametricDataTypeUse = new ParametricDataTypeUse();
		parametricDataTypeUse.setName("Fut");
		parametricDataTypeUse.addParamNoTransform(type.treeCopyNoTransform());
		return parametricDataTypeUse;
	}

	public static FullType convertType(IASTSimpleDeclSpecifier simpleDeclSpec) {
		if (simpleDeclSpec.isUnsigned()) {
			throw new UnsupportedOperationException("Currently unsigned types are not allowed");
		}
		switch (simpleDeclSpec.getType()) {
		case IASTSimpleDeclSpecifier.t_int:
			return SimpleType.SIGNED_INT_TYPE;
		case IASTSimpleDeclSpecifier.t_void:
			return SimpleType.VOID_TYPE;
		default:
			throw new UnsupportedOperationException("Currently only int and void types are allowed");
		}
	}

	public static TypeUse returnTypeFor(FullType returnType) {
		if (returnType == SimpleType.VOID_TYPE)
			return TypesUtils.getUnitTypeUse();
		else
			return TypesUtils.getValueTypeUse();
	}

}
