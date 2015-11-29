<?php
class block_ocw extends block_base {
    public function init() {
        $this->title = get_string('ocw', 'block_ocw');
    }
	
	public function get_content() {
    if ($this->content !== null) {
      return $this->content;
    }
	
	global $OUTPUT;
	
	$strsearch  = get_string('search');
    
		$this->content         =  new stdClass;
		
		$this->content->text  = 'Enter Search Keywords <br/><div class="searchform">';
        $this->content->text .= '<form action="/moodle/blocks/ocw/view.php" style="display:inline"><fieldset class="invisiblefieldset">';
        $this->content->text .= '<legend class="accesshide">'.$strsearch.'</legend>';
        
        $this->content->text .= '<label class="accesshide" for="search">Search Keyword</label>'.
                                '<input id="search" name="search" type="text" size="26" />';
		$this->content->text .= '<br><input type="radio" name="type" value="c">Search by Course<br>
							     <input type="radio" name="type" value="k">Search by Keyword<br><br>';
        $this->content->text .= '<button id="searchform_button" type="submit" title="'.$strsearch.'">Search</button><br />';
      
        $this->content->text .= $OUTPUT->help_icon('search');
        $this->content->text .= '</fieldset></form></div>';
 
    return $this->content;
  }
}  