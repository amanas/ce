<?php 
    
# apc_delete ("amanas: complex - tournament 3 - run 0");

if ($_GET["action"] == "post") {
    apc_store($_GET["name"], file_get_contents('php://input'), 0);
}

if ($_GET["action"] == "list") {
    $result = array();
    $iter = new APCIterator('user');
    foreach ($iter as $item) {
        array_push($result, $item['key']);
    }
    echo json_encode($result);
}

if ($_GET["action"] == "get") {
    echo apc_fetch($_GET["name"]);
}

?>