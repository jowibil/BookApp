package com.bookappp.app.ui.reader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bookappp.app.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUri

/**
 * Fragment wrapper around Readium's EpubNavigatorFragment so it can be embedded in
 * Compose (AGENTS.md: embed the View-based renderer, don't rewrite it in Compose).
 * Pattern follows Readium's own EpubReaderFragment example.
 *
 * After process death the FragmentManager restores this fragment before ReaderViewModel
 * has reopened the book, while Readium insists on a FragmentFactory before
 * `super.onCreate`. So when no session exists yet we install Readium's dummy factory and
 * swap in a real navigator as soon as [ReaderSession] publishes the reopened book.
 *
 * The fragment is created for one book (see [ARG_READER_BOOK_ID]) and refuses any session
 * for a different one: a navigator bound to the wrong publication is what turns chapter
 * jumps into blank pages.
 */
@OptIn(ExperimentalReadiumApi::class, FlowPreview::class)
class EpubReaderFragment : Fragment(), EpubNavigatorFragment.Listener {

    /** Null until the real navigator is attached (see class docs). */
    private var navigator: EpubNavigatorFragment? = null

    /** Re-injects the pull script after chapter/settings changes; canceled on destroy. */
    private var pullInstallJob: Job? = null

    /** Guards against a second script callback before the next chapter is on screen. */
    private var advancing = false

    private lateinit var readerRepository: ReaderRepository

    /** The book this host was created for; fixed for the fragment's whole lifetime. */
    private val bookId: Long by lazy { requireArguments().getLong(ARG_READER_BOOK_ID) }

    /** True when onCreate had to install Readium's stand-in navigator factory. */
    private var usingDummyNavigator = false

    private val settingsViewModel: ReaderSettingsViewModel by lazy {
        ViewModelProvider(requireActivity())[ReaderSettingsViewModel::class.java]
    }

