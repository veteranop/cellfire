<?php
header("Content-Type: application/json");
define("_JEXEC", 1);
$base = dirname(__FILE__);
require_once $base . "/includes/defines.php";
require_once $base . "/includes/framework.php";
$app = \Joomla\CMS\Factory::getApplication("site");
$db  = \Joomla\CMS\Factory::getContainer()->get(\Joomla\Database\DatabaseInterface::class);

$helperOk = class_exists('\Joomla\CMS\User\UserHelper');

try {
    $hash   = \Joomla\CMS\User\UserHelper::hashPassword("Test1234!");
    $hashOk = true;
} catch (\Throwable $e) {
    $hash   = null;
    $hashOk = $e->getMessage();
}

try {
    $cols    = $db->getTableColumns("#__users", false);
    $colNames = array_keys((array)$cols);
} catch (\Throwable $e) {
    $colNames = "ERROR: " . $e->getMessage();
}

echo json_encode(["helperOk" => $helperOk, "hashOk" => $hashOk, "columns" => $colNames]);
