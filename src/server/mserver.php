<?php

function uploadFile($id) {
$target_dir = "/var/local/patrac/".$id."/";
$target_file = $target_dir . basename($_FILES["fileToUpload"]["name"]);
$uploadOk = 1;

// Check if file already exists
if (file_exists($target_file)) {
    echo "Sorry, file already exists.";
    $uploadOk = 0;
}
// Check file size
if ($_FILES["fileToUpload"]["size"] > 1000000) {
    echo "Sorry, your file is too large.";
    $uploadOk = 0;
}

// Check if $uploadOk is set to 0 by an error
if ($uploadOk == 0) {
    echo "Sorry, your file was not uploaded.";
// if everything is ok, try to upload file
} else {
    if (move_uploaded_file($_FILES["fileToUpload"]["tmp_name"], $target_file)) {
        echo "The file ". basename( $_FILES["fileToUpload"]["name"]). " has been uploaded.";
        return basename( $_FILES["fileToUpload"]["name"]);
    } else {
        echo "Sorry, there was an error uploading your file.";
        return "";
    }
}
}


mysql_connect("127.0.0.1", "*", "*") or die(mysql_error());;
mysql_select_db("patrac") or die(mysql_error());

if (!isset($_REQUEST["searchid"])) die();
if ($_REQUEST["searchid"] == '') die();
$SQL = "SELECT searchid FROM searches WHERE searchid = '".$_REQUEST["searchid"]."'";
$res = mysql_query($SQL) or die(mysql_error());; 
$row = mysql_fetch_array($res);
if ($row["searchid"] != $_REQUEST["searchid"]) die();

switch ($_REQUEST["operation"]) {
    case "getid":
        $id = uniqid();
        echo "ID:".$id;
	    $SQL = "INSERT INTO users (id, user_name, searchid) VALUES ('".$id."', '".$_REQUEST["user_name"]."', '".$_REQUEST["searchid"]."')";
	    mysql_query($SQL) or die(mysql_error()); 
        mkdir("/var/local/patrac/".$id."/", 0777);
        break;
    case "sendlocation":
        $SQL = "INSERT INTO locations (id, lat, lon, searchid) VALUES ('".$_REQUEST["id"]."', ".$_REQUEST["lat"].", ".$_REQUEST["lon"]."', '".$_REQUEST["searchid"]."')";
        mysql_query($SQL) or die(mysql_error());; 
        echo "OK ".$SQL;
        break;
    case "getlocations":
        $SQL = "SELECT DISTINCT id FROM locations";
        $res = mysql_query($SQL) or die(mysql_error());
        while ($row = mysql_fetch_array($res)) { 
            $SQL2 = "SELECT id, lat, lon FROM locations WHERE id = '".$row["id"]."' ORDER BY dt_created DESC LIMIT 1";
            $res2 = mysql_query($SQL2) or die(mysql_error());; 
            $row2 = mysql_fetch_array($res2);
            echo $row["id"].";".$row2["lat"].";".$row2["lon"]."\n";
        }
        break;
    case "getmessages":
        $SQL = "SELECT * FROM messages WHERE id = '".$_REQUEST["id"]."'";
        $res = mysql_query($SQL) or die(mysql_error()); 
        while ($row = mysql_fetch_array($res)) { 
            echo "M;".$row["id"].";".$row["message"].";".$row["file"].";".$row["dt_created"]."\n";
        }
        break;
    case "insertmessage":
        echo "UPLOADING";
        $filename = uploadFile($_REQUEST["id"]);
        $SQL = "INSERT INTO messages (id, message, file, searchid) VALUES ('".$_REQUEST["id"]."', '".$_REQUEST["message"]."', '".$filename."', '".$_REQUEST["searchid"]."'')";
        mysql_query($SQL) or die(mysql_error());
        echo "OK ".$SQL;
        break;
    case "getfile":
        $attachment_location = "/var/local/patrac/".$_REQUEST["id"]."/".$_REQUEST["filename"];
        if (file_exists($attachment_location)) {
            header($_SERVER["SERVER_PROTOCOL"] . " 200 OK");
            header("Cache-Control: public"); // needed for internet explorer
            header("Content-Type: application/octet-stream");
            header("Content-Transfer-Encoding: Binary");
            header("Content-Length:".filesize($attachment_location));
            header("Content-Disposition: attachment; filename=filePath");
            readfile($attachment_location);
            die();
        } else {
            die("Error: File not found.");
        }
        break;
}

mysql_close();
?>
