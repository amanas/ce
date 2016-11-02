<?php 
    
if (!empty(file_get_contents('php://input'))) {
    apc_store('status', file_get_contents('php://input'));
} else {
    echo apc_fetch('status');
}
    
?>