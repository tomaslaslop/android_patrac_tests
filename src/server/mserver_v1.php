<?php

function uploadFile($id) {


if (!isset($_FILES["fileToUpload"])) return "";

$target_dir = "/var/local/patrac/".$id."/";
$target_file = $target_dir . basename($_FILES["fileToUpload"]["name"]);
$uploadOk = 1;

// Check if file already exists
if (file_exists($target_file)) {
    echo "Sorry, file already exists.";
    $uploadOk = 0;
    return '';
}
// Check file size
if ($_FILES["fileToUpload"]["size"] > 1000000) {
    echo "Sorry, your file is too large.";
    $uploadOk = 0;
    return '';
}

// Check if $uploadOk is set to 0 by an error
if ($uploadOk == 0) {
    echo "Sorry, your file was not uploaded.";
    // if everything is ok, try to upload file
    return '';
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

function insertMessage($id, $message, $filename, $searchid, $shared, $from_id) {
        $SQL = "INSERT INTO messages (id, message, file, searchid, shared, from_id) VALUES ('".$id."', '".$message."', '".$filename."', '".$searchid."', ".$shared.", '".$from_id."')";
        mysql_query($SQL) or die($SQL." ".mysql_error());
        echo "OK ".$SQL;
}

//file_put_contents("/tmp/mserver_debug.txt", file_get_contents("php://input"));
//echo "OK";

mysql_connect("127.0.0.1", "username", "userpassword") or die(mysql_error());;
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
        if ($_REQUEST["lat"] == "" || $_REQUEST["lon"] == "") {
        } else {  
            $SQL = "INSERT INTO locations (id, lat, lon, searchid, dt_created) VALUES ('".$id."', ".$_REQUEST["lat"].", ".$_REQUEST["lon"].", '".$_REQUEST["searchid"]."', utc_timestamp())";
            mysql_query($SQL) or die(mysql_error());
        }
        mkdir("/var/local/patrac/".$id."/", 0777);
        break;
    
    case "sendlocation":
        $SQL = "INSERT INTO locations (id, lat, lon, searchid, dt_created) VALUES ('".$_REQUEST["id"]."', ".$_REQUEST["lat"].", ".$_REQUEST["lon"].", '".$_REQUEST["searchid"]."', utc_timestamp())";
        mysql_query($SQL) or die(mysql_error());; 
        echo "Positions saved:1";
        break;

    case "sendlocations":
        $coordsString = $_REQUEST["coords"];
        $coords = explode(",", $coordsString);
        $count = 0;
        foreach($coords as $coord) {
            $coordString = trim($coord);
            $coord = explode(";", $coordString);
            $SQL = "INSERT INTO locations (id, lon, lat, searchid, dt_created) VALUES ('".$_REQUEST["id"]."', ".$coord[0].", ".$coord[1].", '".$_REQUEST["searchid"]."', utc_timestamp())";
            mysql_query($SQL) or die(mysql_error()." ".$SQL);
            $count++; 
        }
        echo "Positions saved:".$count;
        break;

    case "getlocations":
        $SQL = "SELECT DISTINCT id FROM locations WHERE searchid = '".$_REQUEST["searchid"]."'";
        $res = mysql_query($SQL) or die(mysql_error());
        while ($row = mysql_fetch_array($res)) { 
            $SQL2 = "SELECT id, lat, lon, dt_created FROM locations WHERE id = '".$row["id"]."' ORDER BY dt_created DESC LIMIT 1";
            $res2 = mysql_query($SQL2) or die(mysql_error());; 
            $row2 = mysql_fetch_array($res2);
            $SQL3 = "SELECT user_name FROM users WHERE id = '".$row["id"]."'";
            $res3 = mysql_query($SQL3) or die(mysql_error());; 
            $row3 = mysql_fetch_array($res3);
            $diff = strtotime(date('Y-m-d H:i:s')) - strtotime($row2["dt_created"]);
            if ($diff > 300) {
               echo $row["id"].";".$row2["dt_created"].";D;".$row3["user_name"].";".$row2["lon"]." ".$row2["lat"].";".$diff."\n"; 
            } else {
               echo $row["id"].";".$row2["dt_created"].";A;".$row3["user_name"].";".$row2["lon"]." ".$row2["lat"].";".$diff."\n";
            }
        }
        break;

    case "gettracks":
        $SQL = "SELECT DISTINCT id FROM locations WHERE searchid = '".$_REQUEST["searchid"]."'";
        $res = mysql_query($SQL) or die(mysql_error());
        while ($row = mysql_fetch_array($res)) { 
            $SQL2 = "SELECT id, lat, lon, dt_created FROM locations WHERE id = '".$row["id"]."' ORDER BY dt_created DESC LIMIT 1";
            $res2 = mysql_query($SQL2) or die(mysql_error());
            $row2 = mysql_fetch_array($res2);
            $SQL3 = "SELECT user_name FROM users WHERE id = '".$row["id"]."'";
            $res3 = mysql_query($SQL3) or die(mysql_error()); 
            $row3 = mysql_fetch_array($res3);
            $diff = strtotime(date('Y-m-d H:i:s')) - strtotime($row2["dt_created"]);
            $SQL4 = "SELECT lat, lon FROM locations WHERE id = '".$row["id"]."' ORDER BY dt_created";
            $res4 = mysql_query($SQL4) or die(mysql_error());
            $points="";
            $position = 0;           
            while ($row4 = mysql_fetch_array($res4)) {
                if ($position > 0) {
                   $points=$points.";".$row4["lon"]." ".$row4["lat"]; 
                } else {
                   $points=$row4["lon"]." ".$row4["lat"];
                }
                $position++;
            }  
            if ($diff > 300) {
               echo $row["id"].";".$row2["dt_created"].";D;".$row3["user_name"].";".$points."\n"; 
            } else {
               echo $row["id"].";".$row2["dt_created"].";A;".$row3["user_name"].";".$points."\n";
            }
        }
        break;

    case "getgpx_last":
        header($_SERVER["SERVER_PROTOCOL"] . " 200 OK");
        header("Cache-Control: public"); // needed for internet explorer
        header("Content-Type: application/gpx+xml");
        header("Content-Transfer-Encoding: Binary");
        header("Content-Disposition: attachment; filename=server_last.gpx");
        $SQL = "SELECT DISTINCT id FROM locations WHERE searchid = '".$_REQUEST["searchid"]."'";
        if (isset($_REQUEST["id"])) $SQL .= " AND id = '".$_REQUEST["id"]."'";
        $res = mysql_query($SQL) or die(mysql_error());
        echo "<?xml version=\"1.0\"?>\n";
        echo "<gpx version=\"1.1\" creator=\"Patrac Server\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ogr=\"http://osgeo.org/gdal\" xmlns=\"http://www.topografix.com/GPX/1/1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n";
        while ($row = mysql_fetch_array($res)) { 
            $SQL2 = "SELECT lat, lon, dt_created FROM locations WHERE id = '".$row["id"]."' ORDER BY dt_created DESC LIMIT 1";
            $res2 = mysql_query($SQL2) or die(mysql_error());
            $SQL3 = "SELECT user_name FROM users WHERE id = '".$row["id"]."'";
            $res3 = mysql_query($SQL3) or die(mysql_error()); 
            $row3 = mysql_fetch_array($res3);
            //echo "<trk><name>".$row3["user_name"]." (".$row["id"].")</name>\n";
            //echo "<trkseg>\n";
            while ($row2 = mysql_fetch_array($res2)) {
               $timeutc = str_replace(" ","T", $row2["dt_created"])."Z";
               echo "<wpt lat=\"".$row2["lat"]."\" lon=\"".$row2["lon"]."\"><name>".$row3["user_name"]."</name><desc>SessionId: ".$row["id"]."</desc><time>".$timeutc."</time></wpt>\n";
            }
            //echo "</trkseg></trk>";
        }
        echo "</gpx>";
        die();
        break;
    
    case "getgpx":
        header($_SERVER["SERVER_PROTOCOL"] . " 200 OK");
        header("Cache-Control: public"); // needed for internet explorer
        header("Content-Type: application/gpx+xml");
        header("Content-Transfer-Encoding: Binary");
        header("Content-Disposition: attachment; filename=server.gpx");
        $SQL = "SELECT DISTINCT id FROM locations WHERE searchid = '".$_REQUEST["searchid"]."'";
        if (isset($_REQUEST["id"])) $SQL .= " AND id = '".$_REQUEST["id"]."'";
        $res = mysql_query($SQL) or die(mysql_error());
        echo "<?xml version=\"1.0\"?>\n";
        echo "<gpx version=\"1.1\" creator=\"Patrac Server\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ogr=\"http://osgeo.org/gdal\" xmlns=\"http://www.topografix.com/GPX/1/1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n";
        while ($row = mysql_fetch_array($res)) { 
            $SQL2 = "SELECT lat, lon, dt_created FROM locations WHERE id = '".$row["id"]."' ORDER BY dt_created DESC";
            $res2 = mysql_query($SQL2) or die(mysql_error());
            $SQL3 = "SELECT user_name FROM users WHERE id = '".$row["id"]."'";
            $res3 = mysql_query($SQL3) or die(mysql_error()); 
            $row3 = mysql_fetch_array($res3);
            echo "<trk><name>".$row3["user_name"]."</name><desc>SessionId: ".$row["id"]."</desc>\n";
            echo "<trkseg>\n";
            while ($row2 = mysql_fetch_array($res2)) {
               $timeutc = str_replace(" ","T", $row2["dt_created"])."Z";
               echo "<trkpt lat=\"".$row2["lat"]."\" lon=\"".$row2["lon"]."\"><name>".$row3["user_name"]."</name><time>".$timeutc."</time></trkpt>\n";
            }
            echo "</trkseg></trk>";
        }
        echo "</gpx>";
        die();
        break;

    case "getmessages":
        //LIMIT 1 aby odešla v jednom požadavku vždy jen jedna zpráva
        $SQL = "SELECT * FROM messages WHERE id = '".$_REQUEST["id"]."' and readed <> 1 LIMIT 1";
        $res = mysql_query($SQL) or die(mysql_error()); 
        while ($row = mysql_fetch_array($res)) { 
            echo "M;".$row["id"].";".$row["message"].";".$row["file"].";".$row["dt_created"].";".$row["shared"]."\n";
            $SQL = "UPDATE messages SET readed = 1 WHERE sysid = ".$row["sysid"];
            mysql_query($SQL) or die(mysql_error()); 
        }
        break;

    case "insertmessage":
        $from_id = $_REQUEST["from_id"];
        if ($from_id == "") $from_id = "NN" + uniqid();
        $filename = uploadFile($_REQUEST["id"]);
        if ($filename == '') echo "NO FILE PROVIDED ";
        insertMessage($_REQUEST["id"], $_REQUEST["message"], $filename, $_REQUEST["searchid"], 0, $from_id);
        break;

    case "insertmessages":
        $from_id = $_REQUEST["from_id"];
        if ($from_id == "") $from_id = "NN" + uniqid();
        if (strpos($_REQUEST["ids"], ';') !== false) {
            echo "UPLOADING ";
            $filename = uploadFile("shared");
            if ($filename == '') echo "NO FILE PROVIDED ";
            $ids = explode(";", $_REQUEST["ids"]);
            foreach($ids as $id) {
                $id = trim($id);
                insertMessage($id, $_REQUEST["message"], $filename, $_REQUEST["searchid"], 1, $from_id);
            }
        } else {
            $filename = uploadFile($_REQUEST["ids"]);
            if ($filename == '') echo "NO FILE PROVIDED ";
            insertMessage($_REQUEST["ids"], $_REQUEST["message"], $filename, $_REQUEST["searchid"], 0, $from_id);
        }
        break;

    case "getfile":
        $attachment_location = "/var/local/patrac/".$_REQUEST["id"]."/".$_REQUEST["filename"];
        if (file_exists($attachment_location)) {
            header($_SERVER["SERVER_PROTOCOL"] . " 200 OK");
            header("Cache-Control: public"); // needed for internet explorer
            header("Content-Type: application/octet-stream");
            header("Content-Transfer-Encoding: Binary");
            header("Content-Length:".filesize($attachment_location));
            header("Content-Disposition: attachment; filename=".$_REQUEST["filename"]);
            readfile($attachment_location);
            die();
        } else {
            die("Error: File not found.");
        }
        break;
}

mysql_close();

/*
http://gisak.vsb.cz/patrac/mserver.php?operation=sendlocation&searchid=AAA111BBB&id=5a6715f7a244c&lat=10&lon=20

curl --form name=myfileparam --form file=@/home/jencek/test.gpx -Fjson='{'message': message, 'id': id, 'operation': 'insertmessage', 'searchid': 'AAA111BBB', 'fileToUpload', 'file'}' -Fsubmit=Build http://gisak.vsb.cz/patrac/mserver.php


curl --form name=myfileparam --form file=@/home/jencek/test.gpx -Fjson='{"parameter": {'message': message, 'id': id, 'operation': 'insertmessage', 'searchid': 'AAA111BBB', 'fileToUpload', 'file'}}' -Fsubmit=Build http://gisak.vsb.cz/patrac/mserver.php

data = json.dumps({'message': message, 'id': id, 'operation': 'insertmessage', 'searchid': 'AAA111BBB'})
        with open(filename1, 'rb') as f: r = requests.post('http://gisak.vsb.cz/patrac/mserver.php', data = {'message': message, 'id': id, 'operation': 'insertmessage'}, files={'fileToUpload': f})
        print r.text


-Fjson='{"parameter": {"name": "myfileparam", "file": "file"}}'

curl -X POST -F 'image=@/path/to/pictures/picture.jpg' http://domain.tld/upload
curl -X POST -d "searchid=AAA111BBB&operation=insertmessage&id=5a671dc761847&message=AAA" http://gisak.vsb.cz/patrac/mserver.php
curl -X POST -F 'searchid=AAA111BBB&operation=insertmessage&id=5a671dc761847&message=AAA&fileToUpload=@/home/jencek/test.gpx' http://gisak.vsb.cz/patrac/mserver.php

curl --form searchid=AAA111BBB --form operation=insertmessage --form id=5a671dc761847 --form message=AAA --form fileToUpload=@/home/jencek/test.gpx http://gisak.vsb.cz/patrac/mserver.php

*/

?>