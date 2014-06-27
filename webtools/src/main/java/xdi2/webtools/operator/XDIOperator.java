package xdi2.webtools.operator;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.client.XDIClient;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.client.http.XDIHttpClient;
import xdi2.client.util.XDIClientUtil;
import xdi2.core.Graph;
import xdi2.core.constants.XDIAuthenticationConstants;
import xdi2.core.constants.XDIConstants;
import xdi2.core.constants.XDIDictionaryConstants;
import xdi2.core.constants.XDILinkContractConstants;
import xdi2.core.features.linkcontracts.GenericLinkContract;
import xdi2.core.features.linkcontracts.PublicLinkContract;
import xdi2.core.features.linkcontracts.RootLinkContract;
import xdi2.core.features.linkcontracts.policy.PolicyAnd;
import xdi2.core.features.linkcontracts.policy.PolicyOr;
import xdi2.core.features.linkcontracts.policy.PolicyUtil;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.io.XDIReader;
import xdi2.core.io.XDIReaderRegistry;
import xdi2.core.io.XDIWriter;
import xdi2.core.io.XDIWriterRegistry;
import xdi2.core.io.writers.XDIDisplayWriter;
import xdi2.core.util.CopyUtil;
import xdi2.core.util.GraphUtil;
import xdi2.core.util.XDI3Util;
import xdi2.core.util.iterators.MappingStatementXriIterator;
import xdi2.core.xri3.CloudNumber;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3Statement;
import xdi2.discovery.XDIDiscoveryClient;
import xdi2.discovery.XDIDiscoveryResult;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;
import xdi2.messaging.target.contributor.impl.keygen.GenerateKeyContributor;
import xdi2.webtools.util.SecretTokenInsertingCopyStrategy;

/**
 * Servlet implementation class for Servlet: XDIOperator
 *
 */
