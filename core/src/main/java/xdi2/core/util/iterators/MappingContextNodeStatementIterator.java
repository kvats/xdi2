package xdi2.core.util.iterators;

import java.util.Arrays;
import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.Statement.ContextNodeStatement;

/**
 * A MappingIterator that maps XDI context nodes to their statements.
 * 
 * @author markus
 */
public class MappingContextNodeStatementIterator extends MappingIterator<ContextNode, ContextNodeStatement> {

	public MappingContextNodeStatementIterator(Iterator<ContextNode> contextNodes) {

		super(contextNodes);
	}

	public MappingContextNodeStatementIterator(ContextNode contextNode) {

		super(Arrays.asList(new ContextNode[] { contextNode }).iterator());
	}

	@Override
	public ContextNodeStatement map(ContextNode contextNode) {

		return contextNode.getStatement();
	}
}
