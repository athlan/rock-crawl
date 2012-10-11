<!DOCTYPE html>
<html>
<head>
    <g:javascript library="jquery" />
    <meta name="layout" content="main"/>
    <title>Welcome to Grails</title>
    <style type="text/css" media="screen">
    #status {
        background-color: #eee;
        border: .2em solid #fff;
        margin: 2em 2em 1em;
        padding: 1em;
        width: 12em;
        float: left;
        -moz-box-shadow: 0px 0px 1.25em #ccc;
        -webkit-box-shadow: 0px 0px 1.25em #ccc;
        box-shadow: 0px 0px 1.25em #ccc;
        -moz-border-radius: 0.6em;
        -webkit-border-radius: 0.6em;
        border-radius: 0.6em;
    }

    .ie6 #status {
        display: inline; /* float double margin fix http://www.positioniseverything.net/explorer/doubled-margin.html */
    }

    #status ul {
        font-size: 0.9em;
        list-style-type: none;
        margin-bottom: 0.6em;
        padding: 0;
    }

    #status li {
        line-height: 1.3;
    }

    #status h1 {
        text-transform: uppercase;
        font-size: 1.1em;
        margin: 0 0 0.3em;
    }

    #page-body {
        margin: 2em 1em 1.25em 18em;
    }

    h2 {
        margin-top: 1em;
        margin-bottom: 0.3em;
        font-size: 1em;
    }

    p {
        line-height: 1.5;
        margin: 0.25em 0;
    }

    #controller-list ul {
        list-style-position: inside;
    }

    #controller-list li {
        line-height: 1.3;
        list-style-position: inside;
        margin: 0.25em 0;
    }

    @media screen and (max-width: 480px) {
        #status {
            display: none;
        }

        #page-body {
            margin: 0 1em 1em;
        }

        #page-body h1 {
            margin-top: 0;
        }
    }
    </style>

</head>
<body>
<div>
    <table>
        <tbody>
        <tr>
            <td width=12.5%><div id = "led0">${status0}</div></td>
            <td width=12.5%><div id = "led1">${status1}</div></td>
            <td width=12.5%><div id = "led2">${status2}</div></td>
            <td width=12.5%><div id = "led3">${status3}</div></td>
            <td width=12.5%><div id = "led4">${status4}</div></td>
            <td width=12.5%><div id = "led5">${status5}</div></td>
            <td width=12.5%><div id = "led6">${status6}</div></td>
            <td width=12.5%><div id = "led7">${status7}</div></td>
        </tr>
        <tr>
            <td width=12.5%>
                <g:remoteLink action="switchLight" params="[position:0]"
                                          update="led0">Switch
                </g:remoteLink>
            </td>
            <td width=12.5%><g:remoteLink action="switchLight" params="[position:1]"
                                          update="led1">Switch</g:remoteLink></td>
            <td width=12.5%><g:remoteLink action="switchLight" params="[position:2]"
                                          update="led2">Switch</g:remoteLink></td>
            <td width=12.5%><g:remoteLink action="switchLight" params="[position:3]"
                                          update="led3">Switch</g:remoteLink></td>
            <td width=12.5%><g:remoteLink action="switchLight" params="[position:4]"
                                          update="led4">Switch</g:remoteLink></td>
            <td width=12.5%><g:remoteLink action="switchLight" params="[position:5]"
                                          update="led5">Switch</g:remoteLink></td>
            <td width=12.5%><g:remoteLink action="switchLight" params="[position:6]"
                                          update="led6">Switch</g:remoteLink></td>
            <td width=12.5%><g:remoteLink action="switchLight" params="[position:7]"
                                          update="led7">Switch</g:remoteLink></td>
        </tr>
        <tr>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors1"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=1\'')}"></g:select>

            </td>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors2"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=2\'')}"></g:select>
            </td>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors3"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=3\'')}"></g:select>
            </td>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors4"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=4\'')}"></g:select>
            </td>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors5"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=5\'')}"></g:select>
            </td>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors6"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=6\'')}"></g:select>
            </td>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors7"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=7\'')}"></g:select>
            </td>
            <td width=12.5%>
                <g:select from="['BLUE', 'GREEN', 'RED', 'WHITE']" name="colors8"
                          noSelection="['':'-LED Color-']" onchange="${remoteFunction(
                        action:'setLightColor',
                        params:'\'color=\' + this.value + \'&position=8\'')}"></g:select>
            </td>
        </tr>
        </tbody>
    </table>
</div>

</body>
</html>
