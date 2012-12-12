<!DOCTYPE html>
<html>
	<head>
	<g:javascript library="jquery" />
	<r:layoutResources />
    <resource:include components="slider" />
	</head>
	<body>

    <h1><div id="Status">None</div></h1>
	    <div>
            <table>
                <tbody>
                    <tr>
                        <td></td>
                        <td><g:remoteLink action="goForward" update="Status"><g:img dir="images" file="arrow_up_green.png"/></g:remoteLink></td>
                        <td></td>
                    </tr>
                    <tr>
                        <td><g:remoteLink action="goLeft" update="Status"><g:img dir="images" file="arrow_left_green.png"/></g:remoteLink></td>
                        <td> <g:remoteLink action="goStop" update="Status"><g:img dir="images" file="stop_button.png"/></g:remoteLink></td>
                        <td><g:remoteLink action="goRight" update="Status"><g:img dir="images" file="arrow_right_green.png"/></g:remoteLink></td>
                    </tr>
                    <tr>
                    		<td></td>
                    		<td><g:remoteLink action="goBackward" update="Status"><g:img dir="images" file="arrow_down_green.png"/></g:remoteLink></td>
                    		<td></td>
                    </tr>
                </tbody>
            </table>
			
	    </div>	    	    
		<g:select from="['SLOW','FAST']" name="speeds" noSelection="['':'-Speed-']" onchange="${remoteFunction(action:'changeSpeed', params: '\'speed=\' + this.value')}"></g:select>

		

	    

	</body>
 <script>

 
 jQuery(document).bind('keydown', function (evt){
    if (evt.which==32) {
        jQuery.ajax({type:'GET',
                    url:'../drive/goStop',
                    success: function(result){
                        $('#Status').html(result);
                    }});
    } else if (evt.which == 37) {		
		jQuery.ajax({type:'GET',
                    url:'../drive/goLeft',
                    success: function(result){
                        $('#Status').html(result);
                    }});
	} else if(evt.which == 38) {
		jQuery.ajax({type:'GET', 
                    url:'../drive/goForward',
                    success: function(result) {
                        $('#Status').html(result);
                    }});
	} else if(evt.which == 39) {
		jQuery.ajax({type:'GET',
                    url:'../drive/goRight',
                    success: function(result) {
                        $('#Status').html(result);
                    }});
	} else if(evt.which == 40) {
		jQuery.ajax({type:'GET',
                    url:'../drive/goBackward',
                    success: function(result) {
                        $('#Status').html(result);
                    }});
 	}
  });
</script>
</html>
