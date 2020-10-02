package de.tud.se.c2abs.absid;

import java.util.ArrayList;
import java.util.List;

import abs.frontend.ast.InterfaceTypeUse;
import abs.frontend.ast.ParamDecl;
import de.tud.se.c2abs.Utils;
import de.tud.se.c2abs.types.FullType;
import de.tud.se.c2abs.vars.FormalParameter;

public class AbsFuncId implements AbsId {
	
	private final String name;
	private final FullType returnType;
	private final List<FormalParameter> formalParameters;
	private boolean defined;
	
	public AbsFuncId(FullType returnType, String name, List<FormalParameter> formalParameters, boolean defined) {
		this.name = name;
		this.returnType = returnType;
		this.formalParameters = new ArrayList<FormalParameter>(formalParameters);
		this.defined = defined;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public String getClassName() {
		return Utils.createClassName(name);
	}
	
	public InterfaceTypeUse getInterfaceTypeUse() {
		final InterfaceTypeUse result = new InterfaceTypeUse();
		result.setName(Utils.createInterfaceName(name));
		return result;
	}
	
	public List<FormalParameter> getFormalParameters() {
		return formalParameters;
	}
	
	public FullType getReturnType() {
		return returnType;
	}
	
	public List<ParamDecl> createParamDecls() {
		final List<ParamDecl> result = new ArrayList<>(formalParameters.size());
		for (FormalParameter formalParameter : formalParameters) {
			final ParamDecl paramDecl = new ParamDecl();
			paramDecl.setName(formalParameter.name.toString());
			paramDecl.setAccess(formalParameter.fullType.getTypeUse());
			result.add(paramDecl);
		}
		return result;
	}
	
	public boolean isDefined() {
		return defined;
	}

	@Override
	public FullType getFullType() {
		throw new UnsupportedOperationException("As function pointers are not allowed, function types are never needed");
	}

	public String getCallMethodName() {
		return "call_" + name;
	}

}
