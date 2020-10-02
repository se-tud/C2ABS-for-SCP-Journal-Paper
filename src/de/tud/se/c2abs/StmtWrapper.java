package de.tud.se.c2abs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import abs.frontend.ast.Block;
import abs.frontend.ast.Stmt;

public class StmtWrapper {
	public static final int RETURN_FLAG = 0b001;
	public static final int BREAK_FLAG = 0b010;
	public static final int CONTINUE_FLAG = 0b100;
	
	private static List<Stmt> combine(final List<Stmt> auxiliaryStatements, final Stmt stmt) {
		if (auxiliaryStatements.isEmpty()) {
			return Collections.singletonList(stmt);
		}
		final List<Stmt> statements = new ArrayList<>(auxiliaryStatements);
		statements.add(stmt);
		return statements;
	}
	
	private final List<Stmt> statements;
	private final int containsFlags;
	
	public StmtWrapper(final Stmt stmt) {
		this(Collections.singletonList(stmt), false);
	}
	
	public StmtWrapper(final List<Stmt> auxiliaryStatements, final Stmt stmt) {
		this(combine(auxiliaryStatements, stmt), false);
	}
	
	public StmtWrapper(final Stmt stmt, final boolean containsReturn) {
		this(Collections.singletonList(stmt), containsReturn);
	}
	
	public StmtWrapper(final Stmt stmt, final int containsFlags) {
		this(Collections.singletonList(stmt), containsFlags);
	}
	
	public StmtWrapper(final List<Stmt> auxiliaryStatements, final Stmt stmt, final boolean containsReturn) {
		this(combine(auxiliaryStatements, stmt), containsReturn);
	}
	
	public StmtWrapper(final List<Stmt> auxiliaryStatements, final Stmt stmt, final int containsFlags) {
		this(combine(auxiliaryStatements, stmt), containsFlags);
	}
	
	public StmtWrapper(final List<Stmt> statements) {
		this(statements, false);
	}
	
	public StmtWrapper(final List<Stmt> statements, final boolean containsReturn) {
		this(statements, containsReturn ? RETURN_FLAG : 0);
	}
	
	public StmtWrapper(final List<Stmt> statements, final int containsFlags) {
		this.statements = statements;
		this.containsFlags = containsFlags;
	}
	
	public List<Stmt> getStatements() {
		return statements;
	}
	
	public boolean containsReturn() {
		return (containsFlags & RETURN_FLAG) != 0;
	}
	
	public boolean containsBreak() {
		return (containsFlags & BREAK_FLAG) != 0;
	}
	
	public boolean containsContinue() {
		return (containsFlags & CONTINUE_FLAG) != 0;
	}
	
	public int getFlags() {
		return containsFlags;
	}
	
	public Block getBlock() {
		if (statements.size() == 1 && statements.get(0) instanceof Block) {
			return (Block) statements.get(0);
		}
		final Block block = new Block();
		for (final Stmt stmt : statements) {
			block.addStmt(stmt);
		}
		return block;
	}
}
