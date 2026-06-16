<?php
/**
 * Cellfire Template — Article default.php (Layout Router)
 *
 * Detects the active menu item alias and auto-loads the matching
 * custom layout. No manual Layout selection needed in the article editor.
 *
 * To add a new page:
 *  1. Create the layout file:  html/com_content/article/cf_[alias].php
 *  2. Add the alias to $layoutMap below
 *  3. Create a Joomla article + menu item with that alias
 */
defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\HTML\HTMLHelper;

$app        = Factory::getApplication();
$activeItem = $app->getMenu()->getActive();
$alias      = $activeItem ? strtolower(trim($activeItem->alias)) : '';

// Map menu alias → layout file (without .php)
$layoutMap = [
	'app'       => 'cf_android',   // Android app showcase with slideshow
	'android'   => 'cf_android',
	'studio'    => 'cf_studio',
	'viewer'    => 'cf_viewer',
	'about'     => 'cf_about',
	'downloads' => 'cf_downloads',
	'account'   => 'cf_account',
	'pricing'   => 'cf_pricing',
	'register'  => 'cf_register',
	'privacy'   => 'cf_privacy',
	'terms'     => 'cf_terms',
];

if (isset($layoutMap[$alias])) {
	$layoutFile = __DIR__ . '/' . $layoutMap[$alias] . '.php';
	if (file_exists($layoutFile)) {
		include $layoutFile;
		return;
	}
}

// ── Default article rendering (all other articles) ──────────────────────────
HTMLHelper::_('bootstrap.collapse');
?>
<div class="cf-article-wrap">

	<?php if ($this->params->get('show_title', 1)) : ?>
	<header class="cf-article-header"><meta charset="utf-8">
		<?php if ($this->item->catid && $this->params->get('show_category')) : ?>
		<div class="cf-article-category"><?= htmlspecialchars($this->item->category_title); ?></div>
		<?php endif; ?>
		<h1 class="cf-article-title"><?= $this->escape($this->item->title); ?></h1>
		<?php if ($this->params->get('show_publish_date')) : ?>
		<div class="cf-article-date"><?= HTMLHelper::_('date', $this->item->publish_up, 'F j, Y'); ?></div>
		<?php endif; ?>
	</header>
	<?php endif; ?>

	<div class="cf-article-body">
		<?= $this->item->text; ?>
	</div>

</div>

<style>
.cf-article-wrap { max-width: 780px; }
.cf-article-header { margin-bottom: 32px; padding-bottom: 24px; border-bottom: 1px solid var(--cf-border); }
.cf-article-category { font-size: 11px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; color: var(--cf-accent); margin-bottom: 10px; }
.cf-article-title { font-size: clamp(26px, 4vw, 40px); font-weight: 700; letter-spacing: -.5px; line-height: 1.15; }
.cf-article-date { font-size: 13px; color: var(--cf-text-2); margin-top: 10px; }
.cf-article-body { font-size: 15px; line-height: 1.8; color: var(--cf-text-2); }
.cf-article-body h2, .cf-article-body h3 { color: var(--cf-text); margin: 32px 0 12px; font-weight: 700; }
.cf-article-body h2 { font-size: 22px; }
.cf-article-body h3 { font-size: 18px; }
.cf-article-body p  { margin-bottom: 16px; }
.cf-article-body a  { color: var(--cf-accent); }
.cf-article-body ul, .cf-article-body ol { padding-left: 24px; margin-bottom: 16px; }
.cf-article-body li { margin-bottom: 6px; }
.cf-article-body img { border-radius: var(--cf-r-md); max-width: 100%; height: auto; }
.cf-article-body blockquote { border-left: 3px solid var(--cf-accent); padding: 12px 20px; margin: 24px 0; color: var(--cf-text-2); background: var(--cf-bg-2); border-radius: 0 var(--cf-r-md) var(--cf-r-md) 0; }
.cf-article-body code { font-family: var(--cf-font-mono, monospace); font-size: 13px; background: var(--cf-bg-2); border: 1px solid var(--cf-border); border-radius: var(--cf-r-sm); padding: 2px 6px; }
.cf-article-body pre { background: var(--cf-bg-2); border: 1px solid var(--cf-border); border-radius: var(--cf-r-md); padding: 16px; overflow-x: auto; margin-bottom: 16px; }
.cf-article-body pre code { background: none; border: none; padding: 0; }
</style>
