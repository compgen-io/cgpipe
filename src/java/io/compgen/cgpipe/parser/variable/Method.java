package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.MethodCallException;

public interface Method {
	public VarValue call(VarValue[] args) throws MethodCallException;
}
