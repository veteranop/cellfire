/**
 * Cellfire Template — template.js
 */
(function () {
	'use strict';

	/* ---- Gallery / Slideshow ---- */
	(function initGallery() {
		// Tab switching
		document.querySelectorAll('.cf-gtab').forEach(function (tab) {
			tab.addEventListener('click', function () {
				var targetId = tab.getAttribute('data-target');
				// Deactivate all tabs and slideshows
				tab.closest('.cf-gallery, .cf-section').querySelectorAll('.cf-gtab').forEach(function (t) {
					t.classList.remove('active');
				});
				document.querySelectorAll('.cf-slideshow').forEach(function (s) {
					s.classList.remove('active');
				});
				// Activate selected
				tab.classList.add('active');
				var target = document.getElementById(targetId);
				if (target) target.classList.add('active');
			});
		});

		// Auto-init: for each gallery, activate the tab whose data-target matches the
		// first .cf-slideshow.active; if none is active, activate the first tab/show.
		document.querySelectorAll('.cf-gallery, .cf-section:has(.cf-slideshow)').forEach(function (gallery) {
			var tabs  = Array.from(gallery.querySelectorAll('.cf-gtab'));
			var shows = Array.from(gallery.querySelectorAll('.cf-slideshow'));
			if (!tabs.length || !shows.length) return;

			// Deactivate everything
			tabs.forEach(function (t)  { t.classList.remove('active'); });
			shows.forEach(function (s) { s.classList.remove('active'); });

			// Activate the first tab and its target
			tabs[0].classList.add('active');
			var firstTarget = document.getElementById(tabs[0].getAttribute('data-target'));
			if (firstTarget) firstTarget.classList.add('active');
		});

		// Init each slideshow
		document.querySelectorAll('.cf-slideshow').forEach(function (show) {
			var track  = show.querySelector('.cf-slide-track');
			var slides = show.querySelectorAll('.cf-slide');
			var prev   = show.querySelector('.cf-slide-prev');
			var next   = show.querySelector('.cf-slide-next');
			var dotsEl = show.querySelector('.cf-slide-dots');
			if (!track || slides.length === 0) return;

			// Force-load any lazy images so off-screen slides are ready
			slides.forEach(function (slide) {
				slide.querySelectorAll('img[loading="lazy"]').forEach(function (img) {
					img.removeAttribute('loading');
				});
			});

			var current = 0;

			// Build dots
			var dots = [];
			if (dotsEl) {
				slides.forEach(function (_, i) {
					var dot = document.createElement('span');
					dot.className = 'cf-slide-dot' + (i === 0 ? ' active' : '');
					dot.addEventListener('click', function () { goTo(i); });
					dotsEl.appendChild(dot);
					dots.push(dot);
				});
			}

			function goTo(idx) {
				current = (idx + slides.length) % slides.length;
				track.style.transform = 'translateX(-' + (current * 100) + '%)';
				dots.forEach(function (d, i) {
					d.classList.toggle('active', i === current);
				});
			}

			if (prev) prev.addEventListener('click', function () { goTo(current - 1); });
			if (next) next.addEventListener('click', function () { goTo(current + 1); });

			// Touch/swipe support
			var touchStartX = null;
			track.addEventListener('touchstart', function (e) {
				touchStartX = e.touches[0].clientX;
			}, { passive: true });
			track.addEventListener('touchend', function (e) {
				if (touchStartX === null) return;
				var dx = e.changedTouches[0].clientX - touchStartX;
				if (Math.abs(dx) > 40) goTo(dx < 0 ? current + 1 : current - 1);
				touchStartX = null;
			}, { passive: true });
		});
	})();

	/* ---- Mobile Navigation ---- */
	const hamburger = document.getElementById('cf-hamburger');
	const navMenu   = document.getElementById('cf-nav-menu');

	if (hamburger && navMenu) {
		// Remember original DOM position so we can restore on close
		var navMenuParent   = navMenu.parentNode;
		var navMenuNextSibling = navMenu.nextSibling;

		function openMenu() {
			// Portal to <body> so z-index is in the root stacking context,
			// above all page elements (Leaflet canvas, cfm-panel, etc.)
			document.body.appendChild(navMenu);
			navMenu.classList.add('is-open');
			hamburger.setAttribute('aria-expanded', 'true');
			document.body.style.overflow = 'hidden';
		}

		function closeMenu() {
			navMenu.classList.remove('is-open');
			hamburger.setAttribute('aria-expanded', 'false');
			document.body.style.overflow = '';
			// Restore to original position in header
			navMenuParent.insertBefore(navMenu, navMenuNextSibling);
		}

		hamburger.addEventListener('click', function (e) {
			e.stopPropagation();
			if (navMenu.classList.contains('is-open')) {
				closeMenu();
			} else {
				openMenu();
			}
		});

		// Close on outside click
		document.addEventListener('click', function (e) {
			if (navMenu.classList.contains('is-open') &&
				!hamburger.contains(e.target) && !navMenu.contains(e.target)) {
				closeMenu();
			}
		});

		// Close on Escape
		document.addEventListener('keydown', function (e) {
			if (e.key === 'Escape' && navMenu.classList.contains('is-open')) {
				closeMenu();
			}
		});

		// Close menu on nav link click (navigate away)
		navMenu.addEventListener('click', function (e) {
			if (e.target.tagName === 'A' && e.target.getAttribute('href') &&
				!e.target.getAttribute('href').startsWith('#')) {
				closeMenu();
			}
		});
	}

	/* ---- Header scroll class ---- */
	var header = document.getElementById('site-header');
	if (header) {
		window.addEventListener('scroll', function () {
			header.classList.toggle('scrolled', window.scrollY > 10);
		}, { passive: true });
	}

	/* ---- Smooth scroll for in-page anchors ---- */
	document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
		anchor.addEventListener('click', function (e) {
			var id = this.getAttribute('href');
			if (id === '#') return;
			var target = document.querySelector(id);
			if (target) {
				e.preventDefault();
				var offset = parseInt(getComputedStyle(document.documentElement)
					.getPropertyValue('--cf-header-h')) || 64;
				var top = target.getBoundingClientRect().top + window.scrollY - offset - 8;
				window.scrollTo({ top: top, behavior: 'smooth' });

				// Close mobile menu if open
				if (navMenu) {
					navMenu.classList.remove('is-open');
					if (hamburger) hamburger.setAttribute('aria-expanded', 'false');
					document.body.style.overflow = '';
				}
			}
		});
	});

	/* ---- Intersection observer: staggered fade-in ---- */
	var observer = new IntersectionObserver(function (entries) {
		entries.forEach(function (entry) {
			if (entry.isIntersecting) {
				entry.target.classList.add('cf-visible');
				observer.unobserve(entry.target);
			}
		});
	}, { threshold: 0.07 });

	// Add stagger delays to grid children then observe
	function staggerAndObserve(selector) {
		var delayClasses = ['cf-animate--d1','cf-animate--d2','cf-animate--d3',
		                    'cf-animate--d4','cf-animate--d5','cf-animate--d6'];
		document.querySelectorAll(selector).forEach(function (el) {
			// Find sibling index within parent grid
			var siblings = Array.from(el.parentElement.children);
			var idx = siblings.indexOf(el) % 6;
			el.classList.add('cf-animate', delayClasses[idx]);
			observer.observe(el);
		});
	}

	staggerAndObserve('.cf-product-card');
	staggerAndObserve('.cf-pricing-card');
	staggerAndObserve('.cf-feature-block');
	staggerAndObserve('.cf-icon-feature');
	staggerAndObserve('.cf-dl-card');
	staggerAndObserve('.cf-about-stat');
	staggerAndObserve('.cf-persona-card');

	// Observe standalone animated elements
	document.querySelectorAll('.cf-step, .cf-animate').forEach(function (el) {
		if (!el.classList.contains('cf-step') && !el.classList.contains('cf-diff-callout')) return;
		observer.observe(el);
	});

})();

