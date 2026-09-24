<?php get_header(); ?>
<article class="single">
    <h1><?php the_title(); ?></h1>
    <div class="meta"><?php the_time('Y-m-d'); ?> · <?php the_author(); ?></div>
    <?php if (has_post_thumbnail()) : ?>
        <?php the_post_thumbnail(); ?>
    <?php endif; ?>
    <div class="content"><?php the_content(); ?></div>
</article>
<?php comments_template(); ?>
<?php get_footer(); ?>
