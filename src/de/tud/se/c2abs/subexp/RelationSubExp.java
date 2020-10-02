package de.tud.se.c2abs.subexp;

import java.util.Arrays;

import abs.frontend.ast.Block;
import abs.frontend.ast.EqExp;
import abs.frontend.ast.GTEQExp;
import abs.frontend.ast.GTExp;
import abs.frontend.ast.LTEQExp;
import abs.frontend.ast.LTExp;
import abs.frontend.ast.NotEqExp;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.ReturnStmt;
import abs.frontend.ast.TypeUse;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class RelationSubExp extends SubExp implements Constants {

	private static String createMethodPrefix(String opName, TypeUse typeUse) {
		final String cmpOp = COMPARE_ + opName;
		if (TypesUtils.isIntTypeUse(typeUse))
			return cmpOp + "_int";
		else if (TypesUtils.isPointerTypeUse(typeUse))
			return cmpOp + "_ptr";
		throw new IllegalArgumentException("Cannot compare type: " + typeUse);
	}

	private final String opName;
	private final ValueArgument arg1;
	private final ValueArgument arg2;

	public RelationSubExp(String opName, ValueArgument arg1, ValueArgument arg2) {
		super(TypesUtils.getValueTypeUse(), createMethodPrefix(opName, arg1.getInnerTypeUse()), Arrays.asList(arg1, arg2), null);

		this.opName = opName;
		this.arg1 = arg1;
		this.arg2 = arg2;
	}

	@Override
	public void populateMethodImplBlock(Block block) {
		final PureExp returnExp;
		final PureExp exp1 = arg1.getValueExp();
		final PureExp exp2 = arg2.getValueExp();
		switch (opName) {
		case EQUALS:
			returnExp = Utils.convertBoolToInt(new EqExp(exp1, exp2));
			break;
		case GREATER_EQUALS:
			returnExp = Utils.convertBoolToInt(new GTEQExp(exp1, exp2));
			break;
		case GREATER_THAN:
			returnExp = Utils.convertBoolToInt(new GTExp(exp1, exp2));
			break;
		case LESS_EQUALS:
			returnExp = Utils.convertBoolToInt(new LTEQExp(exp1, exp2));
			break;
		case LESS_THAN:
			returnExp = Utils.convertBoolToInt(new LTExp(exp1, exp2));
			break;
		case NOT_EQUALS:
			returnExp = Utils.convertBoolToInt(new NotEqExp(exp1, exp2));
			break;
		default:
			throw new UnsupportedOperationException("opName not in switch statement: " + opName);
		}
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(Utils.createValue(returnExp, TypesUtils.getIntTypeUse()));
		block.addStmt(returnStmt);
	}
}
