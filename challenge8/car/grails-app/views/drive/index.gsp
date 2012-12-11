<!DOCTYPE html>
<html>
	<head>
	<g:javascript library="jquery" />
	<r:layoutResources />
	</head>
	<body>
        <div id="Status">None</div>
	    <div>
            <table>
                <tbody>
                    <tr>
                        <td></td>
                        <td><g:remoteLink action="goForward" update="Status">Front</g:remoteLink></td>
                        <td></td>
                    </tr>
                    <tr>
                        <td><g:remoteLink action="goLeft" update="Status">Left</g:remoteLink></td>
                        <td><g:remoteLink action="goBackward" update="Status">Back</g:remoteLink></td>
                        <td><g:remoteLink action="goRight" update="Status">Right</g:remoteLink></td>
                    </tr>
                </tbody>
            </table>
	    </div>
	</body>
</html>