/* ---- Coverage Map: Panel Search Filter ---- */
(function () {
	var panelBody = document.getElementById('cfm-panel-body');
	if (!panelBody) return;

	new MutationObserver(function () {
		if (document.getElementById('cfm-search')) return;
		var tbl = panelBody.querySelector('.cfm-pci-tbl');
		if (!tbl) return;

		var tableWrap = tbl.parentNode;
		var searchWrap = document.createElement('div');
		searchWrap.style.cssText = 'padding:6px 14px 8px;border-bottom:1px solid rgba(255,255,255,.06);flex-shrink:0';
		searchWrap.innerHTML =
			'<input id="cfm-search" type="text" autocomplete="off"' +
			' placeholder="Filter PCI, TAC, carrier, band…"' +
			' style="width:100%;background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.12);' +
			'border-radius:5px;padding:6px 10px;font-size:12px;color:#fff;outline:none;' +
			'font-family:\'Inter\',sans-serif;box-sizing:border-box">';
		panelBody.insertBefore(searchWrap, tableWrap);

		document.getElementById('cfm-search').addEventListener('input', function () {
			var q = this.value.trim().toLowerCase();
			var tbody = panelBody.querySelector('.cfm-pci-tbl tbody');
			if (!tbody) return;
			var anyVisible = false;
			Array.from(tbody.rows).forEach(function (row) {
				if (row.id === 'cfm-empty') return;
				var match = !q || row.textContent.toLowerCase().indexOf(q) !== -1;
				row.style.display = match ? '' : 'none';
				if (match) anyVisible = true;
			});
			var empty = document.getElementById('cfm-empty');
			if (!anyVisible && q) {
				if (!empty) {
					empty = document.createElement('tr');
					empty.id = 'cfm-empty';
					empty.innerHTML = '<td colspan="5" style="padding:12px 8px;color:rgba(255,255,255,.25);' +
						'text-align:center;font-size:12px;font-family:\'Inter\',sans-serif">No matches</td>';
					tbody.appendChild(empty);
				}
			} else if (empty) {
				empty.remove();
			}
		});
	}).observe(panelBody, { childList: true });
}());
