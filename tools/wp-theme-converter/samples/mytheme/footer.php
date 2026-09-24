</main>
<aside class="sidebar">
    <?php if (is_active_sidebar('sidebar-1')) : ?>
        <ul><?php dynamic_sidebar('sidebar-1'); ?></ul>
    <?php endif; ?>
</aside>
<footer class="site-footer">
    <p>&copy; <?php echo date('Y'); ?> <?php bloginfo('name'); ?></p>
</footer>
<?php wp_footer(); ?>
</body>
</html>
