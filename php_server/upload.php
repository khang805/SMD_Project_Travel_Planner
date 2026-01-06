<?php
// Ensure uploads directory exists
$target_dir = "uploads/";
if (!file_exists($target_dir)) {
    mkdir($target_dir, 0777, true);
}

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_FILES['image'])) {
    
    $file_name = basename($_FILES["image"]["name"]);
    // Add timestamp to prevent overwrite
    $target_file = $target_dir . time() . "_" . $file_name;
    
    if (move_uploaded_file($_FILES["image"]["tmp_name"], $target_file)) {
        // Construct the URL
        $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
        $host = $_SERVER['HTTP_HOST'];
        
        // Get the directory of the current script to support subdirectories (e.g. localhost/travel_planner/)
        $script_dir = dirname($_SERVER['SCRIPT_NAME']);
        // Ensure script_dir ends with slash if it's not root (dirname of /foo.php is /, but dirname of /bar/foo.php is /bar)
        if ($script_dir === '/' || $script_dir === '\\') {
            $script_dir = ''; 
        }
        
        // URL = http://host/subdir/uploads/filename
        $url = "$protocol://$host$script_dir/$target_file";
        
        $response['status'] = 'success';
        $response['url'] = $url;
    } else {
        $response['status'] = 'error';
        $response['message'] = 'Failed to move uploaded file.';
    }
} else {
    $response['status'] = 'error';
    $response['message'] = 'No image file received.';
}

header('Content-Type: application/json');
echo json_encode($response);
?>
