package xdi2.tests.core.features.nodetypes;

import junit.framework.TestCase;
import xdi2.core.Graph;
import xdi2.core.features.nodetypes.XdiAbstractContext;
import xdi2.core.features.nodetypes.XdiCommonRoot;
import xdi2.core.features.nodetypes.XdiInnerRoot;
import xdi2.core.features.nodetypes.XdiPeerRoot;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.syntax.XDIAddress;

public class RootsTest extends TestCase {

	public void testSubGraph() throws Exception {

		Graph graph = MemoryGraphFactory.getInstance().openGraph();
		XdiCommonRoot localRoot = XdiCommonRoot.findCommonRoot(graph);
		XdiPeerRoot peerRoot = localRoot.getPeerRoot(XDIAddress.create("=!:uuid:91f28153-f600-ae24-91f2-8153f600ae24"), true);
		XdiInnerRoot innerRoot = peerRoot.getInnerRoot(XDIAddress.create("=!1111"), XDIAddress.create("$add"), true);
		
		assertTrue(XdiAbstractContext.fromContextNode(localRoot.getContextNode()) instanceof XdiCommonRoot);
		assertTrue(XdiAbstractContext.fromContextNode(peerRoot.getContextNode()) instanceof XdiPeerRoot);
		assertTrue(XdiAbstractContext.fromContextNode(innerRoot.getContextNode()) instanceof XdiInnerRoot);
		
		graph.close();
	}
}
