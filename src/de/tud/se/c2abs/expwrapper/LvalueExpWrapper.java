package de.tud.se.c2abs.expwrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import abs.frontend.ast.AddAddExp;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.FnApp;
import abs.frontend.ast.MultMultExp;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.VarUse;
import de.tud.se.c2abs.subexp.ArrayDecaySubExp;
import de.tud.se.c2abs.subexp.DerefSubExp;
import de.tud.se.c2abs.subexp.GetterSubExp;
import de.tud.se.c2abs.subexp.PointerSubscriptSubExp;
import de.tud.se.c2abs.subexp.SubExp;
import de.tud.se.c2abs.subexp.ArraySubscriptSubExp;
import de.tud.se.c2abs.subexp.args.IntArgument;
import de.tud.se.c2abs.subexp.args.LocationArgument;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.types.ArrayType;
import de.tud.se.c2abs.types.FullType;
import de.tud.se.c2abs.types.PointerType;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.vars.VarOrParamDeclaration;

public class LvalueExpWrapper extends ExpWrapper implements Constants {

	// an LvalueExpWrapper:
	// 1. contains an Lvalue, calculating the location loc
	// 2. when used as an expression, it additionally calls this!getValue(loc);

	public static LvalueExpWrapper createFromVar(final VarOrParamDeclaration var) {
		// nth(globalVars, argNr); // or localVars or params
		return createFromLvalue(
				new Lvalue(false, Collections.emptyList(), var.nthExp(), var.getFullType(), Collections.emptyList()));
	}

	public static LvalueExpWrapper createDeref(final ExpWrapper pointer) {
		final FullType derefType = ((PointerType) pointer.getFullType()).innerType;
		final Lvalue lvalue;
		if (pointer.isFuture) {
			final List<SubExp> subExpressions = new ArrayList<>(pointer.getSubExpressions());
			final DerefSubExp derefSubExp = new DerefSubExp(
					ValueArgument.createPointerValueArg(true, POINTER_ARG, pointer.getExp()));
			subExpressions.add(derefSubExp);
			lvalue = new Lvalue(true, subExpressions, new VarUse(derefSubExp.getTmpVar()), derefType,
					pointer.getSideEffects());
		} else {
			// loc(ptrArg)
			final FnApp locExp = new FnApp();
			locExp.setName(LOCATION_SELECTOR);
			locExp.addParamNoTransform(pointer.getExp());
			lvalue = new Lvalue(false, pointer.getSubExpressions(), locExp, derefType, pointer.getSideEffects());
		}
		return createFromLvalue(lvalue);
	}

	public static LvalueExpWrapper createSubscript(final ExpWrapper arrayOrPointer, final ExpWrapper index) {
		final IntArgument size = new IntArgument(SIZE_ARG, arrayOrPointer.getFullType().getInnerSize());
		if (arrayOrPointer instanceof LvalueExpWrapper) {
			final Lvalue lvalue = ((LvalueExpWrapper) arrayOrPointer).lvalue;
			if (lvalue.getFullType() instanceof ArrayType) {
				return createArraySubscript(lvalue, size, index);
			}
		}
		if (arrayOrPointer.getFullType() instanceof PointerType) {
			// p[index] is equal to *(p + index)
			return createPointerSubscript(arrayOrPointer, size, index);
		} else {
			throw new IllegalArgumentException("Can only subscript an array lvalue or pointer expr!");
		}
	}

	private static LvalueExpWrapper createArraySubscript(final Lvalue array, final IntArgument sizeArg,
			final ExpWrapper index) {
		final Lvalue lvalue;
		final List<SubExp> subExpressions = new ArrayList<>(array.getSubExpressions());
		subExpressions.addAll(index.getSubExpressions());
		final List<PureExp> sideEffects = new ArrayList<>(array.getSideEffects());
		sideEffects.addAll(index.getSideEffects());
		final FullType subscriptType = ((ArrayType) array.getFullType()).getSubscriptType();
		if (array.isFuture || index.isFuture) {
			final LocationArgument arrayArg = new LocationArgument(array);
			final ValueArgument indexArg = ValueArgument.createIntValueArg(index.isFuture, INDEX_ARG, index.getExp());
			final ArraySubscriptSubExp arraySubscriptSubExp = new ArraySubscriptSubExp(arrayArg, sizeArg, indexArg);
			subExpressions.add(arraySubscriptSubExp);
			lvalue = new Lvalue(true, subExpressions, new VarUse(arraySubscriptSubExp.getTmpVar()), subscriptType,
					sideEffects);
		} else {
			// Loc(mem(array), offset(array) + (size * index))
			final DataConstructorExp locExp = new DataConstructorExp();
			locExp.setConstructor(LOCATION_CONSTRUCTOR);
			final FnApp memExp = new FnApp();
			memExp.setName(MEMORY_SELECTOR);
			memExp.addParamNoTransform(array.getExp());
			locExp.addParamNoTransform(memExp);
			final FnApp offsetExp = new FnApp();
			offsetExp.setName(OFFSET_SELECTOR);
			offsetExp.addParamNoTransform(array.getExp());
			locExp.addParamNoTransform(
					new AddAddExp(offsetExp, new MultMultExp(sizeArg.getActualArgumentExp(), index.getExp())));
			lvalue = new Lvalue(false, subExpressions, locExp, subscriptType, sideEffects);
		}
		return createFromLvalue(lvalue);
	}

