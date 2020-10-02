package de.tud.se.c2abs.utils;

public interface Constants {
	
	// Prerequisite class, data type, method and function names
	public static final String POINTER_TYPE = "Ptr";
	public static final String NULL_POINTER_CONSTRUCTOR = "NullPtr";
	public static final String POINTER_CONSTRUCTOR = "Ptr";
	public static final String LOCATION_SELECTOR = "loc";
	public static final String LOCATION_TYPE = "Loc";
	public static final String LOCATION_CONSTRUCTOR = "Loc";
	public static final String MEMORY_SELECTOR = "mem";
	public static final String OFFSET_SELECTOR = "offset";
	public static final String MEMORY_INTERFACE = "Mem";
	public static final String GET_VALUE_METHOD = "getVal";
	public static final String OFFSET_PARAM = "offset";
	public static final String SET_VALUE_METHOD = "setVal";
	public static final String SET_INT_VALUE_METHOD = "setIntVal";
	public static final String SET_POINTER_VALUE_METHOD = "setPtrVal";
	public static final String NEW_VALUE_PARAM = "newVal";
	public static final String MEMORY_CLASS = "Mem";
	public static final String VALUES_PARAM = "vals";
	public static final String VALUE_TYPE = "Val";
	public static final String INT_VALUE_CONSTRUCTOR = "IntVal";
	public static final String INT_VALUE_SELECTOR = "getInt";
	public static final String POINTER_VALUE_CONSTRUCTOR = "PtrVal";
	public static final String POINTER_VALUE_SELECTOR = "getPtr";
	public static final String REPLACE_FUNCTION = "replace";
	
	public static final String POINTER_ARG = "ptr";
	public static final String SIZE_ARG = "size";
	public static final String DECAY = "decay";
	public static final String INDEX_ARG = "index";
	
	// unary operators
	public static final String UNARY_MINUS = "unary_minus";
	public static final String NOT = "not";
	// binary arithmetic
	public static final String PLUS = "plus";
	public static final String MINUS = "minus";
	public static final String MULT = "mult";
	// binary relations
	public static final String EQUALS = "eq";
	public static final String GREATER_EQUALS = "ge";
	public static final String GREATER_THAN = "gt";
	public static final String LESS_EQUALS = "le";
	public static final String LESS_THAN = "lt";
	public static final String NOT_EQUALS = "ne";
	
	// pointer arithmetic operations
	public static final String POINTER_PLUS = "pointerPlus";
	public static final String POINTER_MINUS = "pointerMinus";
	public static final String OFFSET = "offset";

	public static final String VALUE = "value";
	public static final String ARG = "arg";
	public static final String ARG1 = "arg1";
	public static final String ARG2 = "arg2";
	public static final String LOCATION_ARG = "loc";
	public static final String DEREF = "deref";
	public static final String RESULT = "result";
	public static final String FUTURE_RESULT = "futureResult";
	public static final String GLOBAL = "globalVars";
	public static final String LOCAL = "localVars";
	public static final String PARAMS = "params";
	public static final String ARRAY = "Array";

	public static final String TMP_ = "tmp_";
	public static final String OP_ = "op_";
	public static final String COMPARE_ = "cmp_";
	public static final String POINTER_ = "Pointer_";
	public static final String GET_ = "get_";
	public static final String GLOBAL_ = "global_";
	public static final String SET_ = "set_";
	public static final String ADDRESS_OF = "address_of";
	public static final String SUBSCRIPT = "subscript";
	public static final String POINTER_SUBSCRIPT = "pointerSubscript";
	public static final String FUT_ = "fut_";
	public static final String VAL_ = "val_";
	public static final String SIDE_EFFECT = "se";
	public static final String SIDE_EFFECT_ = "se_";
	public static final String FUNCTION_ = "Function_";
	public static final String LOCAL_VARS_ = "LocalVars_";

	public static final String _FUT = "_fut";
	public static final String _VAL = "_val";

	public static final String FUT_VALUE = FUT_ + VALUE;
	public static final String FUT_ARG = FUT_ + ARG;
	public static final String FUT_ARG1 = FUT_ + ARG1;
	public static final String FUT_ARG2 = FUT_ + ARG2;
	public static final String GET_DEREF = GET_ + DEREF;
	public static final String SET_DEREF = SET_ + DEREF;
	public static final String GET_GLOBAL_ = GET_ + GLOBAL_;
	public static final String SET_GLOBAL_ = SET_ + GLOBAL_;
	
}
