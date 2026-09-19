/**
 * Ozalpa Superpowers Wiki - Core JavaScript Utilities
 * - Theme switcher (light / dark) with localStorage persistence
 * - Mermaid.js integration with dynamic theme synchronization
 * - Auto-wrapping code blocks with copy-to-clipboard buttons
 * - Sidebar active link auto-detection and search filter
 * - Mobile sidebar toggler handling
 */

(function () {
  'use strict';

  const THEME_STORAGE_KEY = 'ozalpa-wiki-theme';

  /**
   * Determine preferred theme
   */
  function getPreferredTheme() {
    const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    if (storedTheme) {
      return storedTheme;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  /**
   * Apply theme to <html> and update UI toggle buttons
   */
  function setTheme(theme) {
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem(THEME_STORAGE_KEY, theme);

    // Update theme toggle icons/buttons if present
    const toggleBtns = document.querySelectorAll('[data-wiki-theme-toggle]');
    toggleBtns.forEach((btn) => {
      const icon = btn.querySelector('i');
      if (icon) {
        if (theme === 'dark') {
          icon.className = 'bi bi-sun-fill';
          btn.setAttribute('title', 'Mudar para tema claro');
        } else {
          icon.className = 'bi bi-moon-stars-fill';
          btn.setAttribute('title', 'Mudar para tema escuro');
        }
      }
    });

    // Notify Mermaid of theme change if already initialized
    if (window.mermaid && typeof window.mermaid.initialize === 'function') {
      try {
        window.mermaid.initialize({
          startOnLoad: false,
          theme: theme === 'dark' ? 'dark' : 'default',
          securityLevel: 'loose',
          fontFamily: 'system-ui, -apple-system, sans-serif'
        });
      } catch (e) {
        console.warn('Mermaid re-init error:', e);
      }
    }
  }

  // Apply theme immediately to prevent flash
  const initialTheme = getPreferredTheme();
  setTheme(initialTheme);

  /**
   * Initialize Mermaid Diagrams
   */
  function initMermaid() {
    if (!window.mermaid) return;

    const currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';
    mermaid.initialize({
      startOnLoad: true,
      theme: currentTheme === 'dark' ? 'dark' : 'default',
      securityLevel: 'loose',
      fontFamily: 'system-ui, -apple-system, sans-serif',
      flowchart: {
        useMaxWidth: true,
        htmlLabels: true,
        curve: 'basis'
      }
    });
  }

  /**
   * Setup code block copy buttons
   */
  function initCodeCopyButtons() {
    const preBlocks = document.querySelectorAll('pre');
    preBlocks.forEach((pre) => {
      // Check if wrapper already exists
      if (pre.parentElement && pre.parentElement.classList.contains('code-block-wrapper')) {
        return;
      }

      const codeElement = pre.querySelector('code');
      if (!codeElement) return;

      const wrapper = document.createElement('div');
      wrapper.className = 'code-block-wrapper';
      pre.parentNode.insertBefore(wrapper, pre);
      wrapper.appendChild(pre);

      const copyBtn = document.createElement('button');
      copyBtn.type = 'button';
      copyBtn.className = 'btn btn-sm btn-outline-secondary btn-copy';
      copyBtn.innerHTML = '<i class="bi bi-clipboard"></i>';
      copyBtn.setAttribute('title', 'Copiar código');

      copyBtn.addEventListener('click', async () => {
        const textToCopy = codeElement.innerText;
        try {
          if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(textToCopy);
          } else {
            // Fallback for file:// or unsecure context
            const textarea = document.createElement('textarea');
            textarea.value = textToCopy;
            textarea.style.position = 'fixed';
            textarea.style.left = '-9999px';
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
          }

          copyBtn.innerHTML = '<i class="bi bi-check2 text-success"></i>';
          copyBtn.classList.add('border-success');
          setTimeout(() => {
            copyBtn.innerHTML = '<i class="bi bi-clipboard"></i>';
            copyBtn.classList.remove('border-success');
          }, 2000);
        } catch (err) {
          console.error('Falha ao copiar:', err);
          copyBtn.innerHTML = '<i class="bi bi-x text-danger"></i>';
          setTimeout(() => {
            copyBtn.innerHTML = '<i class="bi bi-clipboard"></i>';
          }, 2000);
        }
      });

      wrapper.appendChild(copyBtn);
    });
  }

  /**
   * Highlight active sidebar item according to current URL
   */
  function initSidebarActiveState() {
    const currentPath = window.location.pathname.split('/').pop() || 'index.html';
    const navLinks = document.querySelectorAll('.wiki-nav-link');

    navLinks.forEach((link) => {
      const href = link.getAttribute('href');
      if (!href) return;
      const targetPage = href.split('/').pop();
      if (targetPage === currentPath) {
        link.classList.add('active');
      } else {
        link.classList.remove('active');
      }
    });
  }

  /**
   * Sidebar search filter
   */
  function initSidebarSearch() {
    const searchInput = document.getElementById('wiki-sidebar-search-input');
    if (!searchInput) return;

    searchInput.addEventListener('input', (e) => {
      const query = e.target.value.toLowerCase().trim();
      const navLinks = document.querySelectorAll('.wiki-nav-link');
      const navGroups = document.querySelectorAll('.wiki-nav-group');

      navLinks.forEach((link) => {
        const text = link.textContent.toLowerCase();
        const matches = text.includes(query);
        link.style.display = matches ? 'flex' : 'none';
      });

      // Hide headers if all links in group are hidden
      navGroups.forEach((group) => {
        const visibleLinks = group.querySelectorAll('.wiki-nav-link[style*="display: flex"]');
        const header = group.querySelector('.wiki-nav-header');
        if (header) {
          if (query && visibleLinks.length === 0) {
            header.style.display = 'none';
          } else {
            header.style.display = 'flex';
          }
        }
      });
    });
  }

  /**
   * Setup Mobile Sidebar Toggle
   */
  function initMobileSidebar() {
    const toggleBtn = document.querySelector('[data-wiki-toggle="sidebar"]');
    const sidebar = document.querySelector('.wiki-sidebar');
    if (!toggleBtn || !sidebar) return;

    // Create backdrop if not existing
    let backdrop = document.querySelector('.wiki-sidebar-backdrop');
    if (!backdrop) {
      backdrop = document.createElement('div');
      backdrop.className = 'wiki-sidebar-backdrop';
      document.body.appendChild(backdrop);
    }

    const toggle = () => {
      const isOpen = sidebar.classList.toggle('show');
      backdrop.classList.toggle('show', isOpen);
    };

    toggleBtn.addEventListener('click', toggle);
    backdrop.addEventListener('click', toggle);
  }

  /**
   * Setup Theme Switcher Click Handler
   */
  function initThemeToggle() {
    const toggleBtns = document.querySelectorAll('[data-wiki-theme-toggle]');
    toggleBtns.forEach((btn) => {
      btn.addEventListener('click', () => {
        const currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        setTheme(newTheme);
      });
    });
  }

  // DOM Content Loaded Handler
  document.addEventListener('DOMContentLoaded', () => {
    initThemeToggle();
    initMermaid();
    initCodeCopyButtons();
    initSidebarActiveState();
    initSidebarSearch();
    initMobileSidebar();
  });
})();
