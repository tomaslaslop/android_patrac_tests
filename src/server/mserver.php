<?php

function uploadFile($id) {
$target_dir = "/var/www/html/patrac/uploads/".$id."/";
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

switch ($_REQUEST["operation"]) {
    case "getid":
        $id = uniqid();
        echo "ID:".$id;
        mkdir("uploads/".$id."/", 0777);
        break;
    case "sendlocation":
        $SQL = "INSERT INTO locations (id, lat, lon) VALUES ('".$_REQUEST["id"]."', ".$_REQUEST["lat"].", ".$_REQUEST["lon"].")";
        mysql_query($SQL) or die(mysql_error());; 
        echo "OK ".$SQL;
        break;
    case "getlocations":
        $SQL = "SELECT DISTINCT id FROM locations";
        $res = mysql_query($SQL) or die(mysql_error());; 
        while ($row = mysql_fetch_array($res)) { 
            $SQL2 = "SELECT id, lat, lon FROM locations WHERE id = '".$row["id"]."' ORDER BY dt_created DESC LIMIT 1";
            $res2 = mysql_query($SQL2) or die(mysql_error());; 
            $row2 = mysql_fetch_array($res2);
            echo $row["id"].";".$row2["lat"].";".$row2["lon"]."\n";
        }
        break;
    case "getmessages":
        $SQL = "SELECT * FROM messages WHERE id = '".$_REQUEST["id"]."'";
        $res = mysql_query($SQL) or die(mysql_error());; 
        while ($row = mysql_fetch_array($res)) { 
            echo "M;".$row["id"].";".$row["message"].";".$row["file"].";".$row["dt_created"]."\n";
        }
        break;
    case "insertmessage":
        echo "UPLOADING";
        $filename = uploadFile($_REQUEST["id"]);
        $SQL = "INSERT INTO messages (id, message, file) VALUES ('".$_REQUEST["id"]."', '".$_REQUEST["message"]."', '".$filename."')";
        mysql_query($SQL) or die(mysql_error());; 
        echo "OK ".$SQL;
        break;
}

mysql_close();
?>