	private static LvalueExpWrapper createPointerSubscript(final ExpWrapper pointer, final IntArgument sizeArg,
			final ExpWrapper index) {
		final Lvalue lvalue;
		final List<SubExp> subExpressions = new ArrayList<>(pointer.getSubExpressions());
		subExpressions.addAll(index.getSubExpressions());
		final List<PureExp> sideEffects = new ArrayList<>(pointer.getSideEffects());
		sideEffects.addAll(index.getSideEffects());
		final FullType subscriptType = ((PointerType) pointer.getFullType()).innerType;
		if (pointer.isFuture || index.isFuture) {
			final ValueArgument pointerArg = ValueArgument.createPointerValueArg(pointer.isFuture, POINTER_ARG,
					pointer.getExp());
			final ValueArgument indexArg = ValueArgument.createIntValueArg(index.isFuture, INDEX_ARG, index.getExp());
			final PointerSubscriptSubExp pointerSubscriptSubExp = new PointerSubscriptSubExp(pointerArg, sizeArg,
					indexArg);
			subExpressions.add(pointerSubscriptSubExp);
			lvalue = new Lvalue(true, subExpressions, new VarUse(pointerSubscriptSubExp.getTmpVar()), subscriptType,
					sideEffects);
		} else {
			// Loc(mem(loc(ptr)), offset(loc(ptr)) + (size * index))
			final DataConstructorExp newLocExp = new DataConstructorExp();
			newLocExp.setConstructor(LOCATION_CONSTRUCTOR);
			final FnApp memExp = new FnApp();
			memExp.setName(MEMORY_SELECTOR);
			final FnApp locSelExp = new FnApp();
			locSelExp.setName(LOCATION_SELECTOR);
			locSelExp.addParamNoTransform(pointer.getExp());
			memExp.addParamNoTransform(locSelExp);
			newLocExp.addParamNoTransform(memExp);
			final FnApp offsetExp = new FnApp();
			offsetExp.setName(OFFSET_SELECTOR);
			offsetExp.addParamNoTransform(locSelExp.treeCopyNoTransform());
			newLocExp.addParamNoTransform(
					new AddAddExp(offsetExp, new MultMultExp(sizeArg.getActualArgumentExp(), index.getExp())));
			lvalue = new Lvalue(false, subExpressions, newLocExp, subscriptType, sideEffects);
		}
		return createFromLvalue(lvalue);
	}

	private static LvalueExpWrapper createFromLvalue(final Lvalue lvalue) {
		if (lvalue.getFullType() instanceof ArrayType) {
			final PointerType decayType = ((ArrayType) lvalue.getFullType()).decay();
			// arrays are not read, but rather decay to pointers
			if (lvalue.isFuture) {
				final List<SubExp> subExpressions = new ArrayList<>(lvalue.getSubExpressions());
				final ArrayDecaySubExp arrayDecaySubExp = new ArrayDecaySubExp(new LocationArgument(lvalue));
				subExpressions.add(arrayDecaySubExp);
				return new LvalueExpWrapper(true, lvalue, subExpressions, new VarUse(arrayDecaySubExp.getTmpVar()),
						decayType, lvalue.getSideEffects());
			} else {
				// (loc : Array<tau>) -> (Ptr(loc) : Pointer<tau>)
				final DataConstructorExp ptrExp = new DataConstructorExp();
				ptrExp.setConstructor(POINTER_CONSTRUCTOR);
				ptrExp.addParamNoTransform(lvalue.getExp());
				return new LvalueExpWrapper(false, lvalue, lvalue.getSubExpressions(), ptrExp, decayType,
						lvalue.getSideEffects());
			}
		}
		// Fut<Val> fv = this!getVal(loc);
		final GetterSubExp getterSubExp = new GetterSubExp(new LocationArgument(lvalue));
		final List<SubExp> subExpressions = new ArrayList<>(lvalue.getSubExpressions());
		subExpressions.add(getterSubExp);
		return new LvalueExpWrapper(true, lvalue, subExpressions, new VarUse(getterSubExp.getTmpVar()),
				lvalue.getFullType(), lvalue.getSideEffects());
	}

	public final Lvalue lvalue;

	private LvalueExpWrapper(final boolean isFuture, final Lvalue lvalue, final List<SubExp> subExpressions,
			final PureExp innerExp, final FullType type, final List<PureExp> sideEffects) {

		super(isFuture, subExpressions, innerExp, type, sideEffects);

		this.lvalue = lvalue;
	}
}
