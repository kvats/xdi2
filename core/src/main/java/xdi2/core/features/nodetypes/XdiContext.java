package xdi2.core.features.nodetypes;

import java.io.Serializable;
import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;

public interface XdiContext<EQ extends XdiContext<EQ>> extends Serializable, Comparable<XdiContext<?>> {

	public ContextNode getContextNode();
	public Graph getGraph();
	public XDIArc getXDIArc();
	public XDIAddress getXDIAddress();
	public XDIArc getBaseXDIArc();

	public EQ dereference();
	public EQ getReferenceXdiContext();
	public EQ getReplacementXdiContext();
	public Iterator<EQ> getIdentityXdiContexts();

	public XdiRoot findRoot();
	public XdiCommonRoot findLocalRoot();
	public XdiInnerRoot getXdiInnerRoot(XDIAddress innerRootPredicateAddress, boolean create);
	public XdiEntityCollection getXdiEntityCollection(XDIArc contextNodeArc, boolean create);
	public XdiEntityCollection getXdiEntityCollection(XDIAddress contextNodeAddress, boolean create);
	public XdiAttributeCollection getXdiAttributeCollection(XDIArc contextNodeArc, boolean create);
	public XdiAttributeCollection getXdiAttributeCollection(XDIAddress contextNodeAddress, boolean create);
	public XdiEntitySingleton getXdiEntitySingleton(XDIArc contextNodeArc, boolean create);
	public XdiEntitySingleton getXdiEntitySingleton(XDIAddress contextNodeAddress, boolean create);
	public XdiAttributeSingleton getXdiAttributeSingleton(XDIArc contextNodeArc, boolean create);
	public XdiAttributeSingleton getXdiAttributeSingleton(XDIAddress contextNodeAddress, boolean create);
	public XdiEntity getXdiEntity(XDIArc contextNodeArc, boolean create);
	public XdiEntity getXdiEntity(XDIAddress contextNodeAddress, boolean create);
	public XdiAttribute getXdiAttribute(XDIArc contextNodeArc, boolean create);
	public XdiAttribute getXdiAttribute(XDIAddress contextNodeAddress, boolean create);
}
