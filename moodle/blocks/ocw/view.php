<?php
 
require_once('../../config.php');
require_once('ocw_form.php');
 
global $DB, $OUTPUT, $PAGE, $DB;


$search = required_param('search', PARAM_TEXT); 
// Next look for optional variables.
$id = optional_param('id', 0, PARAM_INT);
  
$PAGE->set_pagelayout('standard');
$PAGE->set_heading('OCW Search');

$ocw = new ocw_form('/moodle/blocks/ocw/view.php',array('query'=>$search));

echo $OUTPUT->header();
$ocw->display();
echo $OUTPUT->footer();



?>