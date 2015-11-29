<?php require_once("{$CFG->libdir}/formslib.php");
 
class ocw_form extends moodleform {
 
    function definition() {
		
        $mform =& $this->_form;
        // add group for text areas
		$mform->addElement('header','displayinfo', 'Search Results');
		 
		// add page title element.
		$mform->addElement('text', 'search', 'Search', array('value' => $this->_customdata['query'] ));

		
		// Send data over to the SON via sockets
		
		if(!($sock = socket_create(AF_INET, SOCK_STREAM, 0)))
		{
		//	echo ("1");
		    $errorcode = socket_last_error();
		    $errormsg = socket_strerror($errorcode);
		     
		   // die("Couldn't create socket: [$errorcode] $errormsg \n");
		}
		 
		//echo "Socket created \n";
		 
		//Connect socket to remote server
		if(!socket_connect($sock , '192.168.1.12' , 60000))
		{
		    $errorcode = socket_last_error();
		    $errormsg = socket_strerror($errorcode);
		     
		 //   die("Could not connect: [$errorcode] $errormsg \n");
		}
		 
		//echo "Connection established \n";
		
		$message = $this->_customdata['query'];
		$message .= "\n";

		//echo $message;
		 
		//Send the message to the server
		if( ! socket_write ( $sock , $message , strlen($message)))
		{
		    $errorcode = socket_last_error();
		    $errormsg = socket_strerror($errorcode);
		     
		   // die("Could not send data: [$errorcode] $errormsg \n");
		}
		 
		//echo "Message send successfully \n";

		if(socket_recv ( $sock , $result , 200 , MSG_WAITALL ) === FALSE)
		{
		    $errorcode = socket_last_error();
		    $errormsg = socket_strerror($errorcode);
		     
		   // die("Could not receive data: [$errorcode] $errormsg \n");
		}
		 

		//display the received result


		//$mform->addRule('search', null, 'required', null, 'client'); 
		$result .= "http://192.168.1.3/moodle/draftfile.php/5/user/draft/8026124/homework1.pdf|file2|file3|file4";
	
		$resArray = explode('|', $result);
		$mform->addElement('html','Your keywords were found in following files. Click on a file to download it! <br/><br/>');
		for ($i=0; $i<sizeof($resArray); $i++){
			
			$mform->addElement('html', ($i+1) . '. &nbsp <a href="'.$resArray[$i].'">'.basename($resArray[$i]).'</a> <br/>');
		}
    }
}

?>