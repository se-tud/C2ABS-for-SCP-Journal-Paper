package de.tud.se.c2abs.expwrapper;

import java.util.ArrayList;
import java.util.List;

import abs.frontend.ast.AndGuard;
import abs.frontend.ast.AwaitStmt;
import abs.frontend.ast.ClaimGuard;
import abs.frontend.ast.Guard;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.Stmt;
import de.tud.se.c2abs.subexp.SubExp;
import de.tud.se.c2abs.types.FullType;

public class ExpWrapper {

	public final boolean isFuture;
	private final List<SubExp> subExpressions;
	private final PureExp exp;
	private final FullType fullType;
	private final List<PureExp> sideEffects; // tmp vars of future type

	public ExpWrapper(boolean isFuture, List<SubExp> subExpressions, PureExp exp, FullType fullType,
			List<PureExp> sideEffects) {

		this.isFuture = isFuture;
		this.subExpressions = subExpressions;
		this.exp = exp;
		this.fullType = fullType;
		this.sideEffects = sideEffects;
	}

	public List<SubExp> getSubExpressions() {
		return subExpressions;
	}

	public PureExp getExp() {
		return exp.treeCopyNoTransform();
	}

	public FullType getFullType() {
		return fullType;
	}

	public List<PureExp> getSideEffects() {
		return sideEffects;
	}

	public List<Stmt> extractStatements() {
		final List<Stmt> statements = new ArrayList<>();
		if (!subExpressions.isEmpty()) {
			for (final SubExp subExp : subExpressions) {
				statements.add(subExp.getVarDeclStmt());
			}
		}

		// initialize guard
		Guard guard = isFuture ? new ClaimGuard(getExp()) : null;

		// add await evaluation of side effects to guard
		for (final PureExp sideEffect : sideEffects) {
			final ClaimGuard claimGuard = new ClaimGuard(sideEffect);
			guard = guard == null ? claimGuard : new AndGuard(guard, claimGuard);
		}

		if (guard != null) {
			// add await statement
			final AwaitStmt awaitStmt = new AwaitStmt();
			awaitStmt.setGuard(guard);
			statements.add(awaitStmt);
		}

		return statements;
	}

}
