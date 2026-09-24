<?php get_header(); ?>
<div class="post-list">
<?php if (have_posts()) : while (have_posts()) : the_post(); ?>
    <article class="post-item">
        <h2><a href="<?php the_permalink(); ?>"><?php the_title(); ?></a></h2>
        <div class="meta"><?php the_time('Y-m-d'); ?> · <?php the_author(); ?></div>
        <p><?php the_excerpt(); ?></p>
    </article>
<?php endwhile; else : ?>
    <p>暂无内容</p>
<?php endif; ?>
</div>
<?php get_sidebar(); ?>
<?php get_footer(); ?>