public class XDIOperator extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

	private static final long serialVersionUID = -3912797900351698765L;

	private static Logger log = LoggerFactory.getLogger(XDIOperator.class);

	private static XDIWriter xdiMessageWriter;
	private static String sampleInput;
	private static String sampleEndpoint;

	static {

		Properties xdiMessageWriterParameters = new Properties();

		xdiMessageWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_IMPLIED, "0");
		xdiMessageWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_ORDERED, "0");
		xdiMessageWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_INNER, "1");
		xdiMessageWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_PRETTY, "0");

		xdiMessageWriter = XDIWriterRegistry.forFormat("XDI DISPLAY", xdiMessageWriterParameters);

		sampleInput = "=alice";

		sampleEndpoint = "PROD";
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		request.setAttribute("resultFormat", XDIDisplayWriter.FORMAT_NAME);
		request.setAttribute("writeImplied", null);
		request.setAttribute("writeOrdered", "on");
		request.setAttribute("writeInner", "on");
		request.setAttribute("writePretty", null);
		request.setAttribute("input", sampleInput);
		request.setAttribute("secretToken", "");
		request.setAttribute("endpoint", sampleEndpoint);

		if (request.getParameter("input") != null) {

			request.setAttribute("input", request.getParameter("input"));
		}

		if (request.getParameter("endpoint") != null) {

			request.setAttribute("endpoint", request.getParameter("endpoint"));
		}

		request.getRequestDispatcher("/XDIOperator.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		if ("message".equals(request.getParameter("cmd"))) {

			message(request, response);
		} else {

			if ("login".equals(request.getParameter("cmd"))) 
				login(request, response);
			else if ("logout".equals(request.getParameter("cmd"))) 
				logout(request, response);
			else if ("buildPlain".equals(request.getParameter("cmd"))) 
				buildPlain(request, response);
			else if ("buildCloudNames".equals(request.getParameter("cmd"))) 
				buildCloudNames(request, response);
			else if ("buildRootLinkContract".equals(request.getParameter("cmd"))) 
				buildRootLinkContract(request, response);
			else if ("buildPublicLinkContract".equals(request.getParameter("cmd"))) 
				buildPublicLinkContract(request, response);
			else if ("buildGenericLinkContract".equals(request.getParameter("cmd"))) 
				buildGenericLinkContract(request, response);
			else if ("buildKeyPairs".equals(request.getParameter("cmd"))) 
				buildKeyPairs(request, response);

			request.setAttribute("resultFormat", XDIDisplayWriter.FORMAT_NAME);
			request.setAttribute("writeImplied", null);
			request.setAttribute("writeOrdered", "on");
			request.setAttribute("writeInner", "on");
			request.setAttribute("writePretty", null);
		}

		request.getRequestDispatcher("/XDIOperator.jsp").forward(request, response);
	}

	private void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String input = request.getParameter("input");
		String secretToken = request.getParameter("secretToken");
		String endpoint = request.getParameter("endpoint");
		String error = null;

		XDIDiscoveryResult discoveryResult = null;

		try {

			// start discovery

			XDIDiscoveryClient discoveryClient = null;

			if ("PROD".equals(endpoint)) discoveryClient = XDIDiscoveryClient.NEUSTAR_PROD_DISCOVERY_CLIENT;
			if ("OTE".equals(endpoint)) discoveryClient = XDIDiscoveryClient.NEUSTAR_OTE_DISCOVERY_CLIENT;
			if (discoveryClient == null) throw new NullPointerException();

			discoveryClient.setRegistryCache(null);
			discoveryClient.setAuthorityCache(null);

			// discover

			discoveryResult = discoveryClient.discover(XDI3Segment.create(input), null);

			if (discoveryResult == null) throw new RuntimeException("No discovery result");
			if (discoveryResult.getCloudNumber() == null) throw new RuntimeException("No cloud number");
			if (discoveryResult.getXdiEndpointUri() == null) throw new RuntimeException("No XDI endpoint URI");

			// authenticate

			XDIClientUtil.authenticateSecretToken(discoveryResult.getCloudNumber(), discoveryResult.getXdiEndpointUri(), secretToken);

			// check result

			CloudNumber cloudNumber = discoveryResult.getCloudNumber();
			String xdiEndpointUri = discoveryResult.getXdiEndpointUri();

			// login

			request.getSession().setAttribute("sessionInput", input);
			request.getSession().setAttribute("sessionSecretToken", secretToken);
			request.getSession().setAttribute("sessionCloudNumber", cloudNumber);
			request.getSession().setAttribute("sessionXdiEndpointUri", xdiEndpointUri);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("input", input);
		request.setAttribute("secretToken", secretToken);
		request.setAttribute("endpoint", endpoint);
		request.setAttribute("error", error);
	}

	private void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String error = null;

		try {

			// logout

			request.getSession().removeAttribute("sessionInput");
			request.getSession().removeAttribute("sessionSecretToken");
			request.getSession().removeAttribute("sessionCloudNumber");
			request.getSession().removeAttribute("sessionXdiEndpointUri");
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("error", error);
	}

	private void buildPlain(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String submit = request.getParameter("submit");
		String error = null;

		CloudNumber cloudNumber = (CloudNumber) request.getSession().getAttribute("sessionCloudNumber");

		StringWriter output = new StringWriter();

		try {

			// build

			MessageEnvelope messageEnvelope = new MessageEnvelope();
			Message message = messageEnvelope.createMessage(cloudNumber.getXri());
			message.setToPeerRootXri(cloudNumber.getPeerRootXri());
			message.setLinkContractXri(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			message.setSecretToken("********");

			if ("Plain XDI get".equals(submit)) {

				message.createGetOperation(XDI3Util.concatXris(cloudNumber.getXri(), XDI3Segment.create("<#email>")));
			} else if ("Plain XDI set".equals(submit)) {

				message.createSetOperation(XDI3Statement.fromLiteralComponents(XDI3Util.concatXris(cloudNumber.getXri(), XDI3Segment.create("<#email>"), XDIConstants.XRI_S_VALUE), "test@email.com"));
			} else if ("Plain XDI del".equals(submit)) {

				message.createDelOperation(XDI3Util.concatXris(cloudNumber.getXri(), XDI3Segment.create("<#email>")));
			}

			xdiMessageWriter.write(messageEnvelope.getGraph(), output);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("message", output.getBuffer().toString());
		request.setAttribute("error", error);
		request.setAttribute("tab", "0");
	}

	private void buildCloudNames(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String cloudName = request.getParameter("cloudName");
		String submit = request.getParameter("submit");
		String error = null;

		CloudNumber cloudNumber = (CloudNumber) request.getSession().getAttribute("sessionCloudNumber");

		StringWriter output = new StringWriter();

		try {

			// build

			MessageEnvelope messageEnvelope = new MessageEnvelope();
			Message message = messageEnvelope.createMessage(cloudNumber.getXri());
			message.setToPeerRootXri(cloudNumber.getPeerRootXri());
			message.setLinkContractXri(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			message.setSecretToken("********");

			if ("Get cloud names".equals(submit)) {

				message.createGetOperation(XDI3Statement.fromComponents(cloudNumber.getXri(), XDIDictionaryConstants.XRI_S_IS_REF, XDIConstants.XRI_S_VARIABLE));
			} else if ("Set cloud name".equals(submit)) {

				message.createSetOperation(XDI3Statement.fromComponents(cloudNumber.getXri(), XDIDictionaryConstants.XRI_S_IS_REF, XDI3Segment.create(cloudName)));
			} else if ("Del cloud name".equals(submit)) {

				message.createDelOperation(XDI3Statement.fromComponents(cloudNumber.getXri(), XDIDictionaryConstants.XRI_S_IS_REF, XDI3Segment.create(cloudName)));
			}

			xdiMessageWriter.write(messageEnvelope.getGraph(), output);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("message", output.getBuffer().toString());
		request.setAttribute("error", error);
		request.setAttribute("tab", "1");
	}

	private void buildRootLinkContract(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String submit = request.getParameter("submit");
		String error = null;

		CloudNumber cloudNumber = (CloudNumber) request.getSession().getAttribute("sessionCloudNumber");

		StringWriter output = new StringWriter();

		try {

			// build

			MessageEnvelope messageEnvelope = new MessageEnvelope();
			Message message = messageEnvelope.createMessage(cloudNumber.getXri());
			message.setToPeerRootXri(cloudNumber.getPeerRootXri());
			message.setLinkContractXri(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			message.setSecretToken("********");

			if ("Get root link contract".equals(submit)) {

				message.createGetOperation(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			} else if ("Set root link contract".equals(submit)) {

				Graph graph = MemoryGraphFactory.getInstance().openGraph();

				GraphUtil.setOwnerPeerRootXri(graph, cloudNumber.getPeerRootXri());

				RootLinkContract rootLinkContract = RootLinkContract.findRootLinkContract(graph, true);
				rootLinkContract.setPermissionTargetAddress(XDILinkContractConstants.XRI_S_ALL, XDIConstants.XRI_S_ROOT);

				PolicyAnd policyAnd = rootLinkContract.getPolicyRoot(true).createAndPolicy(true);
				PolicyUtil.createSenderIsOperator(policyAnd, cloudNumber.getXri());

				PolicyOr policyOr = policyAnd.createOrPolicy(true);
				PolicyUtil.createSecretTokenValidOperator(policyOr);
				PolicyUtil.createSignatureValidOperator(policyOr);

				message.createSetOperation(new MappingStatementXriIterator(graph.getRootContextNode().getAllStatements()));
			} else if ("Del root link contract".equals(submit)) {

				message.createDelOperation(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			}

			xdiMessageWriter.write(messageEnvelope.getGraph(), output);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("message", output.getBuffer().toString());
		request.setAttribute("error", error);
		request.setAttribute("tab", "2");
	}

	private void buildPublicLinkContract(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String submit = request.getParameter("submit");
		String error = null;

		CloudNumber cloudNumber = (CloudNumber) request.getSession().getAttribute("sessionCloudNumber");

		StringWriter output = new StringWriter();

		try {

			// build

			MessageEnvelope messageEnvelope = new MessageEnvelope();
			Message message = messageEnvelope.createMessage(cloudNumber.getXri());
			message.setToPeerRootXri(cloudNumber.getPeerRootXri());
			message.setLinkContractXri(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			message.setSecretToken("********");

			if ("Get public link contract".equals(submit)) {

				message.createGetOperation(PublicLinkContract.createPublicLinkContractXri(cloudNumber.getXri()));
			} else if ("Set public link contract".equals(submit)) {

				Graph graph = MemoryGraphFactory.getInstance().openGraph();

				GraphUtil.setOwnerPeerRootXri(graph, cloudNumber.getPeerRootXri());

				PublicLinkContract publicLinkContract = PublicLinkContract.findPublicLinkContract(graph, true);
				XDI3Segment publicAddress = XDI3Util.concatXris(cloudNumber.getXri(), XDILinkContractConstants.XRI_S_PUBLIC);
				publicLinkContract.setPermissionTargetAddress(XDILinkContractConstants.XRI_S_GET, publicAddress);

				message.createSetOperation(new MappingStatementXriIterator(graph.getRootContextNode().getAllStatements()));
			} else if ("Del public link contract".equals(submit)) {

				message.createDelOperation(PublicLinkContract.createPublicLinkContractXri(cloudNumber.getXri()));
			}

			xdiMessageWriter.write(messageEnvelope.getGraph(), output);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("message", output.getBuffer().toString());
		request.setAttribute("error", error);
		request.setAttribute("tab", "3");
	}

	private void buildGenericLinkContract(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String requestingAuthority = request.getParameter("requestingAuthority");
		String submit = request.getParameter("submit");
		String error = null;

		CloudNumber cloudNumber = (CloudNumber) request.getSession().getAttribute("sessionCloudNumber");

		StringWriter output = new StringWriter();

		try {

			// build

			MessageEnvelope messageEnvelope = new MessageEnvelope();
			Message message = messageEnvelope.createMessage(cloudNumber.getXri());
			message.setToPeerRootXri(cloudNumber.getPeerRootXri());
			message.setLinkContractXri(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			message.setSecretToken("********");

			if ("Get generic link contract".equals(submit)) {

				message.createGetOperation(GenericLinkContract.createGenericLinkContractXri(cloudNumber.getXri(), XDI3Segment.create(requestingAuthority), null));
			} else if ("Set generic link contract".equals(submit)) {

				Graph graph = MemoryGraphFactory.getInstance().openGraph();

				GenericLinkContract genericLinkContract = GenericLinkContract.findGenericLinkContract(graph, cloudNumber.getXri(), XDI3Segment.create(requestingAuthority), null, true);
				genericLinkContract.setPermissionTargetAddress(XDILinkContractConstants.XRI_S_GET, XDI3Util.concatXris(cloudNumber.getXri(), XDI3Segment.create("<#email>")));

				PolicyAnd policyAnd = genericLinkContract.getPolicyRoot(true).createAndPolicy(true);
				PolicyUtil.createSenderIsOperator(policyAnd, XDI3Segment.create(requestingAuthority));

				message.createSetOperation(new MappingStatementXriIterator(graph.getRootContextNode().getAllStatements()));
			} else if ("Del generic link contract".equals(submit)) {

				message.createDelOperation(GenericLinkContract.createGenericLinkContractXri(cloudNumber.getXri(), XDI3Segment.create(requestingAuthority), null));
			}

			xdiMessageWriter.write(messageEnvelope.getGraph(), output);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("requestingAuthority", requestingAuthority);
		request.setAttribute("message", output.getBuffer().toString());
		request.setAttribute("error", error);
		request.setAttribute("tab", "4");
	}

	private void buildKeyPairs(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String submit = request.getParameter("submit");
		String error = null;

		CloudNumber cloudNumber = (CloudNumber) request.getSession().getAttribute("sessionCloudNumber");

		StringWriter output = new StringWriter();

		try {

			// build

			MessageEnvelope messageEnvelope = new MessageEnvelope();
			Message message = messageEnvelope.createMessage(cloudNumber.getXri());
			message.setToPeerRootXri(cloudNumber.getPeerRootXri());
			message.setLinkContractXri(RootLinkContract.createRootLinkContractXri(cloudNumber.getXri()));
			message.setSecretToken("********");

			if ("Get key pairs".equals(submit)) {

				message.createGetOperation(XDI3Util.concatXris(cloudNumber.getXri(), XDIAuthenticationConstants.XRI_S_MSG_ENCRYPT_KEYPAIR));
				message.createGetOperation(XDI3Util.concatXris(cloudNumber.getXri(), XDIAuthenticationConstants.XRI_S_MSG_SIG_KEYPAIR));
			} else if ("Generate key pairs".equals(submit)) {

				message.createOperation(GenerateKeyContributor.XRI_S_DO_KEYPAIR, XDI3Statement.fromComponents(XDI3Util.concatXris(cloudNumber.getXri(), XDIAuthenticationConstants.XRI_S_MSG_ENCRYPT_KEYPAIR), XDIDictionaryConstants.XRI_S_IS_TYPE, XDI3Segment.create("$rsa$2048")));
				message.createOperation(GenerateKeyContributor.XRI_S_DO_KEYPAIR, XDI3Statement.fromComponents(XDI3Util.concatXris(cloudNumber.getXri(), XDIAuthenticationConstants.XRI_S_MSG_SIG_KEYPAIR), XDIDictionaryConstants.XRI_S_IS_TYPE, XDI3Segment.create("$rsa$2048")));
			} else if ("Del key pairs".equals(submit)) {

				message.createDelOperation(XDI3Util.concatXris(cloudNumber.getXri(), XDIAuthenticationConstants.XRI_S_MSG_ENCRYPT_KEYPAIR));
				message.createDelOperation(XDI3Util.concatXris(cloudNumber.getXri(), XDIAuthenticationConstants.XRI_S_MSG_SIG_KEYPAIR));
			}

			xdiMessageWriter.write(messageEnvelope.getGraph(), output);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		// display results

		request.setAttribute("message", output.getBuffer().toString());
		request.setAttribute("error", error);
		request.setAttribute("tab", "5");
	}

	private void message(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String resultFormat = request.getParameter("resultFormat");
		String writeImplied = request.getParameter("writeImplied");
		String writeOrdered = request.getParameter("writeOrdered");
		String writeInner = request.getParameter("writeInner");
		String writePretty = request.getParameter("writePretty");
		String message = request.getParameter("message");
		String output = "";
		String stats = "-1";
		String error = null;

		String sessionXdiEndpointUri = (String) request.getSession().getAttribute("sessionXdiEndpointUri");

		Properties xdiResultWriterParameters = new Properties();

		xdiResultWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_IMPLIED, "on".equals(writeImplied) ? "1" : "0");
		xdiResultWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_ORDERED, "on".equals(writeOrdered) ? "1" : "0");
		xdiResultWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_INNER, "on".equals(writeInner) ? "1" : "0");
		xdiResultWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_PRETTY, "on".equals(writePretty) ? "1" : "0");

		XDIReader xdiReader = XDIReaderRegistry.getAuto();
		XDIWriter xdiResultWriter = XDIWriterRegistry.forFormat(resultFormat, xdiResultWriterParameters);

		MessageEnvelope messageEnvelope = null;
		MessageResult messageResult = null;

		long start = System.currentTimeMillis();

		try {

			// parse the message envelope

			String secretToken = (String) request.getSession().getAttribute("sessionSecretToken");
			Graph tempGraph = MemoryGraphFactory.getInstance().openGraph();
			xdiReader.read(tempGraph, new StringReader(message));

			messageEnvelope = new MessageEnvelope();

			CopyUtil.copyGraph(tempGraph, messageEnvelope.getGraph(), new SecretTokenInsertingCopyStrategy(secretToken));

			// send the message envelope and read result

			XDIClient client = new XDIHttpClient(sessionXdiEndpointUri);

			messageResult = client.send(messageEnvelope, null);

			// output the message result

			StringWriter writer = new StringWriter();

			xdiResultWriter.write(messageResult.getGraph(), writer);

			output = StringEscapeUtils.escapeHtml(writer.getBuffer().toString());
		} catch (Exception ex) {

			if (ex instanceof Xdi2ClientException) {

				messageResult = ((Xdi2ClientException) ex).getErrorMessageResult();

				// output the message result

				if (messageResult != null) {

					StringWriter writer2 = new StringWriter();
					xdiResultWriter.write(messageResult.getGraph(), writer2);
					output = StringEscapeUtils.escapeHtml(writer2.getBuffer().toString());
				}
			}

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		long stop = System.currentTimeMillis();

		stats = "";
		stats += Long.toString(stop - start) + " ms time. ";
		if (messageEnvelope != null) stats += Long.toString(messageEnvelope.getMessageCount()) + " message(s). ";
		if (messageEnvelope != null) stats += Long.toString(messageEnvelope.getOperationCount()) + " operation(s). ";
		if (messageResult != null) stats += Long.toString(messageResult.getGraph().getRootContextNode(true).getAllStatementCount()) + " result statement(s). ";

		// display results

		request.setAttribute("resultFormat", resultFormat);
		request.setAttribute("writeImplied", writeImplied);
		request.setAttribute("writeOrdered", writeOrdered);
		request.setAttribute("writeInner", writeInner);
		request.setAttribute("writePretty", writePretty);
		request.setAttribute("message", message);
		request.setAttribute("output", output);
		request.setAttribute("stats", stats);
		request.setAttribute("error", error);
	}
}
