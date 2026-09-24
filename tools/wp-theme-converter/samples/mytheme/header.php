<!DOCTYPE html>
<html <?php language_attributes(); ?>>
<head>
<meta charset="<?php bloginfo('charset'); ?>">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><?php bloginfo('name'); ?> - <?php bloginfo('description'); ?></title>
<link rel="stylesheet" href="<?php echo get_stylesheet_uri(); ?>">
<script src="<?php echo get_template_directory_uri(); ?>/js/main.js"></script>
<?php wp_head(); ?>
</head>
<body <?php body_class(); ?>>
<header class="site-header">
    <a href="<?php echo home_url(); ?>"><?php bloginfo('name'); ?></a>
    <p><?php bloginfo('description'); ?></p>
</header>
<main>
