package xdi2.core.features.linkcontracts;

import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.features.linkcontracts.template.LinkContractTemplate;
import xdi2.core.features.nodetypes.XdiEntitySingleton;
import xdi2.core.util.iterators.MappingIterator;
import xdi2.core.util.iterators.NotNullIterator;
import xdi2.core.util.iterators.ReadOnlyIterator;

public class LinkContractTemplates {

	private LinkContractTemplates() { }

	/**
	 * Given a graph, lists all link contract templates.
	 * @param graph The graph.
	 * @return An iterator over link contract templates.
	 */
	public static ReadOnlyIterator<LinkContractTemplate> getAllLinkContractTemplates(Graph graph) {

		ContextNode root = graph.getRootContextNode(true);
		Iterator<ContextNode> allContextNodes = root.getAllContextNodes();

		return new MappingContextNodeLinkContractTemplateIterator(allContextNodes);
	}

	/*
	 * Helper classes
	 */

	public static class MappingContextNodeLinkContractTemplateIterator extends NotNullIterator<LinkContractTemplate> {

		public MappingContextNodeLinkContractTemplateIterator(Iterator<ContextNode> iterator) {

			super(new MappingIterator<ContextNode, LinkContractTemplate> (iterator) {

				@Override
				public LinkContractTemplate map(ContextNode contextNode) {

					XdiEntitySingleton.Variable xdiVariable = XdiEntitySingleton.Variable.fromContextNode(contextNode);
					if (xdiVariable == null) return null;

					return LinkContractTemplate.fromXdiEntitySingletonVariable(xdiVariable);
				}
			});
		}
	}
}
