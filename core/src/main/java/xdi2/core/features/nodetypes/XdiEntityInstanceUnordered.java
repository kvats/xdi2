package xdi2.core.features.nodetypes;

import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.syntax.XDIXRef;
import xdi2.core.util.GraphUtil;
import xdi2.core.util.iterators.MappingIterator;
import xdi2.core.util.iterators.NotNullIterator;

/**
 * An XDI unordered entity instance, represented as a context node.
 * 
 * @author markus
 */
public class XdiEntityInstanceUnordered extends XdiAbstractEntity implements XdiInstanceUnordered<XdiEntityCollection, XdiEntity, XdiEntityCollection, XdiEntityInstanceUnordered, XdiEntityInstanceOrdered, XdiEntityInstance>, XdiEntityInstance {

	private static final long serialVersionUID = 1027868266675630350L;

	protected XdiEntityInstanceUnordered(ContextNode contextNode) {

		super(contextNode);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if a context node is a valid XDI unordered entity instance.
	 * @param contextNode The context node to check.
	 * @return True if the context node is a valid XDI unordered entity instance.
	 */
	public static boolean isValid(ContextNode contextNode) {

		if (contextNode == null) throw new NullPointerException();

		if (contextNode.getXDIArc() == null || ! isValidXDIArc(contextNode.getXDIArc())) return false;
		if (contextNode.getContextNode() != null && XdiAttributeCollection.isValid(contextNode.getContextNode())) return false;
		if (contextNode.getContextNode() != null && XdiAbstractAttribute.isValid(contextNode.getContextNode())) return false;

		return true;
	}

	/**
	 * Factory method that creates an XDI unordered entity instance bound to a given context node.
	 * @param contextNode The context node that is an XDI unordered entity instance.
	 * @return The XDI unordered entity instance.
	 */
	public static XdiEntityInstanceUnordered fromContextNode(ContextNode contextNode) {

		if (contextNode == null) throw new NullPointerException();

		if (! isValid(contextNode)) return null;

		if (contextNode.getXDIArc().isDefinition() && contextNode.getXDIArc().isVariable()) return new Definition.Variable(contextNode);
		if (contextNode.getXDIArc().isDefinition() && ! contextNode.getXDIArc().isVariable()) return new Definition(contextNode);
		if (! contextNode.getXDIArc().isDefinition() && contextNode.getXDIArc().isVariable()) return new Variable(contextNode);
		return new XdiEntityInstanceUnordered(contextNode);
	}

	public static XdiEntityInstanceUnordered fromXDIAddress(XDIAddress XDIaddress) {

		return fromContextNode(GraphUtil.contextNodeFromComponents(XDIaddress));
	}

	/*
	 * Instance methods
	 */

	/**
	 * Returns the parent XDI collection of this XDI unordered entity instance.
	 * @return The parent XDI collection.
	 */
	@Override
	public XdiEntityCollection getXdiCollection() {

		return XdiEntityCollection.fromContextNode(this.getContextNode().getContextNode());
	}

	/*
	 * Methods for arcs
	 */

	public static XDIArc createXDIArc(boolean immutable, boolean relative, String literal, XDIXRef xref) {

		return XdiAbstractInstanceUnordered.createXDIArc(false, immutable, relative, literal, xref);
	}

	public static XDIArc createXDIArc(boolean immutable, boolean relative, String literal) {

		return createXDIArc(immutable, relative, literal, null);
	}

	public static XDIArc createXDIArc(String literal) {

		return createXDIArc(true, false, literal);
	}

	public static XDIArc createXDIArc() {

		return createXDIArc((String) null);
	}

	public static XDIArc createXDIArc(XDIArc XDIarc) {

		return createXDIArc(
				XDIarc.isImmutable(), 
				XDIarc.isRelative(), 
				XDIarc.getLiteral(), 
				XDIarc.getXRef());
	}

	public static boolean isValidXDIArc(XDIArc XDIarc) {

		if (XDIarc == null) throw new NullPointerException();

		if (! XdiAbstractInstanceUnordered.isValidXDIArc(XDIarc, false)) return false;

		return true;
	}

	/*
	 * Definition and Variable classes
	 */

	public static class Definition extends XdiEntityInstanceUnordered implements XdiDefinition<XdiEntity> {

		private static final long serialVersionUID = 517841074677834425L;

		private Definition(ContextNode contextNode) {

			super(contextNode);
		}

		public static boolean isValid(ContextNode contextNode) {

			return XdiEntityInstanceUnordered.isValid(contextNode) &&
					contextNode.getXDIArc().isDefinition() &&
					! contextNode.getXDIArc().isVariable();
		}

		public static Definition fromContextNode(ContextNode contextNode) {

			if (contextNode == null) throw new NullPointerException();

			if (! isValid(contextNode)) return null;

			return new Definition(contextNode);
		}

		public static class Variable extends Definition implements XdiVariable<XdiEntity> {

			private static final long serialVersionUID = -7805697920934511410L;

			private Variable(ContextNode contextNode) {

				super(contextNode);
			}

			public static boolean isValid(ContextNode contextNode) {

				return XdiEntityInstanceUnordered.isValid(contextNode) &&
						contextNode.getXDIArc().isDefinition() &&
						contextNode.getXDIArc().isVariable();
			}

			public static Variable fromContextNode(ContextNode contextNode) {

				if (contextNode == null) throw new NullPointerException();

				if (! isValid(contextNode)) return null;

				return new Variable(contextNode);
			}
		}
	}

	public static class Variable extends XdiEntityInstanceUnordered implements XdiVariable<XdiEntity> {

		private static final long serialVersionUID = -3855592195407455246L;

		private Variable(ContextNode contextNode) {

			super(contextNode);
		}

		public static boolean isValid(ContextNode contextNode) {

			return XdiEntityInstanceUnordered.isValid(contextNode) &&
					! contextNode.getXDIArc().isDefinition() &&
					contextNode.getXDIArc().isVariable();
		}

		public static Variable fromContextNode(ContextNode contextNode) {

			if (contextNode == null) throw new NullPointerException();

			if (! isValid(contextNode)) return null;

			return new Variable(contextNode);
		}
	}

	/*
	 * Helper classes
	 */

	public static class MappingContextNodeXdiEntityInstanceUnorderedIterator extends NotNullIterator<XdiEntityInstanceUnordered> {

		public MappingContextNodeXdiEntityInstanceUnorderedIterator(Iterator<ContextNode> contextNodes) {

			super(new MappingIterator<ContextNode, XdiEntityInstanceUnordered> (contextNodes) {

				@Override
				public XdiEntityInstanceUnordered map(ContextNode contextNode) {

					return XdiEntityInstanceUnordered.fromContextNode(contextNode);
				}
			});
		}
	}
}
