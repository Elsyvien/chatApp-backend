<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>JSP - Hello World</title>
</head>
<body>
<h1><%= "Hello World!" %>
</h1>
<div id="server-status" style="margin:10px 0; padding:5px; border-radius:5px; display:inline-block; background:#eee; color:#333;">
  Server Status: <span id="status-indicator" style="font-weight:bold; color:gray;">Unbekannt</span>
</div>
<script>
function checkServerStatus() {
  fetch(window.location.pathname, {method: 'HEAD'})
    .then(() => {
      document.getElementById('status-indicator').textContent = 'Online';
      document.getElementById('status-indicator').style.color = 'green';
    })
    .catch(() => {
      document.getElementById('status-indicator').textContent = 'Offline';
      document.getElementById('status-indicator').style.color = 'red';
    });
}
checkServerStatus();
setInterval(checkServerStatus, 10000);
</script>
</body>
</html>
