/* Sehat landing - interactions & motion */
(function () {
  'use strict';
  var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---- Nav: shadow on scroll ---- */
  var nav = document.getElementById('nav');
  function onScroll() {
    if (window.scrollY > 12) nav.classList.add('scrolled');
    else nav.classList.remove('scrolled');
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  /* ---- Mobile menu: jump to features (lightweight) ---- */
  var toggle = document.getElementById('navToggle');
  if (toggle) {
    toggle.addEventListener('click', function () {
      var f = document.getElementById('features');
      if (f) f.scrollIntoView ? f.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth' }) : (location.hash = '#features');
    });
  }

  /* ---- Scroll reveal ---- */
  var revealEls = [].slice.call(document.querySelectorAll('.reveal'));
  if (reduce || !('IntersectionObserver' in window)) {
    revealEls.forEach(function (el) { el.classList.add('in'); });
  } else {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
      });
    }, { threshold: 0.14, rootMargin: '0px 0px -8% 0px' });
    revealEls.forEach(function (el) { io.observe(el); });
  }

  /* ---- Animated count-up for hero step widget ---- */
  function animateCount(el, target, duration, formatter) {
    if (!el) return;
    if (reduce) { el.innerHTML = formatter ? formatter(target) : target; return; }
    var start = null;
    function frame(ts) {
      if (start === null) start = ts;
      var p = Math.min((ts - start) / duration, 1);
      var eased = 1 - Math.pow(1 - p, 3);
      var val = Math.round(target * eased);
      el.innerHTML = formatter ? formatter(val) : val;
      if (p < 1) requestAnimationFrame(frame);
    }
    requestAnimationFrame(frame);
  }

  var stepEl = document.getElementById('stepCount');
  var bpmEl = document.getElementById('bpmCount');
  var heroStarted = false;
  function startHero() {
    if (heroStarted) return;
    heroStarted = true;
    animateCount(stepEl, 7842, 1700, function (v) { return v.toLocaleString('en-US'); });
    animateCount(bpmEl, 79, 1300, function (v) { return v + ' <small>BPM</small>'; });
  }
  // kick off shortly after load so the entrance reveal lands first
  window.addEventListener('load', function () { setTimeout(startHero, 350); });
  setTimeout(startHero, 1200); // fallback if load already fired

  /* ---- FAQ: keep only one open (accordion feel) ---- */
  var items = [].slice.call(document.querySelectorAll('.faq__item'));
  items.forEach(function (item) {
    item.addEventListener('toggle', function () {
      if (item.open) {
        items.forEach(function (other) { if (other !== item) other.open = false; });
      }
    });
  });
})();
