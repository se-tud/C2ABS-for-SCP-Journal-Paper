package de.tud.se.c2abs.subexp;

import java.util.Arrays;
import java.util.List;

import abs.frontend.ast.AddAddExp;
import abs.frontend.ast.Block;
import abs.frontend.ast.MinusExp;
import abs.frontend.ast.MultMultExp;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.ReturnStmt;
import abs.frontend.ast.SubAddExp;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.subexp.args.ValueArgument;
import de.tud.se.c2abs.utils.Constants;
import de.tud.se.c2abs.utils.TypesUtils;

public class IntArithmeticSubExp extends SubExp implements Constants {
	
	private static String createMethodPrefix(String opName) {
		return OP_ + opName;
	}

	private final String opName;
	private final List<ValueArgument> arguments;

	public IntArithmeticSubExp(String opName, ValueArgument... arguments) {
		this(opName, Arrays.asList(arguments));
	}

	private IntArithmeticSubExp(String opName, List<ValueArgument> arguments) {
		super(TypesUtils.getValueTypeUse(), createMethodPrefix(opName), arguments, null);

		this.opName = opName;
		this.arguments = arguments;
	}

	@Override
	public void populateMethodImplBlock(Block block) {
		final PureExp returnExp;
		switch (opName) {
		case UNARY_MINUS:
			returnExp = new MinusExp(arguments.get(0).getInnerExp());
			break;
		case NOT:
			returnExp = Utils.negateInt(arguments.get(0).getInnerExp());
			break;
		default:
			final PureExp exp1 = arguments.get(0).getInnerExp();
			final PureExp exp2 = arguments.get(1).getInnerExp();
			switch (opName) {
			case PLUS:
				returnExp = new AddAddExp(exp1, exp2);
				break;
			case MINUS:
				returnExp = new SubAddExp(exp1, exp2);
				break;
			case MULT:
				returnExp = new MultMultExp(exp1, exp2);
				break;
			default:
				throw new UnsupportedOperationException("opName not in switch statement: " + opName);
			}
			break;
		}
		final ReturnStmt returnStmt = new ReturnStmt();
		returnStmt.setRetExp(Utils.createValue(returnExp, TypesUtils.getIntTypeUse()));
		block.addStmt(returnStmt);
	}
}
