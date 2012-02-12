<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>XDI Local Messenger</title>
<link rel="stylesheet" target="_blank" href="style.css" TYPE="text/css" MEDIA="screen">
</head>
<body>

	<div class="header">
	<img src="images/logo64.png" align="middle">&nbsp;&nbsp;&nbsp;<span id="appname">XDI Local Messenger</span>
	&nbsp;&nbsp;&nbsp;&nbsp;
	<% for (int i=0; i<((Integer) request.getAttribute("sampleInputs")).intValue(); i++) { %>
		<a href="XDILocalMessenger?sample=<%= i+1 %>">Sample <%= i+1 %></a>&nbsp;&nbsp;
	<% } %>
	<a href="index.jsp">&gt;&gt;&gt;Other Apps...</a>
	</div>

	<% if (request.getAttribute("error") != null) { %>
			
		<p><font color="red"><%= request.getAttribute("error") != null ? request.getAttribute("error") : "" %></font></p>

	<% } %>

	<form action="XDILocalMessenger" method="post">

		<table width="100%" cellspacing="0" cellpadding="0" border="0">
		<tr>
		<td width="50%" style="padding-right: 10px">
			<textarea class="input" name="input" style="width: 100%" rows="12"><%= request.getAttribute("input") != null ? request.getAttribute("input") : "" %></textarea><br>
		</td>
		<td width="50%" style="padding-left: 10px">
			<textarea class="input" name="message" style="width: 100%" rows="12"><%= request.getAttribute("message") != null ? request.getAttribute("message") : "" %></textarea><br>
		</td>
		</tr>
		</table>

		<% String versioningSupport = (String) request.getAttribute("versioningSupport"); if (versioningSupport == null) versioningSupport = ""; %>
		<% String linkContractSupport = (String) request.getAttribute("linkContractSupport"); if (linkContractSupport == null) linkContractSupport = ""; %>
		<% String to = (String) request.getAttribute("to"); if (to == null) to = ""; %>

		Result Format:
		<select name="to">
		<option value="XDI/JSON" <%= to.equals("XDI/JSON") ? "selected" : "" %>>XDI/JSON</option>
		<option value="STATEMENTS" <%= to.equals("STATEMENTS") ? "selected" : "" %>>STATEMENTS</option>
		</select>
		&nbsp;
		<input name="versioningSupport" type="checkbox" <%= versioningSupport.equals("on") ? "checked" : "" %>>Versioning
		<input name="linkContractSupport" type="checkbox" <%= linkContractSupport.equals("on") ? "checked" : "" %>>Link Contracts&nbsp;
		<input type="submit" value="Go!">
		&nbsp;&nbsp;&nbsp;&nbsp;<a href="XDILocalMessengerHelp.jsp">What can I do here?</a>

		<% if (request.getAttribute("stats") != null) { %>
			<p>
			<%= request.getAttribute("stats") %>

			<% if (request.getAttribute("output") != null) { %>
				Copy&amp;Paste: <textarea style="width: 100px; height: 1.2em; overflow: hidden"><%= request.getAttribute("output") != null ? request.getAttribute("output") : "" %></textarea>
			<% } %>
			</p>
		<% } %>

		<% if (request.getAttribute("output") != null) { %>
			<div class="result"><pre><%= request.getAttribute("output") != null ? request.getAttribute("output") : "" %></pre></div><br>
		<% } %>
	</form>
	
</body>
</html>