    private val externalLinkLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* fire-and-forget */ }

    /**
     * Callbacks for the injected rubber-band script. The WebView invokes these on its JS
     * bridge thread, so UI work is posted back to the main thread.
     */
    private val chapterPullBridge = object {
        @JavascriptInterface
        fun onPullArmed() {
            view?.post { view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        }

        @JavascriptInterface
        fun onPullAdvance() {
            view?.post { advanceToNextChapter() }
        }

        @JavascriptInterface
        fun onPullPrevious() {
            view?.post { retreatToPreviousChapter() }
        }
    }

    /**
     * Adds our own action to the WebView's text selection menu. Readium 3 has no
     * selection-change listener, so this menu is how a highlight starts.
     */
    private val selectionActionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.add(Menu.NONE, MENU_ITEM_HIGHLIGHT, Menu.NONE, R.string.reader_highlight_add)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId != MENU_ITEM_HIGHLIGHT) return false
            val selectable = navigator as? SelectableNavigator
            if (selectable == null) {
                mode.finish()
                return true
            }
            // Reading the selection is a suspend call into the WebView, and it must
            // happen before the action mode tears the selection down.
            viewLifecycleOwner.lifecycleScope.launch {
                val selection = runCatching { selectable.currentSelection() }.getOrNull()
                if (selection == null) {
                    // Do not leave the selection menu open after a no-op: that looked
                    // like the Highlight action was broken.
                    mode.finish()
                    ReaderMessages.show(R.string.reader_highlight_selection_failed)
                    return@launch
                }
                ReaderHighlightBridge.publishSelection(
                    locator = selection.locator,
                    text = selection.locator.text.highlight.orEmpty(),
                )
                selectable.clearSelection()
                mode.finish()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) = Unit
    }

    /** Tapping a highlight in the page opens its note editor on the Compose side. */
    private val decorationListener = object : DecorableNavigator.Listener {
        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
            val highlightId = event.decoration.id.toLongOrNull() ?: return false
            ReaderHighlightBridge.activate(highlightId)
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        readerRepository = readerRepositoryFor(requireContext().applicationContext)
        // Only a session for this book may be rendered here; a session for another book
        // means the navigator is about to be recreated, not reused.
        val session = ReaderSession.current.value?.takeIf { it.bookId == bookId }
        val publication = session?.let { readerRepository.publicationFor(it.bookId) }

        // IMPORTANT: fragmentFactory must be set before super.onCreate (Readium guide).
        if (session != null && publication != null) {
            childFragmentManager.fragmentFactory = createNavigatorFactory(publication, session)
        } else {
            // Restored before the book was reopened: keep restored child fragments
            // constructible, they are replaced by the real navigator below.
            usingDummyNavigator = true
            childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_reader, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // On a normal open the session already exists; after process death it arrives
        // once the publication is reopened, so wait for it rather than crashing.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ReaderSession.current.collect { session ->
                    if (session != null && session.bookId == bookId && navigator == null) {
                        attachNavigator(session)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        navigator?.let { (it as? DecorableNavigator)?.removeDecorationListener(decorationListener) }
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        // The WebView can be recreated while the reader is in the background.
        navigator?.let { schedulePullInstall(it) }
    }

    override fun onStop() {
        // Debounced saves can still be pending when the app goes to the background;
        // write the latest locator once more before the STARTED collector is cancelled.
        val locator = navigator?.currentLocator?.value
        super.onStop()
        if (locator != null) {
            lifecycleScope.launch { readerRepository.saveProgress(bookId, locator) }
        }
    }

    override fun onDestroy() {
        pullInstallJob?.cancel()
        pullInstallJob = null
        // ViewModel-driven navigation must not act on a dead navigator.
        navigator?.let { ReaderNavigatorHolder.detach(it) }
        navigator = null
        super.onDestroy()
    }

    /** Creates (or adopts) the real navigator; safe to call once per fragment instance. */
    private fun attachNavigator(session: ReaderSession.Session) {
        if (session.bookId != bookId) return
        val publication = readerRepository.publicationFor(session.bookId)
        if (publication == null) {
            // Opened but no longer in memory: say so instead of leaving a blank viewport.
            ReaderMessages.show(R.string.reader_error_open_failed)
            return
        }
        childFragmentManager.fragmentFactory = createNavigatorFactory(publication, session)

        // A navigator restored with the dummy publication cannot render this book.
        if (usingDummyNavigator) {
            childFragmentManager.findFragmentByTag(NAVIGATOR_TAG)?.let { restored ->
                childFragmentManager.beginTransaction().remove(restored).commitNow()
            }
        }

        val attached = childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? EpubNavigatorFragment
            ?: run {
                // Plain FragmentTransaction API: the ktx commit/add extensions conflict
                // with Navigation's own overloads in this module's classpath.
                childFragmentManager.beginTransaction()
                    .add(R.id.reader_navigator_container, EpubNavigatorFragment::class.java, null, NAVIGATOR_TAG)
                    .commitNow()
                childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as EpubNavigatorFragment
            }

        navigator = attached
        // Publish for ViewModels (bookmarks/TOC/search/highlights) that navigate remotely.
        ReaderNavigatorHolder.attach(bookId, attached)
        startObservers(attached)
    }

    private fun createNavigatorFactory(
        publication: Publication,
        session: ReaderSession.Session,
    ): FragmentFactory = EpubNavigatorFactory(publication).createFragmentFactory(
        initialLocator = session.initialLocator,
        initialPreferences = ReaderPreferencesMapper.toEpubPreferences(
            settingsViewModel.settings.value,
        ),
        listener = this,
        configuration = EpubNavigatorFragment.Configuration {
            selectionActionModeCallback = this@EpubReaderFragment.selectionActionModeCallback
            // The reader scrolls (see ReaderPreferencesMapper.scroll), so taps at the
            // page edges must not page-jump under the user's finger.
            disablePageTurnsWhileScrolling = true
            // Lets the page report a deliberate pull past its last line. Android's
            // WebView has no native rubber band, so the script also draws it.
            registerJavascriptInterface(JAVASCRIPT_INTERFACE) { _ -> chapterPullBridge }
        },
    )

    private fun startObservers(attached: EpubNavigatorFragment) {
        // Persist reading position while STARTED, coalescing the many locator
        // emissions produced by scrolling. onStop writes the final value (AGENTS.md).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                attached.currentLocator
                    .debounce(PROGRESS_SAVE_DEBOUNCE_MS)
                    .onEach { locator -> readerRepository.saveProgress(bookId, locator) }
                    .launchIn(this)
            }
        }

        // A new spine document means a new WebView: inject the pull gesture there and
        // clear the advance guard.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                attached.currentLocator
                    // Resource path only: a fragment change within the same chapter is
                    // not a new WebView and must not re-arm the advance guard.
                    .map { it.href.toString().substringBefore('#') }
                    .distinctUntilChanged()
                    .onEach {
                        advancing = false
                        schedulePullInstall(attached)
                    }
                    .launchIn(this)
            }
        }

        val decorableNavigator = attached as? DecorableNavigator
        decorableNavigator?.addDecorationListener(HIGHLIGHTS_GROUP, decorationListener)

        // Draw saved highlights; the ViewModel owns the data, we only map it to
        // Readium decorations here so the rest of the app stays Readium-free.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ReaderHighlightBridge.decorations
                    .onEach { highlights ->
                        decorableNavigator?.applyDecorations(
                            decorations = highlights.map { highlight ->
                                Decoration(
                                    id = highlight.id.toString(),
                                    locator = highlight.locator,
                                    style = Decoration.Style.Highlight(tint = highlight.color.argb),
                                )
                            },
                            group = HIGHLIGHTS_GROUP,
                        )
                    }
                    .launchIn(this)
            }
        }

        // Live updates: every DataStore change is re-submitted to the navigator.
        settingsViewModel.settings
            .onEach { settings ->
                attached.submitPreferences(ReaderPreferencesMapper.toEpubPreferences(settings))
                // Preference changes can rebuild the page document; let that settle
                // before re-injecting the script.
                schedulePullInstall(attached, debounceMs = PULL_SETTINGS_DEBOUNCE_MS)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    /**
     * Pull-to-move, Kotlin side: resolves the neighbouring spine resource and opens it.
     * The gesture deliberately lives past the first/last line, so ordinary reading never
     * turns the page by accident.
     */
    private fun advanceToNextChapter() = moveToChapter { attached ->
        readerRepository.nextReadingOrderLocator(bookId, attached.currentLocator.value)
    }

    private fun retreatToPreviousChapter() = moveToChapter { attached ->
        readerRepository.previousReadingOrderLocator(bookId, attached.currentLocator.value)
    }

    private fun moveToChapter(resolve: suspend (EpubNavigatorFragment) -> Locator?) {
        val attached = navigator ?: return
        if (advancing || !settingsViewModel.settings.value.autoAdvanceChapters) return
        // The JS bridge posts to the old view; it may already be gone.
        if (view == null || !isAdded) return
        advancing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val target = resolve(attached)
            if (target == null) {
                advancing = false
                return@launch
            }
            val moved = runCatching { attached.go(target, animated = false) }
                .getOrDefault(false)
            if (!moved) {
                ReaderMessages.show(R.string.reader_error_jump_failed)
                advancing = false
                return@launch
            }
            // The href observer clears this as soon as the new chapter is attached;
            // the delay covers a navigation Readium accepts without switching pages.
            delay(ADVANCE_GUARD_MS)
            advancing = false
        }
    }

    /**
     * Injects or updates the pull script in the visible chapter. Calls are retried
     * because the WebView is still loading right after a chapter change.
     */
    private fun schedulePullInstall(
        attached: EpubNavigatorFragment,
        debounceMs: Long = 0L,
    ) {
        pullInstallJob?.cancel()
        pullInstallJob = viewLifecycleOwner.lifecycleScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val currentHref = attached.currentLocator.value.href.toString()
            val script = buildPullScript(
                enabled = settingsViewModel.settings.value.autoAdvanceChapters,
                hasNext = readerRepository.hasNextReadingOrderResource(bookId, currentHref),
                hasPrev = readerRepository.hasPreviousReadingOrderResource(bookId, currentHref),
            )
            repeat(PULL_INSTALL_ATTEMPTS) {
                val result = runCatching { attached.evaluateJavascript(script) }.getOrNull()
                if (result?.contains("ok") == true || result?.contains("installed") == true) {
                    return@launch
                }
                delay(PULL_INSTALL_RETRY_MS)
            }
        }
    }

    /**
     * The injected rubber band at either end of a chapter. A deliberate upward pull at
     * the bottom arms "next", a downward pull at the top arms "previous"; a short drag
     * only springs back. That threshold keeps ordinary scrolling from skipping a chapter.
     */
    private fun buildPullScript(enabled: Boolean, hasNext: Boolean, hasPrev: Boolean): String {
        val nextHint = JSONObject.quote(getString(R.string.reader_pull_next_hint))
        val nextRelease = JSONObject.quote(getString(R.string.reader_pull_next_release))
        val prevHint = JSONObject.quote(getString(R.string.reader_pull_prev_hint))
        val prevRelease = JSONObject.quote(getString(R.string.reader_pull_prev_release))
        return """
            (function() {
              var enabled = $enabled;
              var hasNext = $hasNext;
              var hasPrev = $hasPrev;
              if (!document.body) return 'retry';
              if (window.__bookapppPullInstalled) {
                window.__bookapppAutoAdvance = enabled;
                window.__bookapppHasNext = hasNext;
                window.__bookapppHasPrev = hasPrev;
                return 'ok';
              }
              window.__bookapppPullInstalled = true;
              window.__bookapppAutoAdvance = enabled;
              window.__bookapppHasNext = hasNext;
              window.__bookapppHasPrev = hasPrev;

              var NEXT_HINT = $nextHint;
              var NEXT_RELEASE = $nextRelease;
              var PREV_HINT = $prevHint;
              var PREV_RELEASE = $prevRelease;
              var ARM_AT = 118;
              var MAX_STRETCH = 175;
              var BODY_RATIO = 0.42;
              var body = document.body;
              var originalTransition = body.style.transition || '';
              var originalTransform = body.style.transform || '';
              var springTimer = 0;

              var indicator = document.getElementById('bookappp-pull-indicator');
              if (!indicator) {
                indicator = document.createElement('div');
                indicator.id = 'bookappp-pull-indicator';
                indicator.setAttribute('aria-hidden', 'true');
                indicator.style.cssText = 'position:fixed;left:0;right:0;bottom:0;display:flex;justify-content:center;align-items:flex-end;padding:0 20px 18px 20px;pointer-events:none;z-index:2147483647;opacity:0;transform:translateY(10px);transition:opacity .16s ease,transform .16s ease;';
                var pill = document.createElement('div');
                pill.style.cssText = 'display:flex;align-items:center;gap:8px;max-width:100%;padding:9px 15px;border-radius:999px;background:rgba(255,255,255,.92);color:#17171a;font:600 12.5px/1.2 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;box-shadow:0 8px 26px rgba(0,0,0,.22);border:1px solid rgba(0,0,0,.06);';
                var chevron = document.createElement('span');
                chevron.id = 'bookappp-pull-chevron';
                chevron.textContent = '↑';
                chevron.style.cssText = 'display:inline-block;font-size:14px;';
                var label = document.createElement('span');
                label.id = 'bookappp-pull-label';
                label.textContent = NEXT_HINT;
                pill.appendChild(chevron);
                pill.appendChild(label);
                indicator.appendChild(pill);
                document.documentElement.appendChild(indicator);
              }
              var label = document.getElementById('bookappp-pull-label');
              var chevron = document.getElementById('bookappp-pull-chevron');

              var tracking = false;
              var startY = 0;
              var pull = 0;
              var armed = false;
              var cooldownUntil = 0;
              var direction = '';
              var lastDirection = 'next';
              var allowNextOnStart = false;
              var allowPrevOnStart = false;

              function atEnd() {
                var scrolling = document.scrollingElement || document.documentElement;
                var y = window.pageYOffset || scrolling.scrollTop || body.scrollTop || 0;
                var height = Math.max(scrolling.scrollHeight, body.scrollHeight);
                return y + window.innerHeight >= height - 4;
              }

              function atTop() {
                var scrolling = document.scrollingElement || document.documentElement;
                var y = window.pageYOffset || scrolling.scrollTop || body.scrollTop || 0;
                return y <= 4;
              }

              function placeIndicator(dir) {
                if (dir === 'prev') {
                  indicator.style.top = '0';
                  indicator.style.bottom = 'auto';
                  indicator.style.alignItems = 'flex-start';
                  indicator.style.padding = '18px 20px 0 20px';
                  indicator.style.transform = 'translateY(-10px)';
                } else {
                  indicator.style.top = 'auto';
                  indicator.style.bottom = '0';
                  indicator.style.alignItems = 'flex-end';
                  indicator.style.padding = '0 20px 18px 20px';
                  indicator.style.transform = 'translateY(10px)';
                }
              }

              function updateIndicator(stretch) {
                var progress = Math.min(1, stretch / ARM_AT);
                indicator.style.opacity = String(0.2 + 0.8 * progress);
                var startOffset = direction === 'prev' ? -10 : 10;
                indicator.style.transform = 'translateY(' + ((1 - progress) * startOffset).toFixed(1) + 'px) scale(' + (0.94 + 0.06 * progress).toFixed(3) + ')';
                var hint = direction === 'prev' ? PREV_HINT : NEXT_HINT;
                var release = direction === 'prev' ? PREV_RELEASE : NEXT_RELEASE;
                label.textContent = armed ? release : hint;
                chevron.textContent = direction === 'prev' ? '↓' : '↑';
                chevron.style.transform = armed ? 'scale(1.2)' : 'scale(1)';
              }

              function springBack() {
                window.clearTimeout(springTimer);
                body.style.transition = 'transform .30s cubic-bezier(.22,1.2,.36,1)';
                indicator.style.transition = 'opacity .18s ease, transform .18s ease';
                body.style.transform = originalTransform;
                indicator.style.opacity = '0';
                indicator.style.transform = 'translateY(' + (lastDirection === 'prev' ? '-10' : '10') + 'px)';
                springTimer = window.setTimeout(function() {
                  body.style.transition = originalTransition;
                  body.style.transform = originalTransform;
                }, 320);
              }

              function resetPull() {
                tracking = false;
                armed = false;
                pull = 0;
                direction = '';
                allowNextOnStart = false;
                allowPrevOnStart = false;
              }

              function cancelPull() {
                if (!tracking) return;
                resetPull();
                springBack();
              }

              function onStart(event) {
                if (!window.__bookapppAutoAdvance || event.touches.length !== 1) return;
                var canNext = window.__bookapppHasNext && atEnd();
                var canPrev = window.__bookapppHasPrev && atTop();
                if (!canNext && !canPrev) return;
                var target = event.target;
                if (target && target.closest && target.closest('a,button,input,textarea,select,label,[contenteditable="true"]')) return;
                try {
                  var selection = window.getSelection();
                  if (selection && String(selection).length > 0) return;
                } catch (error) {}
                window.clearTimeout(springTimer);
                tracking = true;
                startY = event.touches[0].clientY;
                pull = 0;
                armed = false;
                direction = '';
                allowNextOnStart = canNext;
                allowPrevOnStart = canPrev;
              }

              function onMove(event) {
                if (!tracking) return;
                if (event.touches.length !== 1) {
                  cancelPull();
                  return;
                }
                var dy = startY - event.touches[0].clientY;

                // The first few pixels choose the direction, so a horizontal swipe or
                // a tiny wobble never turns into a chapter change.
                if (!direction) {
                  if (dy > 6 && allowNextOnStart) {
                    direction = 'next';
                    lastDirection = 'next';
                    placeIndicator('next');
                  } else if (dy < -6 && allowPrevOnStart) {
                    direction = 'prev';
                    lastDirection = 'prev';
                    placeIndicator('prev');
                  } else {
                    return;
                  }
                }

                if (direction === 'next' && !atEnd()) {
                  cancelPull();
                  return;
                }
                if (direction === 'prev' && !atTop()) {
                  cancelPull();
                  return;
                }

                pull = direction === 'next' ? dy : -dy;
                if (pull <= 0) {
                  cancelPull();
                  return;
                }
                if (event.cancelable) event.preventDefault();
                var stretch = MAX_STRETCH * (1 - Math.exp(-pull / MAX_STRETCH));
                var nextArmed = stretch >= ARM_AT;
                if (nextArmed !== armed) {
                  armed = nextArmed;
                  if (armed && window.BookApppReader && window.BookApppReader.onPullArmed) {
                    try { window.BookApppReader.onPullArmed(); } catch (error) {}
                  }
                }
                if (stretch > 4) {
                  indicator.style.transition = 'none';
                  updateIndicator(stretch);
                }
                body.style.transition = 'none';
                var offset = stretch * BODY_RATIO;
                body.style.transform = 'translateY(' + (direction === 'prev' ? offset : -offset).toFixed(1) + 'px)';
              }

              function onEnd() {
                if (!tracking) return;
                var move = armed ? direction : '';
                resetPull();
                springBack();
                if (!move || Date.now() < cooldownUntil) return;
                cooldownUntil = Date.now() + 900;
                try {
                  if (move === 'prev') {
                    if (window.BookApppReader && window.BookApppReader.onPullPrevious) {
                      window.BookApppReader.onPullPrevious();
                    }
                  } else if (window.BookApppReader && window.BookApppReader.onPullAdvance) {
                    window.BookApppReader.onPullAdvance();
                  }
                } catch (error) {}
              }

              document.addEventListener('touchstart', onStart, { passive: true, capture: true });
              document.addEventListener('touchmove', onMove, { passive: false, capture: true });
              document.addEventListener('touchend', onEnd, { passive: true, capture: true });
              document.addEventListener('touchcancel', cancelPull, { passive: true, capture: true });
              return 'installed';
            })();
        """.trimIndent()
    }

    /** External links open in a browser app, per the 3.0 migration guide. */
    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        val uri: Uri = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        runCatching { externalLinkLauncher.launch(intent) }
    }

    private companion object {
        const val NAVIGATOR_TAG = "epub_navigator"
        const val HIGHLIGHTS_GROUP = "highlights"
        const val MENU_ITEM_HIGHLIGHT = 1
        const val JAVASCRIPT_INTERFACE = "BookApppReader"

        const val PULL_INSTALL_ATTEMPTS = 20
        const val PULL_INSTALL_RETRY_MS = 250L
        const val PULL_SETTINGS_DEBOUNCE_MS = 350L
        const val ADVANCE_GUARD_MS = 900L
        const val PROGRESS_SAVE_DEBOUNCE_MS = 500L
    }
}
