package com.bookappp.app.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bookappp.app.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.adapter.pdfium.navigator.PdfiumSettings
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

/**
 * Hosts Readium's PdfNavigatorFragment (Pdfium engine) for PDF books, embedded in
 * Compose through a FragmentContainerView like [EpubReaderFragment].
 *
 * The restore rules are the same: after process death the FragmentManager recreates this
 * fragment before the book is reopened, so when no session exists yet we install Readium's
 * dummy factory and swap in the real navigator once [ReaderSession] publishes the book.
 */
@OptIn(ExperimentalReadiumApi::class, FlowPreview::class)
class PdfReaderFragment : Fragment(), PdfNavigatorFragment.Listener {

    /** Null until the real navigator is attached (see class docs). */
    private var navigator: Navigator? = null

    /** Typed handle for live PDF preference updates. */
    private var pdfConfigurable: Configurable<PdfiumSettings, PdfiumPreferences>? = null

    private lateinit var readerRepository: ReaderRepository

    private val settingsViewModel: ReaderSettingsViewModel by lazy {
        ViewModelProvider(requireActivity())[ReaderSettingsViewModel::class.java]
    }

    /** The book this host was created for; fixed for the fragment's whole lifetime. */
    private val bookId: Long by lazy { requireArguments().getLong(ARG_READER_BOOK_ID) }

    /** True when onCreate had to install Readium's stand-in navigator factory. */
    private var usingDummyNavigator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        readerRepository = readerRepositoryFor(requireContext().applicationContext)
        // Only a session for this book may be rendered here; see EpubReaderFragment.
        val session = ReaderSession.current.value?.takeIf { it.bookId == bookId }
        val publication = session?.let { readerRepository.publicationFor(it.bookId) }

        // IMPORTANT: fragmentFactory must be set before super.onCreate (Readium guide).
        if (session != null && publication != null) {
            childFragmentManager.fragmentFactory = createNavigatorFactory(publication, session)
        } else {
            usingDummyNavigator = true
            childFragmentManager.fragmentFactory =
                PdfNavigatorFragment.createDummyFactory(PdfiumEngineProvider())
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
        // Wait for the reopened book after process death instead of crashing.
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

    override fun onStop() {
        // Flush a debounced position when the reader leaves the foreground.
        val locator = navigator?.currentLocator?.value
        super.onStop()
        if (locator != null) {
            lifecycleScope.launch { readerRepository.saveProgress(bookId, locator) }
        }
    }

    override fun onDestroy() {
        // ViewModel-driven navigation must not act on a dead navigator.
        navigator?.let { ReaderNavigatorHolder.detach(it) }
        pdfConfigurable = null
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

        val attached = childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? Navigator
            ?: run {
                // Plain FragmentTransaction API: the ktx commit/add extensions conflict
                // with Navigation's own overloads in this module's classpath.
                childFragmentManager.beginTransaction()
                    .add(R.id.reader_navigator_container, PdfNavigatorFragment::class.java, null, NAVIGATOR_TAG)
                    .commitNow()
                childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as Navigator
            }

        navigator = attached
        @Suppress("UNCHECKED_CAST")
        pdfConfigurable = attached as? Configurable<PdfiumSettings, PdfiumPreferences>
        // Publish for ViewModels (TOC/bookmarks/search) that navigate remotely.
        ReaderNavigatorHolder.attach(bookId, attached)

        // Live PDF layout/fit changes are submitted to the attached navigator while the
        // settings screen stays open over the page.
        settingsViewModel.settings
            .onEach { settings ->
                pdfConfigurable?.submitPreferences(ReaderPreferencesMapper.toPdfPreferences(settings))
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // Persist reading position while STARTED, coalescing locator changes during
        // scrolling. onStop writes the final value (AGENTS.md).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                attached.currentLocator
                    .debounce(PROGRESS_SAVE_DEBOUNCE_MS)
                    .onEach { locator -> readerRepository.saveProgress(bookId, locator) }
                    .launchIn(this)
            }
        }
    }

    private fun createNavigatorFactory(
        publication: Publication,
        session: ReaderSession.Session,
    ): FragmentFactory = PdfNavigatorFactory(publication, PdfiumEngineProvider())
        .createFragmentFactory(
            initialLocator = session.initialLocator,
            // The reader offers both layouts; the saved preference decides whether the
            // document opens as one continuous column or as page-snapping pagination.
            initialPreferences = ReaderPreferencesMapper.toPdfPreferences(
                settingsViewModel.settings.value,
            ),
            listener = this,
        )

    private companion object {
        const val NAVIGATOR_TAG = "pdf_navigator"
        const val PROGRESS_SAVE_DEBOUNCE_MS = 500L
    }
}
